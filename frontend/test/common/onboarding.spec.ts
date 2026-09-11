import { Config, driver, DriveStep, PopoverDOM } from 'driver.js';
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Component, createApp } from 'vue';
import auth from '../../src/common/auth';
import { startOnboarding, stopOnboarding, tourSteps } from '../../src/common/onboarding';
import i18n from '../../src/i18n';

vi.mock('../../src/common/auth', () => ({ default: Promise.resolve({ hasRole: vi.fn(() => true) }) }));
// one shared drive fn, so tests can assert the tour was actually started across driver() instances;
// destroy fires onDestroyed like the real driver does, so stopOnboarding() is testable
vi.mock('driver.js', () => {
  const drive = vi.fn();
  return {
    driver: vi.fn((config?: Config) => ({
      drive,
      destroy: vi.fn(() => (config?.onDestroyed as unknown as (() => void) | undefined)?.())
    }))
  };
});

const { t, te } = i18n.global;

describe('tourSteps', () => {
  it('includes the create vault step for users allowed to create vaults', () => {
    const steps = tourSteps(true);

    expect(steps.map(step => step.target)).toEqual([
      undefined,
      '[data-tour="vaultList"]',
      '[data-tour="addVault"]',
      '[data-tour="adminNav"]',
      '[data-tour="profile"]',
      undefined
    ]);
  });

  it('swaps in the use vault step for users without the create-vaults role', () => {
    const steps = tourSteps(false);

    expect(steps.map(step => step.target)).toEqual([
      undefined,
      '[data-tour="vaultList"]',
      '[data-tour="vaultList"]',
      '[data-tour="adminNav"]',
      '[data-tour="profile"]',
      undefined
    ]);
    expect(steps[2].titleKey).toBe('onboarding.useVault.title');
  });

  ([
    [true, 'onboarding.vaultList.description'],
    [false, 'onboarding.vaultList.description.shared']
  ] as const).forEach(([canCreateVaults, descriptionKey]) => {
    it(`describes the vault list for canCreateVaults: ${canCreateVaults}`, () => {
      expect(tourSteps(canCreateVaults)[1].descriptionKey).toBe(descriptionKey);
    });
  });

  [true, false].forEach(canCreateVaults => {
    it(`resolves every message key against en-US (canCreateVaults: ${canCreateVaults})`, () => {
      for (const step of tourSteps(canCreateVaults)) {
        expect(te(step.titleKey, 'en-US'), `missing key ${step.titleKey}`).toBe(true);
        if (step.descriptionKey) {
          expect(te(step.descriptionKey, 'en-US'), `missing key ${step.descriptionKey}`).toBe(true);
        }
      }
    });
  });

  it('renders a decorative vignette for the welcome, vault list, and profile steps', () => {
    const vignettes = renderVignettes();

    expect(vignettes.map(([titleKey]) => titleKey)).toEqual(['onboarding.welcome.title', 'onboarding.vaultList.title', 'onboarding.profile.title']);
    for (const [titleKey, html] of vignettes) {
      // anchored to the root element, so a nested icon's aria-hidden cannot satisfy it
      expect(html, `${titleKey} must be decorative`).toMatch(/^<div [^>]*aria-hidden="true"/);
    }
    const rendered = vignettes.map(([, html]) => html).join('\n');
    expect(rendered).toContain(t('onboarding.vaultList.example1'));
    expect(rendered).toContain(t('onboarding.vaultList.example2'));
  });

  it('anchors every step target in a component', () => {
    const componentsDir = join(dirname(fileURLToPath(import.meta.url)), '../../src/components');
    const sources = readdirSync(componentsDir, { recursive: true, encoding: 'utf8' })
      .filter(name => name.endsWith('.vue'))
      .map(name => readFileSync(join(componentsDir, name), 'utf8'))
      .join('\n');

    for (const target of tourSteps(true).map(step => step.target).filter(target => target !== undefined)) {
      const anchor = target.slice(1, -1);
      expect(sources, `missing anchor ${anchor}`).toContain(anchor);
    }
  });
});

describe('startOnboarding', () => {
  beforeEach(setUpTourDom);
  afterEach(tearDownTourDom);

  it('drives all steps and mounts the vignettes with the description text', async () => {
    const steps = await drive(() => startOnboarding('user-1'));

    expect(steps.map(step => step.popover?.title)).toEqual([
      t('onboarding.welcome.title'),
      t('onboarding.vaultList.title'),
      t('onboarding.addVault.title'),
      t('onboarding.adminNav.title'),
      t('onboarding.profile.title'),
      t('onboarding.getApp.title')
    ]);
    expect(steps.every(step => step.popover?.description)).toBe(true);
    expect(steps[1].element).toBeInstanceOf(HTMLElement);
    const vaultListStep = renderStep(1);
    // the illustration is decorative, while the app step's list carries real instructions
    expect(vaultListStep.firstElementChild!.getAttribute('aria-hidden')).toBe('true');
    expect(vaultListStep.innerHTML).toContain(`<p>${t('onboarding.vaultList.description')}</p>`);
    const appStep = renderStep(steps.length - 1);
    expect(appStep.firstElementChild!.getAttribute('aria-hidden')).toBeNull();
    expect(appStep.innerHTML).toContain('<ol');
    expect(appStep.innerHTML).toContain(t('onboarding.getApp.step1'));
    expect(appStep.innerHTML).toContain('href="https://cryptomator.org/downloads/');
    expect(appStep.innerHTML).toContain('src="/download-qr.svg"');
    const { hasRole } = await auth;
    expect(vi.mocked(hasRole)).toHaveBeenCalledWith('create-vaults');
    expect(driveMock()).toHaveBeenCalledOnce();
  });

  it('localizes the close button provided by driver.js', async () => {
    await drive(() => startOnboarding('user-1'));

    expect(renderPopover(0).closeButton.getAttribute('aria-label')).toBe(t('common.close'));
  });

  it('skips a step whose target element is not visible', async () => {
    document.querySelector('[data-tour="adminNav"]')!.remove();

    const steps = await drive(() => startOnboarding('user-1'));

    expect(steps.map(step => step.popover?.title)).toEqual([
      t('onboarding.welcome.title'),
      t('onboarding.vaultList.title'),
      t('onboarding.addVault.title'),
      t('onboarding.profile.title'),
      t('onboarding.getApp.title')
    ]);
    expect(renderStep(3).innerHTML).toContain(t('onboarding.profile.description'));
  });

  it('leaves no unresolved message key in the rendered tour', async () => {
    const steps = await drive(() => startOnboarding('user-1'));

    const config = lastConfig();
    const rendered = [
      ...steps.map((step, index) => `${step.popover?.title}\n${renderStep(index).innerHTML}`),
      config.progressText, config.nextBtnText, config.prevBtnText, config.doneBtnText
    ].join('\n');
    // vue-i18n echoes unresolved keys verbatim, so a dotted key in the output means a missing message
    expect(rendered).not.toMatch(/(?:onboarding|common)\.[a-z]/i);
  });

  it('renders a text-only step without a vignette', async () => {
    await drive(() => startOnboarding('user-1'));

    const addVaultStep = renderStep(2).innerHTML;
    expect(addVaultStep).toContain(`<p>${t('onboarding.addVault.description')}</p>`);
    expect(addVaultStep).not.toContain('<div');
  });

  it('leaves the popover empty for an unknown or missing step index', async () => {
    const steps = await drive(() => startOnboarding('user-1'));

    const first = renderStep(0);
    expect(first.innerHTML).not.toBe('');
    expect(renderStep(steps.length).innerHTML).toBe('');
    // a content-less render must still have unmounted the previous step's app
    expect(first.innerHTML).toBe('');
    expect(renderStep().innerHTML).toBe('');
  });

  it('unmounts the previous step content on each render and on destroy', async () => {
    const dispose = vi.spyOn(i18n, 'dispose');
    await drive(() => startOnboarding('user-1'));

    const first = renderStep(0);
    expect(first.innerHTML).toContain('/logo.svg');
    const second = renderStep(1);
    expect(first.innerHTML).toBe('');
    expect(second.innerHTML).toContain(t('onboarding.vaultList.example1'));
    invokeDestroyed(lastConfig());
    expect(second.innerHTML).toBe('');
    // the popover apps share the module-wide i18n instance, which must survive their teardown
    expect(dispose).not.toHaveBeenCalled();
  });

  it('marks the tour as completed once driver reports it destroyed', async () => {
    await drive(() => startOnboarding('user-1'));

    expect(localStorage.getItem('hub.onboardingCompleted.user-1')).toBeNull();
    invokeDestroyed(lastConfig());
    expect(localStorage.getItem('hub.onboardingCompleted.user-1')).not.toBeNull();
  });

  it('stops a running tour without marking it as completed', async () => {
    await drive(() => startOnboarding('user-1'));

    stopOnboarding();

    expect(localStorage.getItem('hub.onboardingCompleted.user-1')).toBeNull();
  });

  it('omits the QR code on mobile devices', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)');

    const steps = await drive(() => startOnboarding('user-1'));

    const appStep = renderStep(steps.length - 1).innerHTML;
    expect(appStep).not.toContain('download-qr.svg');
    expect(appStep).toContain('apps.apple.com');
  });
});

/* ---------- MOCKS ---------- */

function setUpTourDom() {
  vi.clearAllMocks();
  localStorage.clear();
  document.body.innerHTML = ['vaultList', 'addVault', 'adminNav', 'profile'].map(anchor => `<div data-tour="${anchor}"></div>`).join('');
}

function tearDownTourDom() {
  vi.restoreAllMocks();
  document.body.innerHTML = '';
}

async function drive(run: () => Promise<void>): Promise<DriveStep[]> {
  vi.useFakeTimers();
  try {
    const pending = run();
    await vi.runAllTimersAsync();
    await pending;
  } finally {
    vi.useRealTimers();
  }
  return lastConfig().steps!;
}

function lastConfig(): Config {
  return vi.mocked(driver).mock.calls.at(-1)![0]!;
}

function driveMock() {
  return vi.mocked(driver).mock.results.at(-1)!.value.drive;
}

function renderPopover(index?: number): { description: HTMLElement, closeButton: HTMLElement } {
  const config = lastConfig();
  const popover = { description: document.createElement('div'), closeButton: document.createElement('button') };
  type RenderHook = NonNullable<Config['onPopoverRender']>;
  const state = index !== undefined ? { activeIndex: index } : {};
  (config.onPopoverRender as RenderHook)(popover as unknown as PopoverDOM, { config, state, driver: undefined as never, index: index ?? 0 } as Parameters<RenderHook>[1]);
  return popover;
}

function renderStep(index?: number): HTMLElement {
  return renderPopover(index).description;
}

function renderComponent(component: Component): string {
  const host = document.createElement('div');
  const app = createApp(component);
  app.mount(host);
  const html = host.innerHTML;
  app.unmount();
  return html;
}

function renderVignettes(): [string, string][] {
  return tourSteps(true)
    .filter(step => step.vignette && step.descriptionKey)
    .map(step => [step.titleKey, renderComponent(step.vignette!)]);
}

function invokeDestroyed(config: Config) {
  type DestroyHook = NonNullable<Config['onDestroyed']>;
  (config.onDestroyed as DestroyHook | undefined)?.(undefined, {} as DriveStep, { config, state: {}, index: 0, driver: undefined as never } as Parameters<DestroyHook>[2]);
}

import { Config, driver, DriveStep, PopoverDOM } from 'driver.js';
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Component, createApp } from 'vue';
import auth from '../../src/common/auth';
import backend, { UserDto } from '../../src/common/backend';
import { startOnboarding, startOnboardingIfNeeded, stopOnboarding, tourSteps } from '../../src/common/onboarding';
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

  it('swaps in a floating use vault step for users without the create-vaults role', () => {
    const steps = tourSteps(false);

    expect(steps.map(step => step.target)).toEqual([
      undefined,
      '[data-tour="vaultList"]',
      undefined,
      '[data-tour="adminNav"]',
      '[data-tour="profile"]',
      undefined
    ]);
    expect(steps[2].titleKey).toBe('onboarding.useVault.title');
  });

  ([
    [true, 'onboarding.vaultList.owner.description'],
    [false, 'onboarding.vaultList.member.description']
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
    expect(rendered).toContain(t('onboarding.vaultList.example.finance'));
    expect(rendered).toContain(t('onboarding.vaultList.example.marketing'));
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
    const steps = await drive(() => startOnboarding(me));

    expect(steps.map((step, index) => renderPopover(index).title.textContent)).toEqual([
      t('onboarding.welcome.title'),
      t('onboarding.vaultList.title'),
      t('onboarding.addVault.title'),
      t('onboarding.adminNav.title'),
      t('onboarding.profile.title'),
      t('onboarding.getApp.title')
    ]);
    // blank placeholders keep driver.js from hiding the elements the render hook fills
    expect(steps.every(step => step.popover?.title && step.popover?.description)).toBe(true);
    expect(steps[1].element).toBeInstanceOf(HTMLElement);
    const vaultListStep = renderPopover(1);
    // the illustration is decorative, while the app step's list carries real instructions
    expect(vaultListStep.description.firstElementChild!.getAttribute('aria-hidden')).toBe('true');
    expect(vaultListStep.description.innerHTML).toContain(`<p>${t('onboarding.vaultList.owner.description')}</p>`);
    const appStep = renderPopover(steps.length - 1);
    expect(appStep.description.firstElementChild!.getAttribute('aria-hidden')).toBeNull();
    expect(appStep.description.innerHTML).toContain('<ol');
    expect(appStep.description.innerHTML).toContain(t('onboarding.getApp.step1'));
    expect(appStep.description.innerHTML).toContain('href="https://cryptomator.org/downloads/');
    expect(appStep.description.innerHTML).toContain('src="/download-qr.svg"');
    expect(appStep.nextButton.textContent).toBe(t('onboarding.done'));
    const { hasRole } = await auth;
    expect(vi.mocked(hasRole)).toHaveBeenCalledWith('create-vaults');
    expect(driveMock()).toHaveBeenCalledOnce();
  });

  it('localizes the popover chrome provided by driver.js', async () => {
    await drive(() => startOnboarding(me));

    const popover = renderPopover(0);
    expect(popover.progress.textContent).toBe(t('onboarding.progress', [1, 6]));
    expect(popover.previousButton.textContent).toBe(t('common.previous'));
    expect(popover.nextButton.textContent).toBe(t('common.next'));
    expect(popover.closeButton.getAttribute('aria-label')).toBe(t('common.close'));
  });

  it('skips a step whose target element is not visible', async () => {
    document.querySelector('[data-tour="adminNav"]')!.remove();

    const steps = await drive(() => startOnboarding(me));

    expect(steps.map((step, index) => renderPopover(index).title.textContent)).toEqual([
      t('onboarding.welcome.title'),
      t('onboarding.vaultList.title'),
      t('onboarding.addVault.title'),
      t('onboarding.profile.title'),
      t('onboarding.getApp.title')
    ]);
    expect(renderStep(3).innerHTML).toContain(t('onboarding.profile.description'));
  });

  it('keeps the account step as a floating card when its anchor is hidden', async () => {
    document.querySelector('[data-tour="profile"]')!.remove();

    const steps = await drive(() => startOnboarding(me));

    expect(renderPopover(4).title.textContent).toBe(t('onboarding.profile.title'));
    expect(steps[4].element).toBeUndefined();
  });

  it('skips a step whose target element is disabled', async () => {
    document.querySelector('[data-tour="addVault"]')!.replaceWith(createDisabledButton());

    const steps = await drive(() => startOnboarding(me));

    expect(steps.map((step, index) => renderPopover(index).title.textContent)).not.toContain(t('onboarding.addVault.title'));
    expect(steps).toHaveLength(5);
  });

  it('leaves no unresolved message key in the rendered tour', async () => {
    const steps = await drive(() => startOnboarding(me));

    const rendered = steps.map((step, index) => {
      const popover = renderPopover(index);
      return [popover.title.textContent, popover.progress.textContent, popover.previousButton.textContent, popover.nextButton.textContent, popover.description.innerHTML].join('\n');
    }).join('\n');
    // vue-i18n echoes unresolved keys verbatim, so a dotted key in the output means a missing message
    expect(rendered).not.toMatch(/(?:onboarding|common)\.[a-z]/i);
  });

  it('renders a text-only step without a vignette', async () => {
    await drive(() => startOnboarding(me));

    const addVaultStep = renderStep(2).innerHTML;
    expect(addVaultStep).toContain(`<p>${t('onboarding.addVault.description')}</p>`);
    expect(addVaultStep).not.toContain('<div');
  });

  it('leaves the popover empty for an unknown or missing step index', async () => {
    const steps = await drive(() => startOnboarding(me));

    const first = renderStep(0);
    expect(first.innerHTML).not.toBe('');
    expect(renderStep(steps.length).innerHTML).toBe('');
    // a content-less render must still have unmounted the previous step's app
    expect(first.innerHTML).toBe('');
    expect(renderStep().innerHTML).toBe('');
  });

  it('unmounts the previous step content on each render and on destroy', async () => {
    const dispose = vi.spyOn(i18n, 'dispose');
    await drive(() => startOnboarding(me));

    const first = renderStep(0);
    expect(first.innerHTML).toContain('/logo.svg');
    const second = renderStep(1);
    expect(first.innerHTML).toBe('');
    expect(second.innerHTML).toContain(t('onboarding.vaultList.example.finance'));
    invokeDestroyed(lastConfig());
    expect(second.innerHTML).toBe('');
    // the popover apps share the module-wide i18n instance, which must survive their teardown
    expect(dispose).not.toHaveBeenCalled();
  });

  it('marks the tour as completed once driver reports it destroyed', async () => {
    await drive(() => startOnboarding(me));

    expect(me.onboardingCompleted).toBe(false);
    invokeDestroyed(lastConfig());
    expect(me.onboardingCompleted).toBe(true);
    await vi.waitFor(() => expect(persistedDto?.onboardingCompleted).toBe(true));
    expect(persistedDto?.setupCode).toBe('freshly-fetched');
    // withDevices, so the PUT round-trips the devices unchanged
    expect(backend.users.me).toHaveBeenCalledWith(true, false);
  });

  it('keeps the tour dismissed for the session even when persisting fails', async () => {
    vi.mocked(backend.users.putMe).mockRejectedValue(new Error('backend unavailable'));
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    await drive(() => startOnboarding(me));

    invokeDestroyed(lastConfig());

    await vi.waitFor(() => expect(consoleError).toHaveBeenCalledWith('Persisting the onboarding completion failed:', expect.any(Error)));
    expect(backend.users.putMe).toHaveBeenCalled();
    // asserted after the rejection was handled, so a catch block reverting the flag would fail here
    expect(me.onboardingCompleted).toBe(true);
  });

  it('stops a running tour without marking it as completed', async () => {
    await drive(() => startOnboarding(me));

    stopOnboarding();

    expect(me.onboardingCompleted).toBe(false);
    // completion would fetch the user synchronously, so this cannot be a timing artifact
    expect(backend.users.me).not.toHaveBeenCalled();
    expect(backend.users.putMe).not.toHaveBeenCalled();
  });

  it('omits the QR code on mobile devices', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)');

    const steps = await drive(() => startOnboarding(me));

    const appStep = renderStep(steps.length - 1).innerHTML;
    expect(appStep).not.toContain('download-qr.svg');
    expect(appStep).toContain('apps.apple.com');
  });
});

describe('startOnboardingIfNeeded', () => {
  beforeEach(setUpTourDom);
  afterEach(tearDownTourDom);

  it('starts the tour for a user who has not completed onboarding', async () => {
    await drive(() => startOnboardingIfNeeded(me));

    expect(driveMock()).toHaveBeenCalledOnce();
  });

  it('does not start the tour for a user who has completed onboarding', async () => {
    me.onboardingCompleted = true;

    await startOnboardingIfNeeded(me);

    expect(vi.mocked(driver)).not.toHaveBeenCalled();
  });
});

/* ---------- MOCKS ---------- */

let me: UserDto;
let persistedDto: UserDto | undefined;

function setUpTourDom() {
  vi.clearAllMocks();
  me = { type: 'USER', id: 'user-1', name: 'User 1', enabled: true, onboardingCompleted: false, devices: [], accessibleVaults: [] };
  persistedDto = undefined;
  // the fresh copy carries a marker, so tests can prove the PUT carried the re-fetched DTO and not the cached one
  vi.spyOn(backend.users, 'me').mockImplementation(async () => ({ ...me, onboardingCompleted: false, setupCode: 'freshly-fetched' }));
  // captured at call time, so a mutation after the PUT cannot satisfy the assertion retroactively
  vi.spyOn(backend.users, 'putMe').mockImplementation(async dto => {
    persistedDto = dto && { ...dto };
  });
  document.body.innerHTML = ['vaultList', 'addVault', 'adminNav', 'profile'].map(anchor => `<div data-tour="${anchor}"></div>`).join('');
}

function tearDownTourDom() {
  vi.restoreAllMocks();
  document.body.innerHTML = '';
}

function createDisabledButton(): HTMLButtonElement {
  const button = document.createElement('button');
  button.setAttribute('data-tour', 'addVault');
  button.disabled = true;
  return button;
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

function renderPopover(index?: number): { description: HTMLElement, title: HTMLElement, progress: HTMLElement, previousButton: HTMLElement, nextButton: HTMLElement, closeButton: HTMLElement } {
  const config = lastConfig();
  const popover = {
    description: document.createElement('div'),
    title: document.createElement('header'),
    progress: document.createElement('span'),
    previousButton: document.createElement('button'),
    nextButton: document.createElement('button'),
    closeButton: document.createElement('button')
  };
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

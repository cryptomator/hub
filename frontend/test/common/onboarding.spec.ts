import { Config, driver, DriveStep } from 'driver.js';
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { previewOnboarding, PreviewVariant, previewVariants, startOnboarding, tourSteps } from '../../src/common/onboarding';
import enUs from '../../src/i18n/en-US.json';

const { driveMock, hasRoleMock } = vi.hoisted(() => ({ driveMock: vi.fn(), hasRoleMock: vi.fn(() => true) }));
vi.mock('../../src/common/auth', () => ({ default: Promise.resolve({ hasRole: hasRoleMock }) }));
vi.mock('driver.js', () => ({ driver: vi.fn(() => ({ drive: driveMock })) }));

const messages = enUs as Record<string, string>;

describe('tourSteps', () => {
  it('includes the create vault step for users allowed to create vaults', () => {
    const steps = tourSteps(true);

    expect(steps.map(step => step.target)).toEqual([
      undefined,
      '[data-tour="vaultList"]',
      '[data-tour="addVault"]',
      '[data-tour="adminNav"]',
      '[data-tour="profile"]'
    ]);
  });

  it('swaps in the use vault step for users without the create-vaults role', () => {
    const steps = tourSteps(false);

    expect(steps.map(step => step.target)).toEqual([
      undefined,
      '[data-tour="vaultList"]',
      '[data-tour="vaultList"]',
      '[data-tour="adminNav"]',
      '[data-tour="profile"]'
    ]);
    expect(steps[2].titleKey).toBe('onboarding.useVault.title');
  });

  it('drops the admin nav step when excluded', () => {
    expect(tourSteps(true, false).map(step => step.target)).not.toContain('[data-tour="adminNav"]');
  });

  it.each([
    [true, 'onboarding.vaultList.description'],
    [false, 'onboarding.vaultList.description.shared']
  ])('describes the vault list for canCreateVaults: %s', (canCreateVaults, descriptionKey) => {
    expect(tourSteps(canCreateVaults)[1].descriptionKey).toBe(descriptionKey);
  });

  it.each([[true], [false]])('resolves every message key against en-US (canCreateVaults: %s)', (canCreateVaults) => {
    for (const step of tourSteps(canCreateVaults)) {
      expect(enUs, `missing key ${step.titleKey}`).toHaveProperty([step.titleKey]);
      expect(enUs, `missing key ${step.descriptionKey}`).toHaveProperty([step.descriptionKey]);
    }
  });

  it('renders a vignette for the welcome, vault list, and profile steps', () => {
    const vignettes = tourSteps(true).filter(step => step.vignette).map(step => [step.titleKey, step.vignette!()] as const);

    expect(vignettes.map(([titleKey]) => titleKey)).toEqual(['onboarding.welcome.title', 'onboarding.vaultList.title', 'onboarding.profile.title']);
    const rendered = vignettes.map(([, html]) => html).join('\n');
    expect(rendered).toContain('Finance');
    expect(rendered).toContain('Marketing');
    // vue-i18n echoes unresolved keys verbatim, so a dotted key in the output means a missing message
    expect(rendered).not.toMatch(/(?:onboarding|common)\.[a-z]/i);
  });

  it('anchors every step target in a component', () => {
    const componentsDir = join(dirname(fileURLToPath(import.meta.url)), '../../src/components');
    const sources = readdirSync(componentsDir)
      .filter(name => name.endsWith('.vue'))
      .map(name => readFileSync(join(componentsDir, name), 'utf8'))
      .join('\n');

    for (const target of tourSteps(true).map(step => step.target).filter(target => target !== undefined)) {
      const anchor = target.slice(1, -1);
      expect(sources, `missing anchor ${anchor}`).toContain(anchor);
    }
  });
});

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

function invokeDestroyed(config: Config) {
  type DestroyHook = NonNullable<Config['onDestroyed']>;
  (config.onDestroyed as DestroyHook | undefined)?.(undefined, {} as DriveStep, { config, state: {}, index: 0, driver: undefined as never } as Parameters<DestroyHook>[2]);
}

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  document.body.innerHTML = ['vaultList', 'addVault', 'adminNav', 'profile'].map(anchor => `<div data-tour="${anchor}"></div>`).join('');
  // happy-dom never reports an offsetParent, which would make driveTour skip every targeted step
  Object.defineProperty(HTMLElement.prototype, 'offsetParent', { configurable: true, get: () => document.body });
});

afterEach(() => {
  vi.restoreAllMocks();
  Reflect.deleteProperty(HTMLElement.prototype, 'offsetParent');
  document.body.innerHTML = '';
});

describe('startOnboarding', () => {
  it('drives all steps with the vignettes prepended and the app step appended', async () => {
    const steps = await drive(() => startOnboarding('user-1'));

    expect(steps.map(step => step.popover?.title)).toEqual([
      messages['onboarding.welcome.title'],
      messages['onboarding.vaultList.title'],
      messages['onboarding.addVault.title'],
      messages['onboarding.adminNav.title'],
      messages['onboarding.profile.title'],
      messages['onboarding.getApp.title']
    ]);
    expect(steps[1].element).toBeInstanceOf(HTMLElement);
    expect(steps[1].popover?.description).toContain('<div class="onboarding-vignette" aria-hidden="true">');
    expect(steps[1].popover?.description).toContain(`<p>${messages['onboarding.vaultList.description']}</p>`);
    const appStep = steps.at(-1)?.popover?.description;
    expect(appStep).toContain('<div class="onboarding-vignette">');
    expect(appStep).toContain(messages['onboarding.getApp.step1']);
    expect(appStep).toContain('href="https://cryptomator.org/downloads/');
    expect(appStep).toContain('/download-qr.svg');
    expect(hasRoleMock).toHaveBeenCalledWith('create-vaults');
    expect(driveMock).toHaveBeenCalledOnce();
  });

  it('leaves no unresolved message key in the driver config', async () => {
    await drive(() => startOnboarding('user-1'));

    const config = lastConfig();
    const rendered = [
      ...config.steps!.flatMap(step => [step.popover?.title, step.popover?.description]),
      config.progressText, config.nextBtnText, config.prevBtnText, config.doneBtnText
    ].join('\n');
    expect(rendered).not.toMatch(/(?:onboarding|common)\.[a-z]/i);
  });

  it('marks the tour as completed once driver reports it destroyed', async () => {
    await drive(() => startOnboarding('user-1'));

    expect(localStorage.getItem('hub.onboardingCompleted.user-1')).toBeNull();
    invokeDestroyed(lastConfig());
    expect(localStorage.getItem('hub.onboardingCompleted.user-1')).not.toBeNull();
  });

  it('omits the QR code on mobile devices', async () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)');

    const steps = await drive(() => startOnboarding('user-1'));

    expect(steps.at(-1)?.popover?.description).not.toContain('/download-qr.svg');
    expect(steps.at(-1)?.popover?.description).toContain('apps.apple.com');
  });
});

describe('previewOnboarding', () => {
  it.each<[PreviewVariant, string[]]>([
    ['admin', ['onboarding.welcome.title', 'onboarding.vaultList.title', 'onboarding.addVault.title', 'onboarding.adminNav.title', 'onboarding.profile.title', 'onboarding.getApp.title']],
    ['create-vaults', ['onboarding.welcome.title', 'onboarding.vaultList.title', 'onboarding.addVault.title', 'onboarding.profile.title', 'onboarding.getApp.title']],
    ['user', ['onboarding.welcome.title', 'onboarding.vaultList.title', 'onboarding.useVault.title', 'onboarding.profile.title', 'onboarding.getApp.title']]
  ])('drives the %s variant without querying roles', async (variant, titleKeys) => {
    const steps = await drive(() => previewOnboarding(variant));

    expect(steps.map(step => step.popover?.title)).toEqual(titleKeys.map(key => messages[key]));
    expect(hasRoleMock).not.toHaveBeenCalled();
  });

  it('never marks the tour as completed', async () => {
    for (const variant of previewVariants) {
      await drive(() => previewOnboarding(variant));
      invokeDestroyed(lastConfig());
    }

    expect(Object.keys(localStorage).filter(key => key.startsWith('hub.onboardingCompleted'))).toHaveLength(0);
    expect(driveMock).toHaveBeenCalledTimes(previewVariants.length);
  });
});

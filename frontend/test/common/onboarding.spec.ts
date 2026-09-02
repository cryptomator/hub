import { describe, expect, it, vi } from 'vitest';
import { tourSteps } from '../../src/common/onboarding';
import enUs from '../../src/i18n/en-US.json';

vi.mock('../../src/common/auth', () => ({ default: Promise.resolve({}) }));

describe('tourSteps', () => {
  it('appends the create vault step for users allowed to create vaults', () => {
    const steps = tourSteps(true);

    expect(steps.map(step => step.target)).toEqual([
      undefined,
      '[data-tour="vaultList"]',
      '[data-tour="appCard"]',
      '[data-tour="adminNav"]',
      '[data-tour="profile"]',
      '[data-tour="addVault"]'
    ]);
  });

  it('omits the create vault step for users without the create-vaults role', () => {
    const steps = tourSteps(false);

    expect(steps.map(step => step.target)).toEqual([
      undefined,
      '[data-tour="vaultList"]',
      '[data-tour="appCard"]',
      '[data-tour="adminNav"]',
      '[data-tour="profile"]'
    ]);
  });

  it.each([
    [true, 'onboarding.vaultList.description'],
    [false, 'onboarding.vaultList.description.shared']
  ])('describes the vault list for canCreateVaults: %s', (canCreateVaults, descriptionKey) => {
    const step = tourSteps(canCreateVaults).find(s => s.target === '[data-tour="vaultList"]');

    expect(step?.descriptionKey).toBe(descriptionKey);
  });

  it.each([[true], [false]])('resolves every message key against en-US (canCreateVaults: %s)', (canCreateVaults) => {
    for (const step of tourSteps(canCreateVaults)) {
      expect(enUs, `missing key ${step.titleKey}`).toHaveProperty([step.titleKey]);
      expect(enUs, `missing key ${step.descriptionKey}`).toHaveProperty([step.descriptionKey]);
    }
  });
});

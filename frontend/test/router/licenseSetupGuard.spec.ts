import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RouteLocationNormalized } from 'vue-router';
import config from '../../src/common/config';
import { licenseSetupGuard } from '../../src/router/licenseSetupGuard';

vi.mock('../../src/common/config', () => ({ default: { get: vi.fn() } }));

describe('licenseSetupGuard', () => {
  function route(meta: Record<string, unknown> = {}): RouteLocationNormalized {
    return { meta } as unknown as RouteLocationNormalized;
  }

  beforeEach(() => {
    vi.mocked(config.get).mockReset();
  });

  it('redirects to /app/setup-license if license setup is required', () => {
    vi.mocked(config.get).mockReturnValue({ licenseSetupRequired: true } as ReturnType<typeof config.get>);

    expect(licenseSetupGuard(route())).toEqual({ path: '/app/setup-license', replace: true });
  });

  it('passes routes with skipLicenseSetup meta without consulting the config', () => {
    expect(licenseSetupGuard(route({ skipLicenseSetup: true }))).toBe(true);

    expect(config.get).not.toHaveBeenCalled();
  });

  it('passes if no license setup is required', () => {
    vi.mocked(config.get).mockReturnValue({ licenseSetupRequired: false } as ReturnType<typeof config.get>);

    expect(licenseSetupGuard(route())).toBe(true);
  });
});

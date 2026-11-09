import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { EntitlementsDto } from '../../src/common/backend';
import { Deferred } from '../../src/common/util';

const mocked = vi.hoisted(() => ({
  getEntitlements: vi.fn<() => Promise<EntitlementsDto>>()
}));

vi.mock('../../src/common/backend', () => ({ default: { license: { getEntitlements: mocked.getEntitlements } } }));

function entitlementsWithSeats(seats: number): EntitlementsDto {
  return { seats, showTrialHint: false, auditLogRetentionDays: 0, emergencyAccessEnabled: false, keycloakAccessEnabled: true };
}

describe('License entitlements', () => {
  let licenseEntitlements: typeof import('../../src/common/entitlements').default;

  beforeEach(async () => {
    mocked.getEntitlements.mockReset();
    vi.resetModules(); // fresh singleton per test
    licenseEntitlements = (await import('../../src/common/entitlements')).default;
  });

  it('shares one request between concurrent get() calls', async () => {
    mocked.getEntitlements.mockResolvedValueOnce(entitlementsWithSeats(1));

    const [first, second] = await Promise.all([licenseEntitlements.get(), licenseEntitlements.get()]);

    expect(first.seats).to.eq(1);
    expect(second).to.equal(first);
    expect(mocked.getEntitlements).toHaveBeenCalledTimes(1);
  });

  it('caches successfully loaded entitlements', async () => {
    mocked.getEntitlements.mockResolvedValueOnce(entitlementsWithSeats(1));

    await licenseEntitlements.get();
    const cached = await licenseEntitlements.get();

    expect(cached.seats).to.eq(1);
    expect(mocked.getEntitlements).toHaveBeenCalledTimes(1);
  });

  it('retries after a failed request', async () => {
    mocked.getEntitlements.mockRejectedValueOnce(new Error('network error')).mockResolvedValueOnce(entitlementsWithSeats(2));

    await expect(licenseEntitlements.get()).rejects.toThrow('network error');
    const retried = await licenseEntitlements.get();

    expect(retried.seats).to.eq(2);
    expect(mocked.getEntitlements).toHaveBeenCalledTimes(2);
  });

  it('always fetches on reload()', async () => {
    mocked.getEntitlements.mockResolvedValueOnce(entitlementsWithSeats(1)).mockResolvedValueOnce(entitlementsWithSeats(2));

    await licenseEntitlements.get();
    const reloaded = await licenseEntitlements.reload();

    expect(reloaded.seats).to.eq(2);
    expect((await licenseEntitlements.get()).seats).to.eq(2);
    expect(mocked.getEntitlements).toHaveBeenCalledTimes(2);
  });

  it('does not let a superseded request that fails clear a newer reload', async () => {
    const stale = new Deferred<EntitlementsDto>();
    mocked.getEntitlements.mockReturnValueOnce(stale.promise).mockResolvedValueOnce(entitlementsWithSeats(2));

    const staleGet = licenseEntitlements.get();
    await licenseEntitlements.reload();
    stale.reject(new Error('network error'));
    await expect(staleGet).rejects.toThrow('network error');
    const afterwards = await licenseEntitlements.get();

    expect(afterwards.seats).to.eq(2);
    expect(mocked.getEntitlements).toHaveBeenCalledTimes(2);
  });
});

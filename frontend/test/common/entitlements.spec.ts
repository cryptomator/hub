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

  it('caches successfully loaded entitlements and exposes them via current', async () => {
    mocked.getEntitlements.mockResolvedValueOnce(entitlementsWithSeats(1));
    expect(licenseEntitlements.current.value).to.be.undefined;

    await licenseEntitlements.get();
    const cached = await licenseEntitlements.get();

    expect(cached.seats).to.eq(1);
    expect(licenseEntitlements.current.value?.seats).to.eq(1);
    expect(mocked.getEntitlements).toHaveBeenCalledTimes(1);
  });

  it('updates current reactively, also for consumers that captured it before loading', async () => {
    mocked.getEntitlements.mockResolvedValueOnce(entitlementsWithSeats(1)).mockResolvedValueOnce(entitlementsWithSeats(2));
    const { computed } = await import('vue'); // same module instance as the freshly imported store after vi.resetModules()
    const current = licenseEntitlements.current; // components capture it during setup, before the first load
    const seats = computed(() => current.value?.seats);
    expect(seats.value).to.be.undefined;

    await licenseEntitlements.get();
    expect(seats.value).to.eq(1);

    await licenseEntitlements.reload();
    expect(seats.value).to.eq(2);
  });

  it('retries after a failed request', async () => {
    mocked.getEntitlements.mockRejectedValueOnce(new Error('network error')).mockResolvedValueOnce(entitlementsWithSeats(2));

    await expect(licenseEntitlements.get()).rejects.toThrow('network error');
    const retried = await licenseEntitlements.get();

    expect(retried.seats).to.eq(2);
    expect(licenseEntitlements.current.value?.seats).to.eq(2);
    expect(mocked.getEntitlements).toHaveBeenCalledTimes(2);
  });

  it('keeps the previously loaded entitlements in current if a reload fails', async () => {
    mocked.getEntitlements.mockResolvedValueOnce(entitlementsWithSeats(1)).mockRejectedValueOnce(new Error('network error'));

    await licenseEntitlements.get();
    await expect(licenseEntitlements.reload()).rejects.toThrow('network error');

    expect(licenseEntitlements.current.value?.seats).to.eq(1);
  });

  it('always fetches on reload() and updates current', async () => {
    mocked.getEntitlements.mockResolvedValueOnce(entitlementsWithSeats(1)).mockResolvedValueOnce(entitlementsWithSeats(2));

    await licenseEntitlements.get();
    const reloaded = await licenseEntitlements.reload();

    expect(reloaded.seats).to.eq(2);
    expect(licenseEntitlements.current.value?.seats).to.eq(2);
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

  it('does not let a superseded request that succeeds overwrite a newer reload', async () => {
    const stale = new Deferred<EntitlementsDto>();
    mocked.getEntitlements.mockReturnValueOnce(stale.promise).mockResolvedValueOnce(entitlementsWithSeats(2));

    const staleGet = licenseEntitlements.get();
    await licenseEntitlements.reload();
    stale.resolve(entitlementsWithSeats(1));
    await staleGet;

    expect(licenseEntitlements.current.value?.seats).to.eq(2);
    expect((await licenseEntitlements.get()).seats).to.eq(2);
  });
});

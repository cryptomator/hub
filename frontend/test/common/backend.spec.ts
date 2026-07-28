import { AxiosError, AxiosResponse } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { asError, LicenseUserInfoDto, NotFoundError } from '../../src/common/backend';

vi.mock('../../src/common/auth', () => ({ default: Promise.resolve({}) }));
vi.mock('../../src/common/config', () => ({ default: {}, backendBaseURL: '/api/' }));

describe('asError', () => {
  function axiosError(data: unknown, status = 500): AxiosError {
    const response = { data: data, status } as AxiosResponse;
    return new AxiosError(`Request failed with status code ${status}`, AxiosError.ERR_BAD_RESPONSE, undefined, undefined, response);
  }

  it('maps empty-body 404 to NotFoundError', () => {
    const error = axiosError('', 404);

    expect(asError(error)).toBeInstanceOf(NotFoundError);
  });

  it('falls back to the axios error for non-404 status codes', () => {
    const error = axiosError('');

    expect(asError(error)).toBe(error);
  });

  it('returns errors as-is', () => {
    const error = new Error('boom');

    expect(asError(error)).toBe(error);
  });

  it('wraps non-error values', () => {
    expect(asError('boom').message).toEqual('Unknown Error');
  });
});

describe('LicenseUserInfoDto', () => {
  const hour = 60 * 60 * 1000;
  const gracePeriod = 3 * 24 * hour; // arbitrary — the actual grace period is determined server-side

  function licenseExpiringAt(expiresAt: Date | null): LicenseUserInfoDto {
    const gracePeriodEndsAt = expiresAt != null ? new Date(expiresAt.getTime() + gracePeriod) : null;
    return new LicenseUserInfoDto(5, 3, expiresAt, gracePeriodEndsAt);
  }

  it('license expiring in the future is not expired', () => {
    const license = licenseExpiringAt(new Date(Date.now() + hour));

    expect(license.isExpired()).toBe(false);
  });

  it('license past its expiration date is expired', () => {
    const license = licenseExpiringAt(new Date(Date.now() - hour));

    expect(license.isExpired()).toBe(true);
  });

  it('license without expiration date cannot expire', () => {
    const license = licenseExpiringAt(null);

    expect(license.isExpired()).toBe(false);
  });

  it('license expiring in the future is not expired when allowing the grace period', () => {
    const license = licenseExpiringAt(new Date(Date.now() + hour));

    expect(license.isExpired('allowGracePeriod')).toBe(false);
  });

  it('license expired within the grace period is not expired when allowing the grace period', () => {
    const license = licenseExpiringAt(new Date(Date.now() - hour));

    expect(license.isExpired('allowGracePeriod')).toBe(false);
  });

  it('license expired almost beyond the grace period is not expired when allowing the grace period', () => {
    const license = licenseExpiringAt(new Date(Date.now() - gracePeriod + hour));

    expect(license.isExpired('allowGracePeriod')).toBe(false);
  });

  it('license expired beyond the grace period is expired even when allowing the grace period', () => {
    const license = licenseExpiringAt(new Date(Date.now() - gracePeriod - hour));

    expect(license.isExpired('allowGracePeriod')).toBe(true);
  });

  it('license without expiration date cannot expire even when allowing the grace period', () => {
    const license = licenseExpiringAt(null);

    expect(license.isExpired('allowGracePeriod')).toBe(false);
  });

  it('license expired beyond the grace period is violated', () => {
    const license = licenseExpiringAt(new Date(Date.now() - gracePeriod - hour));

    expect(license.isViolated()).toBe(true);
  });

  it('license with more used than licensed seats is violated', () => {
    const license = new LicenseUserInfoDto(5, 6, new Date(Date.now() + hour), new Date(Date.now() + gracePeriod + hour));

    expect(license.isViolated()).toBe(true);
  });

  it('license expired within the grace period is not violated', () => {
    const license = licenseExpiringAt(new Date(Date.now() - hour));

    expect(license.isViolated()).toBe(false);
  });
});

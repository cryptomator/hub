import { AxiosError, AxiosResponse } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { asError, NotFoundError } from '../../src/common/backend';

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

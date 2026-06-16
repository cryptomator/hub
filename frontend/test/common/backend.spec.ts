import { AxiosError, AxiosResponse } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { asError } from '../../src/common/backend';

vi.mock('../../src/common/auth', () => ({ default: Promise.resolve({}) }));
vi.mock('../../src/common/config', () => ({ default: {}, backendBaseURL: '/api/' }));

describe('asError', () => {
  function axiosError(data: unknown): AxiosError {
    const response = { data: data, status: 500 } as AxiosResponse;
    return new AxiosError('Request failed with status code 500', AxiosError.ERR_BAD_RESPONSE, undefined, undefined, response);
  }

  it('prefers the error message provided by the backend', () => {
    const error = axiosError('CREATE_USER_FAILED');

    expect(asError(error).message).toEqual('CREATE_USER_FAILED');
  });

  it('falls back to the axios error message for empty response bodies', () => {
    const error = axiosError('');

    expect(asError(error).message).toEqual('Request failed with status code 500');
  });

  it('falls back to the axios error message for non-string response bodies', () => {
    const error = axiosError({ title: 'Constraint Violation' });

    expect(asError(error).message).toEqual('Request failed with status code 500');
  });

  it('returns errors without backend message as-is', () => {
    const error = new Error('boom');

    expect(asError(error)).toBe(error);
  });

  it('wraps non-error values', () => {
    expect(asError('boom').message).toEqual('Unknown Error');
  });
});

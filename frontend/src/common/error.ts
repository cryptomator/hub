import axios, { AxiosError } from 'axios';

export enum CommonError {
  NotAuthorized,
  NotFound,
  AlreadyExisting
}

export function toCommonError(error: unknown): null | CommonError {
  if (axios.isAxiosError(error)) {
    return axiosErrorToCommonError(error as AxiosError);
  }
  return null;
}

function axiosErrorToCommonError(axiosError: AxiosError): null | CommonError {
  switch (axiosError.response?.status) {
    case 409:
      return CommonError.AlreadyExisting;
    case 404:
      return CommonError.NotFound;
    case 403:
      return CommonError.NotAuthorized;
    default:
      return null;
  }
}

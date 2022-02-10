import AxiosStatic, { AxiosRequestConfig, AxiosResponse } from 'axios';
import authPromise from './auth';

const axiosBaseCfg: AxiosRequestConfig = {
  baseURL: import.meta.env.DEV ? 'http://localhost:9090' : '',
  headers: {
    'Content-Type': 'application/json'
  }
};

const axios = AxiosStatic.create(axiosBaseCfg);
const axiosAuth = AxiosStatic.create(axiosBaseCfg);
axiosAuth.interceptors.request.use(async request => {
  const auth = await authPromise;
  if (!auth.isAuthenticated()) {
    throw new Error('not logged in');
  }
  const token = await auth.bearerToken();
  if (request.headers) {
    request.headers['Authorization'] = `Bearer ${token}`;
  } else {
    request.headers = { 'Authorization': `Bearer ${token}` };
  }
  return request;
});

/* DTOs */

export class VaultDto {
  constructor(public id: string, public name: string, public masterkey: string, public iterations: number, public salt: string) { }
}

export class DeviceDto {
  constructor(public id: string, public name: string, public publicKey: string, public accessTo: VaultDto[]) { }
}

export class UserDto {
  constructor(public id: string, public name: string, public pictureUrl: string, public email: string, public devices: DeviceDto[]) { }
}

/* Services */

class VaultService {
  public async listSharedOrOwned(): Promise<VaultDto[]> {
    return axiosAuth.get('/vaults').then(response => response.data);
  }

  public async get(vaultId: string): Promise<VaultDto> {
    return axiosAuth.get(`/vaults/${vaultId}`).then(response => response.data)
      .catch((err) => rethrowAndConvertIfExpected(err, 404));
  }

  public async getMembers(vaultId: string): Promise<UserDto[]> {
    return axiosAuth.get(`/vaults/${vaultId}/members`).then(response => response.data);
  }

  public async addMember(vaultId: string, userId: string): Promise<AxiosResponse<void>> {
    return axiosAuth.put(`/vaults/${vaultId}/members/${userId}`)
      .catch((err) => rethrowAndConvertIfExpected(err, 404));
  }

  public async getDevicesRequiringAccessGrant(vaultId: string): Promise<DeviceDto[]> {
    return axiosAuth.get(`/vaults/${vaultId}/devices-requiring-access-grant`).then(response => response.data);
  }

  public async createVault(vaultId: string, name: string, masterkey: string, iterations: number, salt: string): Promise<AxiosResponse<any>> {
    const body: VaultDto = { id: vaultId, name: name, masterkey: masterkey, iterations: iterations, salt: salt };
    return axiosAuth.put(`/vaults/${vaultId}`, body)
      .catch((err) => rethrowAndConvertIfExpected(err, 404, 409));
  }

  public async grantAccess(vaultId: string, deviceId: string, jwe: string) {
    await axiosAuth.put(`/vaults/${vaultId}/keys/${deviceId}`, jwe, { headers: { 'Content-Type': 'text/plain' } })
      .catch((err) => rethrowAndConvertIfExpected(err, 404, 409));
  }

  public async revokeUserAccess(vaultId: string, userId: string) {
    await axiosAuth.delete(`/vaults/${vaultId}/members/${userId}`)
      .catch((err) => rethrowAndConvertIfExpected(err, 404));
  }
}
class DeviceService {

  public async removeDevice(deviceId: string): Promise<AxiosResponse<any>> {
    return axiosAuth.delete(`/devices/${deviceId}`)
      .catch((err) => rethrowAndConvertIfExpected(err, 404));
  }

}

class UserService {

  public async syncMe(): Promise<void> {
    return axiosAuth.put('/users/me');
  }
  public async me(withDevices: boolean = false, withAccessibleVaults: boolean = false): Promise<UserDto> {
    return axiosAuth.get<UserDto>(`/users/me?withDevices=${withDevices}&withAccessibleVaults=${withAccessibleVaults}`).then(response => response.data);
  }

  public async listAll(): Promise<UserDto[]> {
    return axiosAuth.get<UserDto[]>('/users/').then(response => response.data);
  }

}

const services = {
  vaults: new VaultService(),
  users: new UserService(),
  devices: new DeviceService()
};

function convertExpectedToBackendError(status: number): BackendError {
  switch (status) {
    case 403:
      return new ForbiddenError();
    case 404:
      return new NotFoundError();
    case 409:
      return new ConflictError();
    default:
      return new BackendError('Status Code ${status} not mapped');
  }
}

/**
 * Rethrows the error object or, if 'error' is an response with an expected http status code, it is converted to an BackendError and then rethrown.
 * @param error A thrown object
 * @param expectedStatusCodes The expected http status codes of the backend call
 */
function rethrowAndConvertIfExpected(error: unknown, ...expectedStatusCodes: number[]): Promise<any> {
  if (AxiosStatic.isAxiosError(error) && error.response != null && expectedStatusCodes.includes(error.response.status)) {
    throw convertExpectedToBackendError;
  } else {
    throw error;
  }
}

export default services;

//-- Error thrown by this module --
export class BackendError extends Error {
  constructor(msg: string) {
    super(msg);
  }
}

export class ForbiddenError extends BackendError {
  constructor() {
    super('Not authorized to access resource');
  }
}

export class NotFoundError extends BackendError {
  constructor() {
    super('Requested resource not found');
  }
}

export class ConflictError extends BackendError {
  constructor() {
    super('Resource already exists');
  }
}

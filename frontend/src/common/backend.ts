import AxiosStatic, { AxiosRequestConfig, AxiosResponse } from 'axios';
import authPromise from './auth';
import { AlreadyExistsError, BackendError, NotAuthorizedError, NotFoundError } from './error';

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
    return axiosAuth.get(`/vaults/${vaultId}`).then(response => response.data);
  }

  public async getMembers(vaultId: string): Promise<UserDto[]> {
    return axiosAuth.get(`/vaults/${vaultId}/members`).then(response => response.data);
  }

  public async addMember(vaultId: string, userId: string): Promise<AxiosResponse<void>> {
    return axiosAuth.put(`/vaults/${vaultId}/members/${userId}`);
  }

  public async getDevicesRequiringAccessGrant(vaultId: string): Promise<DeviceDto[]> {
    return axiosAuth.get(`/vaults/${vaultId}/devices-requiring-access-grant`).then(response => response.data);
  }

  public async createVault(vaultId: string, name: string, masterkey: string, iterations: number, salt: string): Promise<AxiosResponse<any>> {
    const body: VaultDto = { id: vaultId, name: name, masterkey: masterkey, iterations: iterations, salt: salt };
    return axiosAuth.put(`/vaults/${vaultId}`, body)
      .catch((err) => Promise.reject(tryConversionToBackendError(err)));
  }

  public async grantAccess(vaultId: string, deviceId: string, jwe: string) {
    await axiosAuth.put(`/vaults/${vaultId}/keys/${deviceId}`, jwe, { headers: { 'Content-Type': 'text/plain' } });
  }

  public async revokeUserAccess(vaultId: string, userId: string) {
    await axiosAuth.delete(`/vaults/${vaultId}/members/${userId}`);
  }
}
class DeviceService {

  public async removeDevice(deviceId: string): Promise<AxiosResponse<any>> {
    return axiosAuth.delete(`/devices/${deviceId}`);
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


function tryConversionToBackendError(error: unknown): BackendError | unknown {
  if (AxiosStatic.isAxiosError(error)) {
    switch (error.response?.status) {
      case 409:
        return new AlreadyExistsError();
      case 404:
        return new NotFoundError();
      case 403:
        return new NotAuthorizedError();
      default:
        return error;
    }
  }
  return error;
}

export default services;

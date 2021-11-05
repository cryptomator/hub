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

export class VaultDto {
  constructor(public name: string, public masterkey: string, public iterations: number, public salt: string) { }
}

export class UserDto {
  constructor(public name: string, public devices: DeviceDto[]) { }
}

class VaultService {
  public async listAll(): Promise<VaultDto[]> {
    return axiosAuth.get('/vaults').then(response => response.data);
  }

  public async get(vaultId: string): Promise<AxiosResponse<VaultDto>> {
    return axiosAuth.get(`/vaults/${vaultId}`);
  }

  public async createVault(vaultId: string, name: string, masterkey: string, iterations: number, salt: string): Promise<AxiosResponse<any>> {
    const body: VaultDto = { name: name, masterkey: masterkey, iterations: iterations, salt: salt };
    return axiosAuth.put(`/vaults/${vaultId}`, body);
  }

  public async grantAccess(vaultId: string, deviceId: string, deviceSpecificMasterkey: string, ephemeralPublicKey: string) {
    const body: AccessDto = { device_specific_masterkey: deviceSpecificMasterkey, ephemeral_public_key: ephemeralPublicKey };
    await axiosAuth.put(`/vaults/${vaultId}/keys/${deviceId}`, body);
  }

  public async revokeDeviceAccess(vaultId: string, deviceId: string) {
    await axiosAuth.delete(`/vaults/${vaultId}/keys/${deviceId}`);
  }

  public async revokeUserAccess(vaultId: string, userId: string) {
    await axiosAuth.delete(`/vaults/${vaultId}/revoke-user/${userId}`);
  }
}

export class AccessDto {
  constructor(public device_specific_masterkey: string, public ephemeral_public_key: string) { }
}

class DeviceService {

  public async createDevice(deviceId: string, name: string, publicKey: String): Promise<AxiosResponse<any>> {
    const body = { id: deviceId, name: name, publicKey: publicKey };
    return axiosAuth.put(`/devices/${deviceId}`, body);
  }

  public async getDevice(deviceId: string): Promise<AxiosResponse<DeviceDto>> {
    return axiosAuth.get<DeviceDto>(`/devices/${deviceId}`);
  }

  public async listAll(): Promise<DeviceDto[]> {
    return axiosAuth.get<DeviceDto[]>('/devices/').then(response => response.data);
  }
}

export class DeviceDto {
  constructor(public id: string, public name: string, public publicKey: string, public accessTo: VaultDto[]) { }
}

class UserService {

  public async syncMe(): Promise<void> {
    return axiosAuth.put('/users/me');
  }
  public async me(): Promise<string> {
    return axiosAuth.get<string>('/users/me').then(response => response.data);
  }

  public async meIncludingDevices(): Promise<UserDto> {
    return axiosAuth.get<UserDto>('/users/me-extended/').then(response => response.data);
  }

  public async listAllUsersIncludingDevices(): Promise<UserDto[]> {
    return axiosAuth.get<UserDto[]>('/users/devices/').then(response => response.data);
  }
}

const services = {
  vaults: new VaultService(),
  users: new UserService(),
  devices: new DeviceService()
};

export default services;

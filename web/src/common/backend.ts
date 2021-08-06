import AxiosStatic, { AxiosResponse } from 'axios';
import auth from './auth';

const axios = AxiosStatic.create({
  baseURL: import.meta.env.DEV ? 'http://localhost:9090' : '',
  headers: {
    'Content-Type': 'application/json'
  }
});
axios.interceptors.request.use(async request => {
  const token = await auth.bearerToken();
  request.headers['Authorization'] = 'Bearer ' + token;
  return request;
});

class VaultService {

  public async createVault(uuid: string, name: string, masterkey: String, iterations: number, salt: String): Promise<AxiosResponse<any>> {
    if (!auth.isAuthenticated()) {
      return Promise.reject('not logged in');
    }
    const body = { name: name, masterkey: masterkey, iterations: iterations, salt: salt };
    return axios.put('/vaults/' + uuid, body);
  }
}

class UserService {
  public async me(): Promise<string> {
    if (!auth.isAuthenticated()) {
      return Promise.reject('not logged in');
    }
    return axios.get<string>('/users/me').then(response => response.data)
  }
}

export class DeviceDto {
  constructor(public name: string, public publicKey: string) { }
}

class DeviceService {
  public async listAll(): Promise<DeviceDto[]> {
    if (!auth.isAuthenticated()) {
      return Promise.reject('not logged in');
    }
    return axios.get<DeviceDto[]>('/devices/').then(response => response.data)
  }
}

const services = {
  vaults: new VaultService(),
  users: new UserService(),
  devices: new DeviceService(),
};

export default services;

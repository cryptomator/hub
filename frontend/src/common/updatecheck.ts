import AxiosStatic, { AxiosRequestConfig } from 'axios';

const axiosBaseCfg: AxiosRequestConfig = {
  headers: {
    'Content-Type': 'application/json'
  }
};

const axios = AxiosStatic.create(axiosBaseCfg);

export class LatestVersionDto {
  constructor(public stable: string, public beta: string) { }
}

class UpdatesService {
  public async get(): Promise<LatestVersionDto> {
    return axios.get('https://api.cryptomator.org/updates/hub.json').then(response => response.data);
  }
}

export const updateChecker = new UpdatesService();

import AxiosStatic from 'axios';
import { Deferred } from './util';

const axios = AxiosStatic.create({
  baseURL: import.meta.env.DEV ? 'http://localhost:9090' : '',
  headers: {
    'Content-Type': 'application/json'
  }
});

class ConfigDto {
  constructor(public setupCompleted: boolean, public keycloakUrl: string) { }
}

async function loadConfig(): Promise<ConfigDto> {
  return axios.get<ConfigDto>('/setup').then(response => response.data)
}

class ConfigWrapper {
  private data: ConfigDto;
  private deferredSetupCompletion: Deferred<void>;

  public constructor(data: ConfigDto) {
    this.data = data;
    this.deferredSetupCompletion = new Deferred();
    this.checkSetupCompleted();
  }

  public get(): ConfigDto {
    return this.data;
  }

  private checkSetupCompleted(): void {
    if (this.data.setupCompleted) {
      this.deferredSetupCompletion.resolve();
    }
  }

  public async reload(): Promise<void> {
    this.data = await loadConfig();
    this.checkSetupCompleted();
  }

  public awaitSetupCompletion(): Promise<void> {
    return this.deferredSetupCompletion.promise;
  }

}

const config = new ConfigWrapper(await loadConfig());

export default config;

import AxiosStatic from 'axios';
import { Deferred } from './util';

const axios = AxiosStatic.create({
  baseURL: import.meta.env.DEV ? 'http://localhost:9090' : '',
  headers: {
    'Content-Type': 'application/json'
  }
});

export class ConfigDto {
  constructor(public setupCompleted: boolean, public keycloakUrl: string) { }
}

class ConfigWrapper {
  private data: ConfigDto;
  private deferredSetupCompletion: Deferred<void>;

  private static async loadConfig(): Promise<ConfigDto> {
    const response = await axios.get<ConfigDto>('/setup');
    return response.data;
  }

  static async build(): Promise<ConfigWrapper> {
    return new ConfigWrapper(await this.loadConfig());
  }

  private constructor(data: ConfigDto) {
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
    this.data = await ConfigWrapper.loadConfig();
    this.checkSetupCompleted();
  }

  public awaitSetupCompletion(): Promise<void> {
    return this.deferredSetupCompletion.promise;
  }

}

const config = await ConfigWrapper.build();

export default config;

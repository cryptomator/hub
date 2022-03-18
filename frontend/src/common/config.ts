import AxiosStatic from 'axios';

const axios = AxiosStatic.create({
  baseURL: (import.meta.env.DEV ? 'http://localhost:8080' : '') + '/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

export class ConfigDto {
  constructor(public keycloakRealm: string, public keycloakUrl: string, public keycloakClientId: string) { }
}

class ConfigWrapper {

  private data: ConfigDto;

  private static async loadConfig(): Promise<ConfigDto> {
    const response = await axios.get<ConfigDto>('/config');
    return response.data;
  }

  static async build(): Promise<ConfigWrapper> {
    return new ConfigWrapper(await this.loadConfig());
  }

  private constructor(data: ConfigDto) {
    this.data = data;
  }

  public get(): ConfigDto {
    return this.data;
  }

  public async reload(): Promise<void> {
    this.data = await ConfigWrapper.loadConfig();
  }

}

const config = await ConfigWrapper.build();

export default config;

import AxiosStatic from 'axios';

export const frontendBaseURL = `${location.protocol}//${location.host}${import.meta.env.BASE_URL}#`;
export const backendBaseURL = import.meta.env.DEV
  ? 'http://localhost:8080/api'
  : new URL('/api', location.href).href;

const axios = AxiosStatic.create({
  baseURL: backendBaseURL,
  headers: {
    'Content-Type': 'application/json'
  }
});

export class ConfigDto {
  constructor(public keycloakRealm: string, public keycloakUrl: string, public keycloakClientId: string, public keycloakAuthEndpoint: string, public keycloakTokenEndpoint: string) { }
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

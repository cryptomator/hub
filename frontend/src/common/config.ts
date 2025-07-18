import AxiosStatic from 'axios';

// these URLs must end on '/':
export const baseURL = new URL(document.baseURI).pathname;
export const frontendBaseURL = `${baseURL}app/`;
export const absFrontendBaseURL = `${location.origin}${frontendBaseURL}`;
export const backendBaseURL = `${baseURL}api/`;
export const absBackendBaseURL = `${location.origin}${backendBaseURL}`;

const axios = AxiosStatic.create({
  baseURL: backendBaseURL,
  headers: {
    'Content-Type': 'application/json'
  }
});

export type ConfigDto = {
  keycloakRealm: string;
  keycloakUrl: string;
  keycloakClientIdHub: string;
  keycloakClientIdCryptomator: string;
  keycloakAuthEndpoint: string;
  keycloakTokenEndpoint: string;
  serverTime: string;
  apiLevel: number;
};

class ConfigWrapper {
  private data: ConfigDto;
  readonly serverTimeDiff: number;

  private static async loadConfig(): Promise<ConfigDto> {
    const response = await axios.get<ConfigDto>('/config');
    return response.data;
  }

  static async build(): Promise<ConfigWrapper> {
    return new ConfigWrapper(await this.loadConfig());
  }

  private constructor(data: ConfigDto) {
    this.data = data;
    this.serverTimeDiff = Math.floor((Date.parse(data.serverTime) - Date.now()) / 1000);
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

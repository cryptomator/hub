import AxiosStatic from 'axios';

// these URLs must end on '/':
export const baseURL = import.meta.env.BASE_URL;
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

export class ConfigDto {
  constructor(public keycloakRealm: string, public keycloakUrl: string, public keycloakClientId: string, public keycloakAuthEndpoint: string, public keycloakTokenEndpoint: string, public keycloakAdminRealmUrl: string) { }
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

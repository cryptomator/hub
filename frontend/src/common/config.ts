import AxiosStatic from 'axios';

// these URLs must end on '/':
export const baseURL = new URL(document.baseURI).pathname;
export const absBaseURL = `${location.origin}${baseURL}`;
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
  entitlements: {
    seats: number;
    showTrialHint: boolean;
    auditLogRetentionDays: number;
    emergencyAccessEnabled: boolean;
    keycloakAccessEnabled: boolean;
    iosLicense: string;
    androidLicense: string;
    desktopLicense: string;
  };
  ceRegistrationUrl: string;
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

  public async reload(): Promise<ConfigDto> {
    this.data = await ConfigWrapper.loadConfig();
    return this.data;
  }
}

const config = await ConfigWrapper.build();

export default config;

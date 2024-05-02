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

// this is a lazy singleton:
const config: Promise<ConfigDto> = (async () => {
  const response = await axios.get<ConfigDto>('/config');
  return response.data;
})();

export default config;

import AxiosStatic from 'axios';

const url = (typeof document !== 'undefined') ? new URL(document.baseURI) : new URL('http://localhost/'); // workaround for testing in Node environment

// these URLs must end on '/':
export const baseURL = url.pathname;
export const frontendBaseURL = `${baseURL}app/`;
export const absFrontendBaseURL = `${url.origin}${frontendBaseURL}`;
export const backendBaseURL = `${baseURL}api/`;
export const absBackendBaseURL = `${url.origin}${backendBaseURL}`;

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

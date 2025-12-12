import Keycloak from 'keycloak-js';
import { buildRedirectSyncMeUri } from '../router';
import config, { ConfigDto } from './config';

class Auth {
  private readonly keycloak: Keycloak;

  static async build(cfg: ConfigDto): Promise<Auth> {
    const keycloak = new Keycloak({
      url: cfg.keycloakUrl,
      realm: cfg.keycloakRealm,
      clientId: cfg.keycloakClientIdHub
    });

    const auth = new Auth(keycloak);

    keycloak.onTokenExpired = () => {
      auth.refreshToken(30);
    };

    await keycloak.init({
      checkLoginIframe: false,
      pkceMethod: 'S256',
    });

    return auth;
  }

  private constructor(keycloak: Keycloak) {
    this.keycloak = keycloak;
  }

  public isAuthenticated(): boolean {
    return this.keycloak.authenticated ?? false;
  }

  public async login(redirectUri: string): Promise<void> {
    await this.keycloak.login({ redirectUri });
  }

  public async logout(redirectUri?: string): Promise<void> {
    await this.keycloak.logout({ redirectUri });
  }

  public async bearerToken(): Promise<string | undefined> {
    await this.refreshToken(10);
    return this.keycloak.token;
  }

  public hasRole(role: string): boolean {
    return this.keycloak.tokenParsed?.realm_access?.roles.includes(role) ?? false;
  }

  private async refreshToken(minValidity?: number, delay: number = 500): Promise<void> {
    if (delay > 16000) { // max wait time of 16 seconds before giving up
      console.error('Auth Token refresh failed after maximum retries. Clearing tokens & logging out.');
      this.keycloak.clearToken();
      return this.logout();
    }
    try {
      await this.keycloak.updateToken(minValidity);
      return; // success
    } catch (err) {
      if (!this.isAuthenticated()) {
        const redirectUrl = buildRedirectSyncMeUri();
        return this.login(redirectUrl);
      } else {
        console.warn(`Auth Token refresh failed, retrying in ${delay}ms`, err);
        await new Promise(res => setTimeout(res, delay));
        return this.refreshToken(minValidity, delay * 2); // exponential backoff
      }
    }
  }
}

// this is a lazy singleton:
const instance: Promise<Auth> = (async () => {
  return await Auth.build(config.get());
})();

export default instance;

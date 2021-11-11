import newKeycloak, { KeycloakInstance } from 'keycloak-js';
import config from './config';

class Auth {
  private keycloak: KeycloakInstance;
  private initialized: Promise<boolean>;

  public constructor() {
    console.assert(config.get().setupCompleted, 'did not run setup yet');

    this.keycloak = newKeycloak({
      url: `${config.get().keycloakUrl}`,
      realm: 'cryptomator', // TODO: read from config
      clientId: 'cryptomator-hub', // TODO: read from config
    });
    this.initialized = this.keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      pkceMethod: 'S256',
    });
    this.keycloak.onTokenExpired = () => this.keycloak.updateToken(30);
  }

  public isAuthenticated(): boolean {
    return this.keycloak.authenticated || false;
  }

  public async loginIfRequired(redirectUri?: string): Promise<void> {
    await this.initialized;
    if (!this.keycloak.authenticated) {
      await this.keycloak.login({
        redirectUri: (redirectUri ?? window.location) + '?login' // keycloak appends '&state=...' which confuses vue-router if there is no '?'
      });
    }
  }

  public async logout(redirectUri?: string): Promise<void> {
    await this.initialized;
    return this.keycloak.logout({
      redirectUri: redirectUri
    });
  }

  public async bearerToken(): Promise<string | undefined> {
    await this.keycloak.updateToken(10);
    return this.keycloak.token;
  }

}

// this is a lazy singleton:
const instance: Promise<Auth> = (async () => {
  await config.awaitSetupCompletion();
  return new Auth();
})();

export default instance;

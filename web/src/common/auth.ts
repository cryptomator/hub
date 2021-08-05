import newKeycloak, { KeycloakInstance } from 'keycloak-js';

class Auth {
  private keycloak: KeycloakInstance;
  private initialized: Promise<boolean>;

  public constructor() {
    this.keycloak = newKeycloak({
      url: 'http://localhost:8080/auth',
      realm: 'cryptomator',
      clientId: 'cryptomator-hub'
    });
    this.initialized = this.keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
    })
  }

  public isAuthenticated(): boolean {
    return this.keycloak.authenticated || false;
  }

  public async loginIfRequired(redirectUri?: string): Promise<void> {
    await this.initialized;
    if (this.keycloak.authenticated) {
      return Promise.resolve();
    } else {
      return this.keycloak.login({
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

  public bearerToken(): string | undefined {
    return this.keycloak.token;
  }

  public userId(): string | undefined {
    return this.keycloak.subject;
  }
}

const auth = new Auth();
export default auth;

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
    this.initialized = this.keycloak.init({})
  }

  public isAuthenticated(): boolean {
    return this.keycloak.authenticated || false;
  }

  public async loginIfRequired(): Promise<boolean> {
    await this.initialized;
    if (this.keycloak.authenticated) {
      return true;
    } else {
      return this.keycloak.login({}).then(() => true).catch(() => false);
    }
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

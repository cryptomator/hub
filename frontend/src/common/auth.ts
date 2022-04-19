import newKeycloak, { KeycloakInstance } from 'keycloak-js';
import config, { ConfigDto } from './config';

class Auth {
  private readonly keycloak: KeycloakInstance;

  static async build(cfg: ConfigDto): Promise<Auth> {
    const keycloak = newKeycloak({
      url: cfg.keycloakUrl,
      realm: cfg.keycloakRealm,
      clientId: cfg.keycloakClientId
    });
    await keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoFallback: false,
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      pkceMethod: 'S256',
    });
    keycloak.onTokenExpired = () => keycloak.updateToken(30);
    return new Auth(keycloak);
  }

  private constructor(keycloak: KeycloakInstance) {
    this.keycloak = keycloak;
  }

  public isAuthenticated(): boolean {
    return this.keycloak.authenticated || false;
  }

  public async loginIfRequired(redirectUri?: string): Promise<void> {
    if (!this.keycloak.authenticated) {
      await this.keycloak.login({
        redirectUri: (redirectUri ?? window.location) + '?login' // keycloak appends '&state=...' which confuses vue-router if there is no '?'
      });
    }
  }

  public async logout(redirectUri?: string): Promise<void> {
    return this.keycloak.logout({
      redirectUri: redirectUri
    });
  }

  public async bearerToken(): Promise<string | undefined> {
    await this.keycloak.updateToken(10);
    return this.keycloak.token;
  }

  public isAdmin(): boolean {
    return this.keycloak.tokenParsed?.realm_access?.roles.includes('admin') ?? false;
  }

  public isVaultOwner(): boolean {
    return this.keycloak.tokenParsed?.resource_access?.cryptomatorhub.roles.includes('vault-owner') ?? false;
  }

  public isUser(): boolean {
    return this.keycloak.tokenParsed?.realm_access?.roles.includes('user') ?? false;
  }

}

// this is a lazy singleton:
const instance: Promise<Auth> = (async () => {
  return await Auth.build(config.get());
})();

export default instance;

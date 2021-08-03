import axios, { AxiosResponse } from 'axios';
import Keycloak, { KeycloakInstance } from 'keycloak-js';
import './style.css';

export class Spi {

  private keycloak: KeycloakInstance;
  private initialized: Promise<boolean>;

  public constructor() {
    this.keycloak = Keycloak({
      url: 'http://localhost:8080/auth',
      realm: 'cryptomator',
      clientId: 'cryptomator-hub'
    });

    this.initialized = this.keycloak.init({ onLoad: 'login-required' })
  }

  public async createVault(uuid: string, name: string, masterkey: String, iterations: number, salt: String): Promise<AxiosResponse<any>> {
    await this.initialized;
    return axios.put('/vaults/', { uuid: uuid, name: name, masterKey: masterkey, costParam: iterations, salt: salt }, {
      headers: {
        'Authorization': 'Bearer ' + this.keycloak.token
      }
    })
     /*.catch(function (error) {
        console.log('refreshing');
        keycloak.updateToken(5).then(function () {
          console.log('Token refreshed');
        }).catch(function (error) {
          console.log('Failed to refresh token', error);
        });
      })*/;
  }
}

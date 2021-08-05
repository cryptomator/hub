import axios, { AxiosResponse } from 'axios';
import auth from './auth';

class VaultService {

  public async createVault(uuid: string, name: string, masterkey: String, iterations: number, salt: String): Promise<AxiosResponse<any>> {
    if (!auth.isAuthenticated()) {
      return Promise.reject('not logged in');
    }
    return axios.put('/vaults/' + uuid, { name: name, masterkey: masterkey, iterations: iterations, salt: salt }, {
      headers: {
        'Authorization': 'Bearer ' + auth.bearerToken()
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

const services = {
  vaults: new VaultService()
};

export default services;

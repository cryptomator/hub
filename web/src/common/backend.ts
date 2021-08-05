import axios, { AxiosResponse } from 'axios';
import auth from './auth';

const REST_BASE = import.meta.env.DEV ? 'http://localhost:9090' : '';

class VaultService {

  public async createVault(uuid: string, name: string, masterkey: String, iterations: number, salt: String): Promise<AxiosResponse<any>> {
    if (!auth.isAuthenticated()) {
      return Promise.reject('not logged in');
    }
    return auth.bearerToken().then(token => {
      return axios.put(REST_BASE + '/vaults/' + uuid, { name: name, masterkey: masterkey, iterations: iterations, salt: salt }, {
        headers: {
          'Authorization': 'Bearer ' + token
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
    }).catch(error => { return Promise.reject(error) })
  }
}

const services = {
  vaults: new VaultService()
};

export default services;

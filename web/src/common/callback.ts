import AxiosStatic, { AxiosInstance, AxiosResponse } from 'axios';

enum CallbackStatus {
  Success = 'SUCCESS',
}

export class CallbackService {
  readonly #axios: AxiosInstance;

  constructor(callbackBase: string) {
    this.#axios = AxiosStatic.create({
      baseURL: callbackBase,
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }

  public async unlockSuccess(deviceSpecificMasterkey: string, ephemeralPublicKey: string): Promise<AxiosResponse<any>> {
    return this.#axios.get('/', {
      params: {
        status: CallbackStatus.Success,
        m: deviceSpecificMasterkey,
        epk: ephemeralPublicKey
      }
    });
  }

}

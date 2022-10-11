import AxiosStatic from 'axios';

const axios = AxiosStatic.create();

export type LatestVersionDto = {
  stable: string;
  beta: string;
};

class UpdatesService {
  public async get(localVersion: string): Promise<LatestVersionDto> {
    let config = {
      headers: {
        'Content-Type': 'application/json',
        'Cryptomator-Hub-Version': localVersion,
        'Cryptomator-Hub-Instance': 'TODO' //for future uses
      }
    };
    return axios.get('https://api.cryptomator.org/updates/hub.json', config)
      .then(response => response.data)
      .catch(error => {
        console.error(error);
        throw new FetchUpdateError('Unable to get update info.');
      });
  }
}

export class FetchUpdateError extends Error {
  constructor(msg: string) {
    super(msg);
  }
}

export const updateChecker = new UpdatesService();

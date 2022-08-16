import AxiosStatic from 'axios';

const axios = AxiosStatic.create();

export class LatestVersionDto {
  constructor(public stable: string, public beta: string) { }
}

class UpdatesService {
  public async get(localVersion: string): Promise<LatestVersionDto> {
    let config = {
      headers: {
        'Content-Type': 'application/json',
        'X-Hub-Version': localVersion,
        'X-Hub-Instance': 'TODO' //for future uses
      }
    };
    return axios.get('https://api.cryptomator.org/updates/hub.json', config)
      .then(response => response.data)
      .catch(err => {
        console.error(err);
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

import { RouteLocationNormalized, RouteLocationRaw } from 'vue-router';
import backend from '../common/backend';

function fetchVaultListData(onErrorRoute: RouteLocationRaw) {
  return async function fetch(from: RouteLocationNormalized, to: RouteLocationNormalized): Promise<RouteLocationRaw | undefined> {
    try {
      to.meta.vaults = await backend.vaults.listSharedOrOwned();
      return undefined;
    } catch (error) {
      console.error('Retrieving vault list failed.', error);
      return onErrorRoute;
    }
  };
}

export default fetchVaultListData;

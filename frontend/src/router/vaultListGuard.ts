import { RouteLocationNormalized, RouteLocationRaw } from 'vue-router';
import backend, { VaultDto } from '../common/backend';

declare module 'vue-router' {
  interface RouteMeta {
    vaults?: VaultDto[]
  }
}
class VaultListGuard {
  public async fetch(to: RouteLocationNormalized): Promise<RouteLocationRaw | undefined> {
    try {
      to.meta.vaults = await backend.vaults.listSharedOrOwned();
    } catch (error) {
      console.error('Retrieving vault list failed.', error);
      return '/error';
    }
  }

  public copy(to: RouteLocationNormalized) {
    return { vaults: to.meta.vaults };
  }
}

const vaultListGuard = new VaultListGuard();

export default vaultListGuard;

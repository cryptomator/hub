import { RouteLocationNormalized, RouteLocationRaw } from 'vue-router';
import backend, { UserDto } from '../common/backend';

declare module 'vue-router' {
  interface RouteMeta {
    user?: UserDto
  }
}
class UnlockSuccessGuard {
  public async fetch(to: RouteLocationNormalized): Promise<RouteLocationRaw | undefined> {
    try {
      to.meta.user = await backend.users.me(true, true);
    } catch (error) {
      console.error('Retrieving user info failed.', error);
      return '/error';
    }
  }

  public copy(to: RouteLocationNormalized) {
    return ({ vaultId: to.query.vault, deviceId: to.query.device, me: to.meta.user });
  }
}

const unlockSuccessGuard = new UnlockSuccessGuard();

export default unlockSuccessGuard;

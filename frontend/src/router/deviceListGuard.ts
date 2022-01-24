import { RouteLocationNormalized, RouteLocationRaw } from 'vue-router';
import backend, { UserDto } from '../common/backend';

declare module 'vue-router' {
  interface RouteMeta {
    user?: UserDto
  }
}
class DeviceListGuard {
  public async fetch(to: RouteLocationNormalized): Promise<RouteLocationRaw | undefined> {
    try {
      to.meta.user = await backend.users.me(true, true);
    } catch (error) {
      console.error('Retrieving device list failed.', error);
      return '/error';
    }
  }

  public copy(to: RouteLocationNormalized) {
    return { user: to.meta.user };
  }
}

const deviceListGuard = new DeviceListGuard();

export default deviceListGuard;

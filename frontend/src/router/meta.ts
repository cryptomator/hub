import 'vue-router';
import { UserDto, VaultDto } from '../common/backend';

declare module 'vue-router' {
  interface RouteMeta {
    // is optional
    isAdmin?: boolean
    skipAuth?: boolean
    vaults?: VaultDto[]
    user?: UserDto[]
  }
}

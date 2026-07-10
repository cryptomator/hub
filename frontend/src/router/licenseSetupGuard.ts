import { RouteLocationNormalized, RouteLocationRaw } from 'vue-router';
import config from '../common/config';

/**
 * Redirects to the license setup view as long as this Hub instance has no license configured yet.
 * Routes with `meta.skipLicenseSetup` (the setup view itself, logout and error pages) are exempt.
 */
export function licenseSetupGuard(to: RouteLocationNormalized): boolean | RouteLocationRaw {
  if (to.meta.skipLicenseSetup) {
    return true;
  }
  if (config.get().licenseSetupRequired) {
    return { path: '/app/setup-license', replace: true };
  }
  return true;
}

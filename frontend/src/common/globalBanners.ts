import { ref } from 'vue';
import authPromise from './auth';
import backend, { LicenseUserInfoDto } from './backend';
import userdata from './userdata';

/**
 * Instance-wide state that is announced on top of every view, independent of the currently shown view.
 *
 * @see GlobalBanners.vue
 */
class GlobalBanners {

  public readonly isAdmin = ref(false);
  public readonly licenseStatus = ref<LicenseUserInfoDto>();

  /** @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333 */
  public readonly hasLegacyDevices = ref(false);
  /** @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333 */
  public readonly anyUserHasLegacyDevices = ref(false);

  /**
   * Reloads all instance-wide state from the backend.
   */
  public async refresh(): Promise<void> {
    this.isAdmin.value = (await authPromise).hasRole('admin');
    await Promise.all([
      this.refreshLicenseStatus(),
      this.refreshLegacyDevices()
    ]);
  }

  /**
   * Reloads the license status of the instance.
   */
  public async refreshLicenseStatus(): Promise<void> {
    this.licenseStatus.value = await backend.license.getUserInfo();
  }

  /**
   * Reloads whether the current user (or, for admins, any user) still has legacy devices.
   * @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333
   */
  public async refreshLegacyDevices(): Promise<void> {
    const me = await userdata.meWithLegacyDevicesAndLastAccess;
    this.hasLegacyDevices.value = (me.devices?.length ?? 0) > 0;
    this.anyUserHasLegacyDevices.value = this.isAdmin.value && await backend.devices.hasLegacyDevices();
  }

}

const instance = new GlobalBanners();
export default instance;

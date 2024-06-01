import { base64 } from 'rfc4648';
import backend, { DeviceDto, UserDto } from './backend';
import { BrowserKeys, UserKeys } from './crypto';
import { JWEParser } from './jwe';

class UserData {

  #me?: Promise<UserDto>;
  #browserKeys?: Promise<BrowserKeys | undefined>;

  /**
   * Gets the user DTO representing the currently logged in user.
   */
  public get me(): Promise<UserDto> {
    if (!this.#me) {
      this.#me = backend.users.me(true);
    }
    return this.#me;
  }

  /**
   * Gets the device key pair stored for this user in the currently used browser.
   */
  public get browserKeys(): Promise<BrowserKeys | undefined> {
    return this.me.then(me => {
      if (!this.#browserKeys) {
        this.#browserKeys = BrowserKeys.load(me.id);
      }
      return this.#browserKeys;
    });
  }

  /**
   * Gets the device that represents the currently used browser.
   */
  public get browser(): Promise<DeviceDto | undefined> {
    return this.me.then(async me => {
      const browserKeys = await this.browserKeys;
      const browserId = await browserKeys?.id();
      return browserId ? me.devices.find(d => d.id === browserId) : undefined;
    });
  }

  /**
   * Gets the ECDH public key of the user.
   * 
   * @see UserDto.ecdhPublicKey
   */
  public get ecdhPublicKey(): Promise<Uint8Array> {
    return this.me.then(me => {
      if (!me.ecdhPublicKey) {
        throw new Error('User not initialized.');
      }
      return base64.parse(me.ecdhPublicKey);
    });
  }

  /**
   * Gets the ECDSA public key of the user, if available.
   * 
   * @see UserDto.ecdsaPublicKey
   */
  public get ecdsaPublicKey(): Promise<Uint8Array | undefined> {
    return this.me.then(me => {
      if (me.ecdsaPublicKey) {
        return base64.parse(me.ecdsaPublicKey);
      } else {
        return undefined;
      }
    });
  }

  /**
   * Invalidates the cached user data and reloads it in the backend.
   */
  public async reload() {
    this.#me = backend.users.me(true);
    this.#browserKeys = undefined;
  }

  /**
   * Creates a new browser key pair for the user.
   * This does not change the device DTO stored in the backend.
   * @returns A new browser key pair for the user.
   */
  public async createBrowserKeys(): Promise<BrowserKeys> {
    const me = await this.me;
    const browserKeys = await BrowserKeys.create();
    await browserKeys.store(me.id);
    this.#browserKeys = Promise.resolve(browserKeys);
    return browserKeys;
  }

  /**
   * Decrypts the user keys using the setup code.
   * @param setupCode the setup code
   * @returns The user's key pairs
   */
  public async decryptUserKeysWithSetupCode(setupCode: string): Promise<UserKeys> {
    const me = await this.me;
    if (!me.privateKey) {
      throw new Error('User not initialized.');
    }
    const userKeys = await UserKeys.recover(me.privateKey, setupCode, await this.ecdhPublicKey, await this.ecdsaPublicKey);
    await this.addEcdsaKeyIfMissing(userKeys);
    return userKeys;
  }

  /**
   * Decrypts the user keys using the device key stored in the currently used browser.
   * @returns The user's key pairs
   */
  public async decryptUserKeysWithBrowser(): Promise<UserKeys> {
    const browserKeys = await this.browserKeys;
    if (!browserKeys) {
      throw new Error('Browser keys not found.');
    }
    const browser = await this.browser;
    if (!browser) {
      throw new Error('Device not initialized.');
    }
    const userKeys = await UserKeys.decryptOnBrowser(browser.userPrivateKey, browserKeys.keyPair.privateKey, await this.ecdhPublicKey, await this.ecdsaPublicKey);
    await this.addEcdsaKeyIfMissing(userKeys);
    return userKeys;
  }

  /**
   * Updates the stored user keys, if the ECDSA key was missing before (added in 1.4.0)
   * @param userKeys The user keys that contain the ECDSA key
   */
  private async addEcdsaKeyIfMissing(userKeys: UserKeys) {
    const me = await this.me;
    if (me.setupCode && !me.ecdsaPublicKey) {
      const payload: { setupCode: string } = await JWEParser.parse(me.setupCode).decryptEcdhEs(userKeys.ecdhKeyPair.privateKey);
      me.ecdsaPublicKey = await userKeys.encodedEcdsaPublicKey();
      me.privateKey = await userKeys.encryptWithSetupCode(payload.setupCode);
      for (const device of me.devices) {
        device.userPrivateKey = await userKeys.encryptForDevice(base64.parse(device.publicKey));
      }
      await backend.users.putMe(me);
    }
  }

}

const instance = new UserData();
export default instance;

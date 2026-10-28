import { base64 } from '@scure/base';
import backend, { DeviceDto, UserDto } from './backend';
import { BrowserKeys, UserKeys } from './crypto';
import { JWE, Recipient } from './jwe';

class UserData {

  #me?: Promise<UserDto>;
  #browserKeys?: Promise<BrowserKeys | undefined>;

  /** @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333 */
  #meWithLegacyDevicesAndLastAccess?: Promise<UserDto>;

  /**
   * Gets the user DTO representing the currently logged in user.
   */
  public get me(): Promise<UserDto> {
    if (!this.#me) {
      this.#me = backend.users.me(true);
    }
    return this.#me;
  }

  /** @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333 */
  public get meWithLegacyDevicesAndLastAccess(): Promise<UserDto> {
    if (!this.#meWithLegacyDevicesAndLastAccess) {
      this.#meWithLegacyDevicesAndLastAccess = backend.users.meWithLegacyDevicesAndAccess();
    }
    return this.#meWithLegacyDevicesAndLastAccess;
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
   * Gets the ECDH public key of the user, verified to belong to the private key decrypted in this browser.
   *
   * Use this (and never the server-supplied `UserDto.ecdhPublicKey`) whenever a secret is encrypted "to oneself", e.g. the
   * owner's access token of a new vault: the server is untrusted and could otherwise substitute an attacker's key.
   * @returns SPKI-encoded ECDH public key
   * @throws {KeyPairMismatchError} if the server-supplied public key does not match the decrypted private key
   */
  public get ecdhPublicKey(): Promise<Uint8Array<ArrayBuffer>> {
    return this.#verifiedEcdhPublicKey();
  }

  async #verifiedEcdhPublicKey(): Promise<Uint8Array<ArrayBuffer>> {
    const userKeys = await this.decryptUserKeysWithBrowser();
    return new Uint8Array(await crypto.subtle.exportKey('spki', userKeys.ecdhKeyPair.publicKey));
  }

  // the unverified ECDH public key as stored on the server; only to be paired (and thereby verified) with the decrypted private key
  get #claimedEcdhPublicKey(): Promise<Uint8Array<ArrayBuffer>> {
    return this.me.then(me => {
      if (!me.ecdhPublicKey) {
        throw new Error('User not initialized.');
      }
      return base64.decode(me.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
    });
  }

  // the unverified ECDSA public key as stored on the server (if available); only to be paired (and thereby verified) with the decrypted private key
  get #claimedEcdsaPublicKey(): Promise<Uint8Array<ArrayBuffer> | undefined> {
    return this.me.then(me => {
      return me.ecdsaPublicKey ? base64.decode(me.ecdsaPublicKey) as Uint8Array<ArrayBuffer> : undefined;
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
   * Invalidates the cached user data with legacy devices and last access and reloads it in the backend.
   * @deprecated since version 1.3.0, to be removed in https://github.com/cryptomator/hub/issues/333
   */
  public async reloadLegacyAccess() {
    this.#meWithLegacyDevicesAndLastAccess = backend.users.meWithLegacyDevicesAndAccess();
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
    if (!me.privateKeys) {
      throw new Error('User not initialized.');
    }
    const userKeys = await UserKeys.recover(me.privateKeys, setupCode, await this.#claimedEcdhPublicKey, await this.#claimedEcdsaPublicKey);
    await this.addEcdsaKeyIfMissing(userKeys);
    return userKeys;
  }

  /**
   * Decrypts the user keys using the device key stored in the currently used browser.
   * @returns The user's key pairs
   */
  public async decryptUserKeysWithBrowser(): Promise<UserKeys>;
  public async decryptUserKeysWithBrowser(browserKeys: BrowserKeys, browser: DeviceDto): Promise<UserKeys>;
  public async decryptUserKeysWithBrowser(browserKeys?: BrowserKeys, browser?: DeviceDto): Promise<UserKeys> {
    browserKeys ??= await this.browserKeys;
    if (!browserKeys) {
      throw new Error('Browser keys not found.');
    }
    browser ??= await this.browser;
    if (!browser) {
      throw new Error('Device not initialized.');
    }
    const userKeys = await UserKeys.decryptOnBrowser(browser.userPrivateKey, browserKeys.keyPair.privateKey, await this.#claimedEcdhPublicKey, await this.#claimedEcdsaPublicKey);
    await this.addEcdsaKeyIfMissing(userKeys, browserKeys, browser);
    return userKeys;
  }

  public async decryptSetupCode(userKeys: UserKeys): Promise<string> {
    const me = await this.me;
    if (me.setupCode) {
      const payload: { setupCode: string } = await JWE.parseCompact(me.setupCode).decrypt(Recipient.ecdhEs('org.cryptomator.hub.userkey', userKeys.ecdhKeyPair.privateKey));
      return payload.setupCode;
    } else {
      throw new Error('User not set up yet.');
    }
  }

  /**
   * Updates the stored user keys, if the ECDSA key was missing before (added in 1.4.0).
   *
   * The private keys are only re-wrapped for the currently used browser (whose public key is known locally). Other devices
   * listed by the server cannot be authenticated by the client, so wrapping the private keys to their alleged public keys
   * would let the server obtain them by adding a device of its own; they keep their old (ECDSA-less) key material instead.
   * @param userKeys The user keys that contain the ECDSA key
   * @param browserKeys The key pair of the currently used browser, if the user keys have been decrypted with it
   * @param browser The device representing the currently used browser, if the user keys have been decrypted with it
   */
  private async addEcdsaKeyIfMissing(userKeys: UserKeys, browserKeys?: BrowserKeys, browser?: DeviceDto) {
    const me = await this.me;
    if (me.setupCode && !me.ecdsaPublicKey) {
      const setupCode = await this.decryptSetupCode(userKeys);
      me.ecdsaPublicKey = await userKeys.encodedEcdsaPublicKey();
      me.privateKeys = await userKeys.encryptWithSetupCode(setupCode);
      if (browserKeys && browser) {
        const thisBrowser = me.devices.find(device => device.id === browser.id);
        if (thisBrowser) {
          thisBrowser.userPrivateKey = await userKeys.encryptForDevice(browserKeys.keyPair.publicKey);
        }
      }
      await backend.users.putMe(me);
    }
  }

}

const instance = new UserData();
export default instance;

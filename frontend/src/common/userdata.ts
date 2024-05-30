import { base64 } from 'rfc4648';
import backend, { DeviceDto, UserDto } from './backend';
import { BrowserKeys, UserKeys } from './crypto';

class UserData {

  #me?: Promise<UserDto>;
  #browserKeys?: Promise<BrowserKeys | undefined>;

  public get me(): Promise<UserDto> {
    if (!this.#me) {
      this.#me = backend.users.me(true);
    }
    return this.#me;
  }

  public get browserKeys(): Promise<BrowserKeys | undefined> {
    return this.me.then(me => {
      if (!this.#browserKeys) {
        this.#browserKeys = BrowserKeys.load(me.id);
      }
      return this.#browserKeys;
    });
  }

  public get browser(): Promise<DeviceDto | undefined> {
    return this.me.then(async me => {
      const browserKeys = await this.browserKeys;
      if (browserKeys == null) {
        return undefined;
      } else {
        const browserId = await browserKeys.id();
        return me.devices.find(d => d.id == browserId);
      }
    });
  }

  public async reload() {
    this.#me = backend.users.me(true);
    this.#browserKeys = undefined;
  }

  public async createBrowserKeys(): Promise<BrowserKeys> {
    const me = await this.me;
    const browserKeys = await BrowserKeys.create();
    await browserKeys.store(me.id);
    this.#browserKeys = Promise.resolve(browserKeys);
    return browserKeys;
  }

  public async decryptUserKeysWithSetupCode(setupCode: string): Promise<UserKeys> {
    const me = await this.me;
    if (!me.privateKey || !me.ecdhPublicKey) {
      throw new Error('User not initialized.');
    }
    const ecdhPublicKey = base64.parse(me.ecdhPublicKey);
    const ecdsaPublicKey = me.ecdsaPublicKey ? base64.parse(me.ecdsaPublicKey) : undefined;
    const userKeys = await UserKeys.recover(me.privateKey, setupCode, ecdhPublicKey, ecdsaPublicKey);
    if (!me.ecdsaPublicKey) { // Update user, if ECDSA key was missing before (added in 1.4.0)
      me.ecdsaPublicKey = await userKeys.encodedEcdsaPublicKey();
      await backend.users.putMe(me);
    }
    return userKeys;
  }

  public async decryptUserKeysWithBrowser(): Promise<UserKeys> {
    const me = await this.me;
    if (!me.ecdhPublicKey) {
      throw new Error('User not initialized.');
    }
    const browserKeys = await this.browserKeys;
    if (!browserKeys) {
      throw new Error('Browser keys not found.');
    }
    const browser = await this.browser;
    if (!browser) {
      throw new Error('Device not initialized.');
    }
    const ecdhPublicKey = base64.parse(me.ecdhPublicKey);
    const ecdsaPublicKey = me.ecdsaPublicKey ? base64.parse(me.ecdsaPublicKey) : undefined;
    const userKeys = await UserKeys.decryptOnBrowser(browser.userPrivateKey, browserKeys.keyPair.privateKey, ecdhPublicKey, ecdsaPublicKey);
    if (!me.ecdsaPublicKey) { // Update user, if ECDSA key was missing before (added in 1.4.0)
      me.ecdsaPublicKey = await userKeys.encodedEcdsaPublicKey();
      await backend.users.putMe(me);
    }
    return userKeys;
  }

}

const instance = new UserData();
export default instance;

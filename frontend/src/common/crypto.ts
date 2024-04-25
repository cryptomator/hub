import { base16, base64 } from 'rfc4648';
import { JWE, Recipient } from './jwe';
import { DB } from './util';


export class UnwrapKeyError extends Error {
  readonly actualError: any;

  constructor(actualError: any) {
    super('Unwrapping key failed');
    this.actualError = actualError;
  }
}

interface UserKeysJWEPayload {
  key: string
}

export const GCM_NONCE_LEN = 12;

export class UserKeys {
  public static readonly KEY_USAGES: KeyUsage[] = ['deriveBits'];

  public static readonly KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = {
    name: 'ECDH',
    namedCurve: 'P-384'
  };

  readonly keyPair: CryptoKeyPair;

  protected constructor(keyPair: CryptoKeyPair) {
    this.keyPair = keyPair;
  }

  /**
   * Creates a new user key pair
   * @returns A new user key pair
   */
  public static async create(): Promise<UserKeys> {
    const keyPair = crypto.subtle.generateKey(UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
    return new UserKeys(await keyPair);
  }

  /**
   * Recovers the user key pair using a recovery code. All other information can be retrieved from the backend.
   * @param encodedPublicKey The public key (base64-encoded SPKI)
   * @param encryptedPrivateKey The JWE holding the encrypted private key
   * @param setupCode The password used to protect the private key
   * @returns Decrypted UserKeys
   * @throws {UnwrapKeyError} when attempting to decrypt the private key using an incorrect setupCode
   */
  public static async recover(encodedPublicKey: string, encryptedPrivateKey: string, setupCode: string): Promise<UserKeys> {
    const jwe: UserKeysJWEPayload = await JWE.parseCompact(encryptedPrivateKey).decrypt(Recipient.pbes2('org.cryptomator.hub.setupCode', setupCode));
    const decodedPublicKey = base64.parse(encodedPublicKey, { loose: true });
    const decodedPrivateKey = base64.parse(jwe.key, { loose: true });
    const privateKey = crypto.subtle.importKey('pkcs8', decodedPrivateKey, UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
    const publicKey = crypto.subtle.importKey('spki', decodedPublicKey, UserKeys.KEY_DESIGNATION, true, []);
    return new UserKeys({ privateKey: await privateKey, publicKey: await publicKey });
  }

  /**
   * Gets the base64-encoded public key in SPKI format.
   * @returns base64-encoded public key
   */
  public async encodedPublicKey(): Promise<string> {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.keyPair.publicKey));
    return base64.stringify(publicKey);
  }

  /**
   * Encrypts the user's private key using a key derived from the given setupCode
   * @param setupCode The password to protect the private key.
   * @returns A JWE holding the encrypted private key
   * @see Recipient.pbes2
   */
  public async encryptedPrivateKey(setupCode: string): Promise<string> {
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.keyPair.privateKey));
    try {
      const payload: UserKeysJWEPayload = {
        key: base64.stringify(rawkey)
      };
      const jwe = await JWE.build(payload).encrypt(Recipient.pbes2('org.cryptomator.hub.setupCode', setupCode));
      return jwe.compactSerialization();
    } finally {
      rawkey.fill(0x00);
    }
  }

  /**
   * Encrypts the user's private key using the given public key
   * @param devicePublicKey The device's public key (DER-encoded)
   * @returns a JWE containing the PKCS#8-encoded private key
   * @see Recipient.ecdhEs
   */
  public async encryptForDevice(devicePublicKey: CryptoKey | Uint8Array): Promise<string> {
    const publicKey = await UserKeys.publicKey(devicePublicKey);
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.keyPair.privateKey));
    try {
      const payload: UserKeysJWEPayload = {
        key: base64.stringify(rawkey)
      };
      const jwe = await JWE.build(payload).encrypt(Recipient.ecdhEs('org.cryptomator.hub.deviceKey', publicKey));
      return jwe.compactSerialization();
    } finally {
      rawkey.fill(0x00);
    }
  }

  /**
   * Decrypts the user's private key using the browser's private key
   * @param jwe JWE containing the PKCS#8-encoded private key
   * @param browserPrivateKey The browser's private key
   * @param userPublicKey User public key
   * @returns The user's key pair
   */
  public static async decryptOnBrowser(jwe: string, browserPrivateKey: CryptoKey, userPublicKey: CryptoKey | BufferSource): Promise<UserKeys> {
    const publicKey = await UserKeys.publicKey(userPublicKey);
    let rawKey = new Uint8Array();
    try {
      const payload: UserKeysJWEPayload = await JWE.parseCompact(jwe).decrypt(Recipient.ecdhEs('org.cryptomator.hub.deviceKey', browserPrivateKey));
      rawKey = base64.parse(payload.key);
      const privateKey = await crypto.subtle.importKey('pkcs8', rawKey, UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
      return new UserKeys({ publicKey: publicKey, privateKey: privateKey });
    } finally {
      rawKey.fill(0x00);
    }
  }

  private static async publicKey(publicKey: CryptoKey | BufferSource): Promise<CryptoKey> {
    if (publicKey instanceof CryptoKey) {
      return publicKey;
    } else {
      return await crypto.subtle.importKey('spki', publicKey, UserKeys.KEY_DESIGNATION, true, []);
    }
  }
}

export class BrowserKeys {
  public static readonly KEY_USAGES: KeyUsage[] = ['deriveBits'];

  private static readonly KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = {
    name: 'ECDH',
    namedCurve: 'P-384'
  };

  readonly keyPair: CryptoKeyPair;

  protected constructor(keyPair: CryptoKeyPair) {
    this.keyPair = keyPair;
  }

  /**
   * Creates a new device key pair for this browser
   * @returns A new device key pair
   */
  public static async create(): Promise<BrowserKeys> {
    const keyPair = crypto.subtle.generateKey(BrowserKeys.KEY_DESIGNATION, false, BrowserKeys.KEY_USAGES);
    return new BrowserKeys(await keyPair);
  }

  /**
   * Attempts to load previously stored key pair from the browser's IndexedDB.
   * @returns a promise resolving to the loaded browser key pair
   */
  public static async load(userId: string): Promise<BrowserKeys | undefined> {
    const keyPair: CryptoKeyPair = await DB.transaction('keys', 'readonly', tx => {
      const keyStore = tx.objectStore('keys');
      return keyStore.get(userId);
    });
    if (keyPair) {
      return new BrowserKeys(keyPair);
    } else {
      return undefined;
    }
  }

  /**
   * Deletes the key pair for the given user.
   * @returns a promise resolving on success
   */
  public static async delete(userId: string): Promise<void> {
    await DB.transaction('keys', 'readwrite', tx => {
      const keyStore = tx.objectStore('keys');
      return keyStore.delete(userId);
    });
  }

  /**
   * Stores the key pair in the browser's IndexedDB. See https://www.w3.org/TR/WebCryptoAPI/#concepts-key-storage
   * @returns a promise that will resolve if the key pair has been saved
   */
  public async store(userId: string): Promise<void> {
    await DB.transaction('keys', 'readwrite', tx => {
      const keyStore = tx.objectStore('keys');
      return keyStore.put(this.keyPair, userId);
    });
  }

  public async id(): Promise<string> {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.keyPair.publicKey));
    const hash = new Uint8Array(await crypto.subtle.digest({ name: 'SHA-256' }, publicKey));
    return base16.stringify(hash).toUpperCase();
  }

  public async encodedPublicKey() {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.keyPair.publicKey));
    return base64.stringify(publicKey);
  }
}

export async function getFingerprint(key: string | undefined) {
  if (key) {
    const encodedKey = new TextEncoder().encode(key);
    const hashBuffer = await crypto.subtle.digest('SHA-256', encodedKey);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const hashHex = hashArray
      .map((b) => b.toString(16).padStart(2, '0').toUpperCase())
      .join('');
    return hashHex;
  }
}


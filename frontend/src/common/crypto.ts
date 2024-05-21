import { base16, base64, base64url } from 'rfc4648';
import { VaultDto } from './backend';
import { JWE, Recipient } from './jwe';
import { DB } from './util';

/**
 * Represents a JSON Web Key (JWK) as defined in RFC 7517.
 * @see https://datatracker.ietf.org/doc/html/rfc7517#section-5
 */
export type JsonWebKeySet = {
  keys: JsonWebKey & { kid?: string }[] // RFC defines kid, but webcrypto spec does not
}

export class UnwrapKeyError extends Error {
  readonly actualError: any;

  constructor(actualError: any) {
    super('Unwrapping key failed');
    this.actualError = actualError;
  }
}

export interface AccessTokenPayload {
  /**
   * The vault key (base64-encoded DER-formatted)
   */
  key: string,
  [key: string]: string | number | boolean | object | undefined
}

export const GCM_NONCE_LEN = 12;

export interface AccessTokenProducing {

  /**
   * Creates a user-specific access token for the given vault.
   * @param userPublicKey the public key of the user
   * @param isOwner whether to also include owner secrets for this user (UVF only)
   */
  encryptForUser(userPublicKey: CryptoKey | Uint8Array, isOwner?: boolean): Promise<string>;

}

export interface VaultTemplateProducing {

  /**
   * Produces a zip file containing the vault template.
   * @param apiURL absolute base URL of the API
   * @param vault The vault
   */
  exportTemplate(apiURL: string, vault: VaultDto): Promise<Blob>;

}

/**
 * Represents a vault member by their public key.
 */
export class OtherVaultMember {

  protected constructor(readonly publicKey: Promise<CryptoKey>) { }

  /**
   * Creates a new vault member with the given public key
   * @param publicKey The public key of the vault member
   * @returns A vault member with the given public key
   */
  public static withPublicKey(publicKey: CryptoKey | Uint8Array): OtherVaultMember {
    let keyPromise: Promise<CryptoKey>;
    if (publicKey instanceof Uint8Array) {
      keyPromise = crypto.subtle.importKey('spki', publicKey, UserKeys.KEY_DESIGNATION, false, []);
    } else {
      keyPromise = Promise.resolve(publicKey);
    }
    return new OtherVaultMember(keyPromise);
  }

  /**
   * Creates an access token for this vault member.
   * @param payload The payload to encrypt
   * @return A ECDH-ES encrypted JWE containing the encrypted payload
   */
  public async createAccessToken(payload: AccessTokenPayload): Promise<string> {
    const jwe = await JWE.build(payload).encrypt(Recipient.ecdhEs('org.cryptomator.hub.userkey', await this.publicKey));
    return jwe.compactSerialization();
  }

}

/**
 * The current user's key pair.
 */
export class UserKeys { // TODO: rename to CurrentUserKeyPair
  public static readonly KEY_USAGES: KeyUsage[] = ['deriveBits'];

  public static readonly KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = {
    name: 'ECDH',
    namedCurve: 'P-384'
  };
  protected constructor(readonly keyPair: CryptoKeyPair) { }

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
    const jwe: AccessTokenPayload = await JWE.parseCompact(encryptedPrivateKey).decrypt(Recipient.pbes2('org.cryptomator.hub.setupCode', setupCode));
    const decodedPublicKey = base64.parse(encodedPublicKey, { loose: true });
    const decodedPrivateKey = base64.parse(jwe.key, { loose: true });
    const privateKey = crypto.subtle.importKey('pkcs8', decodedPrivateKey, UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
    const publicKey = crypto.subtle.importKey('spki', decodedPublicKey, UserKeys.KEY_DESIGNATION, true, []);
    return new UserKeys({ privateKey: await privateKey, publicKey: await publicKey });
  }

  /**
   * Decrypts the access token using the user's private key
   * @param jwe The encrypted access token
   * @returns The token's payload
   */
  public async decryptAccessToken(jwe: string): Promise<AccessTokenPayload> {
    const payload = await JWE.parseCompact(jwe).decrypt(Recipient.ecdhEs('org.cryptomator.hub.userkey', this.keyPair.privateKey));
    return payload;
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
   * @param p2c Optional number of iterations for PBKDF2.
   * @returns A JWE holding the encrypted private key
   * @see Recipient.pbes2
   */
  public async encryptedPrivateKey(setupCode: string, p2c?: number): Promise<string> {
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.keyPair.privateKey));
    try {
      const payload: AccessTokenPayload = {
        key: base64.stringify(rawkey)
      };
      const jwe = await JWE.build(payload).encrypt(Recipient.pbes2('org.cryptomator.hub.setupCode', setupCode, p2c));
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
      const payload: AccessTokenPayload = {
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
      const payload: AccessTokenPayload = await JWE.parseCompact(jwe).decrypt(Recipient.ecdhEs('org.cryptomator.hub.deviceKey', browserPrivateKey));
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

/**
 * Computes the JWK Thumbprint (RFC 7638) using SHA-256.
 * @param key A key to compute the thumbprint for
 * @throws Error if the key is not supported
 */
export async function getJwkThumbprint(key: JsonWebKey | CryptoKey): Promise<string> {
  let jwk: JsonWebKey;
  if (key instanceof CryptoKey) {
    jwk = await crypto.subtle.exportKey('jwk', key);
  } else {
    jwk = key;
  }
  // see https://datatracker.ietf.org/doc/html/rfc7638#section-3.2
  let orderedJson: string;
  switch (jwk.kty) {
    case 'EC':
      orderedJson = `{"crv":"${jwk.crv}","kty":"${jwk.kty}","x":"${jwk.x}","y":"${jwk.y}"}`;
      break;
    case 'RSA':
      orderedJson = `{"e":"${jwk.e}","kty":"${jwk.kty}","n":"${jwk.n}"}`;
      break;
    case 'oct':
      orderedJson = `{"k":"${jwk.k}","kty":"${jwk.kty}"}`;
      break;
    default: throw new Error('Unsupported key type');
  }
  const bytes = new TextEncoder().encode(orderedJson);
  const hashBuffer = await crypto.subtle.digest('SHA-256', bytes);
  return base64url.stringify(new Uint8Array(hashBuffer), { pad: false });
}

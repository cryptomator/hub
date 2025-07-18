import { base16, base64, base64url } from 'rfc4648';
import { VaultDto } from './backend';
import { JWE, Recipient } from './jwe';
import { DB, UTF8 } from './util';

/**
 * Represents a JSON Web Key (JWK) as defined in RFC 7517.
 * @see https://datatracker.ietf.org/doc/html/rfc7517#section-5
 */
export type JsonWebKeySet = {
  keys: JsonWebKey & { kid?: string }[] // RFC defines kid, but webcrypto spec does not
}

export class UnwrapKeyError extends Error {
  readonly actualError: unknown;

  constructor(actualError: unknown) {
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

interface UserKeyPayload extends AccessTokenPayload {
  /**
   * @deprecated use `ecdhPrivateKey` instead
   */
  key: string,
  ecdhPrivateKey: string,
  ecdsaPrivateKey: string,
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
  public static withPublicKey(publicKey: CryptoKey | BufferSource): OtherVaultMember {
    const keyPromise = asPublicKey(publicKey, UserKeys.ECDH_KEY_DESIGNATION);
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
export class UserKeys {
  public static readonly ECDH_PRIV_KEY_USAGES: KeyUsage[] = ['deriveBits'];

  public static readonly ECDH_KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = { name: 'ECDH', namedCurve: 'P-384' };

  public static readonly ECDSA_PRIV_KEY_USAGES: KeyUsage[] = ['sign'];

  public static readonly ECDSA_PUB_KEY_USAGES: KeyUsage[] = ['verify'];

  public static readonly ECDSA_KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = { name: 'ECDSA', namedCurve: 'P-384' };

  protected constructor(readonly ecdhKeyPair: CryptoKeyPair, readonly ecdsaKeyPair: CryptoKeyPair) { }

  /**
   * Creates new user key pairs
   * @returns A set of new user key pairs
   */
  public static async create(): Promise<UserKeys> {
    const ecdhKeyPair = crypto.subtle.generateKey(UserKeys.ECDH_KEY_DESIGNATION, true, UserKeys.ECDH_PRIV_KEY_USAGES);
    const ecdsaKeyPair = crypto.subtle.generateKey(UserKeys.ECDSA_KEY_DESIGNATION, true, UserKeys.ECDSA_PRIV_KEY_USAGES);
    return new UserKeys(await ecdhKeyPair, await ecdsaKeyPair);
  }

  /**
   * Recovers the user key pairs using a recovery code. All other information can be retrieved from the backend.
   * @param encodedEcdhPublicKey The ECDH public key (base64-encoded SPKI)
   * @param encodedEcdsaPublicKey The ECDSA public key (base64-encoded SPKI)
   * @param privateKeys The JWE holding the encrypted private keys
   * @param setupCode The password used to protect the private keys
   * @returns Decrypted UserKeys
   * @throws {UnwrapKeyError} when attempting to decrypt the private key using an incorrect setupCode
   */
  public static async recover(privateKeys: string, setupCode: string, userEcdhPublicKey: CryptoKey | BufferSource, userEcdsaPublicKey?: CryptoKey | BufferSource): Promise<UserKeys> {
    const jwe: UserKeyPayload = await JWE.parseCompact(privateKeys).decrypt(Recipient.pbes2('org.cryptomator.hub.setupCode', setupCode));
    return UserKeys.createFromJwe(jwe, userEcdhPublicKey, userEcdsaPublicKey);
  }

  /**
   * Decrypts the user's private key using the browser's private key
   * @param jwe JWE containing the PKCS#8-encoded private key
   * @param browserPrivateKey The browser's private key
   * @param userEcdhPublicKey User's public ECDH key
   * @param userEcdsaPublicKey User's public ECDSA key (will be generated if missing - added in Hub 1.4.0)
   * @returns The user's key pair
   */
  public static async decryptOnBrowser(jwe: string, browserPrivateKey: CryptoKey, userEcdhPublicKey: CryptoKey | BufferSource, userEcdsaPublicKey?: CryptoKey | BufferSource): Promise<UserKeys> {
    const payload: UserKeyPayload = await JWE.parseCompact(jwe).decrypt(Recipient.ecdhEs('org.cryptomator.hub.deviceKey', browserPrivateKey));
    return UserKeys.createFromJwe(payload, userEcdhPublicKey, userEcdsaPublicKey);
  }

  private static async createFromJwe(jwe: UserKeyPayload, ecdhPublicKey: CryptoKey | BufferSource, ecdsaPublicKey?: CryptoKey | BufferSource): Promise<UserKeys> {
    const ecdhKeyPair: CryptoKeyPair = {
      publicKey: await asPublicKey(ecdhPublicKey, UserKeys.ECDH_KEY_DESIGNATION),
      privateKey: await crypto.subtle.importKey('pkcs8', base64.parse(jwe.ecdhPrivateKey ?? jwe.key, { loose: true }), UserKeys.ECDH_KEY_DESIGNATION, true, UserKeys.ECDH_PRIV_KEY_USAGES)
    };
    let ecdsaKeyPair: CryptoKeyPair;
    if (jwe.ecdsaPrivateKey && ecdsaPublicKey) {
      ecdsaKeyPair = {
        publicKey: await asPublicKey(ecdsaPublicKey, UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES),
        privateKey: await crypto.subtle.importKey('pkcs8', base64.parse(jwe.ecdsaPrivateKey, { loose: true }), UserKeys.ECDSA_KEY_DESIGNATION, true, UserKeys.ECDSA_PRIV_KEY_USAGES)
      };
    } else {
      // ECDSA key was added in Hub 1.4.0. If it's missing, we generate a new one.
      ecdsaKeyPair = await crypto.subtle.generateKey(UserKeys.ECDSA_KEY_DESIGNATION, true, UserKeys.ECDSA_PRIV_KEY_USAGES);
    }
    return new UserKeys(ecdhKeyPair, ecdsaKeyPair);
  }

  /**
   * Gets the base64-encoded ECDH public key in SPKI format.
   * @returns base64-encoded public key
   */
  public async encodedEcdhPublicKey(): Promise<string> {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.ecdhKeyPair.publicKey));
    return base64.stringify(publicKey);
  }

  /**
   * Gets the base64-encoded ECDSA public key in SPKI format.
   * @returns base64-encoded public key
   */
  public async encodedEcdsaPublicKey(): Promise<string> {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.ecdsaKeyPair.publicKey));
    return base64.stringify(publicKey);
  }

  /**
   * Encrypts the user's private key using a key derived from the given setupCode
   * @param setupCode The password to protect the private key.
   * @param p2c Optional number of iterations for PBKDF2.
   * @returns A JWE holding the encrypted private key
   * @see Recipient.pbes2
   */
  public async encryptWithSetupCode(setupCode: string, p2c?: number): Promise<string> {
    const payload = await this.prepareForEncryption();
    const jwe = await JWE.build(payload).encrypt(Recipient.pbes2('org.cryptomator.hub.setupCode', setupCode, p2c));
    return jwe.compactSerialization();
  }

  /**
   * Encrypts the user's private key using the given public key
   * @param devicePublicKey The device's public key (DER-encoded)
   * @returns a JWE containing the PKCS#8-encoded private key
   * @see Recipient.ecdhEs
   */
  public async encryptForDevice(devicePublicKey: CryptoKey | Uint8Array): Promise<string> {
    const publicKey = await asPublicKey(devicePublicKey, BrowserKeys.KEY_DESIGNATION);
    const payload = await this.prepareForEncryption();
    const jwe = await JWE.build(payload).encrypt(Recipient.ecdhEs('org.cryptomator.hub.deviceKey', publicKey));
    return jwe.compactSerialization();
  }

  /**
   * Decrypts the access token using the user's ECDH private key
   * @param jwe The encrypted access token
   * @returns The token's payload
   */
  public async decryptAccessToken(jwe: string): Promise<AccessTokenPayload> {
    const payload = await JWE.parseCompact(jwe).decrypt(Recipient.ecdhEs('org.cryptomator.hub.userkey', this.ecdhKeyPair.privateKey));
    return payload;
  }

  private async prepareForEncryption(): Promise<UserKeyPayload> {
    const encodedEcdhPrivateKey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.ecdhKeyPair.privateKey));
    const encodedEcdsaPrivateKey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.ecdsaKeyPair.privateKey));
    try {
      return {
        key: base64.stringify(encodedEcdhPrivateKey), // redundant for backwards compatibility
        ecdhPrivateKey: base64.stringify(encodedEcdhPrivateKey),
        ecdsaPrivateKey: base64.stringify(encodedEcdsaPrivateKey)
      };
    } finally {
      encodedEcdhPrivateKey.fill(0x00);
      encodedEcdsaPrivateKey.fill(0x00);
    }
  }
}

export class BrowserKeys {
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

export async function asPublicKey(publicKey: CryptoKey | BufferSource, keyDesignation: EcKeyImportParams, keyUsages: KeyUsage[] = []): Promise<CryptoKey> {
  if (publicKey instanceof CryptoKey) {
    return publicKey;
  } else {
    return await crypto.subtle.importKey('spki', publicKey, keyDesignation, true, keyUsages);
  }
}

/**
 * Computes the JWK Thumbprint (RFC 7638) using SHA-256.
 * @param key A key to compute the thumbprint for
 * @returns The thumbprint as a base64url-encoded string
 * @throws Error if the key is not supported
 */
export async function getJwkThumbprintStr(key: JsonWebKey | CryptoKey): Promise<string> {
  const thumbprint = await getJwkThumbprint(key);
  return base64url.stringify(new Uint8Array(thumbprint), { pad: false });
}

/**
 * Computes the JWK Thumbprint (RFC 7638) using SHA-256.
 * @param key A key to compute the thumbprint for
 * @returns The thumbprint as a Uint8Array
 * @throws Error if the key is not supported
 */
export async function getJwkThumbprint(key: JsonWebKey | CryptoKey): Promise<Uint8Array> {
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
  const bytes = UTF8.encode(orderedJson);
  return new Uint8Array(await crypto.subtle.digest('SHA-256', bytes));
}

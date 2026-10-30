import { VaultDto } from './backend';
import { base16, base64, base64urlnopad } from '@scure/base';
import { JWE, Recipient } from './jwe';
import { DB, UTF8 } from './util';

/**
 * Represents a JSON Web Key (JWK) as defined in RFC 7517.
 * @see https://datatracker.ietf.org/doc/html/rfc7517#section-5
 */
export type JsonWebKeySet = {
  keys: JsonWebKey & { kid?: string }[] // RFC defines kid, but webcrypto spec does not
};

export class UnwrapKeyError extends Error {

  readonly actualError: unknown;

  constructor(actualError: unknown) {
    super('Unwrapping key failed');
    this.actualError = actualError;
  }

}

/**
 * Thrown when a public key does not belong to the private key it is supposed to be paired with.
 */
export class KeyPairMismatchError extends Error {

  constructor(keyDescription: string) {
    super(`${keyDescription} public key does not match the private key`);
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
  encryptForUser(userPublicKey: CryptoKey | BufferSource, isOwner?: boolean): Promise<string>;

}

export interface VaultTemplateProducing {

  /**
   * Produces a zip file containing the vault template.
   * @param apiURL absolute base URL of the API
   * @param vault The vault
   */
  exportTemplate(apiURL: string, vault: VaultDto): Promise<Blob>;

}

export interface RecoveryKeyProducing {

  /**
   * Creates a recovery key and pads it to a multiple of 3 bytes (if necessary), so it can be safely encoded using both base64 and the word encoder.
   * @return The recovery key as a byte array
   */
  createPaddedRecoveryKeyBytes(): Promise<Uint8Array>;

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
    return JWE.build(payload).withRecipients(Recipient.ecdhEs('org.cryptomator.hub.userkey', await this.publicKey)).toCompact();
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

  /**
   * Pairs the decrypted private keys with the public keys the server claims to belong to them.
   * @throws {KeyPairMismatchError} if a public key does not belong to the corresponding private key
   */
  private static async createFromJwe(jwe: UserKeyPayload, ecdhPublicKey: CryptoKey | BufferSource, ecdsaPublicKey?: CryptoKey | BufferSource): Promise<UserKeys> {
    const ecdhKeyPair: CryptoKeyPair = {
      publicKey: await asPublicKey(ecdhPublicKey, UserKeys.ECDH_KEY_DESIGNATION),
      privateKey: await crypto.subtle.importKey('pkcs8', base64.decode(jwe.ecdhPrivateKey ?? jwe.key) as Uint8Array<ArrayBuffer>, UserKeys.ECDH_KEY_DESIGNATION, true, UserKeys.ECDH_PRIV_KEY_USAGES)
    };
    await UserKeys.assertMatchingKeyPair(ecdhKeyPair, 'ECDH');
    let ecdsaKeyPair: CryptoKeyPair;
    if (jwe.ecdsaPrivateKey) {
      if (!ecdsaPublicKey) {
        // the user demonstrably has an ECDSA key pair, so a missing public key means the server dropped it (which would otherwise trigger the key generation below and a re-wrap of the private keys):
        throw new KeyPairMismatchError('ECDSA');
      }
      ecdsaKeyPair = {
        publicKey: await asPublicKey(ecdsaPublicKey, UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES),
        privateKey: await crypto.subtle.importKey('pkcs8', base64.decode(jwe.ecdsaPrivateKey) as Uint8Array<ArrayBuffer>, UserKeys.ECDSA_KEY_DESIGNATION, true, UserKeys.ECDSA_PRIV_KEY_USAGES)
      };
      await UserKeys.assertMatchingKeyPair(ecdsaKeyPair, 'ECDSA');
    } else if (ecdsaPublicKey) {
      // the user already has a published ECDSA key pair (added in another browser), which this JWE predates. Generating
      // a new pair here would not match the published one, so the device needs to be re-initialized with the setup code:
      throw new KeyPairMismatchError('ECDSA');
    } else {
      // ECDSA key was added in Hub 1.4.0. If it's missing, we generate a new one.
      ecdsaKeyPair = await crypto.subtle.generateKey(UserKeys.ECDSA_KEY_DESIGNATION, true, UserKeys.ECDSA_PRIV_KEY_USAGES);
    }
    return new UserKeys(ecdhKeyPair, ecdsaKeyPair);
  }

  // The public keys are supplied by the server, which is untrusted in Hub's zero-knowledge model. Comparing the EC point of
  // both keys ensures the server cannot pair the user's genuine private keys with a foreign public key, which would otherwise
  // make the client wrap secrets "to oneself" for whoever holds the matching private key. Note that this only ties the public
  // key to the private key that was decrypted; it does not by itself authenticate the private key's origin (the setup-code
  // path does, as the setup code is a secret the server never sees; the device-key path relies on the stored device JWE).
  private static async assertMatchingKeyPair(keyPair: CryptoKeyPair, keyDescription: string): Promise<void> {
    if (!await sameEcPoint(keyPair.privateKey, keyPair.publicKey)) {
      throw new KeyPairMismatchError(keyDescription);
    }
  }

  /**
   * Gets the base64-encoded ECDH public key in SPKI format.
   * @returns base64-encoded public key
   */
  public async encodedEcdhPublicKey(): Promise<string> {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.ecdhKeyPair.publicKey));
    return base64.encode(publicKey);
  }

  /**
   * Gets the base64-encoded ECDSA public key in SPKI format.
   * @returns base64-encoded public key
   */
  public async encodedEcdsaPublicKey(): Promise<string> {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.ecdsaKeyPair.publicKey));
    return base64.encode(publicKey);
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
    return JWE.build(payload).withRecipients(Recipient.pbes2('org.cryptomator.hub.setupCode', setupCode, p2c)).toCompact();
  }

  /**
   * Encrypts the user's private key using the given public key
   * @param devicePublicKey The device's public key (DER-encoded)
   * @returns a JWE containing the PKCS#8-encoded private key
   * @see Recipient.ecdhEs
   */
  public async encryptForDevice(devicePublicKey: CryptoKey | Uint8Array<ArrayBuffer>): Promise<string> {
    const publicKey = await asPublicKey(devicePublicKey, BrowserKeys.KEY_DESIGNATION);
    const payload = await this.prepareForEncryption();
    return JWE.build(payload).withRecipients(Recipient.ecdhEs('org.cryptomator.hub.deviceKey', publicKey)).toCompact();
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
        key: base64.encode(encodedEcdhPrivateKey), // redundant for backwards compatibility
        ecdhPrivateKey: base64.encode(encodedEcdhPrivateKey),
        ecdsaPrivateKey: base64.encode(encodedEcdsaPrivateKey)
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
    const keyPair: CryptoKeyPair = await DB.transaction(DB.BROWSER_KEYS_STORE, 'readonly', tx => {
      const keyStore = tx.objectStore(DB.BROWSER_KEYS_STORE);
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
    await DB.transaction(DB.BROWSER_KEYS_STORE, 'readwrite', tx => {
      const keyStore = tx.objectStore(DB.BROWSER_KEYS_STORE);
      return keyStore.delete(userId);
    });
  }

  /**
   * Stores the key pair in the browser's IndexedDB. See https://www.w3.org/TR/WebCryptoAPI/#concepts-key-storage
   * @returns a promise that will resolve if the key pair has been saved
   */
  public async store(userId: string): Promise<void> {
    await DB.transaction(DB.BROWSER_KEYS_STORE, 'readwrite', tx => {
      const keyStore = tx.objectStore(DB.BROWSER_KEYS_STORE);
      return keyStore.put(this.keyPair, userId);
    });
  }

  public async id(): Promise<string> {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.keyPair.publicKey));
    const hash = new Uint8Array(await crypto.subtle.digest({ name: 'SHA-256' }, publicKey));
    return base16.encode(hash);
  }

  public async encodedPublicKey() {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.keyPair.publicKey));
    return base64.encode(publicKey);
  }

}

/**
 * The user's own public keys as they were known at a moment they were authentic, pinned in the browser's IndexedDB.
 *
 * The device-key path (`UserKeys.decryptOnBrowser`) decrypts a JWE that is stored on the untrusted server and encrypted to a
 * device public key that is also stored on the server. A malicious server could therefore replace the user's entire key pair.
 * Pinning the public keys at setup time (freshly generated or decrypted with the setup code, which the server never sees)
 * and comparing every subsequently unlocked key pair against the pin detects such a substitution. Browsers set up before
 * pinning existed pin the keys of their first unlock after the upgrade instead (trust on first use).
 */
export class PinnedUserKeys {

  protected constructor(readonly ecdhPublicKey: CryptoKey, readonly ecdsaPublicKey: CryptoKey) { }

  /**
   * Pins the public keys of the given user keys, which must stem from an authenticated source.
   * @param userKeys The user keys to pin
   * @returns The pinned public keys (not yet stored)
   */
  public static of(userKeys: UserKeys): PinnedUserKeys {
    return new PinnedUserKeys(userKeys.ecdhKeyPair.publicKey, userKeys.ecdsaKeyPair.publicKey);
  }

  /**
   * Attempts to load previously pinned public keys from the browser's IndexedDB.
   * @param userId The user whose keys to load
   * @returns The pinned public keys, if any
   */
  public static async load(userId: string): Promise<PinnedUserKeys | undefined> {
    const pinned: { ecdhPublicKey: CryptoKey, ecdsaPublicKey: CryptoKey } | undefined = await DB.transaction(DB.PINNED_USER_KEYS_STORE, 'readonly', tx => {
      return tx.objectStore(DB.PINNED_USER_KEYS_STORE).get(userId);
    });
    return pinned ? new PinnedUserKeys(pinned.ecdhPublicKey, pinned.ecdsaPublicKey) : undefined;
  }

  /**
   * Deletes the pinned public keys of the given user.
   * @param userId The user whose pin to delete
   */
  public static async delete(userId: string): Promise<void> {
    await DB.transaction(DB.PINNED_USER_KEYS_STORE, 'readwrite', tx => {
      return tx.objectStore(DB.PINNED_USER_KEYS_STORE).delete(userId);
    });
  }

  /**
   * Stores the pinned public keys in the browser's IndexedDB, replacing any previous pin of the user.
   * @param userId The user whose keys are pinned
   */
  public async store(userId: string): Promise<void> {
    await DB.transaction(DB.PINNED_USER_KEYS_STORE, 'readwrite', tx => {
      return tx.objectStore(DB.PINNED_USER_KEYS_STORE).put({ ecdhPublicKey: this.ecdhPublicKey, ecdsaPublicKey: this.ecdsaPublicKey }, userId);
    });
  }

  /**
   * Asserts that the given (freshly unlocked) user keys are the pinned ones.
   * @param userKeys The user keys to check
   * @throws {KeyPairMismatchError} if either public key differs from the pinned one
   */
  public async assertMatches(userKeys: UserKeys): Promise<void> {
    if (!await sameEcPoint(this.ecdhPublicKey, userKeys.ecdhKeyPair.publicKey)) {
      throw new KeyPairMismatchError('Pinned ECDH');
    }
    if (!await sameEcPoint(this.ecdsaPublicKey, userKeys.ecdsaKeyPair.publicKey)) {
      throw new KeyPairMismatchError('Pinned ECDSA');
    }
  }

}

// compares the EC points of two (public or private) EC keys; both keys must be extractable
async function sameEcPoint(a: CryptoKey, b: CryptoKey): Promise<boolean> {
  const [jwkA, jwkB] = await Promise.all([crypto.subtle.exportKey('jwk', a), crypto.subtle.exportKey('jwk', b)]);
  return jwkA.crv === jwkB.crv && jwkA.x === jwkB.x && jwkA.y === jwkB.y;
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
  return base64urlnopad.encode(new Uint8Array(thumbprint));
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

import * as miscreant from 'miscreant';
import { base16, base32, base64, base64url } from 'rfc4648';
import { JWEBuilder, JWEParser } from './jwe';
import { CRC32, DB, wordEncoder } from './util';
export class UnwrapKeyError extends Error {
  readonly actualError: unknown;

  constructor(actualError: unknown) {
    super('Unwrapping key failed');
    this.actualError = actualError;
  }
}

export interface VaultConfigPayload {
  jti: string
  format: number
  cipherCombo: string
  shorteningThreshold: number
}

export interface VaultConfigHeaderHub {
  clientId: string
  authEndpoint: string
  tokenEndpoint: string
  authSuccessUrl: string
  authErrorUrl: string
  apiBaseUrl: string
  // deprecated:
  devicesResourceUrl: string
}

interface JWEPayload {
  key: string
}

interface UserKeyPayload {
  /**
   * @deprecated use `ecdhPrivateKey` instead
   */
  key: string,
  ecdhPrivateKey: string,
  ecdsaPrivateKey: string,
}

const GCM_NONCE_LEN = 12;

export class VaultKeys {
  // in this browser application, this 512 bit key is used
  // as a hmac key to sign the vault config.
  // however when used by cryptomator, it gets split into
  // a 256 bit encryption key and a 256 bit mac key
  private static readonly MASTERKEY_KEY_DESIGNATION: HmacImportParams | HmacKeyGenParams = {
    name: 'HMAC',
    hash: 'SHA-256',
    length: 512
  };

  readonly masterKey: CryptoKey;

  protected constructor(masterkey: CryptoKey) {
    this.masterKey = masterkey;
  }

  /**
   * Creates a new masterkey
   * @returns A new masterkey
   */
  public static async create(): Promise<VaultKeys> {
    const key = crypto.subtle.generateKey(
      VaultKeys.MASTERKEY_KEY_DESIGNATION,
      true,
      ['sign']
    );
    return new VaultKeys(await key);
  }

  /**
   * Decrypts the vault's masterkey using the user's private key
   * @param jwe JWE containing the vault key
   * @param userPrivateKey The user's private key
   * @returns The masterkey
   */
  public static async decryptWithUserKey(jwe: string, userPrivateKey: CryptoKey): Promise<VaultKeys> {
    let rawKey = new Uint8Array();
    try {
      const payload: JWEPayload = await JWEParser.parse(jwe).decryptEcdhEs(userPrivateKey);
      rawKey = base64.parse(payload.key);
      const masterkey = crypto.subtle.importKey('raw', rawKey, VaultKeys.MASTERKEY_KEY_DESIGNATION, true, ['sign']);
      return new VaultKeys(await masterkey);
    } finally {
      rawKey.fill(0x00);
    }
  }

  /**
   * Unwraps keys protected by the legacy "Vault Admin Password".
   * @param vaultAdminPassword Vault Admin Password
   * @param wrappedMasterkey The wrapped masterkey
   * @param wrappedOwnerPrivateKey The wrapped owner private key
   * @param ownerPublicKey The owner public key
   * @param salt PBKDF2 Salt
   * @param iterations PBKDF2 Iterations
   * @returns The unwrapped key material.
   * @throws WrongPasswordError, if the wrong password is used
   * @deprecated Only used during "claim vault ownership" workflow for legacy vaults
   */
  public static async decryptWithAdminPassword(vaultAdminPassword: string, wrappedMasterkey: string, wrappedOwnerPrivateKey: string, ownerPublicKey: string, salt: string, iterations: number): Promise<[VaultKeys, CryptoKeyPair]> {
    // pbkdf2:
    const encodedPw = new TextEncoder().encode(vaultAdminPassword);
    const pwKey = crypto.subtle.importKey('raw', encodedPw, 'PBKDF2', false, ['deriveKey']);
    const kek = crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        hash: 'SHA-256',
        salt: base64.parse(salt, { loose: true }),
        iterations: iterations
      },
      await pwKey,
      { name: 'AES-GCM', length: 256 },
      false,
      ['unwrapKey']
    );
    // unwrapping
    const decodedMasterKey = base64.parse(wrappedMasterkey, { loose: true });
    const decodedPrivateKey = base64.parse(wrappedOwnerPrivateKey, { loose: true });
    const decodedPublicKey = base64.parse(ownerPublicKey, { loose: true });
    try {
      const masterkey = crypto.subtle.unwrapKey(
        'raw',
        decodedMasterKey.slice(GCM_NONCE_LEN),
        await kek,
        { name: 'AES-GCM', iv: decodedMasterKey.slice(0, GCM_NONCE_LEN) },
        VaultKeys.MASTERKEY_KEY_DESIGNATION,
        true,
        ['sign']
      );
      const privKey = crypto.subtle.unwrapKey(
        'pkcs8',
        decodedPrivateKey.slice(GCM_NONCE_LEN),
        await kek,
        { name: 'AES-GCM', iv: decodedPrivateKey.slice(0, GCM_NONCE_LEN) },
        { name: 'ECDSA', namedCurve: 'P-384' },
        false,
        ['sign']
      );
      const pubKey = crypto.subtle.importKey(
        'spki',
        decodedPublicKey,
        { name: 'ECDSA', namedCurve: 'P-384' },
        true,
        ['verify']
      );
      return [new VaultKeys(await masterkey), { privateKey: await privKey, publicKey: await pubKey }];
    } catch (error) {
      throw new UnwrapKeyError(error);
    }
  }

  /**
   * Restore the master key from a given recovery key, create a new admin signature key pair.
   * @param recoveryKey The recovery key
   * @returns The recovered master key
   * @throws Error, if passing a malformed recovery key
   */
  public static async recover(recoveryKey: string): Promise<VaultKeys> {
    // decode and check recovery key:
    const decoded = wordEncoder.decode(recoveryKey);
    if (decoded.length !== 66) {
      throw new Error('Invalid recovery key length.');
    }
    const decodedKey = decoded.subarray(0, 64);
    const crc32 = CRC32.compute(decodedKey);
    if (decoded[64] !== (crc32 & 0xFF)
      || decoded[65] !== (crc32 >> 8 & 0xFF)) {
      throw new Error('Invalid recovery key checksum.');
    }

    // construct new VaultKeys from recovered key
    const key = crypto.subtle.importKey(
      'raw',
      decodedKey,
      VaultKeys.MASTERKEY_KEY_DESIGNATION,
      true,
      ['sign']
    );
    return new VaultKeys(await key);
  }

  public async createVaultConfig(kid: string, hubConfig: VaultConfigHeaderHub, payload: VaultConfigPayload): Promise<string> {
    const header = JSON.stringify({
      kid: kid,
      typ: 'jwt',
      alg: 'HS256',
      hub: hubConfig
    });
    const payloadJson = JSON.stringify(payload);
    const encoder = new TextEncoder();
    const unsignedToken = base64url.stringify(encoder.encode(header), { pad: false }) + '.' + base64url.stringify(encoder.encode(payloadJson), { pad: false });
    const encodedUnsignedToken = new TextEncoder().encode(unsignedToken);
    const signature = await crypto.subtle.sign(
      'HMAC',
      this.masterKey,
      encodedUnsignedToken
    );
    return unsignedToken + '.' + base64url.stringify(new Uint8Array(signature), { pad: false });
  }

  public async hashDirectoryId(cleartextDirectoryId: string): Promise<string> {
    const dirHash = new TextEncoder().encode(cleartextDirectoryId);
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.masterKey));
    try {
      // miscreant lib requires mac key first and then the enc key
      const encKey = rawkey.subarray(0, rawkey.length / 2 | 0);
      const macKey = rawkey.subarray(rawkey.length / 2 | 0);
      const shiftedRawKey = new Uint8Array([...macKey, ...encKey]);
      const key = await miscreant.SIV.importKey(shiftedRawKey, 'AES-SIV');
      const ciphertext = await key.seal(dirHash, []);
      // hash is only used as deterministic scheme for the root dir
      const hash = await crypto.subtle.digest('SHA-1', ciphertext);
      return base32.stringify(new Uint8Array(hash));
    } finally {
      rawkey.fill(0x00);
    }
  }

  /**
   * Encrypts this masterkey using the given public key
   * @param userPublicKey The recipient's public key (DER-encoded)
   * @returns a JWE containing this Masterkey
   */
  public async encryptForUser(userPublicKey: CryptoKey | BufferSource): Promise<string> {
    const publicKey = await asPublicKey(userPublicKey, UserKeys.ECDH_KEY_DESIGNATION);
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.masterKey));
    try {
      const payload: JWEPayload = {
        key: base64.stringify(rawkey)
      };
      return JWEBuilder.ecdhEs(publicKey).encrypt(payload);
    } finally {
      rawkey.fill(0x00);
    }
  }

  /**
   * Encode masterkey for offline backup purposes, allowing re-importing the key for recovery purposes
   */
  public async createRecoveryKey(): Promise<string> {
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.masterKey));

    // add 16 bit checksum:
    const crc32 = CRC32.compute(rawkey);
    const checksum = new Uint8Array(2);
    checksum[0] = crc32 & 0xff;      // append the least significant byte of the crc
    checksum[1] = crc32 >> 8 & 0xff; // followed by the second-least significant byte
    const combined = new Uint8Array([...rawkey, ...checksum]);

    // encode using human-readable words:
    return wordEncoder.encodePadded(combined);
  }
}

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
    const jwe: UserKeyPayload = await JWEParser.parse(privateKeys).decryptPbes2(setupCode);
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
    const payload: UserKeyPayload = await JWEParser.parse(jwe).decryptEcdhEs(browserPrivateKey);
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
   * @returns A JWE holding the encrypted private key
   * @see JWEBuilder.pbes2
   */
  public async encryptWithSetupCode(setupCode: string, iterations?: number): Promise<string> {
    const payload = await this.prepareForEncryption();
    return await JWEBuilder.pbes2(setupCode, iterations).encrypt(payload);
  }

  /**
   * Encrypts the user's private key using the given public key
   * @param devicePublicKey The device's public key (DER-encoded)
   * @returns a JWE containing the PKCS#8-encoded private key
   * @see JWEBuilder.ecdhEs
   */
  public async encryptForDevice(devicePublicKey: CryptoKey | Uint8Array): Promise<string> {
    const publicKey = await asPublicKey(devicePublicKey, BrowserKeys.KEY_DESIGNATION);
    const payload = await this.prepareForEncryption();
    return JWEBuilder.ecdhEs(publicKey).encrypt(payload);
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
  const bytes = new TextEncoder().encode(orderedJson);
  return new Uint8Array(await crypto.subtle.digest('SHA-256', bytes));
}

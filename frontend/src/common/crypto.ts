import * as miscreant from 'miscreant';
import { base16, base32, base64, base64url } from 'rfc4648';
import { JWEBuilder, JWEParser } from './jwe';
import { CRC32, DB, wordEncoder } from './util';

import { VaultMetadataJWEAutomaticAccessGrantDto } from './backend';

export class UnwrapKeyError extends Error {
  readonly actualError: any;

  constructor(actualError: any) {
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

interface UserKeysJWEPayload {
  key: string
}

interface VaultKeysWEPayload {
  key: string
  uvfKey: string
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

  // in uvf setting, the vault masterKey is used to encrypt the vault metadata JWE using A256KW
  private static readonly UVF_MASTERKEY_KEY_DESIGNATION = { name: 'AES-KW', length: 256 };

  readonly masterKey: CryptoKey;
  readonly uvfMasterKey: CryptoKey;

  protected constructor(masterKey: CryptoKey, uvfMasterKey: CryptoKey) {
    this.masterKey = masterKey;
    this.uvfMasterKey = uvfMasterKey;
  }

  /**
   * Creates a new masterkey (vault8 and uvf)
   * @returns A new masterkey
   */
  public static async create(): Promise<VaultKeys> {
    const key = crypto.subtle.generateKey(
      VaultKeys.MASTERKEY_KEY_DESIGNATION,
      true,
      ['sign']
    );
    const uvfKey = crypto.subtle.generateKey(
      VaultKeys.UVF_MASTERKEY_KEY_DESIGNATION,
      true,
      ['wrapKey', 'unwrapKey']
    );
    return new VaultKeys(await key, await uvfKey);
  }


  /**
   * Decrypts the vault's masterkey (vault8 and uvf) using the user's private key
   * @param jwe JWE containing the vault key
   * @param userPrivateKey The user's private key
   * @returns The masterkey
   */
  public static async decryptWithUserKey(jwe: string, userPrivateKey: CryptoKey): Promise<VaultKeys> {
    let rawKey = new Uint8Array();
    let rawUvfKey = new Uint8Array();
    try {
      const payload: VaultKeysWEPayload = await JWEParser.parse(jwe).decryptEcdhEs(userPrivateKey);
      rawKey = base64.parse(payload.key);
      rawUvfKey = base64.parse(payload.uvfKey);
      const masterKey = crypto.subtle.importKey('raw', rawKey, VaultKeys.MASTERKEY_KEY_DESIGNATION, true, ['sign']);
      const uvfMasterKey = crypto.subtle.importKey('raw', rawUvfKey, VaultKeys.UVF_MASTERKEY_KEY_DESIGNATION, true, ['wrapKey', 'unwrapKey']);
      return new VaultKeys(await masterKey, await uvfMasterKey);
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
      // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 upstream legacy integration for uvf?
      return [new VaultKeys(await masterkey, await masterkey), { privateKey: await privKey, publicKey: await pubKey }];
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
    // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 upstream legacy integration for uvf?
    return new VaultKeys(await key, await key);
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
   * Encrypts this masterkey (vault8 and uvf) using the given public key
   * @param userPublicKey The recipient's public key (DER-encoded)
   * @returns a JWE containing this Masterkey
   */
  public async encryptForUser(userPublicKey: Uint8Array): Promise<string> {
    const publicKey = await crypto.subtle.importKey('spki', userPublicKey, UserKeys.KEY_DESIGNATION, false, []);
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.masterKey));
    const rawUvfKey = new Uint8Array(await crypto.subtle.exportKey('raw', this.uvfMasterKey));
    try {
      const payload: VaultKeysWEPayload = {
        key: base64.stringify(rawkey),
        uvfKey: base64.stringify(rawUvfKey)
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
    const jwe: UserKeysJWEPayload = await JWEParser.parse(encryptedPrivateKey).decryptPbes2(setupCode);
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
   * @see JWEBuilder.pbes2
   */
  public async encryptedPrivateKey(setupCode: string): Promise<string> {
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.keyPair.privateKey));
    try {
      const payload: UserKeysJWEPayload = {
        key: base64.stringify(rawkey)
      };
      return await JWEBuilder.pbes2(setupCode).encrypt(payload);
    } finally {
      rawkey.fill(0x00);
    }
  }

  /**
   * Encrypts the user's private key using the given public key
   * @param devicePublicKey The device's public key (DER-encoded)
   * @returns a JWE containing the PKCS#8-encoded private key
   * @see JWEBuilder.ecdhEs
   */
  public async encryptForDevice(devicePublicKey: CryptoKey | Uint8Array): Promise<string> {
    const publicKey = await UserKeys.publicKey(devicePublicKey);
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.keyPair.privateKey));
    try {
      const payload: UserKeysJWEPayload = {
        key: base64.stringify(rawkey)
      };
      return JWEBuilder.ecdhEs(publicKey).encrypt(payload);
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
      const payload: UserKeysJWEPayload = await JWEParser.parse(jwe).decryptEcdhEs(browserPrivateKey);
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
    const hashBuffer = await crypto.subtle.digest("SHA-256", encodedKey);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const hashHex = hashArray
      .map((b) => b.toString(16).padStart(2, "0").toUpperCase())
      .join("");
    return hashHex;
  }
}

export class VaultMetadata {
  // a 256 bit = 32 byte file key for data encryption
  private static readonly RAWKEY_KEY_DESIGNATION: HmacImportParams | HmacKeyGenParams = {
    name: 'HMAC',
    hash: 'SHA-256',
    length: 256
  };

  readonly automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto;
  readonly keys: Record<string,CryptoKey>;
  readonly latestFileKey: string;
  readonly nameKey: string;

  protected constructor(automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto, keys: Record<string,CryptoKey>, latestFileKey: string, nameKey: string) {
    this.automaticAccessGrant = automaticAccessGrant;
    this.keys = keys;
    this.latestFileKey = latestFileKey;
    this.nameKey = nameKey;
  }

  /**
   * Creates new vault metadata with a new file key and name key
   * @returns new vault metadata
   */
  public static async create(automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto): Promise<VaultMetadata> {
    const fileKey = crypto.subtle.generateKey(
      VaultMetadata.RAWKEY_KEY_DESIGNATION,
      true,
      // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 is this correct?
      ['sign']
    );
    const nameKey = crypto.subtle.generateKey(
      VaultMetadata.RAWKEY_KEY_DESIGNATION,
      true,
      // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 is this correct?
      ['sign']
    );
    const fileKeyId = Array(4).fill(null).map(()=>"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(Math.random()*62)).join("")
    const nameKeyId = Array(4).fill(null).map(()=>"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(Math.random()*62)).join("")
    const keys: Record<string,CryptoKey> = {};
    keys[fileKeyId] = await fileKey;
    keys[nameKeyId] = await nameKey;
    return new VaultMetadata(automaticAccessGrant, keys, fileKeyId, nameKeyId);
  }

  /**
   * Decrypts the vault metadata using the vault masterkey
   * @param jwe JWE containing the vault key
   * @param masterKey the vault masterKey
   * @returns vault metadata
   */
  public static async decryptWithMasterKey(jwe: string, masterKey: CryptoKey): Promise<VaultMetadata> {
      const payload = await JWEParser.parse(jwe).decryptA256kw(masterKey);
      const keys: Record<string,string> = payload['keys'];
      const keysImported: Record<string,CryptoKey> = payload['keys'];
      for (const k in keys) {
        // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 is this correct?
        keysImported[k] = await crypto.subtle.importKey('raw', base64.parse(keys[k]), VaultMetadata.RAWKEY_KEY_DESIGNATION, true, ['sign']);
      }
      const latestFileKey = payload['latestFileKey']
      const nameKey = payload['nameKey']
      return new VaultMetadata(
        payload['org.cryptomator.automaticAccessGrant'],
        keysImported,
        latestFileKey,
        nameKey
      );
  }

  /**
   * Encrypts the vault metadata using the given vault masterKey
   * @param userPublicKey The recipient's public key (DER-encoded)
   * @returns a JWE containing this Masterkey
   */
  public async encryptWithMasterKey(masterKey: CryptoKey): Promise<string> {
  const keysExported: Record<string,string> = {};
    for (const k in this.keys) {
      keysExported[k] = base64.stringify(new Uint8Array(await crypto.subtle.exportKey('raw', this.keys[k])));
    }
    const payload = {
        fileFormat: "AES-256-GCM-32k",
        nameFormat: "AES-256-SIV",
        keys: keysExported,
        latestFileKey: this.latestFileKey,
        nameKey: this.nameKey,
        // TODO https://github.com/encryption-alliance/unified-vault-format/pull/21 finalize kdf
        kdf: "1STEP-HMAC-SHA512",
        'org.cryptomator.automaticAccessGrant': this.automaticAccessGrant
    }
    return JWEBuilder.a256kw(masterKey).encrypt(payload);
  }

  public async hashDirectoryId(cleartextDirectoryId: string): Promise<string> {
    const dirHash = new TextEncoder().encode(cleartextDirectoryId);
    // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! MUST NEVER BE RELEASED LIKE THIS
    // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 use rawFileKey,rawNameKey for rootDirHash for now - should depend on nameKey only!!
    const rawkey = new Uint8Array([...new Uint8Array(await crypto.subtle.exportKey('raw', this.keys[this.latestFileKey])),...new Uint8Array(await crypto.subtle.exportKey('raw', this.keys[this.nameKey]))]);
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
}

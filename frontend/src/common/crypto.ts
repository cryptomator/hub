import * as miscreant from 'miscreant';
import { base32, base64, base64url } from 'rfc4648';
import { JWE } from './jwe';
import { JWT, JWTHeader } from './jwt';
import { CRC32, wordEncoder } from './util';

export class WrappedVaultKeys {
  constructor(readonly masterkey: string, readonly signaturePrivateKey: string, readonly signaturePublicKey: string, readonly salt: string, readonly iterations: number) { }
}

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
  devicesResourceUrl: string,
  authSuccessUrl: string
  authErrorUrl: string
}

interface JWEPayload {
  key: string
}

const GCM_NONCE_LEN = 12;
const PBKDF2_ITERATION_COUNT = 1000000;

async function pbkdf2(password: string, salt: Uint8Array, iterations: number, keyParams: AesDerivedKeyParams): Promise<CryptoKey> {
  const encodedPw = new TextEncoder().encode(password);
  const pwKey = await crypto.subtle.importKey(
    'raw',
    encodedPw,
    'PBKDF2',
    false,
    ['deriveKey']
  );
  return await crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      hash: 'SHA-256',
      salt: salt,
      iterations: iterations
    },
    pwKey,
    keyParams,
    false,
    ['wrapKey', 'unwrapKey']
  );
}

export class VaultKeys {
  private static readonly SIGNATURE_KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = {
    name: 'ECDSA',
    namedCurve: 'P-384'
  };

  // in this browser application, this 512 bit key is used
  // as a hmac key to sign the vault config.
  // however when used by cryptomator, it gets split into
  // a 256 bit encryption key and a 256 bit mac key
  private static readonly MASTERKEY_KEY_DESIGNATION: HmacImportParams | HmacKeyGenParams = {
    name: 'HMAC',
    hash: 'SHA-256',
    length: 512
  };

  private static readonly KEK_KEY_DESIGNATION: AesDerivedKeyParams = {
    name: 'AES-GCM',
    length: 256
  };

  readonly masterKey: CryptoKey;
  readonly signatureKeyPair: CryptoKeyPair;

  protected constructor(masterkey: CryptoKey, signatureKeyPair: CryptoKeyPair) {
    this.masterKey = masterkey;
    this.signatureKeyPair = signatureKeyPair;
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
    const keyPair = crypto.subtle.generateKey(
      VaultKeys.SIGNATURE_KEY_DESIGNATION,
      true,
      ['sign', 'verify']
    );
    return new VaultKeys(await key, await keyPair);
  }

  /**
   * Protects the key material. Must only be called for a newly created masterkey, otherwise it will fail.
   * @param password Password used for wrapping
   * @returns The wrapped key material
   */
  public async wrap(password: string): Promise<WrappedVaultKeys> {
    // salt:
    const salt = crypto.getRandomValues(new Uint8Array(16));
    const encodedSalt = base64.stringify(salt);
    // kek:
    const kek = pbkdf2(password, salt, PBKDF2_ITERATION_COUNT, VaultKeys.KEK_KEY_DESIGNATION);
    // masterkey:
    const masterKeyIv = crypto.getRandomValues(new Uint8Array(GCM_NONCE_LEN));
    const wrappedMasterKey = new Uint8Array(await crypto.subtle.wrapKey(
      'raw',
      this.masterKey,
      await kek,
      { name: 'AES-GCM', iv: masterKeyIv }
    ));
    const encodedMasterKey = base64.stringify(new Uint8Array([...masterKeyIv, ...wrappedMasterKey]));
    // secretkey:
    const secretKeyIv = crypto.getRandomValues(new Uint8Array(GCM_NONCE_LEN));
    const wrappedSecretKey = new Uint8Array(await crypto.subtle.wrapKey(
      'pkcs8',
      this.signatureKeyPair.privateKey,
      await kek,
      { name: 'AES-GCM', iv: secretKeyIv }
    ));
    const encodedSecretKey = base64.stringify(new Uint8Array([...secretKeyIv, ...wrappedSecretKey]));
    // publickey:
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.signatureKeyPair.publicKey));
    const encodedPublicKey = base64.stringify(publicKey);
    // result:
    return new WrappedVaultKeys(encodedMasterKey, encodedSecretKey, encodedPublicKey, encodedSalt, PBKDF2_ITERATION_COUNT);
  }

  /**
   * Unwraps the key material.
   * @param password Password used for wrapping
   * @param wrapped The wrapped key material
   * @returns The unwrapped key material.
   * @throws WrongPasswordError, if the wrong password is used
   */
  public static async unwrap(password: string, wrapped: WrappedVaultKeys): Promise<VaultKeys> {
    const kek = pbkdf2(password, base64.parse(wrapped.salt, { loose: true }), wrapped.iterations, VaultKeys.KEK_KEY_DESIGNATION);
    const decodedMasterKey = base64.parse(wrapped.masterkey, { loose: true });
    const decodedPrivateKey = base64.parse(wrapped.signaturePrivateKey, { loose: true });
    const decodedPublicKey = base64.parse(wrapped.signaturePublicKey, { loose: true });
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
      const signPrivKey = crypto.subtle.unwrapKey(
        'pkcs8',
        decodedPrivateKey.slice(GCM_NONCE_LEN),
        await kek,
        { name: 'AES-GCM', iv: decodedPrivateKey.slice(0, GCM_NONCE_LEN) },
        VaultKeys.SIGNATURE_KEY_DESIGNATION,
        false,
        ['sign']
      );
      const signPubKey = crypto.subtle.importKey(
        'spki',
        decodedPublicKey,
        VaultKeys.SIGNATURE_KEY_DESIGNATION,
        true,
        ['verify']
      );
      return new VaultKeys(await masterkey, { privateKey: await signPrivKey, publicKey: await signPubKey });
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
    const keyPair = crypto.subtle.generateKey(
      VaultKeys.SIGNATURE_KEY_DESIGNATION,
      true,
      ['sign', 'verify']
    );
    return new VaultKeys(await key, await keyPair);
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
  public async encryptForUser(userPublicKey: Uint8Array): Promise<string> {
    const publicKey = await crypto.subtle.importKey(
      'spki',
      userPublicKey,
      {
        name: 'ECDH',
        namedCurve: 'P-384'
      },
      false,
      []
    );

    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.masterKey));
    try {
      const payload: JWEPayload = {
        key: base64.stringify(rawkey)
      };
      const payloadJson = new TextEncoder().encode(JSON.stringify(payload));

      return JWE.build(payloadJson, publicKey);
    } finally {
      rawkey.fill(0x00);
    }
  }

  public async signVaultEditRequest(jwtHeader: JWTHeader, jwtPayload: any): Promise<string> {
    return JWT.build(jwtHeader, jwtPayload, this.signatureKeyPair.privateKey);
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

interface UserKeyArchive {
  publicKey: string,
  encryptedPrivateKey: string,
  salt: string,
  iterations: number
}

export class UserKeys {
  private static readonly KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = {
    name: 'ECDH',
    namedCurve: 'P-384'
  };

  private static readonly KEK_DESIGNATION: AesDerivedKeyParams = {
    name: 'AES-GCM',
    length: 256
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
    const keyPair = crypto.subtle.generateKey(UserKeys.KEY_DESIGNATION, true, ['deriveKey']);
    return new UserKeys(await keyPair);
  }

  /**
   * Recovers the user key pair using a recovery code. All other information can be retrieved from the backend.
   * @param encodedPublicKey The public key
   * @param encryptedPrivateKey The encrypted private key
   * @param recoveryCode The password used to protect the private key
   * @param salt The salt used during PBKDF2
   * @param iterations The number of iterations used by PBKDF2
   * @returns 
   */
  public static async recover(encodedPublicKey: string, encryptedPrivateKey: string, recoveryCode: string, salt: string, iterations: number) {
    const kek = pbkdf2(recoveryCode, base64.parse(salt, { loose: true }), iterations, UserKeys.KEK_DESIGNATION);
    const decodedPublicKey = base64.parse(encodedPublicKey, { loose: true });
    const decodedPrivateKey = base64.parse(encryptedPrivateKey, { loose: true });
    const privateKey = crypto.subtle.unwrapKey(
      'pkcs8',
      decodedPrivateKey.slice(GCM_NONCE_LEN),
      await kek,
      { name: 'AES-GCM', iv: decodedPrivateKey.slice(0, GCM_NONCE_LEN) },
      UserKeys.KEK_DESIGNATION,
      false,
      ['sign']
    );
    const publicKey = crypto.subtle.importKey(
      'spki',
      decodedPublicKey,
      UserKeys.KEK_DESIGNATION,
      true,
      ['verify']
    );
    return new UserKeys({ privateKey: await privateKey, publicKey: await publicKey });
  }

  public async export(recoveryCode: string): Promise<UserKeyArchive> {
    const salt = crypto.getRandomValues(new Uint8Array(12));
    const kek = pbkdf2(recoveryCode, salt, PBKDF2_ITERATION_COUNT, UserKeys.KEK_DESIGNATION);
    const iv = crypto.getRandomValues(new Uint8Array(GCM_NONCE_LEN));
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.keyPair.publicKey));
    const wrappedPrivateKey = new Uint8Array(await crypto.subtle.wrapKey(
      'pkcs8',
      this.keyPair.privateKey,
      await kek,
      { name: 'AES-GCM', iv: iv }
    ));
    return {
      publicKey: base64.stringify(publicKey),
      encryptedPrivateKey: base64.stringify(new Uint8Array([...iv, ...wrappedPrivateKey])),
      salt: base64.stringify(salt),
      iterations: PBKDF2_ITERATION_COUNT
    };
  }

  /**
   * Encrypts the user's private key using the given public key
   * @param userPublicKey The device's public key (DER-encoded)
   * @returns a JWE containing the PKCS#8-encoded private key
   */
  public async encryptForDevice(devicePublicKey: CryptoKey | Uint8Array): Promise<string> {
    let publicKey: CryptoKey;
    if (devicePublicKey instanceof Uint8Array) {
      const importParams: EcKeyImportParams = { name: 'ECDH', namedCurve: 'P-384' };
      publicKey = await crypto.subtle.importKey('spki', devicePublicKey, importParams, false, []);
    } else {
      publicKey = devicePublicKey;
    }

    const rawkey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.keyPair.privateKey));
    try {
      const payload: JWEPayload = {
        key: base64.stringify(rawkey)
      };
      const payloadJson = new TextEncoder().encode(JSON.stringify(payload));
      return JWE.build(payloadJson, publicKey);
    } finally {
      rawkey.fill(0x00);
    }
  }
}

export class BrowserKeys {
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
    const keyPair = crypto.subtle.generateKey(BrowserKeys.KEY_DESIGNATION, false, ['deriveKey']);
    return new BrowserKeys(await keyPair);
  }

  /**
   * Attempts to load previously stored key pair from the browser's IndexedDB.
   * @returns a promise resolving to the loaded browser key pair
   */
  public static async load(): Promise<BrowserKeys> {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const req = indexedDB.open('hub');
      req.onsuccess = evt => { resolve(req.result); };
      req.onerror = evt => { reject(req.error); };
      req.onupgradeneeded = evt => { req.result.createObjectStore('keys'); };
    });
    return new Promise<CryptoKeyPair>((resolve, reject) => {
      const transaction = db.transaction('keys', 'readonly');
      const keyStore = transaction.objectStore('keys');
      const query = keyStore.get('browserKeyPair');
      query.onsuccess = evt => { resolve(query.result); };
      query.onerror = evt => { reject(query.error); };
    }).then((keyPair) => {
      return new BrowserKeys(keyPair);
    }).finally(() => {
      db.close();
    });
  }

  /**
   * Stores the key pair in the browser's IndexedDB. See https://www.w3.org/TR/WebCryptoAPI/#concepts-key-storage
   * @returns a promise that will resolve if the key pair has been saved
   */
  public async store(): Promise<void> {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const req = indexedDB.open('hub');
      req.onsuccess = evt => { resolve(req.result); };
      req.onerror = evt => { reject(req.error); };
      req.onupgradeneeded = evt => { req.result.createObjectStore('keys'); };
    });
    return new Promise<void>((resolve, reject) => {
      const transaction = db.transaction('keys', 'readwrite');
      const keyStore = transaction.objectStore('keys');
      const query = keyStore.put(this.keyPair, 'browserKeyPair'); // TODO: use user-specific key
      query.onsuccess = evt => { transaction.commit(); resolve(); };
      query.onerror = evt => { reject(query.error); };
    }).finally(() => {
      db.close();
    });
  }

  public async encodedPublicKey() {
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.keyPair.publicKey));
    return base64url.stringify(publicKey); // device keys use base64url
  }
}

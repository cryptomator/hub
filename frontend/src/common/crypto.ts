import * as miscreant from 'miscreant';
import { base32, base64, base64url } from 'rfc4648';
import { JWE } from './jwe';
import { JWT, JWTHeader } from './jwt';
import { CRC32, WordEncoder } from './util';

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

  private static readonly GCM_NONCE_LEN = 12;
  private static readonly PBKDF2_ITERATION_COUNT = 1000000;
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

  private static async pbkdf2(password: string, salt: Uint8Array, iterations: number): Promise<CryptoKey> {
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
      VaultKeys.KEK_KEY_DESIGNATION,
      false,
      ['wrapKey', 'unwrapKey']
    );
  }

  /**
   * Protects the key material. Must only be called for a newly created masterkey, otherwise it will fail.
   * @param password Password used for wrapping
   * @returns The wrapped key material
   */
  public async wrap(password: string): Promise<WrappedVaultKeys> {
    // salt:
    const salt = new Uint8Array(16);
    crypto.getRandomValues(salt);
    const encodedSalt = base64.stringify(salt);
    // kek:
    const kek = VaultKeys.pbkdf2(password, salt, VaultKeys.PBKDF2_ITERATION_COUNT);
    // masterkey:
    const masterKeyIv = crypto.getRandomValues(new Uint8Array(VaultKeys.GCM_NONCE_LEN));
    const wrappedMasterKey = new Uint8Array(await crypto.subtle.wrapKey(
      'raw',
      this.masterKey,
      await kek,
      { name: 'AES-GCM', iv: masterKeyIv }
    ));
    const encodedMasterKey = base64.stringify(new Uint8Array([...masterKeyIv, ...wrappedMasterKey]));
    // secretkey:
    const secretKeyIv = crypto.getRandomValues(new Uint8Array(VaultKeys.GCM_NONCE_LEN));
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
    return new WrappedVaultKeys(encodedMasterKey, encodedSecretKey, encodedPublicKey, encodedSalt, VaultKeys.PBKDF2_ITERATION_COUNT);
  }

  /**
   * Unwraps the key material.
   * @param password Password used for wrapping
   * @param wrapped The wrapped key material
   * @returns The unwrapped key material.
   * @throws WrongPasswordError, if the wrong password is used
   */
  public static async unwrap(password: string, wrapped: WrappedVaultKeys): Promise<VaultKeys> {
    const kek = VaultKeys.pbkdf2(password, base64.parse(wrapped.salt, { loose: true }), wrapped.iterations);
    const decodedMasterKey = base64.parse(wrapped.masterkey, { loose: true });
    const decodedPrivateKey = base64.parse(wrapped.signaturePrivateKey, { loose: true });
    const decodedPublicKey = base64.parse(wrapped.signaturePublicKey, { loose: true });
    try {
      const masterkey = crypto.subtle.unwrapKey(
        'raw',
        decodedMasterKey.slice(VaultKeys.GCM_NONCE_LEN),
        await kek,
        { name: 'AES-GCM', iv: decodedMasterKey.slice(0, VaultKeys.GCM_NONCE_LEN) },
        VaultKeys.MASTERKEY_KEY_DESIGNATION,
        true,
        ['sign']
      );
      const signPrivKey = crypto.subtle.unwrapKey(
        'pkcs8',
        decodedPrivateKey.slice(VaultKeys.GCM_NONCE_LEN),
        await kek,
        { name: 'AES-GCM', iv: decodedPrivateKey.slice(0, VaultKeys.GCM_NONCE_LEN) },
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
   * @param devicePublicKey The recipient's public key (DER-encoded)
   * @returns a JWE containing this Masterkey
   */
  public async encryptForDevice(devicePublicKey: Uint8Array): Promise<string> {
    const publicKey = await crypto.subtle.importKey(
      'spki',
      devicePublicKey,
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

    // append the 16 left-most bits of the checksum:
    const crc32 = CRC32.compute(rawkey);
    const checksum = new Uint8Array(2);
    checksum[0] = ((crc32 >> 24) & 0xff);
    checksum[1] = ((crc32 >> 16) & 0xff);
    
    const combined = new Uint8Array([...rawkey, ...checksum]);
    
    // encode:
    const wordsFile = `${frontendBaseURL}/4096words_en.txt`;
    // read and split to word list...
    // ...
    // const encoder = new WordEncoder();

    return 'TODO'; // TODO: encode using word encoder
  }
}

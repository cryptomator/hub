import * as miscreant from 'miscreant';
import { base32, base64, base64url } from 'rfc4648';
import { JWE } from './jwe';

export class WrappedMasterkey {
  constructor(readonly masterkey: string, readonly sk: string, readonly pk: string, readonly salt: string, readonly iterations: number) { }
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

export class Masterkey {

  static readonly SK_DESIGNATION: EcKeyImportParams | EcKeyGenParams = {
    name: 'ECDSA',
    namedCurve: 'P-384'
  };

  // in this browser application, this 512 bit key is used
  // as a hmac key to sign the vault config.
  // however when used by cryptomator, it gets split into
  // a 256 bit encryption key and a 256 bit mac key
  static readonly KEY_DESIGNATION: HmacImportParams | HmacKeyGenParams = {
    name: 'HMAC',
    hash: 'SHA-256',
    length: 512
  };
  private static readonly PBKDF2_ITERATION_COUNT = 1000000;
  readonly #key: CryptoKey; // TODO rename to encKey?
  readonly #keypair: CryptoKeyPair; // TODO rename to signKeyPair?

  protected constructor(key: CryptoKey, keypair: CryptoKeyPair) {
    this.#key = key;
    this.#keypair = keypair;
  }

  /**
   * Creates a new masterkey
   * @returns A new masterkey
   */
  public static async create(): Promise<Masterkey> {
    const key = crypto.subtle.generateKey(
      Masterkey.KEY_DESIGNATION,
      true,
      ['sign']
    );
    const keyPair = crypto.subtle.generateKey(
      Masterkey.SK_DESIGNATION,
      true,
      ['sign', 'verify']
    );
    return new Masterkey(await key, await keyPair);
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
      { name: 'AES-GCM', length: 256 },
      false,
      ['wrapKey', 'unwrapKey']
    );
  }

  /**
   * Protects the key material. Must only be called for a newly created masterkey. Otherwise it will fail.
   * // TODO
   * @param password 
   * @returns 
   */
  public async wrap(password: string): Promise<WrappedMasterkey> {
    // salt:
    const salt = new Uint8Array(16);
    crypto.getRandomValues(salt);
    const encodedSalt = base64url.stringify(salt, { pad: false });
    // kek:
    const kek = Masterkey.pbkdf2(password, salt, Masterkey.PBKDF2_ITERATION_COUNT);
    // masterkey:
    const masterKeyIv = crypto.getRandomValues(new Uint8Array(12));
    const wrappedMasterKey = new Uint8Array(await crypto.subtle.wrapKey(
      'raw',
      this.#key,
      await kek,
      { name: 'AES-GCM', iv: masterKeyIv }
    ));
    const encodedMasterKey = base64url.stringify(new Uint8Array([...masterKeyIv, ...wrappedMasterKey]));
    // secretkey:
    const secretKeyIv = crypto.getRandomValues(new Uint8Array(12));
    const wrappedSecretKey = new Uint8Array(await crypto.subtle.wrapKey(
      'pkcs8',
      this.#keypair.privateKey,
      await kek,
      { name: 'AES-GCM', iv: secretKeyIv }
    ));
    const encodedSecretKey = base64.stringify(new Uint8Array([...secretKeyIv, ...wrappedSecretKey]));
    // publickey:
    const publicKey = new Uint8Array(await crypto.subtle.exportKey('spki', this.#keypair.publicKey));
    const encodedPublicKey = base64.stringify(publicKey);
    // result:
    return new WrappedMasterkey(encodedMasterKey, encodedSecretKey, encodedPublicKey, encodedSalt, Masterkey.PBKDF2_ITERATION_COUNT);
  }

  /**
   * Unwraps a masterkey.
   * @param password Password used for wrapping
   * @param wrapped The wrapped key
   * @returns The unwrapped masterkey.
   * @throws WrongPasswordError, if the wrong password is used
   */
  public static async unwrap(password: string, wrapped: WrappedMasterkey): Promise<Masterkey> {
    const kek = Masterkey.pbkdf2(password, base64url.parse(wrapped.salt, { loose: true }), wrapped.iterations);
    const decodedMasterKey = base64url.parse(wrapped.masterkey, { loose: true });
    const decodedSecretKey = base64.parse(wrapped.sk, { loose: true });
    const decodedPublicKey = base64.parse(wrapped.pk, { loose: true });
    try {
      const masterkey = crypto.subtle.unwrapKey(
        'raw',
        decodedMasterKey.slice(12),
        await kek,
        { name: 'AES-GCM', iv: decodedMasterKey.slice(0, 12) },
        Masterkey.KEY_DESIGNATION,
        true,
        ['sign']
      );
      const sk = crypto.subtle.unwrapKey(
        'pkcs8',
        decodedSecretKey.slice(12),
        await kek,
        { name: 'AES-GCM', iv: decodedSecretKey.slice(0, 12) },
        Masterkey.SK_DESIGNATION,
        false,
        ['sign']
      );
      const pk = crypto.subtle.importKey(
        'spki',
        decodedPublicKey,
        Masterkey.SK_DESIGNATION,
        true,
        ['verify']
      );
      return new Masterkey(await masterkey, { privateKey: await sk, publicKey: await pk });
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
      this.#key,
      encodedUnsignedToken
    );
    return unsignedToken + '.' + base64url.stringify(new Uint8Array(signature), { pad: false });
  }

  public async hashDirectoryId(cleartextDirectoryId: string): Promise<string> {
    const dirHash = new TextEncoder().encode(cleartextDirectoryId);
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.#key));
    try {
      // miscreant lib requires mac key first and then the enc key
      const encKey = rawkey.subarray(0, rawkey.length / 2 | 0);
      const macKey = rawkey.subarray(rawkey.length / 2 | 0);
      const shiftedRawKey = new Uint8Array([...macKey, ...encKey]);
      const key = await miscreant.SIV.importKey(shiftedRawKey, 'AES-SIV');
      const ciphertext = await key.seal(dirHash, []);
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

    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.#key));
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

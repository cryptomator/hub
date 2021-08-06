import { Base64Url } from './util';

export class WrappedMasterkey {
  constructor(readonly encrypted: string, readonly salt: string, readonly iterations: number) { }
}

export class DeviceSpecificMasterkey {
  constructor(readonly encrypted: string, readonly publicKey: string) { }
}

export class Masterkey {

  // in this browser application, this 512 bit key is used
  // as a hmac key to sign the vault config.
  // however when used by cryptomator, it gets split into
  // a 256 bit encryption key and a 256 bit mac key
  private static readonly KEY_DESIGNATION: HmacImportParams | HmacKeyGenParams = {
    name: 'HMAC',
    hash: 'SHA-256',
    length: 512
  };
  private static readonly PBKDF2_ITERATION_COUNT = 1000000;
  private key: CryptoKey

  private constructor(key: CryptoKey) {
    this.key = key;
  }

  /**
   * Creates a new masterkey, that can be wrapped
   * @returns A new masterkey
   */
  public static async create(): Promise<Masterkey> {
    const key = crypto.subtle.generateKey(
      Masterkey.KEY_DESIGNATION,
      true,
      ['sign']
    );
    return new Masterkey(await key);
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
      { name: 'AES-KW', length: 256 },
      false,
      ['wrapKey', 'unwrapKey']
    );
  }

  public async wrap(password: string): Promise<WrappedMasterkey> {
    const salt = new Uint8Array(16);
    crypto.getRandomValues(salt);
    const kek = Masterkey.pbkdf2(password, salt, Masterkey.PBKDF2_ITERATION_COUNT);
    const wrapped = crypto.subtle.wrapKey(
      'raw',
      this.key,
      await kek,
      'AES-KW'
    )
    return new WrappedMasterkey(Base64Url.encode(await wrapped), Base64Url.encode(salt), Masterkey.PBKDF2_ITERATION_COUNT);
  }

  /**
   * Unwraps a masterkey.
   * @param password Password used for wrapping
   * @param wrapped The wrapped key
   * @returns The unwrapped masterkey.
   */
  public static async unwrap(password: string, wrapped: WrappedMasterkey): Promise<Masterkey> {
    const kek = Masterkey.pbkdf2(password, Base64Url.decode(wrapped.salt), wrapped.iterations);
    const encrypted = Base64Url.decode(wrapped.encrypted);

    const key = crypto.subtle.unwrapKey(
      'raw',
      encrypted,
      await kek,
      'AES-KW',
      Masterkey.KEY_DESIGNATION,
      false, // unwrapped key not exportable atm (no rewrapping allowed right now)
      ['sign']
    );
    return new Masterkey(await key);
  }

  public async createVaultConfig(jti: string, kid: string): Promise<string> {
    const header = JSON.stringify({
      kid: kid,
      typ: 'jwt',
      alg: 'HS256'
    });
    const payload = JSON.stringify({
      jti: jti,
      format: 8,
      cipherCombo: 'SIV_GCM',
      shorteningThreshold: 220
    });
    const encoder = new TextEncoder();
    const unsignedToken = Base64Url.encode(encoder.encode(header)) + '.' + Base64Url.encode(encoder.encode(payload));
    const encodedUnsignedToken = new TextEncoder().encode(unsignedToken);
    const signature = await crypto.subtle.sign(
      'HMAC',
      this.key,
      encodedUnsignedToken
    );
    return unsignedToken + '.' + Base64Url.encode(signature);
  }

  /**
   * ECIES based on ECDH:
   * This derives a shared secret using ephemeral-static-ECDH, where the given
   * static public key is the trusted recipient.
   * The agreed shared secret is used in an authenticated encryption scheme
   * (in this case AES-KW) to protect the actual masterkey.
   * 
   * Therefore all ECIES components are based on existing primitives. 
   * @param devicePublicKey The recipient's public key
   */
  public async encryptForDevice(devicePublicKey: Uint8Array): Promise<DeviceSpecificMasterkey> {
    const ephemeralKey = await crypto.subtle.generateKey(
      {
        name: 'ECDH',
        namedCurve: 'P-256'
      },
      false,
      ['deriveKey']
    );
    const publicKey = await crypto.subtle.importKey(
      'spki',
      devicePublicKey,
      {
        name: 'ECDH',
        namedCurve: 'P-256'
      },
      false,
      []
    );
    const agreedKey = await crypto.subtle.deriveKey(
      {
        name: 'ECDH',
        public: publicKey
      },
      ephemeralKey.privateKey,
      { name: 'AES-KW', length: 256 },
      false,
      ['wrapKey', 'unwrapKey']
    );
    const wrapped = await crypto.subtle.wrapKey(
      'raw',
      this.key,
      agreedKey,
      'AES-KW'
    )
    const epk = await crypto.subtle.exportKey(
      "raw", ephemeralKey.publicKey
    )
    return new DeviceSpecificMasterkey(Base64Url.encode(wrapped), Base64Url.encode(epk))
  }


}

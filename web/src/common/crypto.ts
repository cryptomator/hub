import * as miscreant from "miscreant";
import { base32, base64url } from "rfc4648";

export class WrappedMasterkey {
  constructor(readonly encrypted: string, readonly salt: string, readonly iterations: number) { }
}

export class DeviceSpecificMasterkey {
  constructor(readonly encrypted: string, readonly publicKey: string) { }
}

export class X936 {

  /**
   * Performs <a href="https://www.secg.org/sec1-v2.pdf">ANSI-X9.63-KDF</a> with SHA-256
   * @param sharedSecret A shared secret
   * @param sharedInfo Additional authenticated data
   * @param keyDataLen Desired key length (in bytes)
   * @return key data
   */
  public static async kdf(sharedSecret: Uint8Array, sharedInfo: Uint8Array, keyDataLen: number): Promise<Uint8Array> {
    const hashLen = 32;
    const n = Math.ceil(keyDataLen / hashLen);
    const buffer = new Uint8Array(n * hashLen);

    const tmp = new ArrayBuffer(sharedSecret.byteLength + 4 + sharedInfo.byteLength);
    for (let i = 0; i < n; i++) {
      new Uint8Array(tmp).set(sharedSecret, 0);
      new DataView(tmp, sharedSecret.byteLength, 4).setUint32(0, i + 1, false);
      new Uint8Array(tmp).set(sharedInfo, sharedSecret.byteLength + 4);
      const digest = await crypto.subtle.digest('SHA-256', tmp);
      buffer.set(new Uint8Array(digest), i * hashLen);
    }
    return buffer.slice(0, keyDataLen);
  }

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
  readonly #key: CryptoKey

  protected constructor(key: CryptoKey) {
    this.#key = key;
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
      this.#key,
      await kek,
      'AES-KW'
    )
    return new WrappedMasterkey(base64url.stringify(new Uint8Array(await wrapped), { pad: false }), base64url.stringify(salt, { pad: false }), Masterkey.PBKDF2_ITERATION_COUNT);
  }

  /**
   * Unwraps a masterkey.
   * @param password Password used for wrapping
   * @param wrapped The wrapped key
   * @returns The unwrapped masterkey.
   */
  public static async unwrap(password: string, wrapped: WrappedMasterkey): Promise<Masterkey> {
    const kek = Masterkey.pbkdf2(password, base64url.parse(wrapped.salt, { loose: true }), wrapped.iterations);
    const encrypted = base64url.parse(wrapped.encrypted, { loose: true });

    const key = crypto.subtle.unwrapKey(
      'raw',
      encrypted,
      await kek,
      'AES-KW',
      Masterkey.KEY_DESIGNATION,
      true,
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
    const unsignedToken = base64url.stringify(encoder.encode(header), { pad: false }) + '.' + base64url.stringify(encoder.encode(payload), { pad: false });
    const encodedUnsignedToken = new TextEncoder().encode(unsignedToken);
    const signature = await crypto.subtle.sign(
      'HMAC',
      this.#key,
      encodedUnsignedToken
    );
    return unsignedToken + '.' + base64url.stringify(new Uint8Array(signature), { pad: false });
  }

  public async hashDirectoryId(cleartextDirectoryId: string): Promise<string> {
    const dirHash = new TextEncoder().encode(cleartextDirectoryId)
    const rawKeyBuffer = await crypto.subtle.exportKey(
      'raw',
      this.#key
    )

    var rawkey = new Uint8Array(rawKeyBuffer)
    // miscreant lib requires mac key first and then the enc key
    const encKey = rawkey.subarray(0, rawkey.length / 2 | 0);
    const macKey = rawkey.subarray(rawkey.length / 2 | 0);
    const shiftedRawKey = new Uint8Array(rawkey.length)
    shiftedRawKey.set(macKey)
    shiftedRawKey.set(encKey, macKey.length)

    const key = await miscreant.SIV.importKey(shiftedRawKey, 'AES-SIV');
    const ciphertext = await key.seal(dirHash, []);
    const hash = await crypto.subtle.digest('SHA-1', ciphertext);
    return base32.stringify(new Uint8Array(hash));
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
        namedCurve: 'P-384'
      },
      false,
      ['deriveBits']
    );
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
    const agreedKey = await crypto.subtle.deriveBits(
      {
        name: 'ECDH',
        public: publicKey
      },
      ephemeralKey.privateKey,
      384
    );
    console.log('agreedKey: ', base64url.stringify(new Uint8Array(agreedKey), { pad: false }));
    const sharedSecret = await X936.kdf(new Uint8Array(agreedKey), new Uint8Array(0), 44);
    console.log('sharedSecret: ', base64url.stringify(new Uint8Array(sharedSecret), { pad: false }));
    const aesKey = await crypto.subtle.importKey(
      'raw',
      sharedSecret.slice(0, 32),
      { name: 'AES-GCM', length: 256 },
      false,
      ['wrapKey']
    );
    const wrapped = await crypto.subtle.wrapKey(
      'raw',
      this.#key,
      aesKey,
      { name: 'AES-GCM', iv: sharedSecret.slice(32, 44), tagLength: 128 }
    );
    const epk = await crypto.subtle.exportKey(
      'spki', ephemeralKey.publicKey
    );
    return new DeviceSpecificMasterkey(base64url.stringify(new Uint8Array(wrapped), { pad: false }), base64url.stringify(new Uint8Array(epk), { pad: false }))
  }


}

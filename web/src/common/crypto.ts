import { Base64Url } from './util';

export class WrappedMasterkey {
  constructor(readonly encrypted: string, readonly salt: string, readonly iterations: number) { }
}

export class Masterkey {

  private static readonly PBKDF2_ITERATION_COUNT = 100000;
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
      {
        name: 'HMAC',
        hash: 'SHA-256',
        length: 512
      },
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
      { name: 'PBKDF2' },
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
      "raw",
      await this.key,
      await kek,
      { "name": "AES-KW" }
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
      "raw",
      encrypted,
      await kek,
      { name: 'AES-KW' },
      {
        name: 'HMAC',
        hash: 'SHA-256',
        length: 512
      },
      false, // unwrapped key not exportable atm (no rewrapping allowed right now)
      ['sign']
    );
    return new Masterkey(await key);
  }

  public async createVaultConfig(jti: string, kid: string): Promise<string> {
    // 'hub+' + location.protocol + '//' + location.hostname + ':' + location.port + '/vault/' + vaultId
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
      { name: 'HMAC' },
      await this.key,
      encodedUnsignedToken
    );
    return unsignedToken + '.' + Base64Url.encode(signature);
  }


}

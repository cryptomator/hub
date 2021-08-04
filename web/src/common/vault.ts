export class Base64Url {
  /**
   * Applies Base64 URL Encoding
   * @param data raw binary data
   * @returns base64url-encoded string representation of data
   */
  public static encode(data: ArrayBufferLike): string {
    const base64 = btoa(String.fromCharCode(...new Uint8Array(data)));
    return base64.replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  }
}

class Masterkey {
  constructor(readonly encrypted: string, readonly salt: string, readonly iterations: number) { }
}

export class Vault {

  private static readonly PBKDF2_ITERATION_COUNT = 100000;
  private masterkey: Promise<CryptoKey>

  private constructor(masterkey: Promise<CryptoKey>) {
    this.masterkey = masterkey;
  }

  public static create(): Vault {
    return new Vault(crypto.subtle.generateKey(
      {
        name: 'HMAC',
        hash: 'SHA-256',
        length: 512
      },
      true,
      ['sign']
    ))
  }

  private async pbkdf2(password: string, salt: Uint8Array, iterations: number): Promise<CryptoKey> {
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

  public async encryptMasterkey(password: string): Promise<Masterkey> {
    const salt = new Uint8Array(16);
    crypto.getRandomValues(salt);
    const kek = this.pbkdf2(password, salt, Vault.PBKDF2_ITERATION_COUNT);
    const wrapped = crypto.subtle.wrapKey(
      "raw",
      await this.masterkey,
      await kek,
      { "name": "AES-KW" }
    )
    return new Masterkey(Base64Url.encode(await wrapped), Base64Url.encode(salt), Vault.PBKDF2_ITERATION_COUNT);
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
      await this.masterkey,
      encodedUnsignedToken
    );
    return unsignedToken + '.' + Base64Url.encode(signature);
  }


}

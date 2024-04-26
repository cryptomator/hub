import { base64url } from 'rfc4648';
import { UnwrapKeyError } from './crypto';

// visible for testing
export class ConcatKDF {
  /**
   * KDF as defined in <a href="https://doi.org/10.6028/NIST.SP.800-56Ar2">NIST SP 800-56A Rev. 2 Section 5.8.1</a> using SHA-256
   * 
   * @param z A shared secret
   * @param keyDataLen Desired key length (in bytes)
   * @param otherInfo Optional context info binding the derived key to a key agreement (see e.g. RFC 7518, Section 4.6.2)
   * @returns key data
   */
  public static async kdf(z: Uint8Array, keyDataLen: number, otherInfo: Uint8Array): Promise<Uint8Array> {
    const hashLen = 32; // output length of SHA-256
    const reps = Math.ceil(keyDataLen / hashLen);
    if (reps >= 0xFFFFFFFF) {
      throw new Error('unsupported keyDataLen');
    }
    if (4 + z.byteLength + otherInfo.byteLength > 0xFFFFFFFF) {
      // technically max hash length for sha256 is 2^64-1 bits, but JS doesn't allow (safe) 64 bit numbers
      // it is safe to restrict this kdf to smaller input lengths
      throw new Error('unsupported input length');
    }
    const key = new Uint8Array(reps * hashLen);
    const tmp = new ArrayBuffer(4 + z.byteLength + otherInfo.byteLength);
    for (let i = 0; i < reps; i++) {
      new DataView(tmp, 0, 4).setUint32(0, i + 1, false);
      new Uint8Array(tmp).set(z, 4);
      new Uint8Array(tmp).set(otherInfo, 4 + z.byteLength);
      const digest = await crypto.subtle.digest('SHA-256', tmp);
      key.set(new Uint8Array(digest), i * hashLen);
    }
    return key.slice(0, keyDataLen);
  }
}

type Header = {
  kid?: string,
  enc?: 'A256GCM' | 'A128GCM',
  alg?: 'ECDH-ES' | 'ECDH-ES+A256KW' | 'PBES2-HS512+A256KW' | 'A256KW',
  apu?: string,
  apv?: string,
  epk?: JsonWebKey,
  p2c?: number,
  p2s?: string,
  [other: string]: undefined | string | number | boolean | object; // allow further properties
}

type PerRecipientProperties = {
  encryptedKey: string;
  header: Header;
}

export type JWEHeader = Header;

export const ECDH_P384: EcKeyImportParams | EcKeyGenParams = {
  name: 'ECDH',
  namedCurve: 'P-384'
};

// #region Recipients

export abstract class Recipient {

  constructor(readonly kid: string) { };

  /**
   * Encryptes a CEK using the recipient-specific `alg`.
   * @param cek The CEK that is used to encrypt a JWE payload.
   * @param commonHeader The protected and unprotected header (not per-recipient)
   * @returns The encrypted CEK and per-recipient header parameters for the used `alg`.
   */
  abstract encrypt(cek: CryptoKey, commonHeader: Header): Promise<PerRecipientProperties>;

  /**
   * Decrypts the CEK using the recipient-specific `alg`.
   * @param header JOSE header applicable for this recipient
   * @param encryptedKey Encrypted CEK for this recipient
   * @returns A non-extractable CEK suitable for decryption with AES-GCM.
   * @throws {UnwrapKeyError} if decryption failed
   */
  abstract decrypt(header: Header, encryptedKey: string): Promise<CryptoKey>;

  /**
   * Create a recipient using `alg: ECDH-ES+A256KW`. Also supports `ECDH-ES` with direct key agreement for decryption
   * 
   * @param kid The key ID used to distinguish multiple recipients
   * @param recipientKey The recipients static public key for encryption or private key for decryption
   * @param apu Optional information about the creator
   * @param apv Optional information about the recipient
   * @returns A new recipient
   */
  public static ecdhEs(kid: string, recipientKey: CryptoKey, apu: Uint8Array = new Uint8Array(), apv: Uint8Array = new Uint8Array()): Recipient {
    return new EcdhRecipient(kid, recipientKey, apu, apv);
  }

  /**
   * Create a recipient using `alg: PBES2-HS512+A256KW`.
   * 
   * @param kid The key ID used to distinguish multiple recipients
   * @param password The password to feed into the KDF
   * @param iterations The PBKDF2 iteration count (defaults to {@link PBES2.DEFAULT_ITERATION_COUNT}) - ignored and read from the header's `p2c` value during decryption
   * @returns A new recipient
   */
  public static pbes2(kid: string, password: string, iterations: number = PBES2.DEFAULT_ITERATION_COUNT): Recipient {
    return new Pbes2Recipient(kid, password, iterations);
  }

  /**
   * Create a recipient using `alg: A256KW`.
   * 
   * @param kid The key ID used to distinguish multiple recipients
   * @param wrappingKey The key used to wrap the CEK
   * @returns A new recipient
   */
  public static a256kw(kid: string, wrappingKey: CryptoKey): Recipient {
    return new A256kwRecipient(kid, wrappingKey);
  }

}

class EcdhRecipient extends Recipient {

  constructor(readonly kid: string, private recipientKey: CryptoKey, private apu: Uint8Array = new Uint8Array(), private apv: Uint8Array = new Uint8Array()) {
    super(kid);
  }

  async encrypt(cek: CryptoKey, commonHeader: Header): Promise<PerRecipientProperties> {
    if (this.recipientKey.type !== 'public') {
      throw new Error('Recipient public key required.');
    }
    const ephemeralKey = await crypto.subtle.generateKey(ECDH_P384, false, ['deriveBits']);
    const header: Header = {
      ...commonHeader,
      alg: 'ECDH-ES+A256KW',
      epk: await crypto.subtle.exportKey('jwk', ephemeralKey.publicKey),
      apu: base64url.stringify(this.apu, { pad: false }),
      apv: base64url.stringify(this.apv, { pad: false })
    };
    const wrappingKey = await ECDH_ES.deriveKey(this.recipientKey, ephemeralKey.privateKey, 384, 32, header, false, { name: 'AES-KW', length: 256 }, ['wrapKey']);
    const encryptedKey = new Uint8Array(await crypto.subtle.wrapKey('raw', cek, wrappingKey, 'AES-KW'));
    return {
      header: header,
      encryptedKey: base64url.stringify(encryptedKey, { pad: false })
    }
  }

  async decrypt(header: Header, encryptedKey: string): Promise<CryptoKey> {
    if (this.recipientKey.type !== 'private') {
      throw new Error('Recipient private key required.');
    }
    if (header.alg === 'ECDH-ES' && header.epk) {
      return this.decryptDirect(header, { name: 'AES-GCM', length: 256 }, ['decrypt']);
    } else if (header.alg === 'ECDH-ES+A256KW' && header.epk) {
      return this.decryptAndUnwrap(header, encryptedKey);
    } else {
      throw new Error('Missing or invalid header parameters.');
    }
  }

  async decryptDirect(header: Header, keyAlgorithm: AesKeyAlgorithm, keyUsage: KeyUsage[]): Promise<CryptoKey> {
    let keyBits: number;
    switch (header.epk!.crv) {
      case 'P-256':
        keyBits = 256;
        break;
      case 'P-384':
        keyBits = 384;
        break;
      default:
        throw new Error('Unsupported curve');
    }
    const epk = await crypto.subtle.importKey('jwk', header.epk!, { name: 'ECDH', namedCurve: header.epk?.crv }, false, []);
    return ECDH_ES.deriveKey(epk, this.recipientKey, keyBits, keyAlgorithm.length / 8, header, false, keyAlgorithm, keyUsage);
  }

  async decryptAndUnwrap(header: Header, encryptedKey: string): Promise<CryptoKey> {
    const wrappingKey = await this.decryptDirect(header, { name: 'AES-KW', length: 256 }, ['unwrapKey']);
    try {
      return await crypto.subtle.unwrapKey('raw', base64url.parse(encryptedKey, { loose: true }), wrappingKey, 'AES-KW', { name: 'AES-GCM' }, false, ['decrypt']);
    } catch (error) {
      throw new UnwrapKeyError(error);
    }
  }

}

class A256kwRecipient extends Recipient {

  constructor(readonly kid: string, private wrappingKey: CryptoKey) {
    super(kid);
  }

  async encrypt(cek: CryptoKey, commonHeader: Header): Promise<PerRecipientProperties> {
    const header: Header = {
      ...commonHeader,
      alg: 'A256KW',
      enc: 'A256GCM'
    };
    const encryptedKey = new Uint8Array(await crypto.subtle.wrapKey('raw', cek, this.wrappingKey, 'AES-KW'));
    return {
      header: header,
      encryptedKey: base64url.stringify(encryptedKey, { pad: false })
    }
  }

  async decrypt(header: Header, encryptedKey: string): Promise<CryptoKey> {
    if (header.alg != 'A256KW') {
      throw new Error('unsupported alg');
    }
    try {
      return await crypto.subtle.unwrapKey('raw', base64url.parse(encryptedKey, { loose: true }), this.wrappingKey, 'AES-KW', { name: 'AES-GCM' }, false, ['decrypt']);
    } catch (error) {
      throw new UnwrapKeyError(error);
    }
  }

}

class Pbes2Recipient extends Recipient {

  constructor(readonly kid: string, private password: string, private iterations: number) {
    super(kid);
  }

  async encrypt(cek: CryptoKey, commonHeader: Header): Promise<PerRecipientProperties> {
    const salt = crypto.getRandomValues(new Uint8Array(16));
    const header: Header = {
      ...commonHeader,
      kid: this.kid,
      alg: 'PBES2-HS512+A256KW',
      p2c: this.iterations,
      p2s: base64url.stringify(salt, { pad: false })
    };
    const wrappingKey = PBES2.deriveWrappingKey(this.password, 'PBES2-HS512+A256KW', salt, this.iterations);
    const encryptedKey = new Uint8Array(await crypto.subtle.wrapKey('raw', cek, await wrappingKey, 'AES-KW'));
    return {
      header: header,
      encryptedKey: base64url.stringify(encryptedKey, { pad: false })
    }
  }

  async decrypt(header: Header, encryptedKey: string): Promise<CryptoKey> {
    if (header.alg != 'PBES2-HS512+A256KW' || !header.p2s || !header.p2c) {
      throw new Error('Missing or invalid header parameters.');
    }
    const salt = base64url.parse(header.p2s!, { loose: true });
    const wrappingKey = await PBES2.deriveWrappingKey(this.password, 'PBES2-HS512+A256KW', salt, header.p2c!);
    try {
      return await crypto.subtle.unwrapKey('raw', base64url.parse(encryptedKey, { loose: true }), wrappingKey, 'AES-KW', { name: 'AES-GCM' }, false, ['decrypt']);
    } catch (error) {
      throw new UnwrapKeyError(error);
    }
  }

}

// #endregion
// #region JWE

export class JWE {

  private constructor(private payload: object) { }

  public static build(payload: object): JWE {
    return new JWE(payload);
  }

  public static parseCompact(token: string): EncryptedJWE {
    const [protectedHeader, encryptedKey, iv, ciphertext, tag] = token.split('.', 5);
    const utf8 = new TextDecoder();
    const header: Header = JSON.parse(utf8.decode(base64url.parse(protectedHeader, { loose: true })));

    return new EncryptedJWE(protectedHeader, [{ encryptedKey: encryptedKey, header: header }], iv, ciphertext, tag);
  }

  public static parseJson(jwe: any): EncryptedJWE { // TODO: json:
    if (!jwe.protected || !jwe.recipients || !jwe.iv || !jwe.ciphertext || !jwe.tag) {
      throw new Error('Malformed JWE');
    }
    return new EncryptedJWE(jwe.protected, jwe.recipients, jwe.iv, jwe.ciphertext, jwe.tag);
  }

  public async encrypt(recipient: Recipient, ...moreRecipients: Recipient[]): Promise<EncryptedJWE> {
    let protectedHeader: Header = {
      enc: 'A256GCM'
    }
    const cek = await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt']);
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const recipients = await Promise.all([recipient, ...moreRecipients].map(r => r.encrypt(cek, protectedHeader)));

    if (recipients.length === 1) {
      protectedHeader = recipients[0].header;
    } else {
      protectedHeader = {
        enc: recipients[0].header.enc
      }
    }

    const utf8enc = new TextEncoder();
    const encodedProtectedHeader = base64url.stringify(utf8enc.encode(JSON.stringify(protectedHeader)), { pad: false });
    const m = utf8enc.encode(JSON.stringify(this.payload));
    const ciphertextAndTag = new Uint8Array(await crypto.subtle.encrypt(
      {
        name: 'AES-GCM',
        iv: iv,
        additionalData: utf8enc.encode(encodedProtectedHeader),
        tagLength: 128
      },
      cek,
      m
    ));
    console.assert(m.byteLength > 16, 'result of GCM encryption expected to contain 128bit tag');
    const ciphertext = ciphertextAndTag.slice(0, m.byteLength - 16);
    const tag = ciphertextAndTag.slice(m.byteLength - 16);

    const encodedIv = base64url.stringify(iv, { pad: false });
    const encodedCiphertext = base64url.stringify(ciphertext, { pad: false });
    const encodedTag = base64url.stringify(tag, { pad: false });
    return new EncryptedJWE(encodedProtectedHeader, recipients, encodedIv, encodedCiphertext, encodedTag)
  }

}


// visible for testing
export class EncryptedJWE {

  constructor(private protectedHeader: string, private perRecipient: PerRecipientProperties[], private iv: string, private ciphertext: string, private tag: string) {
    if (perRecipient.length < 1) {
      throw new Error('Expected at least one recipient.');
    }
  }

  public jsonSerialization(): any {
    if (this.perRecipient.length < 1) {
      throw new Error('JWE JSON Serialization requires at least one recipient.');
    }
    const recipients = this.perRecipient.map(r => ({
      header: r.header,
      encrypted_key: r.encryptedKey
    }));

    return {
      protected: this.protectedHeader,
      recipients: recipients,
      iv: this.iv,
      ciphertext: this.ciphertext,
      tag: this.tag
    };
  }

  public compactSerialization(): string {
    if (this.perRecipient.length !== 1) {
      throw new Error('JWE Compact Serialization requires exactly one recipient.');
    }
    return `${this.protectedHeader}.${this.perRecipient[0].encryptedKey}.${this.iv}.${this.ciphertext}.${this.tag}`;
  }

  public async decrypt(recipient: Recipient): Promise<any> {
    const utf8dec = new TextDecoder();
    const utf8enc = new TextEncoder();
    const protectedHeader: Header = JSON.parse(utf8dec.decode(base64url.parse(this.protectedHeader, { loose: true })));
    const perRecipientData = (this.perRecipient.length === 1)
      ? this.perRecipient[0]
      : this.perRecipientWithKid(recipient.kid);
    const combinedHeader: Header = { ...perRecipientData.header, ...protectedHeader };
    const cek = await recipient.decrypt(combinedHeader, perRecipientData.encryptedKey);
    const ciphertext = base64url.parse(this.ciphertext, { loose: true })
    const tag = base64url.parse(this.tag, { loose: true });
    const ciphertextAndTag = new Uint8Array([...ciphertext, ...tag]);
    const cleartext = new Uint8Array(await crypto.subtle.decrypt(
      {
        name: 'AES-GCM',
        iv: base64url.parse(this.iv, { loose: true }),
        additionalData: utf8enc.encode(this.protectedHeader),
        tagLength: 128
      },
      cek,
      ciphertextAndTag
    ));
    return JSON.parse(utf8dec.decode(cleartext));
  }

  private perRecipientWithKid(kid: string): PerRecipientProperties {
    const result = this.perRecipient.find(r => r.header.kid === kid);
    if (result) {
      return result;
    } else {
      throw new Error(`JWE does not contain recipient with kid: ${kid}`);
    }
  }

}

// #endregion
// #region Utilities

// visible for testing
export class ECDH_ES {
  private static async deriveRawKey(publicKey: CryptoKey, privateKey: CryptoKey, ecdhKeyBits: number, desiredKeyBytes: number, header: JWEHeader): Promise<Uint8Array> {
    let agreedKey = new Uint8Array();
    try {
      const algOrEnc = header.alg === 'ECDH-ES' ? header.enc : header.alg; // see definition of AlgorithmID in RFC 7518, Section 4.6.2
      const algorithmId = ECDH_ES.lengthPrefixed(new TextEncoder().encode(algOrEnc));
      const partyUInfo = ECDH_ES.lengthPrefixed(base64url.parse(header.apu || '', { loose: true }));
      const partyVInfo = ECDH_ES.lengthPrefixed(base64url.parse(header.apv || '', { loose: true }));
      const suppPubInfo = new ArrayBuffer(4);
      const suppPrivInfo = new Uint8Array();
      new DataView(suppPubInfo).setUint32(0, desiredKeyBytes * 8, false);
      agreedKey = new Uint8Array(await crypto.subtle.deriveBits(
        {
          name: 'ECDH',
          public: publicKey
        },
        privateKey,
        ecdhKeyBits
      ));
      const otherInfo = new Uint8Array([...algorithmId, ...partyUInfo, ...partyVInfo, ...new Uint8Array(suppPubInfo), ...suppPrivInfo]);
      return ConcatKDF.kdf(new Uint8Array(agreedKey), desiredKeyBytes, otherInfo);
    } finally {
      agreedKey.fill(0x00);
    }
  }

  public static async deriveKey(publicKey: CryptoKey, privateKey: CryptoKey, ecdhKeyBits: number, desiredKeyBytes: number, header: JWEHeader, exportable: boolean, algorithm: AesKeyAlgorithm, usage: KeyUsage[]): Promise<CryptoKey> {
    let derivedKey = new Uint8Array();
    try {
      derivedKey = await this.deriveRawKey(publicKey, privateKey, ecdhKeyBits, desiredKeyBytes, header);
      return crypto.subtle.importKey('raw', derivedKey, algorithm, exportable, usage);
    } finally {
      derivedKey.fill(0x00);
    }
  }

  public static async deriveContentKey(publicKey: CryptoKey, privateKey: CryptoKey, ecdhKeyBits: number, desiredKeyBytes: number, header: JWEHeader, exportable: boolean = false): Promise<CryptoKey> {
    return this.deriveKey(publicKey, privateKey, ecdhKeyBits, desiredKeyBytes, header, exportable, { name: 'AES-GCM', length: desiredKeyBytes * 8 }, ['encrypt', 'decrypt']);
  }

  public static async deriveWrappingKey(publicKey: CryptoKey, privateKey: CryptoKey, ecdhKeyBits: number, desiredKeyBytes: number, header: JWEHeader, exportable: boolean = false): Promise<CryptoKey> {
    return this.deriveKey(publicKey, privateKey, ecdhKeyBits, desiredKeyBytes, header, exportable, { name: 'AES-KW', length: desiredKeyBytes * 8 }, ['wrapKey', 'unwrapKey']);
  }

  public static lengthPrefixed(data: Uint8Array): Uint8Array {
    const result = new Uint8Array(4 + data.byteLength);
    new DataView(result.buffer, 0, 4).setUint32(0, data.byteLength, false);
    result.set(data, 4);
    return result;
  }
}

// visible for testing
export class PBES2 {
  public static readonly DEFAULT_ITERATION_COUNT = 1000000;
  private static readonly NULL_BYTE = Uint8Array.of(0x00);

  // TODO: can we dedup this with crypto.ts's PBKDF2? Or is the latter unused anyway, once we migrate all ciphertext to JWE containers
  public static async deriveWrappingKey(password: string, alg: 'PBES2-HS512+A256KW' | 'PBES2-HS256+A128KW', salt: Uint8Array, iterations: number, extractable: boolean = false): Promise<CryptoKey> {
    let hash, keyLen;
    if (alg == 'PBES2-HS512+A256KW') {
      hash = 'SHA-512';
      keyLen = 256;
    } else if (alg == 'PBES2-HS256+A128KW') {
      hash = 'SHA-256';
      keyLen = 128;
    } else {
      throw new Error('only PBES2-HS512+A256KW and PBES2-HS256+A128KW supported');
    }
    const utf8enc = new TextEncoder();
    const encodedPw = utf8enc.encode(password);
    const pwKey = crypto.subtle.importKey(
      'raw',
      encodedPw,
      'PBKDF2',
      false,
      ['deriveKey']
    );
    return crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        hash: hash,
        salt: new Uint8Array([...utf8enc.encode(alg), ...PBES2.NULL_BYTE, ...salt]), // see https://www.rfc-editor.org/rfc/rfc7518#section-4.8.1.1
        iterations: iterations
      },
      await pwKey,
      {
        name: 'AES-KW',
        length: keyLen
      },
      extractable,
      ['wrapKey', 'unwrapKey']
    );
  }
}

// #endregion

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

export type JWEHeader = {
  readonly alg: 'ECDH-ES' | 'PBES2-HS512+A256KW' | 'A256KW',
  readonly enc: 'A256GCM' | 'A128GCM', // A128GCM for testing only, as we use test vectors with 128 bit keys
  readonly apu?: string,
  readonly apv?: string,
  readonly epk?: JsonWebKey,
  readonly p2c?: number,
  readonly p2s?: string
}

export const ECDH_P384: EcKeyImportParams | EcKeyGenParams = {
  name: 'ECDH',
  namedCurve: 'P-384'
};

export class JWEParser {
  readonly header: JWEHeader;
  readonly encryptedKey: Uint8Array;
  readonly iv: Uint8Array;
  readonly ciphertext: Uint8Array;
  readonly tag: Uint8Array;

  private constructor(readonly encodedHeader: string, readonly encodedEncryptedKey: string, readonly encodedIv: string, readonly encodedCiphertext: string, readonly encodedTag: string) {
    const utf8dec = new TextDecoder();
    this.header = JSON.parse(utf8dec.decode(base64url.parse(encodedHeader, { loose: true })));
    this.encryptedKey = base64url.parse(encodedEncryptedKey, { loose: true });
    this.iv = base64url.parse(encodedIv, { loose: true });
    this.ciphertext = base64url.parse(encodedCiphertext, { loose: true });
    this.tag = base64url.parse(encodedTag, { loose: true });
  }

  /**
   * Decodes the JWE.
   * @param jwe The JWE string
   * @returns Decoded JWE, ready to decrypt.
   */
  public static parse(jwe: string): JWEParser {
    const [encodedHeader, encodedEncryptedKey, encodedIv, encodedCiphertext, encodedTag] = jwe.split('.', 5);
    return new JWEParser(encodedHeader, encodedEncryptedKey, encodedIv, encodedCiphertext, encodedTag);
  }

  /**
   * Decrypts the JWE, assuming alg == ECDH-ES, enc == A256GCM and keys on the P-384 curve.
   * @param recipientPrivateKey The recipient's private key
   * @returns Decrypted payload
   */
  public async decryptEcdhEs(recipientPrivateKey: CryptoKey): Promise<any> {
    if (this.header.alg != 'ECDH-ES' || this.header.enc != 'A256GCM' || !this.header.epk) {
      throw new Error('unsupported alg or enc');
    }
    const ephemeralKey = await crypto.subtle.importKey('jwk', this.header.epk, ECDH_P384, false, []);
    const cek = await ECDH_ES.deriveContentKey(ephemeralKey, recipientPrivateKey, 384, 32, this.header);
    return this.decrypt(cek);
  }

  /**
   * Decrypts the JWE, assuming alg == PBES2-HS512+A256KW and enc == A256GCM.
   * @param password The password to feed into the KDF
   * @returns Decrypted payload
   * @throws {UnwrapKeyError} if decryption failed (wrong password?)
   */
  public async decryptPbes2(password: string): Promise<any> {
    if (this.header.alg != 'PBES2-HS512+A256KW' || this.header.enc != 'A256GCM' || !this.header.p2s || !this.header.p2c) {
      throw new Error('unsupported alg or enc');
    }
    const saltInput = base64url.parse(this.header.p2s, { loose: true });
    const wrappingKey = await PBES2.deriveWrappingKey(password, this.header.alg, saltInput, this.header.p2c);
    try {
      const cek = crypto.subtle.unwrapKey('raw', this.encryptedKey, wrappingKey, 'AES-KW', { name: 'AES-GCM', length: 256 }, false, ['decrypt']);
      return this.decrypt(await cek);
    } catch (error) {
      throw new UnwrapKeyError(error);
    }
  }

  /**
   * Decrypts the JWE, assuming alg == A256KW and enc == A256GCM.
   * @param kek The key used to wrap the CEK
   * @returns Decrypted payload
   * @throws {UnwrapKeyError} if decryption failed (wrong kek?)
   */
  public async decryptA256kw(kek: CryptoKey): Promise<any> {
    if (this.header.alg != 'A256KW' || this.header.enc != 'A256GCM') {
      throw new Error('unsupported alg or enc');
    }
    try {
      const cek = crypto.subtle.unwrapKey('raw', this.encryptedKey, kek, 'AES-KW', { name: 'AES-GCM', length: 256 }, false, ['decrypt']);
      return this.decrypt(await cek);
    } catch (error) {
      throw new UnwrapKeyError(error);
    }
  }

  private async decrypt(cek: CryptoKey): Promise<any> {
    const utf8enc = new TextEncoder();
    const m = new Uint8Array(this.ciphertext.length + this.tag.length);
    m.set(this.ciphertext, 0);
    m.set(this.tag, this.ciphertext.length);
    const payloadJson = new Uint8Array(await crypto.subtle.decrypt(
      {
        name: 'AES-GCM',
        iv: this.iv,
        additionalData: utf8enc.encode(this.encodedHeader),
        tagLength: 128
      },
      cek,
      m
    ));
    return JSON.parse(new TextDecoder().decode(payloadJson));
  }
}

export class JWEBuilder {
  private constructor(readonly header: Promise<JWEHeader>, readonly encryptedKey: Promise<Uint8Array>, readonly cek: Promise<CryptoKey>) { }

  /**
   * Prepares a new JWE using alg: ECDH-ES and enc: A256GCM.
   * 
   * @param recipientPublicKey Static public key of the JWE's recipient
   * @param apu Optional information about the creator
   * @param apv Optional information about the recipient
   * @returns A new JWEBuilder ready to encrypt the payload
   */
  public static ecdhEs(recipientPublicKey: CryptoKey, apu: Uint8Array = new Uint8Array(), apv: Uint8Array = new Uint8Array()): JWEBuilder {
    /* key agreement and header params described in RFC 7518, Section 4.6: */
    const ephemeralKey = crypto.subtle.generateKey(ECDH_P384, false, ['deriveBits']);
    const header = (async () => <JWEHeader>{
      alg: 'ECDH-ES',
      enc: 'A256GCM',
      epk: await crypto.subtle.exportKey('jwk', (await ephemeralKey).publicKey),
      apu: base64url.stringify(apu, { pad: false }),
      apv: base64url.stringify(apv, { pad: false })
    })();
    const encryptedKey = (async () => Uint8Array.of())(); // empty for Direct Key Agreement as per spec
    const cek = (async () => ECDH_ES.deriveContentKey(recipientPublicKey, (await ephemeralKey).privateKey, 384, 32, await header))();
    return new JWEBuilder(header, encryptedKey, cek);
  }

  /**
   * Prepares a new JWE using alg: PBES2-HS512+A256KW and enc: A256GCM.
   * 
   * @param password The password to feed into the KDF
   * @param iterations The PBKDF2 iteration count (defaults to {@link PBES2.DEFAULT_ITERATION_COUNT} )
   * @param apu Optional information about the creator
   * @param apv Optional information about the recipient
   * @returns A new JWEBuilder ready to encrypt the payload
   */
  public static pbes2(password: string, iterations: number = PBES2.DEFAULT_ITERATION_COUNT, apu: Uint8Array = new Uint8Array(), apv: Uint8Array = new Uint8Array()): JWEBuilder {
    const saltInput = crypto.getRandomValues(new Uint8Array(16));
    const header = (async () => <JWEHeader>{
      alg: 'PBES2-HS512+A256KW',
      enc: 'A256GCM',
      p2s: base64url.stringify(saltInput, { pad: false }),
      p2c: iterations,
      apu: base64url.stringify(apu, { pad: false }),
      apv: base64url.stringify(apv, { pad: false })
    })();
    const wrappingKey = PBES2.deriveWrappingKey(password, 'PBES2-HS512+A256KW', saltInput, iterations);
    const cek = crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt']);
    const encryptedKey = (async () => new Uint8Array(await crypto.subtle.wrapKey('raw', await cek, await wrappingKey, 'AES-KW')))();
    return new JWEBuilder(header, encryptedKey, cek);
  }

  /**
   * Prepares a new JWE using alg: A256KW and enc: A256GCM.
   * 
   * @param kek The key used to wrap the CEK
   * @returns A new JWEBuilder ready to encrypt the payload
   */
  public static a256kw(kek: CryptoKey): JWEBuilder {
    const header = (async () => <JWEHeader>{
      alg: 'A256KW',
      enc: 'A256GCM'
    })();
    const cek = crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt']);
    const encryptedKey = (async () => new Uint8Array(await crypto.subtle.wrapKey('raw', await cek, kek, 'AES-KW')))();
    return new JWEBuilder(header, encryptedKey, cek);
  }

  /**
   * Builds the JWE.
   * @param payload Payload to be encrypted
   * @returns The JWE
   */
  public async encrypt(payload: object) {
    const utf8enc = new TextEncoder();

    /* JWE assembly and content encryption described in RFC 7516: */
    const encodedHeader = base64url.stringify(utf8enc.encode(JSON.stringify(await this.header)), { pad: false });
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const encodedIv = base64url.stringify(iv, { pad: false });
    const encodedEncryptedKey = base64url.stringify(await this.encryptedKey, { pad: false });
    const m = new Uint8Array(await crypto.subtle.encrypt(
      {
        name: 'AES-GCM',
        iv: iv,
        additionalData: utf8enc.encode(encodedHeader),
        tagLength: 128
      },
      await this.cek,
      utf8enc.encode(JSON.stringify(payload))
    ));
    console.assert(m.byteLength > 16, 'result of GCM encryption expected to contain 128bit tag');
    const ciphertext = m.slice(0, m.byteLength - 16);
    const tag = m.slice(m.byteLength - 16);
    const encodedCiphertext = base64url.stringify(ciphertext, { pad: false });
    const encodedTag = base64url.stringify(tag, { pad: false });
    return `${encodedHeader}.${encodedEncryptedKey}.${encodedIv}.${encodedCiphertext}.${encodedTag}`;
  }
}

// visible for testing
export class ECDH_ES {
  public static async deriveContentKey(publicKey: CryptoKey, privateKey: CryptoKey, ecdhKeyBits: number, desiredKeyBytes: number, header: JWEHeader, exportable: boolean = false): Promise<CryptoKey> {
    let agreedKey = new Uint8Array();
    let derivedKey = new Uint8Array();
    try {
      const algorithmId = ECDH_ES.lengthPrefixed(new TextEncoder().encode(header.enc));
      const partyUInfo = ECDH_ES.lengthPrefixed(base64url.parse(header.apu || '', { loose: true }));
      const partyVInfo = ECDH_ES.lengthPrefixed(base64url.parse(header.apv || '', { loose: true }));
      const suppPubInfo = new ArrayBuffer(4);
      new DataView(suppPubInfo).setUint32(0, desiredKeyBytes * 8, false);
      agreedKey = new Uint8Array(await crypto.subtle.deriveBits(
        {
          name: 'ECDH',
          public: publicKey
        },
        privateKey,
        ecdhKeyBits
      ));
      const otherInfo = new Uint8Array([...algorithmId, ...partyUInfo, ...partyVInfo, ...new Uint8Array(suppPubInfo)]);
      derivedKey = await ConcatKDF.kdf(new Uint8Array(agreedKey), desiredKeyBytes, otherInfo);
      return crypto.subtle.importKey('raw', derivedKey, { name: 'AES-GCM', length: desiredKeyBytes * 8 }, exportable, ['encrypt', 'decrypt']);
    } finally {
      derivedKey.fill(0x00);
      agreedKey.fill(0x00);
    }
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

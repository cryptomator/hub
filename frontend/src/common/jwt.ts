import { base64url } from 'rfc4648';
import { UTF8 } from './util';

export type JWTHeader = {
  alg: 'ES384';
  typ: 'JWT';
  b64: true;
  [other: string]: undefined | string | number | boolean | object; // allow further properties
}

export const ECDSA_P384: EcKeyImportParams | EcKeyGenParams = {
  name: 'ECDSA',
  namedCurve: 'P-384'
};

export class JWT {
  /**
   * Creates an ES384 JWT (signed with ECDSA using P-384 and SHA-384).
   * 
   * See <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519</a>,
   * <a href="https://datatracker.ietf.org/doc/html/rfc7515">RFC 7515</a> and
   * <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.4">RFC 7518, Section 3.4</a>
   * 
   * @param payload The payload
   * @param signerPrivateKey The signers's private key
   */
  public static async build(header: JWTHeader, payload: object, signerPrivateKey: CryptoKey): Promise<string> {
    const encodedHeader = base64url.stringify(UTF8.encode(JSON.stringify(header)), { pad: false });
    const encodedPayload = base64url.stringify(UTF8.encode(JSON.stringify(payload)), { pad: false });
    const encodedSignature = await this.es384sign(encodedHeader, encodedPayload, signerPrivateKey);
    return encodedHeader + '.' + encodedPayload + '.' + encodedSignature;
  }

  // visible for testing
  public static async es384sign(encodedHeader: string, encodedPayload: string, signerPrivateKey: CryptoKey): Promise<string> {
    const headerAndPayload = UTF8.encode(encodedHeader + '.' + encodedPayload);
    const signature = await window.crypto.subtle.sign(
      {
        name: 'ECDSA',
        hash: { name: 'SHA-384' },
      },
      signerPrivateKey,
      headerAndPayload
    );
    return base64url.stringify(new Uint8Array(signature), { pad: false });
  }

  /**
   * Decodes and verifies an ES384 JWT (signed with ECDSA using P-384 and SHA-384).
   * @param jwt 
   * @param signerPublicKey 
   * @returns header and payload
   * @throws Error if the JWT is invalid
   */
  public static async parse(jwt: string, signerPublicKey: CryptoKey): Promise<[JWTHeader, object]> {
    const [encodedHeader, encodedPayload] = jwt.split('.');
    const header: JWTHeader = JSON.parse(UTF8.decode(base64url.parse(encodedHeader, { loose: true })));
    if (header.alg !== 'ES384') {
      throw new Error('Unsupported algorithm');
    }
    const validSignature = await this.es384verify(jwt, signerPublicKey);
    if (!validSignature) {
      throw new Error('Invalid signature');
    }
    const payload = JSON.parse(UTF8.decode(base64url.parse(encodedPayload, { loose: true })));
    return [header, payload];
  }

  // visible for testing
  public static async es384verify(jwt: string, signerPublicKey: CryptoKey): Promise<boolean> {
    const [encodedHeader, encodedPayload, encodedSignature] = jwt.split('.');
    const headerAndPayload = UTF8.encode(encodedHeader + '.' + encodedPayload);
    const signature = base64url.parse(encodedSignature);
    return window.crypto.subtle.verify(
      {
        name: 'ECDSA',
        hash: { name: 'SHA-384' },
      },
      signerPublicKey,
      signature,
      headerAndPayload
    );
  }
}

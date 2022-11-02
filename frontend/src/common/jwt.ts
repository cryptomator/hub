import { base64url } from 'rfc4648';

export interface JWTHeader {

  alg: 'ES384';
  typ: 'JWT';
  b64: true;
}

export class JWT {
  /**
   * Creates a ES384 JWT (signed with ECDSA using P-384 and SHA-384).
   * 
   * See <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519</a>,
   * <a href="https://datatracker.ietf.org/doc/html/rfc7515">RFC 7515</a> and
   * <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.4">RFC 7518, Section 3.4</a>
   * 
   * @param payload The payload
   * @param signerPrivateKey The signers's private key
   */
  public static async build(header: JWTHeader, payload: any, signerPrivateKey: CryptoKey): Promise<string> {
    const encodedHeader = base64url.stringify(new TextEncoder().encode(JSON.stringify(header)), { pad: false });
    const encodedPayload = base64url.stringify(new TextEncoder().encode(JSON.stringify(payload)), { pad: false });
    const encodedSignature = await this.es384sign(encodedHeader, encodedPayload, signerPrivateKey);
    return encodedHeader + '.' + encodedPayload + '.' + encodedSignature;
  }

  // visible for testing
  public static async es384sign(encodedHeader: string, encodedPayload: string, signerPrivateKey: CryptoKey): Promise<string> {
    const headerAndPayload = new TextEncoder().encode(encodedHeader + '.' + encodedPayload);
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
}

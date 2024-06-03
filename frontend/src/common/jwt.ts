import { base64url } from 'rfc4648';

export type JWTHeader = {
  alg: 'ES384';
  typ: 'JWT';
  b64: true;
}

export class JWT {
  public header: any;
  public payload: any;
  public signature: Uint8Array;

  private constructor(header: any, payload: any, signature: Uint8Array) {
    this.header = header;
    this.payload = payload;
    this.signature = signature;
  }

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

  /**
   * Parses an encoded JWT token.
   * Note that only basic JSON /encoding checks are made, neither the header nor the payload are checked for required attributes.
   * @param token The encoded JWT.
   * @returns A generic, and maybe still invalid JWT object
   */
  public static async parse(token: string): Promise<JWT> {
    const jwtSections = token.split('.');
    if (jwtSections.length != 3 || !jwtSections[0] || !jwtSections[1] || !jwtSections[2]) {
      throw new Error('Invalid JWT');
    }

    const header = JSON.parse(new TextDecoder().decode(base64url.parse(jwtSections[0], { loose: true })));
    const payload = JSON.parse(new TextDecoder().decode(base64url.parse(jwtSections[1], { loose: true })));
    const signature = base64url.parse(jwtSections[2], { loose: true });
    return new JWT(header, payload, signature);
  }
}

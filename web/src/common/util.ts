export class Base64Url {
  /**
   * Applies base64url encoding
   * @param data raw binary data
   * @returns base64url-encoded string representation of data
   */
  public static encode(data: ArrayBufferLike): string {
    const base64 = btoa(String.fromCharCode(...new Uint8Array(data)));
    return base64.replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  }

  /**
   * Decodes a base64url-encoded string
   * @param encoded base64url-encoded string
   * @returns decoded data
   */
  public static decode(encoded: string): Uint8Array {
    const base64 = encoded.replace(/\-/g, '+').replace(/_/g, '/');
    return Uint8Array.from(atob(base64), c => c.charCodeAt(0));
  }
}


/**
 * Create a new random UUID
 * @returns A (version 4) UUID
 */
export function uuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

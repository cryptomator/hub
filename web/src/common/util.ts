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

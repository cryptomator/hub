import { dictionary } from './4096words_en';

export class UTF8 {
  private static readonly encoder = new TextEncoder();
  private static readonly decoder = new TextDecoder('utf-8', { fatal: true });

  /**
   * Encodes a string into a byte sequence, using the UTF-8 NFC encoding.
   * @param data string to encode
   * @returns Uint8Array containing the UTF-8 NFC encoded string
   */
  public static encode(data: string): Uint8Array<ArrayBuffer> {
    return UTF8.encoder.encode(data.normalize('NFC'));
  }

  /**
   * Decodes a UTF-8 encoded string from a Uint8Array or SharedArrayBuffer.
   * @param data data to decode
   * @param options optional TextDecodeOptions
   * @throws {TypeError} if the data is not valid UTF-8
   * @returns decoded string
   */
  public static decode(data: AllowSharedBufferSource, options?: TextDecodeOptions): string {
    return UTF8.decoder.decode(data, options);
  }
}

export class DB {
  private static readonly NAME = 'hub';

  public static async transaction<T>(objectStore: string, mode: IDBTransactionMode, query: (transaction: IDBTransaction) => IDBRequest<T>): Promise<T> {
    const db = await new Promise<IDBDatabase>((resolve, reject) => {
      const req = indexedDB.open(DB.NAME);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error!);
      req.onupgradeneeded = () => req.result.createObjectStore(objectStore);
    });
    const transaction = db.transaction(objectStore, mode);
    return new Promise<T>((resolve, reject) => {
      const req = query(transaction);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error!);
    }).finally(() => {
      db.close();
    });
  }
}

export class Deferred<T> {
  public promise: Promise<T>;
  public reject: (reason?: unknown) => void;
  public resolve: (value: T) => void;
  public status: 'pending' | 'resolved' | 'rejected' = 'pending';

  constructor() {
    this.reject = () => { };
    this.resolve = () => { };
    this.promise = new Promise<T>((resolve, reject) => {
      this.reject = (r) => { this.status = 'rejected'; reject(r); };
      this.resolve = (t) => { this.status = 'resolved'; resolve(t); };
    });
  }
}
/**
 * Creates a cancellable debounced function that delays invoking the provided function until at least `wait` milliseconds have elapsed since the last time it was invoked.
 * Sources: https://decipher.dev/30-seconds-of-typescript/docs/debounce/ and https://wiki.selfhtml.org/wiki/JavaScript/Tutorials/Debounce_und_Throttle
 * @param func function to debounce
 * @param wait time to wait before calling function
 * @returns debounced function
 */
// eslint-disable-next-line @typescript-eslint/no-unsafe-function-type
export const debounce = (func: Function, wait = 300) => {
  let timeoutId: ReturnType<typeof setTimeout>;
  function debounceCore(this: unknown, ...args: unknown[]) {
    cancel();
    timeoutId = setTimeout(() => func.apply(this, args), wait);
  }
  function cancel() {
    clearTimeout(timeoutId);
  }
  debounceCore.cancel = cancel;
  return debounceCore;
};

// based on https://stackoverflow.com/a/18639903/4014509
export class CRC32 {
  static readonly TABLE = new Uint32Array(256);

  static {
    for (let i = 256; i--;) {
      let tmp = i;
      for (let k = 8; k--;) {
        tmp = tmp & 1 ? 0xEDB88320 ^ tmp >>> 1 : tmp >>> 1;
      }
      CRC32.TABLE[i] = tmp;
    }
  }

  public static compute(data: Uint8Array): number {
    let crc = 0xFFFFFFFF;
    for (let i = 0, l = data.length; i < l; i++) {
      crc = CRC32.TABLE[(crc ^ data[i]) & 0xFF] ^ (crc >>> 8);
    }
    return (crc ^ -1) >>> 0;
  }
}

class WordEncoder {
  static readonly WORD_COUNT = 4096;
  static readonly DELIMITER = ' ';

  private readonly words: string[];
  private readonly indices: Map<string, number>;

  constructor(words: string[]) {
    this.words = words;
    this.indices = new Map<string, number>();
    for (const [i, word] of words.entries()) {
      this.indices.set(word, i);
    }
  }

  public encodePadded(input: Uint8Array): string {
    if (input.length % 3 != 0) {
      throw new Error('input needs to be padded to a multiple of three');
    }
    const result: string[] = [];
    for (let i = 0; i < input.length; i += 3) {
      const b1 = input[i];
      const b2 = input[i + 1];
      const b3 = input[i + 2];
      const firstWordIndex = (0xFF0 & (b1 << 4)) + (0x00F & (b2 >> 4)); // 0xFFF000
      const secondWordIndex = (0xF00 & (b2 << 8)) + (0x0FF & b3); // 0x000FFF
      console.assert(firstWordIndex < WordEncoder.WORD_COUNT);
      console.assert(secondWordIndex < WordEncoder.WORD_COUNT);
      result.push(this.words[firstWordIndex], this.words[secondWordIndex]);
    }
    return result.join(WordEncoder.DELIMITER);
  }

  public decode(encoded: string): Uint8Array {
    const split = encoded.split(/\s+/).filter(s => s !== '');
    if (split.length % 2 != 0) {
      throw new Error(`input needs to be a multiple of two words: "${encoded}"`);
    }
    const result = new Uint8Array(split.length / 2 * 3);
    for (let i = 0; i < split.length; i += 2) {
      const w1 = split[i];
      const w2 = split[i + 1];
      const firstWordIndex = this.indices.get(w1);
      const secondWordIndex = this.indices.get(w2);
      if (firstWordIndex === undefined || secondWordIndex === undefined) {
        throw new Error(`Can't decode "${w1} ${w2}". Word not in dictionary`);
      } else {
        const b1 = (0xFF & (firstWordIndex >> 4));
        const b2 = ((0xF0 & (firstWordIndex << 4)) + (0x0F & (secondWordIndex >> 8)));
        const b3 = (0xFF & secondWordIndex);
        result[i / 2 * 3] = b1;
        result[i / 2 * 3 + 1] = b2;
        result[i / 2 * 3 + 2] = b3;
      }
    }
    return result;
  }
}

export const wordEncoder = new WordEncoder(dictionary);

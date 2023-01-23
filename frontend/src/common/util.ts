export class Deferred<T> {
  public promise: Promise<T>;
  public reject: (reason?: any) => void;
  public resolve: (value: T) => void;

  constructor() {
    this.reject = () => { };
    this.resolve = () => { };
    this.promise = new Promise<T>((resolve, reject) => {
      this.reject = reject;
      this.resolve = resolve;
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
export const debounce = (func: Function, wait = 300) => {
  let timeoutId: ReturnType<typeof setTimeout>;
  function debounceCore(this: any, ...args: any[]) {
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
  static TABLE = new Uint32Array(256);

  static {
    // Pre-generate crc32 polynomial lookup table
    // http://wiki.osdev.org/CRC32#Building_the_Lookup_Table
    // ... Actually use Alex's because it generates the correct bit order
    //     so no need for the reversal function
    for (var i = 256; i--;) {
      var tmp = i;
      for (var k = 8; k--;) {
        tmp = tmp & 1 ? 3988292384 ^ tmp >>> 1 : tmp >>> 1;
      }
      CRC32.TABLE[i] = tmp;
    }
  }

  public static compute (data: Uint8Array): number {
    // crc32b
    // Example input        : [97, 98, 99, 100, 101] (Uint8Array)
    // Example output       : 2240272485 (Uint32)
    var crc = -1; // Begin with all bits set ( 0xffffffff )
    for (var i = 0, l = data.length; i < l; i++) {
      crc = crc >>> 8 ^ CRC32.TABLE[ crc & 255 ^ data[i] ];
    }
    return (crc ^ -1) >>> 0; // Apply binary NOT
  }
}
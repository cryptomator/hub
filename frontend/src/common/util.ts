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

export class Deferred<T> {
  public promise: Promise<T>;
  public reject: (reason?: any) => void;
  public resolve: (value: T) => void;
  constructor() {
    this.reject = (reason) => { };
    this.resolve = (value) => { };
    this.promise = new Promise<T>((resolve, reject) => {
      this.reject = reject;
      this.resolve = resolve;
    });
  }
}

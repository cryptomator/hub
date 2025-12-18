import { describe, expect, it } from 'vitest';
import { CRC32, UTF8, wordEncoder } from '../../src/common/util';

describe('WordEncoder', () => {
  describe('decode(encode(x)) == x', () => {
    [0, 3, 6, 9, 30, 60, 90, 255].forEach(function (numBytes) {
      it(`input length length = ${numBytes}`, () => {
        const bytes = new Uint8Array(numBytes);
        for (let i = 0; i < bytes.length; i++) {
          bytes[i] = 0xFF & i;
        }

        const encoded = wordEncoder.encodePadded(bytes);
        const decoded = wordEncoder.decode(encoded);

        expect(decoded).toEqual(bytes);
      });
    });
  });
});

describe('CRC32', () => {
  it('crc32(\'123456789\') == 0xCBF43926', () => {
    const input = UTF8.encode('123456789');

    const result = CRC32.compute(input);

    expect(result).toEqual(0xCBF43926);
  });
});

import { expect, use as chaiUse } from 'chai';
import chaiBytes from 'chai-bytes';
import { describe } from 'mocha';
import { CRC32, wordEncoder } from '../../src/common/util';

chaiUse(chaiBytes);

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

        expect(decoded).to.equalBytes(bytes);
      });
    });
  });
});

describe('CRC32', () => {
  it('crc32(\'123456789\') == 0xCBF43926', () => {
    let input = new TextEncoder().encode('123456789');

    let result = CRC32.compute(input);

    expect(result).to.eql(0xCBF43926);
  });
});

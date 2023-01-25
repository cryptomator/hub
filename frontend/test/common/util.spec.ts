import { expect, use as chaiUse } from 'chai';
import chaiBytes from 'chai-bytes';
import { describe } from 'mocha';
import { wordEncoder } from '../../src/common/util';

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

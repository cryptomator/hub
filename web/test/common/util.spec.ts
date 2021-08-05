import { expect } from 'chai';
import { describe } from 'mocha';
import { Base64Url } from '../../src/common/util';

describe('Base64Url', () => {

  describe('encode', () => {
    it('should encode to AAAAAAA', () => {
      const bytes = new Uint8Array(5);
      const base64 = Base64Url.encode(bytes)
      expect(base64).to.equal("AAAAAAA");
    })

    it('should encode to AAH-_w', () => {
      const bytes = new Uint8Array([0, 1, 254, 255]);
      const base64 = Base64Url.encode(bytes)
      expect(base64).to.equal("AAH-_w");
    })
  })

  describe('decode', () => {
    it('should decode AAAAAAA', () => {
      const base64 = Base64Url.decode("AAAAAAA")
      expect(base64).to.eql(new Uint8Array(5));
    })

    it('should decode AAH-_w', () => {
      const base64 = Base64Url.decode("AAH-_w")
      expect(base64).to.eql(new Uint8Array([0, 1, 254, 255]));
    })
  })
})

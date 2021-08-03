import { expect } from 'chai';
import { Base64Url } from '../src/vault';

describe('Base64Url', () => {
  it('should encode to AAAAAAA', () => {
    const emptyBytes = new Uint8Array(5);
    const base64 = Base64Url.encode(emptyBytes)
    expect(base64).to.equal("AAAAAAA");
  })

  it('should encode to AAH-_w', () => {
    const emptyBytes = new Uint8Array([0, 1, 254, 255]);
    const base64 = Base64Url.encode(emptyBytes)
    expect(base64).to.equal("AAH-_w");
  })
})

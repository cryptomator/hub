import { Crypto } from '@peculiar/webcrypto';
import { expect } from 'chai';
import { describe } from 'mocha';
import { Masterkey, WrappedMasterkey } from '../../src/common/crypto';

describe('Masterkey', () => {

  before(done => {
    // since this test runs on Node, we need to replace window.crypto:
    // @ts-ignore: global not defined (but will be available within Node)
    global.crypto = new Crypto();
    done()
  })

  it('create', async () => {
    const orig = await Masterkey.create();

    expect(orig).to.be.not.null
  })

  it('create and wrap', async () => {
    const orig = await Masterkey.create();
    const wrapped = await orig.wrap("pass");

    expect(wrapped).to.be.not.null
  })

  describe('from wrapped', () => {
    let wrapped: WrappedMasterkey;

    beforeEach(async () => {
      wrapped = await (await Masterkey.create()).wrap("pass");
    })

    foo: `
    stack:'Error: Trying to add data in unsupported state\n  
     at Decipheriv.update (node:internal/crypto/cipher:183:29)\n 
     at Function.decryptAesKW (/Users/sebastian/Documents/Cryptomator/Hub/web/node_modules/@peculiar/webcrypto/build/webcrypto.js:269:28)
     at Function.decrypt (/Users/sebastian/Documents/Cryptomator/Hub/web/node_modules/@peculiar/webcrypto/build/webcrypto.js:203:29)\n 
     at AesKwProvider.onDecrypt (/Users/sebastian/Documents/Cryptomator/Hub/web/node_modules/@peculiar/webcrypto/build/webcrypto.js:511:26)\n 
     at AesKwProvider.decrypt (/Users/sebastian/Documents/Cryptomator/Hub/web/node_modules/webcrypto-core/build/webcrypto-core.js:176:31)\n 
     at SubtleCrypto.unwrapKey (/Users/sebastian/Documents/Cryptomator/Hub/web/node_modules/webcrypto-core/build/webcrypto-core.js:892:38)\n 
     at Function.unwrap (/Users/sebastian/Documents/Cryptomator/Hub/web/src/common/crypto.ts:78:31)'
    `


    it('unwrap with wrong pw', async () => {
      return Masterkey.unwrap("wrong", wrapped)
        .then(k => {
          expect.fail('should not succeed');
        })
        .catch(e => {
          expect(e).to.have.property('message').that.does.not.contain('should not succeed');
        });
    })

  })


})

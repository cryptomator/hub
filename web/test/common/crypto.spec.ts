import { Crypto } from '@peculiar/webcrypto';
import { expect } from 'chai';
import { describe } from 'mocha';
import { Masterkey, WrappedMasterkey, X936 } from '../../src/common/crypto';

describe('crypto', () => {

  before(done => {
    // since this test runs on Node, we need to replace window.crypto:
    // @ts-ignore: global not defined (but will be available within Node)
    global.crypto = new Crypto();
    done()
  })


  describe('X936', () => {

    const fromHexString = (hexString: string) => {
      var matches = hexString.match(/[\dA-F]{2}/gi);
      if (matches == null) {
        return new Uint8Array(0)
      } else {
        var bytes = matches!.map(hex => parseInt(hex, 16));
        return new Uint8Array(bytes);
      }
    }


    it('kdf 1 ', async () => {
      const sharedSecret = fromHexString('96c05619d56c328ab95fe84b18264b08725b85e33fd34f08');
      const sharedInfo = new Uint8Array(0);

      const result = await X936.kdf(sharedSecret, sharedInfo, 16);

      const expectedResult = fromHexString('443024c3dae66b95e6f5670601558f71');
      expect(result).to.eql(expectedResult)
    })

    it('kdf 2', async () => {
      const sharedSecret = fromHexString('96f600b73ad6ac5629577eced51743dd2c24c21b1ac83ee4');
      const sharedInfo = new Uint8Array(0);

      const result = await X936.kdf(sharedSecret, sharedInfo, 16);

      const expectedResult = fromHexString('b6295162a7804f5667ba9070f82fa522');
      expect(result).to.eql(expectedResult)
    })

    it('kdf 3', async () => {
      const sharedSecret = fromHexString('22518b10e70f2a3f243810ae3254139efbee04aa57c7af7d');
      const sharedInfo = fromHexString('75eef81aa3041e33b80971203d2c0c52');

      const result = await X936.kdf(sharedSecret, sharedInfo, 128);

      const expectedResult = fromHexString('c498af77161cc59f2962b9a713e2b215152d139766ce34a776df11866a69bf2e52a13d9c7c6fc878c50c5ea0bc7b00e0da2447cfd874f6cf92f30d0097111485500c90c3af8b487872d04685d14c8d1dc8d7fa08beb0ce0ababc11f0bd496269142d43525a78e5bc79a17f59676a5706dc54d54d4d1f0bd7e386128ec26afc21');
      expect(result).to.eql(expectedResult)
    })

    it('kdf 4', async () => {
      const sharedSecret = fromHexString('7e335afa4b31d772c0635c7b0e06f26fcd781df947d2990a');
      const sharedInfo = fromHexString('d65a4812733f8cdbcdfb4b2f4c191d87');

      const result = await X936.kdf(sharedSecret, sharedInfo, 128);

      const expectedResult = fromHexString('c0bd9e38a8f9de14c2acd35b2f3410c6988cf02400543631e0d6a4c1d030365acbf398115e51aaddebdc9590664210f9aa9fed770d4c57edeafa0b8c14f93300865251218c262d63dadc47dfa0e0284826793985137e0a544ec80abf2fdf5ab90bdaea66204012efe34971dc431d625cd9a329b8217cc8fd0d9f02b13f2f6b0b');
      expect(result).to.eql(expectedResult)
    })

  })

  describe('Masterkey', () => {

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

});

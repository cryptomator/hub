import { Crypto } from '@peculiar/webcrypto';
import { expect, use as chaiUse } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { describe } from 'mocha';
import { base16 } from 'rfc4648';
import { Masterkey, WrappedMasterkey, X936 } from '../../src/common/crypto';

chaiUse(chaiAsPromised);

describe('crypto', () => {

  before(done => {
    // since this test runs on Node, we need to replace window.crypto:
    // @ts-ignore: global not defined (but will be available within Node)
    global.crypto = new Crypto();
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };
    done();
  });


  describe('X936', () => {

    it('kdf 1 ', async () => {
      const sharedSecret = base16.parse('96c05619d56c328ab95fe84b18264b08725b85e33fd34f08');
      const sharedInfo = new Uint8Array(0);

      const result = await X936.kdf(sharedSecret, sharedInfo, 16);

      const expectedResult = base16.parse('443024c3dae66b95e6f5670601558f71');
      expect(result).to.eql(expectedResult);
    });

    it('kdf 2', async () => {
      const sharedSecret = base16.parse('96f600b73ad6ac5629577eced51743dd2c24c21b1ac83ee4');
      const sharedInfo = new Uint8Array(0);

      const result = await X936.kdf(sharedSecret, sharedInfo, 16);

      const expectedResult = base16.parse('b6295162a7804f5667ba9070f82fa522');
      expect(result).to.eql(expectedResult);
    });

    it('kdf 3', async () => {
      const sharedSecret = base16.parse('22518b10e70f2a3f243810ae3254139efbee04aa57c7af7d');
      const sharedInfo = base16.parse('75eef81aa3041e33b80971203d2c0c52');

      const result = await X936.kdf(sharedSecret, sharedInfo, 128);

      const expectedResult = base16.parse('c498af77161cc59f2962b9a713e2b215152d139766ce34a776df11866a69bf2e52a13d9c7c6fc878c50c5ea0bc7b00e0da2447cfd874f6cf92f30d0097111485500c90c3af8b487872d04685d14c8d1dc8d7fa08beb0ce0ababc11f0bd496269142d43525a78e5bc79a17f59676a5706dc54d54d4d1f0bd7e386128ec26afc21');
      expect(result).to.eql(expectedResult);
    });

    it('kdf 4', async () => {
      const sharedSecret = base16.parse('7e335afa4b31d772c0635c7b0e06f26fcd781df947d2990a');
      const sharedInfo = base16.parse('d65a4812733f8cdbcdfb4b2f4c191d87');

      const result = await X936.kdf(sharedSecret, sharedInfo, 128);

      const expectedResult = base16.parse('c0bd9e38a8f9de14c2acd35b2f3410c6988cf02400543631e0d6a4c1d030365acbf398115e51aaddebdc9590664210f9aa9fed770d4c57edeafa0b8c14f93300865251218c262d63dadc47dfa0e0284826793985137e0a544ec80abf2fdf5ab90bdaea66204012efe34971dc431d625cd9a329b8217cc8fd0d9f02b13f2f6b0b');
      expect(result).to.eql(expectedResult);
    });

  });

  describe('Masterkey', () => {

    it('create', async () => {
      const orig = await Masterkey.create();

      expect(orig).to.be.not.null;
    });

    it('unwrap() with wrong pw', () => {
      const wrapped = new WrappedMasterkey('reGgZc4NTcTIyggz36K_E6aA6ttOJv2T7z6Fb3OGdvFf8uMvYS87J3hR7Pxavhmv3LyjCs8LUl_oLffoo2QsKtQHn0PLd-jb', 'v_4KUaPQAu-rAFTUbxLSQA', 1);
      expect(Masterkey.unwrap('wrong', wrapped)).to.be.rejected;
    });

    it('unwrap() with correct pw', () => {
      const wrapped = new WrappedMasterkey('reGgZc4NTcTIyggz36K_E6aA6ttOJv2T7z6Fb3OGdvFf8uMvYS87J3hR7Pxavhmv3LyjCs8LUl_oLffoo2QsKtQHn0PLd-jb', 'v_4KUaPQAu-rAFTUbxLSQA', 1);
      expect(Masterkey.unwrap('pass', wrapped)).to.be.fulfilled;
    });

    describe('Created Masterkey', () => {
      let masterkey: Masterkey;

      beforeEach(async () => {
        masterkey = await TestMasterkey.create();
      });

      it('wrap()', async () => {
        const wrapped = await masterkey.wrap('pass');

        expect(wrapped).to.be.not.null;
      });


    });


  });

  describe('Hash directory id', () => {
    it('root directory', async () => {
      const masterkey = await TestMasterkey.createMasterkey();
      const result = await masterkey.hashDirectoryId('');
      expect(result).to.eql('VLWEHT553J5DR7OZLRJAYDIWFCXZABOD');
    });

    it('specific directory', async () => {
      const masterkey = await TestMasterkey.createMasterkey();
      const result = await masterkey.hashDirectoryId('918acfbd-a467-3f77-93f1-f4a44f9cfe9c');
      expect(result).to.eql('7C3USOO3VU7IVQRKFMRFV3QE4VEZJECV');
    });
  });
});

class TestMasterkey extends Masterkey {
  constructor(key: CryptoKey) {
    super(key);
  }

  static async createMasterkey() {
    const raw = new Uint8Array(64);
    raw.fill(0x55, 0, 32);
    raw.fill(0x77, 32, 64);
    const key = await crypto.subtle.importKey(
      'raw',
      raw,
      {
        name: 'HMAC',
        hash: 'SHA-256',
        length: 512
      },
      true,
      ['sign']
    );
    return new TestMasterkey(key);
  }
}

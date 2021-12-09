import { Crypto } from '@peculiar/webcrypto';
import { expect, use as chaiUse } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { describe } from 'mocha';
import { base64 } from 'rfc4648';
import { Masterkey, WrappedMasterkey } from '../../src/common/crypto';

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

  describe('Masterkey', () => {

    it('create()', async () => {
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

      it('encryptForDevice()', async () => {
        const deviceKey = base64.parse('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAERxQR+NRN6Wga01370uBBzr2NHDbKIC56tPUEq2HX64RhITGhii8Zzbkb1HnRmdF0aq6uqmUy4jUhuxnKxsv59A6JeK7Unn+mpmm3pQAygjoGc9wrvoH4HWJSQYUlsXDu');

        const encrypted = await masterkey.encryptForDevice(deviceKey);
        expect(encrypted).to.be.not.null;
      });

    });


  });

  describe('Hash directory id', () => {
    it('root directory', async () => {
      const masterkey = await TestMasterkey.create();
      const result = await masterkey.hashDirectoryId('');
      expect(result).to.eql('VLWEHT553J5DR7OZLRJAYDIWFCXZABOD');
    });

    it('specific directory', async () => {
      const masterkey = await TestMasterkey.create();
      const result = await masterkey.hashDirectoryId('918acfbd-a467-3f77-93f1-f4a44f9cfe9c');
      expect(result).to.eql('7C3USOO3VU7IVQRKFMRFV3QE4VEZJECV');
    });
  });
});

class TestMasterkey extends Masterkey {
  constructor(key: CryptoKey) {
    super(key);
  }

  static async create() {
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

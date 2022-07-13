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
    global.crypto = require('node:crypto').webcrypto;
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };
    done();
  });

  describe('Masterkey', () => {

    const wrapped: WrappedMasterkey = {
      masterkey: 'zuYM3aANgVKaMI2ZkIXp3jt2q_jC5APBVf_W7LxlVEz…u6D9neQlc3b5X9j12fVIkv9vTztBezIMGppCQWOCKk=',
      sk: 'H3V026OfdgbdKbn43lq18fYageZcfRP7evV3ajTqzCvT/Ul…Co/rTT3XCRUdrfTBa52IULAUACUc4j+zUS6JF2CDAaU5kP',
      pk: 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1…4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW',
      salt: 'EG3Z0m2huTx74FpOCeSbJw',
      iterations: 1
    };

    it('create()', async () => {
      const orig = await Masterkey.create();

      expect(orig).to.be.not.null;
    });

    it('unwrap() with wrong pw', () => {
      expect(Masterkey.unwrap('wrong', wrapped)).to.be.rejected;
    });

    it('unwrap() with correct pw', () => {
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
  constructor(key: CryptoKey, keypair: CryptoKeyPair) {
    super(key, keypair);
  }

  static async create() {
    const raw = new Uint8Array(64);
    raw.fill(0x55, 0, 32);
    raw.fill(0x77, 32, 64);
    const key = await crypto.subtle.importKey(
      'raw',
      raw,
      Masterkey.KEY_DESIGNATION,
      true,
      ['sign']
    );
    const sk = await crypto.subtle.importKey(
      'jwk',
      {
        kty: 'EC',
        crv: 'P-384',
        // key coordinates from MDN examples:
        d: 'wouCtU7Nw4E8_7n5C1-xBjB4xqSb_liZhYMsy8MGgxUny6Q8NCoH9xSiviwLFfK_',
        x: 'SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ',
        y: 'hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW'

      },
      Masterkey.SK_DESIGNATION,
      true,
      ['sign']
    );
    const pk = await crypto.subtle.importKey(
      'jwk',
      {
        kty: 'EC',
        crv: 'P-384',
        x: 'SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ',
        y: 'hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW'

      },
      Masterkey.SK_DESIGNATION,
      true,
      ['verify']
    );
    return new TestMasterkey(key, { privateKey: sk, publicKey: pk });
  }
}

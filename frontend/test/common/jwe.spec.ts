import { expect } from 'chai';
import { describe } from 'mocha';
import { base64url } from 'rfc4648';
import { ConcatKDF, JWE, JWEHeader } from '../../src/common/jwe';

describe('JWE', () => {
  before(done => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };
    done();
  });

  describe('NIST SP 800-56A Rev. 2 Section 5.8.1', () => {
    it('should fulfill test vectors', async () => {
      const z = new Uint8Array([158, 86, 217, 29, 129, 113, 53, 211, 114, 131, 66, 131, 191, 132, 38, 156, 251, 49, 110, 163, 218, 128, 106, 72, 246, 218, 167, 121, 140, 254, 144, 196]);
      const algorithmId = new Uint8Array([0, 0, 0, 7, 65, 49, 50, 56, 71, 67, 77]);
      const partyUInfo = new Uint8Array([0, 0, 0, 5, 65, 108, 105, 99, 101]);
      const partyVInfo = new Uint8Array([0, 0, 0, 3, 66, 111, 98]);
      const suppPubInfo = new Uint8Array([0, 0, 0, 128]);

      const otherInfo = new Uint8Array([...algorithmId, ...partyUInfo, ...partyVInfo, ...new Uint8Array(suppPubInfo)]);
      let derivedKey = await ConcatKDF.kdf(z, 16, otherInfo);
      expect(new Uint8Array(derivedKey), 'derived key').to.eql(new Uint8Array([86, 170, 141, 234, 248, 35, 109, 32, 92, 34, 40, 205, 113, 167, 16, 26]));
    });
  });

  describe('RFC 7516 / RFC 7518', () => {
    it('should build JWE for given public key', async () => {
      const recipientPublicKey = await crypto.subtle.importKey(
        'jwk',
        {
          kty: 'EC',
          crv: 'P-384',
          x: 'RxQR-NRN6Wga01370uBBzr2NHDbKIC56tPUEq2HX64RhITGhii8Zzbkb1HnRmdF0',
          y: 'aq6uqmUy4jUhuxnKxsv59A6JeK7Unn-mpmm3pQAygjoGc9wrvoH4HWJSQYUlsXDu'
        },
        {
          name: 'ECDH',
          namedCurve: 'P-384'
        },
        false,
        []
      );

      const result = await JWE.build({ hello: 'world' }, recipientPublicKey);
      expect(result).not.to.be.null; // TODO do some tests
    });

    /**
     * All these test vectors are taken from https://www.rfc-editor.org/rfc/rfc7518#appendix-C
     */
    it('should derive expected key using ECDH-ES', async () => {
      const alicePub: JsonWebKey = {
        kty: 'EC',
        crv: 'P-256',
        x: 'gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0',
        y: 'SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps'
      };
      const alicePriv: JsonWebKey = {
        ...alicePub,
        d: '0_NxaRPUMQoAJt50Gz8YiTr8gRTwyEaCumd-MToTmIo'
      };
      const alice = await crypto.subtle.importKey(
        'jwk',
        alicePriv,
        {
          name: 'ECDH',
          namedCurve: 'P-256'
        },
        false,
        ['deriveBits']
      );
      expect(alice.type, 'alice\'s key type').to.eql('private');
      const bob = await crypto.subtle.importKey(
        'jwk',
        {
          kty: 'EC',
          crv: 'P-256',
          x: 'weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ',
          y: 'e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck'
        },
        {
          name: 'ECDH',
          namedCurve: 'P-256'
        },
        false,
        []
      );
      expect(bob.type, 'bob\'s key type').to.eql('public');

      const apu = new Uint8Array([65, 108, 105, 99, 101]);
      const apv = new Uint8Array([66, 111, 98]);
      const header = new JWEHeader('ignored', 'A128GCM', alicePub, base64url.stringify(apu, { pad: false }), base64url.stringify(apv, { pad: false }));
      const derived = await JWE.deriveContentKey(bob, alice, 256, 16, header, true);
      const derivedBytes = await crypto.subtle.exportKey('raw', derived);
      expect(new Uint8Array(derivedBytes), 'derived key').to.eql(new Uint8Array([86, 170, 141, 234, 248, 35, 109, 32, 92, 34, 40, 205, 113, 167, 16, 26]));
    });
  });
});

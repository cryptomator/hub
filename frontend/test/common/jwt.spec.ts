import { expect } from 'chai';
import { describe } from 'mocha';
import { webcrypto } from 'node:crypto';
import { JWT, JWTHeader } from '../../src/common/jwt';

describe('JWT', () => {
  before(done => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: webcrypto });
    // @ts-expect-error: incomplete 'window' type
    global.window = { crypto: global.crypto };
    done();
  });

  describe('RFC 7515 / RFC 7519', () => {
    let signerKey: CryptoKeyPair;

    beforeEach(async () => {
      const signerPublicJwk: JsonWebKey = {
        kty: 'EC',
        crv: 'P-384',
        // key coordinates from MDN examples:
        x: 'SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ',
        y: 'hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW'
      };
      const signerPrivateJwk: JsonWebKey = {
        ...signerPublicJwk,
        // key coordinates from MDN examples:
        d: 'wouCtU7Nw4E8_7n5C1-xBjB4xqSb_liZhYMsy8MGgxUny6Q8NCoH9xSiviwLFfK_',
      };
      signerKey = {
        privateKey: await crypto.subtle.importKey('jwk', signerPrivateJwk, { name: 'ECDSA', namedCurve: 'P-384' }, false, ['sign']),
        publicKey: await crypto.subtle.importKey('jwk', signerPublicJwk, { name: 'ECDSA', namedCurve: 'P-384' }, true, ['verify'])
      };
    });

    it('es384sign()', async () => {
      const encodedHeader = 'eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0';
      const encodedPayload = 'eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0';

      const encodedSignature = await JWT.es384sign(encodedHeader, encodedPayload, signerKey.privateKey);

      expect(encodedSignature).to.be.a('string');
    });

    it('es384verify() a valid signature', async () => {
      const jwt = 'eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0.eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0.9jS7HDRkbwEbmJ_cpkFHcQuNHSsOzSO3ObkT_FBQIIJehYYk-1aAK0KVnOgeDg6hVELy5-XcRHOCETwuTuYG5eQ3jIbxpTviHttJ-r26BYynw6dlmJTuLSvsTjtpoTa_';

      const validSignature = await JWT.es384verify(jwt, signerKey.publicKey);

      expect(validSignature).to.be.true;
    });

    it('es384verify() an invalid signature', async () => {
      const jwt = 'eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0.eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0.9jS7HDRkbwEbmJ_cpkFHcQuNHSsOzSO3ObkT_FBQIIJehYYk-1aAK0KVnOgeDg6hVELy5-XcRHOCETwuTuYG5eQ3jIbxpTviHttJ-r26BYynw6dlmJTuLSvsTjtpoTaX';

      const validSignature = await JWT.es384verify(jwt, signerKey.publicKey);

      expect(validSignature).to.be.false;
    });

    it('build() ES384-signed JWT', async () => {
      const header: JWTHeader = {
        alg: 'ES384',
        typ: 'JWT',
        b64: true,

      };
      const payload = {
        foo: 42,
        bar: 'lol',
        obj: { nested: true }
      };

      const jwt = await JWT.build(header, payload, signerKey.privateKey);

      expect(jwt).to.be.not.null;
    });

    it('parse() ES384-signed JWT', async () => {
      const jwt = 'eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0.eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0.9jS7HDRkbwEbmJ_cpkFHcQuNHSsOzSO3ObkT_FBQIIJehYYk-1aAK0KVnOgeDg6hVELy5-XcRHOCETwuTuYG5eQ3jIbxpTviHttJ-r26BYynw6dlmJTuLSvsTjtpoTa_';

      const [header, payload] = await JWT.parse(jwt, signerKey.publicKey);

      expect(header).to.deep.equal({ alg: 'ES384', typ: 'JWT', b64: true });
      expect(payload).to.deep.equal({ foo: 42, bar: 'lol', obj: { nested: true } });
    });
  });
});

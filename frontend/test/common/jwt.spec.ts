import { expect } from 'chai';
import { describe } from 'mocha';
import { JWT, JWTHeader } from '../../src/common/jwt';

describe('JWT', () => {

  before(done => {
    // since this test runs on Node, we need to replace window.crypto:
    // @ts-ignore: global not defined (but will be available within Node)
    global.crypto = require('node:crypto').webcrypto;
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };
    done();
  });

  describe('RFC 7515 / RFC 7519', () => {

    let signerPrivateKey: CryptoKey;

    beforeEach(async () => {
      signerPrivateKey = await crypto.subtle.importKey(
        'jwk',
        {
          kty: 'EC',
          crv: 'P-384',
          // key coordinates from MDN examples:
          d: 'wouCtU7Nw4E8_7n5C1-xBjB4xqSb_liZhYMsy8MGgxUny6Q8NCoH9xSiviwLFfK_',
          x: 'SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ',
          y: 'hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW'
        },
        {
          name: 'ECDSA',
          namedCurve: 'P-384'
        },
        false,
        ['sign']
      );
    });

    it('es384sign()', () => {
      const encodedHeader = 'eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsImI2NCI6dHJ1ZX0';
      const encodedPayload = 'eyJmb28iOjQyLCJiYXIiOiJsb2wiLCJvYmoiOnsibmVzdGVkIjp0cnVlfX0';

      const encodedSignature = JWT.es384sign(encodedHeader, encodedPayload, signerPrivateKey);

      return expect(encodedSignature).to.eventually.be.fulfilled;
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

      const jwt = await JWT.build(header, payload, signerPrivateKey);

      expect(jwt).to.be.not.null;
    });

  });

});

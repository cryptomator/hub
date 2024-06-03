import { expect, should } from 'chai';
import { describe } from 'mocha';
import { JWT, JWTHeader } from '../../src/common/jwt';

describe('JWT', () => {
  before(done => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
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

  describe('Simple JWT parsing',() => {
    it('parse() accepts any valid base64url encoded *JWT-likeish* string.', () => {
      const validJSON = 'eyJmb28iOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYmFyIjp0cnVlfQ'; // '{"foo":"1234567890","name":"John Doe","bar":true}'

      const parseResult = JWT.parse(validJSON + '.' + validJSON + '.' + validJSON);
      expect(parseResult).to.be.fulfilled;
    }),

    it('parse() throws error on invalid base64url encoded jwt string', () => {
      const invalidJSON = 'eyJhbGciOiJIUzI1NiIsInR5cH0'; //{"foo":"bar","error}
      const validJSON = 'eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ'; //{"sub":"1234567890","name":"John Doe","iat":1516239022}
      const noBase64 = 'foob$r';
      const validBase64 = 'SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c';

      const notEnoughSections = JWT.parse('a.b');
      const emptyHeader = JWT.parse('.a.b');
      const emptyPayload = JWT.parse('a..b');
      const emptySignature = JWT.parse('a.b.');
      const tooManySections = JWT.parse('a.b.c.d');

      const headerNoBase64 = JWT.parse(noBase64 + '.' + validJSON + '.' + validBase64);
      const payloadNoBase64 = JWT.parse(validJSON + '.' + noBase64 + '.' + validBase64);
      const signatureNoBase64 = JWT.parse(validJSON + '.' + validJSON + '.' + noBase64);

      const headerInvalidJSON = JWT.parse(invalidJSON + '.' + validJSON + '.' + validBase64);
      const payloadInvalidJSON = JWT.parse(validJSON + '.' + invalidJSON + '.' + validBase64);
      return Promise.all([
        expect(notEnoughSections).to.be.rejectedWith(Error, /Invalid JWT/),
        expect(emptyHeader).to.be.rejectedWith(Error, /Invalid JWT/),
        expect(emptyPayload).to.be.rejectedWith(Error, /Invalid JWT/),
        expect(emptySignature).to.be.rejectedWith(Error, /Invalid JWT/),
        expect(tooManySections).to.be.rejectedWith(Error, /Invalid JWT/),
        expect(headerNoBase64).to.be.rejected,
        expect(payloadNoBase64).to.be.rejected,
        expect(signatureNoBase64).to.be.rejected,
        expect(headerInvalidJSON).to.be.rejected,
        expect(payloadInvalidJSON).to.be.rejected,
      ]);
    });
  });
});

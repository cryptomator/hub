import { base64, base64url } from 'rfc4648';
import { beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { UnwrapKeyError, UserKeys, getJwkThumbprint, getJwkThumbprintStr } from '../../src/common/crypto';

// key coordinates from MDN examples:
const alicePublic: JsonWebKey = {
  kty: 'EC',
  crv: 'P-384',
  x: 'SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ',
  y: 'hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW'
};
const alicePrivate: JsonWebKey = {
  ...alicePublic,
  d: 'wouCtU7Nw4E8_7n5C1-xBjB4xqSb_liZhYMsy8MGgxUny6Q8NCoH9xSiviwLFfK_',
};

// key coordinates from project wycheproof:
const bobPublic: JsonWebKey = {
  kty: 'EC',
  crv: 'P-384',
  x: 'em7I0xHVyliLrtQb4-mPMMkpSETsu2KZlWU2NdvCLaLwg_KXEeD5xZY7wCG9jLIQ',
  y: 'na9WpV-IOnIAzqnE3kRIjm3En7nDlPUctaSfxp1-igNHkpY65Oq8Y0g6LPGomejI'
};
const bobPrivate: JsonWebKey = {
  ...bobPublic,
  d: 'dm5hQlstqfhGwJ_DVkuTpvhgO3OSx4UWW_INqUjEn9H7He5O3WQ1a58hxYi3Xf2B',
};

describe('crypto', () => {
  let aliceEcdh: CryptoKeyPair, aliceEcdsa: CryptoKeyPair, bobEcdh: CryptoKeyPair;

  beforeAll(async () => {
    // prepare some test key pairs:
    const ecdhP384: EcKeyImportParams = { name: 'ECDH', namedCurve: 'P-384' };
    const ecdsaP384: EcKeyImportParams = { name: 'ECDSA', namedCurve: 'P-384' };
    const aliceEcdhPrv = crypto.subtle.importKey('jwk', alicePrivate, ecdhP384, true, ['deriveKey', 'deriveBits']);
    const aliceEcdhPub = crypto.subtle.importKey('jwk', alicePublic, ecdhP384, true, []);
    const aliceEcdsaPrv = crypto.subtle.importKey('jwk', alicePrivate, ecdsaP384, true, ['sign']);
    const aliceEcdsaPub = crypto.subtle.importKey('jwk', alicePublic, ecdsaP384, true, []);
    const bobEcdhPrv = crypto.subtle.importKey('jwk', bobPrivate, ecdhP384, true, ['deriveKey', 'deriveBits']);
    const bobEcdhPub = crypto.subtle.importKey('jwk', bobPublic, ecdhP384, true, []);
    aliceEcdh = { privateKey: await aliceEcdhPrv, publicKey: await aliceEcdhPub };
    aliceEcdsa = { privateKey: await aliceEcdsaPrv, publicKey: await aliceEcdsaPub };
    bobEcdh = { privateKey: await bobEcdhPrv, publicKey: await bobEcdhPub };
  });

  describe('UserKeys', () => {
    it('create()', async () => {
      const orig = await UserKeys.create();

      expect(orig).not.toBeNull();
    });

    it('recover()', async () => {
      const jwe = 'eyJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIiwicDJzIjoibUJ6Q3dkQVpDVEdJcm8tZlppYkJzZyIsInAyYyI6MTAwMCwiYXB1IjoiIiwiYXB2IjoiIn0.aJQ8SpBAc8-Qj7e2nHcBrXjUg1ffYr-yYzObbhLfP9zIS6xDb_QhGA.h3kS7p9X3kwN0hNN.MXYK5F62aELyVyiiixvdtF5qc2b8hVj1o5pLgmd-pZy5Kw5KDW3QUtpfGM5NdMNSlCJjg7ffTt0oBpD67jIatN2PhqJ9A_G_n-UleYDwSv_c-GLqEuJW7gYm-dydigqjZVQsQWdTiVPaO_USqrPkW7fBm0jG7ibIO4tq23tszbzIFWjrZgUwB9UrBWLzoreBci9zK8iSKZyjSJl75IVBzoVPDGEMukGf-Vie1gHrcLy7OaJ-0_K5Ncwq2-YuDhtIvuq5fpE06RYAFotRPsObmcDGz4BzjTRjSgbbjWBZF0n2rwkpnTOUodkUvvWSfPL7se8-6hke9wNC7YV0rleeiTkRrRUl5wFTce4ahH_paIGMA0sJXV4B6rbb2XV_j9s54edER869jG5m5Vzo4T4HYkmBLXT5JqAvEq4cR6YmBVlZ8REVI2TxXoh56Lb9Z6bEBp2GRKy9jgOcka2xGS6S5rh3sNcfyXjcBJXR4p4yNf8ksxLEQmR2Dt46xKGkqEiTt5uYSCx0HAUyYksr7nWmUkhNQZJwswDfyMGNqIYo6ZRY4gWk9b-GG9-qm27eBl_7oXE8IvWV9mPSiNZgwUcUc2nw35Oj0s9q99xMjJ4sQxPMlKmtz2_1OfN4oXZnb36660Gr3R-txvhML4GucWEe7WgdJlgrLduJxvEnv0Nf6lacdb7se592AlqGpQegfWUWwD3DRSeha7wb0aeqyNXSP7B4aBrd4eLVKsvNUjGeJ6Riy8-5tPFhIh7x7OXXqh8dUyHF9k_fpnbXjT2Ljq5Jn2jLvBUC7HX-ixtGK9Q5GoIg2IDG6-zglcpvLFLaJgZ8-_XIPTG6dHv8u-ST3vHXZeKE4mbkmXgVCsov11OUtT2H1pd79FKUzmGuWHSIAHGnjqsrcLXaqQ5nVF3c0MIt5U0Q6rLeBindcKQ6j648j-7B9mkjedD6_fTNROsK9gBQQ6hefTjCLJT6TPLMbCiR91UYB4YJ1TsNV37WiQgac-R3NCQnj0lG6hpfFLH5TWZ7Q1_7guUZnzhLSbsa36weO4h9fNdur4S6oI5I.4xnbrXWQgQgNV-pFmCQtag';

      const result = await UserKeys.recover(jwe, 'foo', aliceEcdh.publicKey, aliceEcdsa.publicKey);

      expect(result).to.be.not.null;
    });

    it('recover() with incorrect password', async () => {
      const jwe = 'eyJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIiwicDJzIjoibUJ6Q3dkQVpDVEdJcm8tZlppYkJzZyIsInAyYyI6MTAwMCwiYXB1IjoiIiwiYXB2IjoiIn0.aJQ8SpBAc8-Qj7e2nHcBrXjUg1ffYr-yYzObbhLfP9zIS6xDb_QhGA.h3kS7p9X3kwN0hNN.MXYK5F62aELyVyiiixvdtF5qc2b8hVj1o5pLgmd-pZy5Kw5KDW3QUtpfGM5NdMNSlCJjg7ffTt0oBpD67jIatN2PhqJ9A_G_n-UleYDwSv_c-GLqEuJW7gYm-dydigqjZVQsQWdTiVPaO_USqrPkW7fBm0jG7ibIO4tq23tszbzIFWjrZgUwB9UrBWLzoreBci9zK8iSKZyjSJl75IVBzoVPDGEMukGf-Vie1gHrcLy7OaJ-0_K5Ncwq2-YuDhtIvuq5fpE06RYAFotRPsObmcDGz4BzjTRjSgbbjWBZF0n2rwkpnTOUodkUvvWSfPL7se8-6hke9wNC7YV0rleeiTkRrRUl5wFTce4ahH_paIGMA0sJXV4B6rbb2XV_j9s54edER869jG5m5Vzo4T4HYkmBLXT5JqAvEq4cR6YmBVlZ8REVI2TxXoh56Lb9Z6bEBp2GRKy9jgOcka2xGS6S5rh3sNcfyXjcBJXR4p4yNf8ksxLEQmR2Dt46xKGkqEiTt5uYSCx0HAUyYksr7nWmUkhNQZJwswDfyMGNqIYo6ZRY4gWk9b-GG9-qm27eBl_7oXE8IvWV9mPSiNZgwUcUc2nw35Oj0s9q99xMjJ4sQxPMlKmtz2_1OfN4oXZnb36660Gr3R-txvhML4GucWEe7WgdJlgrLduJxvEnv0Nf6lacdb7se592AlqGpQegfWUWwD3DRSeha7wb0aeqyNXSP7B4aBrd4eLVKsvNUjGeJ6Riy8-5tPFhIh7x7OXXqh8dUyHF9k_fpnbXjT2Ljq5Jn2jLvBUC7HX-ixtGK9Q5GoIg2IDG6-zglcpvLFLaJgZ8-_XIPTG6dHv8u-ST3vHXZeKE4mbkmXgVCsov11OUtT2H1pd79FKUzmGuWHSIAHGnjqsrcLXaqQ5nVF3c0MIt5U0Q6rLeBindcKQ6j648j-7B9mkjedD6_fTNROsK9gBQQ6hefTjCLJT6TPLMbCiR91UYB4YJ1TsNV37WiQgac-R3NCQnj0lG6hpfFLH5TWZ7Q1_7guUZnzhLSbsa36weO4h9fNdur4S6oI5I.4xnbrXWQgQgNV-pFmCQtag';

      const attempt = UserKeys.recover(jwe, 'wrong', aliceEcdh.publicKey, aliceEcdsa.publicKey);

      await expect(attempt).rejects.toThrow(UnwrapKeyError);
    });

    it('decryptOnBrowser()', async () => {
      const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi5kZXZpY2VLZXkiLCJhbGciOiJFQ0RILUVTK0EyNTZLVyIsImVwayI6eyJrZXlfb3BzIjpbXSwiZXh0Ijp0cnVlLCJrdHkiOiJFQyIsIngiOiJETTNILXV2TDB2VnMwNDFCTUU2LWM5ZW1BN1NCellDSGI1czJTV1NsM05nWDM2azhPTWVyVlR6VUx4N185bDU4IiwieSI6InBERXk4WnpqXzNNZldtamt5YlpjV3oyVVBJdGhnVWEwSGk3Yks3QVBXTHFSRnFwZmh1eWx4MTdadFhnNnBPZUoiLCJjcnYiOiJQLTM4NCJ9LCJhcHUiOiIiLCJhcHYiOiIifQ.ppy4wTaFZPxHxq1s4LPbHHkC1EFCWq_i0uvvEE9tuO9shzpazi9f7w.fEMTO_2ueYtRd9F0.TjVy520PvstERopn4qhpQB9chotb80_vV3Zj78veWQdzg49a6MH51TVqKKqX1n-6_HyS_chDNR9P0kdLF2ZzrWUJ0x5aenmG7ALeLjrOrxMEf0Fsboxn04q1bRDBq-Gse7Iwhk8NZvVMs_xXDL3wTeIzff_YeeNZcJS_xXHzMpbLbWjQs_x60t56uP2_fF8Wm4Fr5srBOuB5-E3VPYhm_LVOYvBOYKJDl9awWw809UvZwotorI7jG_TwLqYCIgrdkvQxS5Sz05gWdM6yMrYcL5VInxJGSt-dL_0nomN0btmHOv51qD_dyxvFknPkESWD_l4.pWlTbfYgQ_rWI0AY8HXcirJ9pIg7Oh0AvjwR0mdM42Y';
      const deviceKey = bobEcdh;

      const result = UserKeys.decryptOnBrowser(jwe, deviceKey.privateKey, aliceEcdh.publicKey, aliceEcdsa.publicKey);

      expect(result).to.be.not.null;
    });

    it('decryptOnBrowser() with incorrect device key', async () => {
      const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi5kZXZpY2VLZXkiLCJhbGciOiJFQ0RILUVTK0EyNTZLVyIsImVwayI6eyJrZXlfb3BzIjpbXSwiZXh0Ijp0cnVlLCJrdHkiOiJFQyIsIngiOiJETTNILXV2TDB2VnMwNDFCTUU2LWM5ZW1BN1NCellDSGI1czJTV1NsM05nWDM2azhPTWVyVlR6VUx4N185bDU4IiwieSI6InBERXk4WnpqXzNNZldtamt5YlpjV3oyVVBJdGhnVWEwSGk3Yks3QVBXTHFSRnFwZmh1eWx4MTdadFhnNnBPZUoiLCJjcnYiOiJQLTM4NCJ9LCJhcHUiOiIiLCJhcHYiOiIifQ.ppy4wTaFZPxHxq1s4LPbHHkC1EFCWq_i0uvvEE9tuO9shzpazi9f7w.fEMTO_2ueYtRd9F0.TjVy520PvstERopn4qhpQB9chotb80_vV3Zj78veWQdzg49a6MH51TVqKKqX1n-6_HyS_chDNR9P0kdLF2ZzrWUJ0x5aenmG7ALeLjrOrxMEf0Fsboxn04q1bRDBq-Gse7Iwhk8NZvVMs_xXDL3wTeIzff_YeeNZcJS_xXHzMpbLbWjQs_x60t56uP2_fF8Wm4Fr5srBOuB5-E3VPYhm_LVOYvBOYKJDl9awWw809UvZwotorI7jG_TwLqYCIgrdkvQxS5Sz05gWdM6yMrYcL5VInxJGSt-dL_0nomN0btmHOv51qD_dyxvFknPkESWD_l4.pWlTbfYgQ_rWI0AY8HXcirJ9pIg7Oh0AvjwR0mdM42Y';
      const deviceKey = aliceEcdh;

      const attempt = UserKeys.decryptOnBrowser(jwe, deviceKey.privateKey, aliceEcdh.publicKey, aliceEcdsa.publicKey);

      await expect(attempt).rejects.toThrow(UnwrapKeyError);
    });

    describe('After creating new key material', () => {
      let userKeys: UserKeys;

      beforeEach(async () => {
        userKeys = new TestUserKeys(aliceEcdh, aliceEcdsa);
      });

      it('encryptForDevice() creates JWE', async () => {
        const deviceKey = bobEcdh;
        const jwe = await userKeys.encryptForDevice(deviceKey.publicKey);

        expect(jwe).not.toBeNull();
      });

      it('encodedEcdhPublicKey() creates PEM-like string', async () => {
        const result = await userKeys.encodedEcdhPublicKey();

        expect(result).to.eq('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW');
      });

      it('encryptWithSetupCode() creates JWE', async () => {
        const jwe = await userKeys.encryptWithSetupCode('foo', 1000);

        expect(jwe).to.be.a('string');
      });

      it('decryptAccessToken() decrypts JWE', async () => {
        const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoicFotVXExTjNOVElRcHNpZC11UGZMaW95bVVGVFJLM1dkTXVkLWxDcGh5MjQ4bUlJelpDc3RPRzZLTGloZnBkZyIsInkiOiJzMnl6eF9Ca2QweFhIcENnTlJFOWJiQUIyQkNNTF80cWZwcFEza1N2LXhqcEROVWZZdmlxQS1xRERCYnZkNDdYIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.I_rXJagNrrCa9zISf0DZJLQbIZDxEpGxCyjFbNE0iZs6yFeVayNOGQ.7rASe4SqyKJJLHZ4.l6T2N_ATytZUyh1IZTIJJDY4dXCyQVsRB19QIIPrAi0QQiS4gl4.fnOtAJhdvPFFHVi6L5Ma_R8iL3IXq1_xAq2PvdEfx0A';

        const payload = await userKeys.decryptAccessToken(jwe);

        expect(payload.key).to.eq('VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVU=');
      });
    });
  });

  // base64-encoded test key pairs for use in other implementations (Java, Swift, ...)
  describe('Test Key Pairs', () => {
    it('alice private key (PKCS8)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('pkcs8', aliceEcdh.privateKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).toBe('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=');
    });

    it('alice public key (SPKI)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('spki', aliceEcdh.publicKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).toBe('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW');
    });

    it('bob private key (PKCS8)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('pkcs8', bobEcdh.privateKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).toBe('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDB2bmFCWy2p+EbAn8NWS5Om+GA7c5LHhRZb8g2pSMSf0fsd7k7dZDVrnyHFiLdd/YGhZANiAAR6bsjTEdXKWIuu1Bvj6Y8wySlIROy7YpmVZTY128ItovCD8pcR4PnFljvAIb2MshCdr1alX4g6cgDOqcTeREiObcSfucOU9Ry1pJ/GnX6KA0eSljrk6rxjSDos8aiZ6Mg=');
    });

    it('bob public key (SPKI)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('spki', bobEcdh.publicKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEem7I0xHVyliLrtQb4+mPMMkpSETsu2KZlWU2NdvCLaLwg/KXEeD5xZY7wCG9jLIQna9WpV+IOnIAzqnE3kRIjm3En7nDlPUctaSfxp1+igNHkpY65Oq8Y0g6LPGomejI');
    });
  });

  describe('JWK Thumbprint', () => {
    const input: JsonWebKey & Record<string, string> = {
      kty: 'RSA',
      n: '0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw',
      e: 'AQAB',
      alg: 'RS256',
      kid: '2011-04-29'
    };

    // https://datatracker.ietf.org/doc/html/rfc7638#section-3.1
    it('compute example thumbprint from RFC 7638, Section 3.1', async () => {
      const thumbprint = await getJwkThumbprint(input);

      expect(base64url.stringify(thumbprint, { pad: false })).toBe('NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs');
    });

    it('compute example thumbprint from RFC 7638, Section 3.1', async () => {
      const thumbprintStr = await getJwkThumbprintStr(input);

      expect(thumbprintStr).to.eq('NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs');
    });
  });
});

/* ---------- MOCKS ---------- */

class TestUserKeys extends UserKeys {
  public constructor(ecdhKeyPair: CryptoKeyPair, ecdsaKeyPair: CryptoKeyPair) {
    super(ecdhKeyPair, ecdsaKeyPair);
  }
}

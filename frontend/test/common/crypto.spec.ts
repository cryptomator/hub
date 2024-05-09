import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { base64 } from 'rfc4648';
import { UnwrapKeyError, UserKeys } from '../../src/common/crypto';

chaiUse(chaiAsPromised);

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
  let alice: CryptoKeyPair, bob: CryptoKeyPair;

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };

    // prepare some test key pairs:
    const alicePrv = crypto.subtle.importKey('jwk', alicePrivate, UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
    const alicePub = crypto.subtle.importKey('jwk', alicePublic, UserKeys.KEY_DESIGNATION, true, []);
    const bobPrv = crypto.subtle.importKey('jwk', bobPrivate, UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
    const bobPub = crypto.subtle.importKey('jwk', bobPublic, UserKeys.KEY_DESIGNATION, true, []);
    alice = { privateKey: await alicePrv, publicKey: await alicePub };
    bob = { privateKey: await bobPrv, publicKey: await bobPub };
  });

  describe('UserKeys', () => {
    it('create()', async () => {
      const orig = await UserKeys.create();

      expect(orig).to.be.not.null;
    });

    it('recover()', async () => {
      const encodedPublicKey = 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW';
      const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi5zZXR1cENvZGUiLCJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJwMmMiOjEwMDAwMDAsInAycyI6InJRV09pcl82VU9fMjE0eFJsQS02UEEifQ._cMSJto2fFJ9ixEmchid4VRAt-Al6GNVxjo114XdeJktm3WctXShSQ.kwOeIpnXmyZXt4n3.sYx6ZjCkkf8bXdHY-NGrZzG6hsd11Ze97cfuEay3glfJkOyKQgV7TURyKguJWrc-s7m67cq35uE9KUUB6SGLoUMs1ebmRx8emCxVBRjvia5e_gWnjCzIsS0Z3C67R8Tyvh1EadigRfE57HTrP7kJXV5zRf2L1OYOeI1QmmN44Cga2HW0B3zDOG3EXvN3tv3bMu83L3Css1_CZABWyPkrwOBt8LytzEeTWaV8SItdVolPUH9OmW7l0hS3dBMgS4haXlbLR0OxmL3Ag-tZ1G_WxLliWxLMUNAsqWOVzedkRhc9VichvD0KCZ8J5_O18sBPnMQ._2i2LUEV3T1nmY49oq7n_7EqADqAhnmzmTwLPWgqxOw';

      const result = await UserKeys.recover(encodedPublicKey, jwe, 'password');

      expect(result).to.be.not.null;
    });

    it('recover() with incorrect password', async () => {
      const encodedPublicKey = 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW';
      const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi5zZXR1cENvZGUiLCJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJwMmMiOjEwMDAwMDAsInAycyI6InJRV09pcl82VU9fMjE0eFJsQS02UEEifQ._cMSJto2fFJ9ixEmchid4VRAt-Al6GNVxjo114XdeJktm3WctXShSQ.kwOeIpnXmyZXt4n3.sYx6ZjCkkf8bXdHY-NGrZzG6hsd11Ze97cfuEay3glfJkOyKQgV7TURyKguJWrc-s7m67cq35uE9KUUB6SGLoUMs1ebmRx8emCxVBRjvia5e_gWnjCzIsS0Z3C67R8Tyvh1EadigRfE57HTrP7kJXV5zRf2L1OYOeI1QmmN44Cga2HW0B3zDOG3EXvN3tv3bMu83L3Css1_CZABWyPkrwOBt8LytzEeTWaV8SItdVolPUH9OmW7l0hS3dBMgS4haXlbLR0OxmL3Ag-tZ1G_WxLliWxLMUNAsqWOVzedkRhc9VichvD0KCZ8J5_O18sBPnMQ._2i2LUEV3T1nmY49oq7n_7EqADqAhnmzmTwLPWgqxOw';

      const attempt = UserKeys.recover(encodedPublicKey, jwe, 'wrong');

      return expect(attempt).to.be.rejectedWith(UnwrapKeyError);
    });

    it('decryptOnBrowser()', async () => {
      const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi5kZXZpY2VLZXkiLCJhbGciOiJFQ0RILUVTK0EyNTZLVyIsImVwayI6eyJrZXlfb3BzIjpbXSwiZXh0Ijp0cnVlLCJrdHkiOiJFQyIsIngiOiJETTNILXV2TDB2VnMwNDFCTUU2LWM5ZW1BN1NCellDSGI1czJTV1NsM05nWDM2azhPTWVyVlR6VUx4N185bDU4IiwieSI6InBERXk4WnpqXzNNZldtamt5YlpjV3oyVVBJdGhnVWEwSGk3Yks3QVBXTHFSRnFwZmh1eWx4MTdadFhnNnBPZUoiLCJjcnYiOiJQLTM4NCJ9LCJhcHUiOiIiLCJhcHYiOiIifQ.ppy4wTaFZPxHxq1s4LPbHHkC1EFCWq_i0uvvEE9tuO9shzpazi9f7w.fEMTO_2ueYtRd9F0.TjVy520PvstERopn4qhpQB9chotb80_vV3Zj78veWQdzg49a6MH51TVqKKqX1n-6_HyS_chDNR9P0kdLF2ZzrWUJ0x5aenmG7ALeLjrOrxMEf0Fsboxn04q1bRDBq-Gse7Iwhk8NZvVMs_xXDL3wTeIzff_YeeNZcJS_xXHzMpbLbWjQs_x60t56uP2_fF8Wm4Fr5srBOuB5-E3VPYhm_LVOYvBOYKJDl9awWw809UvZwotorI7jG_TwLqYCIgrdkvQxS5Sz05gWdM6yMrYcL5VInxJGSt-dL_0nomN0btmHOv51qD_dyxvFknPkESWD_l4.pWlTbfYgQ_rWI0AY8HXcirJ9pIg7Oh0AvjwR0mdM42Y';
      const deviceKey = bob;

      const result = UserKeys.decryptOnBrowser(jwe, deviceKey.privateKey, alice.publicKey);

      expect(result).to.be.not.null;
    });

    it('decryptOnBrowser() with incorrect device key', async () => {
      const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi5kZXZpY2VLZXkiLCJhbGciOiJFQ0RILUVTK0EyNTZLVyIsImVwayI6eyJrZXlfb3BzIjpbXSwiZXh0Ijp0cnVlLCJrdHkiOiJFQyIsIngiOiJETTNILXV2TDB2VnMwNDFCTUU2LWM5ZW1BN1NCellDSGI1czJTV1NsM05nWDM2azhPTWVyVlR6VUx4N185bDU4IiwieSI6InBERXk4WnpqXzNNZldtamt5YlpjV3oyVVBJdGhnVWEwSGk3Yks3QVBXTHFSRnFwZmh1eWx4MTdadFhnNnBPZUoiLCJjcnYiOiJQLTM4NCJ9LCJhcHUiOiIiLCJhcHYiOiIifQ.ppy4wTaFZPxHxq1s4LPbHHkC1EFCWq_i0uvvEE9tuO9shzpazi9f7w.fEMTO_2ueYtRd9F0.TjVy520PvstERopn4qhpQB9chotb80_vV3Zj78veWQdzg49a6MH51TVqKKqX1n-6_HyS_chDNR9P0kdLF2ZzrWUJ0x5aenmG7ALeLjrOrxMEf0Fsboxn04q1bRDBq-Gse7Iwhk8NZvVMs_xXDL3wTeIzff_YeeNZcJS_xXHzMpbLbWjQs_x60t56uP2_fF8Wm4Fr5srBOuB5-E3VPYhm_LVOYvBOYKJDl9awWw809UvZwotorI7jG_TwLqYCIgrdkvQxS5Sz05gWdM6yMrYcL5VInxJGSt-dL_0nomN0btmHOv51qD_dyxvFknPkESWD_l4.pWlTbfYgQ_rWI0AY8HXcirJ9pIg7Oh0AvjwR0mdM42Y';
      const deviceKey = alice;

      const attempt = UserKeys.decryptOnBrowser(jwe, deviceKey.privateKey, alice.publicKey);

      return expect(attempt).to.be.rejectedWith(UnwrapKeyError);
    });

    describe('After creating new key material', () => {
      let userKeys: UserKeys;

      beforeEach(async () => {
        userKeys = new TestUserKeys(alice);
      });

      it('encryptForDevice() creates JWE', async () => {
        const deviceKey = bob;
        const jwe = await userKeys.encryptForDevice(deviceKey.publicKey);

        expect(jwe).to.be.not.null;
      });

      it('encodedPublicKey() creates JWE', async () => {
        const result = await userKeys.encodedPublicKey();

        expect(result).to.eq('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW');
      });

      it('encryptedPrivateKey() creates JWE', async () => {
        const result = await userKeys.encryptedPrivateKey('password');

        expect(result).to.be.a('string');
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
      const bytes = new Uint8Array(await crypto.subtle.exportKey('pkcs8', alice.privateKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=');
    });

    it('alice public key (SPKI)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('spki', alice.publicKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW');
    });

    it('bob private key (PKCS8)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('pkcs8', bob.privateKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDB2bmFCWy2p+EbAn8NWS5Om+GA7c5LHhRZb8g2pSMSf0fsd7k7dZDVrnyHFiLdd/YGhZANiAAR6bsjTEdXKWIuu1Bvj6Y8wySlIROy7YpmVZTY128ItovCD8pcR4PnFljvAIb2MshCdr1alX4g6cgDOqcTeREiObcSfucOU9Ry1pJ/GnX6KA0eSljrk6rxjSDos8aiZ6Mg=');
    });

    it('bob public key (SPKI)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('spki', bob.publicKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEem7I0xHVyliLrtQb4+mPMMkpSETsu2KZlWU2NdvCLaLwg/KXEeD5xZY7wCG9jLIQna9WpV+IOnIAzqnE3kRIjm3En7nDlPUctaSfxp1+igNHkpY65Oq8Y0g6LPGomejI');
    });
  });
});

/* ---------- MOCKS ---------- */

class TestUserKeys extends UserKeys {
  constructor(keypair: CryptoKeyPair) {
    super(keypair);
  }
}

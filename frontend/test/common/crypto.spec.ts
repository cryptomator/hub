import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { base64 } from 'rfc4648';
import { UnwrapKeyError, UserKeys, VaultKeys } from '../../src/common/crypto';

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
  let aliceEcdh: CryptoKeyPair, aliceEcdsa: CryptoKeyPair, bobEcdh: CryptoKeyPair;

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };

    // prepare some test key pairs:
    let ecdhP384: EcKeyImportParams = { name: 'ECDH', namedCurve: 'P-384' };
    let ecdsaP384: EcKeyImportParams = { name: 'ECDSA', namedCurve: 'P-384' };
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

  describe('VaultKeys', () => {
    it('create()', async () => {
      const orig = await VaultKeys.create();

      expect(orig).to.be.not.null;
    });

    it('recover() succeeds for valid key', async () => {
      let recoveryKey = `
        pathway lift abuse plenty export texture gentleman landscape beyond ceiling around leaf cafe charity 
        border breakdown victory surely computer cat linger restrict infer crowd live computer true written amazed 
        investor boot depth left theory snow whereby terminal weekly reject happiness circuit partial cup ad
        `;

      const recovered = await VaultKeys.recover(recoveryKey);

      const newMasterKey = await crypto.subtle.exportKey('jwk', recovered.masterKey);
      expect(newMasterKey).to.deep.include({
        'k': 'uwHiVreDbmv47K7oZzlwZbHcEql2Z29brbgFxKA7i54pXVPoHoxKK5rzZS3VEhPxHegQKCwa5Mk4ep7OsYutAw'
      });
    });

    it('recover() fails for invalid recovery key', async () => {
      const noMultipleOfTwo = VaultKeys.recover('pathway');
      const notInDict = VaultKeys.recover('hallo bonjour');
      const wrongLength = VaultKeys.recover('pathway lift');
      const invalidCrc = VaultKeys.recover(`
        pathway lift abuse plenty export texture gentleman landscape beyond ceiling around leaf cafe charity 
        border breakdown victory surely computer cat linger restrict infer crowd live computer true written amazed 
        investor boot depth left theory snow whereby terminal weekly reject happiness circuit partial cup wrong
        `);

      return Promise.all([
        expect(noMultipleOfTwo).to.be.rejectedWith(Error, /input needs to be a multiple of two words/),
        expect(notInDict).to.be.rejectedWith(Error, /Word not in dictionary/),
        expect(wrongLength).to.be.rejectedWith(Error, /Invalid recovery key length/),
        expect(invalidCrc).to.be.rejectedWith(Error, /Invalid recovery key checksum/),
      ]);
    });

    describe('Prove Vault Ownership using Vault Admin Password', () => {
      const wrapped = {
        wrappedMasterkey: 'CMPyJiiOQXBZ8FVvFZs6UOh0kW83+eALeK3bwXfFF2CWsguJZIgCJch94liWCh9xTqW84LUZPyo6IDWbSALqbbdiwDcztT8M81/pgadhTETVtHO5Q1CFNLJ9UvY=',
        wrappedOwnerPrivateKey: 'O9snY73/eVElnWRLgM404KH7WwO/Ed30Y0UrQQw6x3vxOdroJcjvPdJeSqLD2x4lVP7ceTjVt3IT2N9Mx+jhUQzqrb1E2EvEYlXrTaID1jSdBXZ6ScrI1RvU0iH9cfXf2cRy2x8QZvJyVMr34gLJ3Di/XGrnc/BrOm+aF2K4F9FJXvJFen3CnAs9ewB3Vk0A1wRLX3hW/Wx7eXt/0i1gxB8T/NcLu7xIU3+uusTHh9uajFkA5+z1+JgNHURaa1bT8j5WTtNWIHYT/sw+erMn6S0Uj1vL',
        ownerPublicKey: 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW',
        salt: 'IdXyKICznXKm41gSb5OqfQ',
        iterations: 1
      };

      it('decryptWithAdminPassword() with wrong pw', () => {
        return expect(VaultKeys.decryptWithAdminPassword('wrong', wrapped.wrappedMasterkey, wrapped.wrappedOwnerPrivateKey, wrapped.ownerPublicKey, wrapped.salt, wrapped.iterations)).to.eventually.be.rejectedWith(UnwrapKeyError);
      });

      it('decryptWithAdminPassword() with correct pw', () => {
        return expect(VaultKeys.decryptWithAdminPassword('pass', wrapped.wrappedMasterkey, wrapped.wrappedOwnerPrivateKey, wrapped.ownerPublicKey, wrapped.salt, wrapped.iterations)).to.eventually.be.fulfilled;
      });
    });

    describe('After creating new key material', () => {
      let vaultKeys: VaultKeys;

      beforeEach(async () => {
        vaultKeys = await TestVaultKeys.create();
      });

      it('encryptForUser()', async () => {
        const userKey = base64.parse('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAERxQR+NRN6Wga01370uBBzr2NHDbKIC56tPUEq2HX64RhITGhii8Zzbkb1HnRmdF0aq6uqmUy4jUhuxnKxsv59A6JeK7Unn+mpmm3pQAygjoGc9wrvoH4HWJSQYUlsXDu');

        const encrypted = await vaultKeys.encryptForUser(userKey);
        expect(encrypted).to.be.not.null;
      });

      it('createRecoveryKey()', async () => {
        const recoveryKey = await vaultKeys.createRecoveryKey();

        expect(recoveryKey).to.eql('water water water water water water water water water water water water water water water water water water water water water asset partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly partly option twist');
      });

      describe('After creating a valid recovery key', () => {
        let recoveryKey: string;

        beforeEach(async () => {
          recoveryKey = await vaultKeys.createRecoveryKey();
        });

        it('recover() imports original key', async () => {
          const recovered = await VaultKeys.recover(recoveryKey);

          const oldMasterKey = await crypto.subtle.exportKey('jwk', vaultKeys.masterKey);
          const newMasterKey = await crypto.subtle.exportKey('jwk', recovered.masterKey);
          expect(newMasterKey).to.deep.include({
            'k': oldMasterKey.k
          });
        });
      });
    });
  });

  describe('UserKeys', () => {
    it('create()', async () => {
      const orig = await UserKeys.create();

      expect(orig).to.be.not.null;
    });

    it('decrypt with device key', async () => {
      const jwe = 'eyJhbGciOiJFQ0RILUVTIiwiZW5jIjoiQTI1NkdDTSIsImVwayI6eyJrZXlfb3BzIjpbXSwiZXh0Ijp0cnVlLCJrdHkiOiJFQyIsIngiOiJiOTJEU0EwOW1FcW53cVBsMTBKdlhiV3BzRllHbndoclVkd21BYWFrdTB3SGdmNGotUzJVNnlLZ2hjU1VRX2RlIiwieSI6ImV0b21LamxCUzc3MmJ1OEFDdmlEX3VGZ2FNNkQyWG9ZR0xTNkkzNTRWXzVvZ3d0akNZNlZjX1h0Z3IzOWhMZ3QiLCJjcnYiOiJQLTM4NCJ9LCJhcHUiOiIiLCJhcHYiOiIifQ..hQyVi7d-S5Qr3z3W.k3Vo14FFoE2vH1tVD16ErYkCcIDqyPxzcO4Snfn9HFNswYchpFsK0jZFREZnJUaaVxX44oPou16uNDCHZoahwNqJfBC6oTN950al9VeP8kufqt7yABavN4pmPSFi_zSZBaJAkMgtOK23UW5bILRx-EjdhXfBZ-BEu1tzsbZV0sDMrAdP3VdMlRV3JYUgokTMJBhY6EzvAK8qO5QBfkQ5VomJW8Ju59Gt1LJAKwKPN36f3WSu8A3xYxSgQXJMh-qcsHvEA4O31gvmAFBeNKNm2ovZNAwizQrQGPFPfBF269_2YK_YSxpWk5-o5SITFUBcCVq8jBVXLZhfmqDWubbmTcS9nRF-UtuVuvMy_bIjg-4KcCQyLSPKoPw9e3tKs6t4KzKu4DB9Qfo8TSp2L1UpZETKD9HExgi1RLdBJEMCydrdPVPjBrMjQvkeEyMLFUeZ3xXXdspPeJN60QskqVdLaF9S0Xx8W8eHhv4DA6nnEEWn_IeS4K1IBhJ3DkDVVZJiQdTANHlkVgZagGzErP0aLwtgbZUHPiMr1bFw81veQxAK2FxsyDgkFaX6rW2eJl6Y2TTDpBCUY9Fj42Q8pFejnrvBvoDDOI_aB9n-2pGcK_nTzntP2aV5InNu5voBYEUJeJjpfwhoTsg_vzfU3zea1CpM3RC_FyuY1mze6PEdL3jR5S6xC4BrGfABKBKNA33XC4rh7ddLkeX-fRFj6MPXzOZOkeDHECbBC_aIutjYVVhWnuQQONLmpBPhzmaCTzztEnZ5f-YGGqTBOQj3cuuGCA7pAjqBei5bgqhOW3spHuB3XM3Ays4nyAc0DgkDtpa0FP2Sp70hT7loCVfjWGZWbuBVuIuChYDVtQgQmZ0gTqES1cRJGhx_HbxoEycZKmKPHdEZEtDOXNNyzjJ7XKugeW4xumcJvAIF5VpXyBoR4AL2Qmb2P-VSzDtNoeGL070Cr55Ux9vJvVZlXWWR6pTqjzLtJcQV9cvDHznogaRtERXqL5zcPTveorUOb4pM5UpCH07qUHQtFPW6eLyMorGBrSuSJJi831idR26o.h6xiowYII-lLOZ8YM32bng';

      const foo = await UserKeys.decryptOnBrowser(jwe, bobEcdh.privateKey, aliceEcdh.publicKey, aliceEcdsa.publicKey);

      expect(jwe).to.be.not.null;
    });

    it('recover() with setup code', async () => {
      const jwe = 'eyJhbGciOiJQQkVTMi1IUzUxMitBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIiwicDJzIjoibUJ6Q3dkQVpDVEdJcm8tZlppYkJzZyIsInAyYyI6MTAwMCwiYXB1IjoiIiwiYXB2IjoiIn0.aJQ8SpBAc8-Qj7e2nHcBrXjUg1ffYr-yYzObbhLfP9zIS6xDb_QhGA.h3kS7p9X3kwN0hNN.MXYK5F62aELyVyiiixvdtF5qc2b8hVj1o5pLgmd-pZy5Kw5KDW3QUtpfGM5NdMNSlCJjg7ffTt0oBpD67jIatN2PhqJ9A_G_n-UleYDwSv_c-GLqEuJW7gYm-dydigqjZVQsQWdTiVPaO_USqrPkW7fBm0jG7ibIO4tq23tszbzIFWjrZgUwB9UrBWLzoreBci9zK8iSKZyjSJl75IVBzoVPDGEMukGf-Vie1gHrcLy7OaJ-0_K5Ncwq2-YuDhtIvuq5fpE06RYAFotRPsObmcDGz4BzjTRjSgbbjWBZF0n2rwkpnTOUodkUvvWSfPL7se8-6hke9wNC7YV0rleeiTkRrRUl5wFTce4ahH_paIGMA0sJXV4B6rbb2XV_j9s54edER869jG5m5Vzo4T4HYkmBLXT5JqAvEq4cR6YmBVlZ8REVI2TxXoh56Lb9Z6bEBp2GRKy9jgOcka2xGS6S5rh3sNcfyXjcBJXR4p4yNf8ksxLEQmR2Dt46xKGkqEiTt5uYSCx0HAUyYksr7nWmUkhNQZJwswDfyMGNqIYo6ZRY4gWk9b-GG9-qm27eBl_7oXE8IvWV9mPSiNZgwUcUc2nw35Oj0s9q99xMjJ4sQxPMlKmtz2_1OfN4oXZnb36660Gr3R-txvhML4GucWEe7WgdJlgrLduJxvEnv0Nf6lacdb7se592AlqGpQegfWUWwD3DRSeha7wb0aeqyNXSP7B4aBrd4eLVKsvNUjGeJ6Riy8-5tPFhIh7x7OXXqh8dUyHF9k_fpnbXjT2Ljq5Jn2jLvBUC7HX-ixtGK9Q5GoIg2IDG6-zglcpvLFLaJgZ8-_XIPTG6dHv8u-ST3vHXZeKE4mbkmXgVCsov11OUtT2H1pd79FKUzmGuWHSIAHGnjqsrcLXaqQ5nVF3c0MIt5U0Q6rLeBindcKQ6j648j-7B9mkjedD6_fTNROsK9gBQQ6hefTjCLJT6TPLMbCiR91UYB4YJ1TsNV37WiQgac-R3NCQnj0lG6hpfFLH5TWZ7Q1_7guUZnzhLSbsa36weO4h9fNdur4S6oI5I.4xnbrXWQgQgNV-pFmCQtag';

      const recovered = await UserKeys.recover(jwe, 'foo', aliceEcdh.publicKey, aliceEcdsa.publicKey);

      expect(recovered).to.be.not.null;
    });

    describe('After creating new key material', () => {
      let userKeys: UserKeys;

      beforeEach(async () => {
        userKeys = new TestUserKeys(aliceEcdh, aliceEcdsa);
      });

      it('encryptForDevice() creates JWE', async () => {
        const deviceKey = bobEcdh;
        const jwe = await userKeys.encryptForDevice(deviceKey.publicKey);

        expect(jwe).to.be.not.null;
      });

      it('encryptWithSetupCode() creates JWE', async () => {
        const jwe = await userKeys.encryptWithSetupCode('foo', 1000);

        expect(jwe).to.be.not.null;
      });
    });
  });

  // base64-encoded test key pairs for use in other implementations (Java, Swift, ...)
  describe('Test Key Pairs', () => {
    it('alice private key (PKCS8)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('pkcs8', aliceEcdh.privateKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=');
    });

    it('alice public key (SPKI)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('spki', aliceEcdh.publicKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW');
    });

    it('bob private key (PKCS8)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('pkcs8', bobEcdh.privateKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDB2bmFCWy2p+EbAn8NWS5Om+GA7c5LHhRZb8g2pSMSf0fsd7k7dZDVrnyHFiLdd/YGhZANiAAR6bsjTEdXKWIuu1Bvj6Y8wySlIROy7YpmVZTY128ItovCD8pcR4PnFljvAIb2MshCdr1alX4g6cgDOqcTeREiObcSfucOU9Ry1pJ/GnX6KA0eSljrk6rxjSDos8aiZ6Mg=');
    });

    it('bob public key (SPKI)', async () => {
      const bytes = new Uint8Array(await crypto.subtle.exportKey('spki', bobEcdh.publicKey));
      const encoded = base64.stringify(bytes);
      expect(encoded).to.eq('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEem7I0xHVyliLrtQb4+mPMMkpSETsu2KZlWU2NdvCLaLwg/KXEeD5xZY7wCG9jLIQna9WpV+IOnIAzqnE3kRIjm3En7nDlPUctaSfxp1+igNHkpY65Oq8Y0g6LPGomejI');
    });
  });

  describe('Hash directory id', () => {
    it('root directory', async () => {
      const vaultKeys = await TestVaultKeys.create();
      const result = await vaultKeys.hashDirectoryId('');
      expect(result).to.eql('VLWEHT553J5DR7OZLRJAYDIWFCXZABOD');
    });

    it('specific directory', async () => {
      const vaultKeys = await TestVaultKeys.create();
      const result = await vaultKeys.hashDirectoryId('918acfbd-a467-3f77-93f1-f4a44f9cfe9c');
      expect(result).to.eql('7C3USOO3VU7IVQRKFMRFV3QE4VEZJECV');
    });
  });
});

/* ---------- MOCKS ---------- */

class TestVaultKeys extends VaultKeys {
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
    return new TestVaultKeys(key);
  }
}

class TestUserKeys extends UserKeys {
  constructor(ecdhKeyPair: CryptoKeyPair, ecdsaKeyPair: CryptoKeyPair) {
    super(ecdhKeyPair, ecdsaKeyPair);
  }
}

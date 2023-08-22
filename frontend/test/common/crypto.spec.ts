import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { base64 } from 'rfc4648';
import { UnwrapKeyError, UserKeys, VaultKeys, WrappedVaultKeys } from '../../src/common/crypto';

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
    let ecdhP384: EcKeyImportParams = { name: 'ECDH', namedCurve: 'P-384' };
    const alicePrv = crypto.subtle.importKey('jwk', alicePrivate, ecdhP384, true, ['deriveKey']);
    const alicePub = crypto.subtle.importKey('jwk', alicePublic, ecdhP384, true, []);
    const bobPrv = crypto.subtle.importKey('jwk', bobPrivate, ecdhP384, true, ['deriveKey']);
    const bobPub = crypto.subtle.importKey('jwk', bobPublic, ecdhP384, true, []);
    alice = { privateKey: await alicePrv, publicKey: await alicePub };
    bob = { privateKey: await bobPrv, publicKey: await bobPub };
  });

  describe('VaultKeys', () => {
    const wrapped: WrappedVaultKeys = {
      masterkey: 'CMPyJiiOQXBZ8FVvFZs6UOh0kW83+eALeK3bwXfFF2CWsguJZIgCJch94liWCh9xTqW84LUZPyo6IDWbSALqbbdiwDcztT8M81/pgadhTETVtHO5Q1CFNLJ9UvY=',
      signaturePrivateKey: 'O9snY73/eVElnWRLgM404KH7WwO/Ed30Y0UrQQw6x3vxOdroJcjvPdJeSqLD2x4lVP7ceTjVt3IT2N9Mx+jhUQzqrb1E2EvEYlXrTaID1jSdBXZ6ScrI1RvU0iH9cfXf2cRy2x8QZvJyVMr34gLJ3Di/XGrnc/BrOm+aF2K4F9FJXvJFen3CnAs9ewB3Vk0A1wRLX3hW/Wx7eXt/0i1gxB8T/NcLu7xIU3+uusTHh9uajFkA5+z1+JgNHURaa1bT8j5WTtNWIHYT/sw+erMn6S0Uj1vL',
      signaturePublicKey: 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW',
      salt: 'IdXyKICznXKm41gSb5OqfQ',
      iterations: 1
    };

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

    it('unwrap() with wrong pw', () => {
      return expect(VaultKeys.unwrap('wrong', wrapped)).to.eventually.be.rejectedWith(UnwrapKeyError);
    });

    it('unwrap() with correct pw', () => {
      return expect(VaultKeys.unwrap('pass', wrapped)).to.eventually.be.fulfilled;
    });

    // unwrapped keys are not exportable, therefore they can not be re-wrapped
    describe('After unwrapping existing key material', () => {
      let unwrapped: VaultKeys;

      beforeEach(async () => {
        unwrapped = await VaultKeys.unwrap('pass', wrapped);
      });

      it('wrap() fails', () => {
        return expect(unwrapped.wrap('pass')).to.eventually.be.rejected;
      });
    });

    describe('After creating new key material', () => {
      let vaultKeys: VaultKeys;

      beforeEach(async () => {
        vaultKeys = await TestVaultKeys.create();
      });

      it('wrap() succeeds', async () => {
        const wrapped = await vaultKeys.wrap('pass');

        expect(wrapped).to.be.not.null;
        expect(wrapped.masterkey).to.be.not.null;
        expect(wrapped.signaturePrivateKey).to.be.not.null;
        expect(wrapped.signaturePublicKey).to.be.not.null;
        expect(wrapped.salt).to.be.not.null;
        expect(wrapped.iterations).to.eq(1000000);
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

        it('recover() creates new signature key pair', async () => {
          const recovered = await VaultKeys.recover(recoveryKey);

          const oldSecKey = await crypto.subtle.exportKey('jwk', vaultKeys.signatureKeyPair.privateKey);
          const newSecKey = await crypto.subtle.exportKey('jwk', recovered.signatureKeyPair.privateKey);
          const oldPubKey = await crypto.subtle.exportKey('jwk', vaultKeys.signatureKeyPair.publicKey);
          const newPubKey = await crypto.subtle.exportKey('jwk', recovered.signatureKeyPair.publicKey);
          expect(newSecKey).to.not.deep.include({
            'd': oldSecKey.d
          });
          expect(newPubKey).to.not.deep.include({
            'x': oldPubKey.x,
            'y': oldPubKey.y
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
      {
        name: 'HMAC',
        hash: 'SHA-256',
        length: 512
      },
      true,
      ['sign']
    );
    const ecdsaP384: EcKeyImportParams = { name: 'ECDSA', namedCurve: 'P-384' };
    const priv = crypto.subtle.importKey('jwk', alicePrivate, ecdsaP384, true, ['sign']);
    const pub = crypto.subtle.importKey('jwk', alicePublic, ecdsaP384, true, ['verify']);
    return new TestVaultKeys(key, { privateKey: await priv, publicKey: await pub });
  }
}

class TestUserKeys extends UserKeys {
  constructor(keypair: CryptoKeyPair) {
    super(keypair);
  }
}

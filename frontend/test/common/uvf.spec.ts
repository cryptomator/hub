import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { UserKeys } from '../../src/common/crypto';
import { MemberKey, RecoveryKey, VaultMetadata } from '../../src/common/uvf';

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

describe('Universal Vault Format', () => {

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };
  });

  describe('UVF Member Key', () => {

    let alice: TestUserKeys;

    before(async () => {
      // prepare some test key pairs:
      const alicePrv = crypto.subtle.importKey('jwk', alicePrivate, UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
      const alicePub = crypto.subtle.importKey('jwk', alicePublic, UserKeys.KEY_DESIGNATION, true, []);
      alice = new TestUserKeys({ privateKey: await alicePrv, publicKey: await alicePub });
    });

    it('serializeKey()', async () => {
      const memberKey = await TestMemberKey.create();

      const encrypted = await memberKey.serializeKey();

      expect(encrypted).to.be.not.null;
    });

    it('load(userKeyPair.decryptAccessToken(...))', async () => {
      const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoicFotVXExTjNOVElRcHNpZC11UGZMaW95bVVGVFJLM1dkTXVkLWxDcGh5MjQ4bUlJelpDc3RPRzZLTGloZnBkZyIsInkiOiJzMnl6eF9Ca2QweFhIcENnTlJFOWJiQUIyQkNNTF80cWZwcFEza1N2LXhqcEROVWZZdmlxQS1xRERCYnZkNDdYIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.I_rXJagNrrCa9zISf0DZJLQbIZDxEpGxCyjFbNE0iZs6yFeVayNOGQ.7rASe4SqyKJJLHZ4.l6T2N_ATytZUyh1IZTIJJDY4dXCyQVsRB19QIIPrAi0QQiS4gl4.fnOtAJhdvPFFHVi6L5Ma_R8iL3IXq1_xAq2PvdEfx0A';

      const payload = await alice.decryptAccessToken(jwe);
      const decrypted = await MemberKey.load(payload.key);

      expect(decrypted).to.be.not.null;
    });
  });

  describe('UVF Metadata', () => {
    // TODO review @sebi what else should we test?
    it('encrypt() and decryptWithMemberKey()', async () => {
      const vaultMemberKey = await MemberKey.create();
      const recoveryKey = await RecoveryKey.create();

      const orig = await VaultMetadata.create({ enabled: true, maxWotDepth: -1 });
      expect(orig).to.be.not.null;
      expect(orig.seeds.get(orig.initialSeedId)).to.not.be.undefined
      expect(orig.seeds.get(orig.initialSeedId)!.length).to.eq(32)
      expect(orig.initialSeedId).to.eq(orig.latestSeedId)
      expect(orig.kdfSalt.length).to.eq(32)

      const uvfMetadata: string = await orig.encrypt(vaultMemberKey, recoveryKey);
      expect(uvfMetadata).to.be.not.null;

      const decrypted: VaultMetadata = await VaultMetadata.decryptWithMemberKey(uvfMetadata, vaultMemberKey);
      const decryptedPayload = decrypted.payload();
      expect(decrypted.seeds).to.deep.eq(orig.seeds)
      expect(decrypted.initialSeedId).to.eq(orig.initialSeedId)
      expect(decrypted.latestSeedId).to.eq(orig.latestSeedId)
      expect(decrypted.automaticAccessGrant).to.deep.eq(orig.automaticAccessGrant);
      expect(decryptedPayload.fileFormat).to.eq('AES-256-GCM-32k');
      expect(decryptedPayload.nameFormat).to.eq('AES-SIV-512-B64URL');
      expect(decryptedPayload.kdf).to.eq('HKDF-SHA512');
    });
  });

  describe('UVF Recovery Key', () => {

    it('create()', async () => {
      const recoveryKey = await RecoveryKey.create();
      expect(recoveryKey).to.be.not.null;
    });

    describe('instance methods', () => {
      let recoveryKey: RecoveryKey;

      beforeEach(async () => {
        // prepare some test key pairs:
        const alicePrv = await crypto.subtle.importKey('jwk', alicePrivate, RecoveryKey.KEY_DESIGNATION, true, RecoveryKey.KEY_USAGES);
        const alicePub = await crypto.subtle.importKey('jwk', alicePublic, RecoveryKey.KEY_DESIGNATION, true, []);
        recoveryKey = new TestRecoveryKey(alicePub, alicePrv);
      });

      it('serializePrivateKey()', async () => {
        const serialized = await recoveryKey.serializePrivateKey();
        expect(serialized).to.eq('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=');
      });

      it('serializePublicKey()', async () => {
        const serialized = await recoveryKey.serializePublicKey();
        expect(serialized).to.eq('MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAESzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQhHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL+WLKjnGjQAw0rNGy5V29+aV+yseW');
      });

      it('createRecoveryKey()', async () => {
        const result = await recoveryKey.createRecoveryKey();
        expect(result).to.eq('cult hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf all baby dig war linear tour sleep humanity threat question neglect stance radar bank coup misery painter tragedy buddy compare winter national approval budget deep screen outdoor audience tear stream cure type ugly chamber supporter franchise accept sexy ad imply being drug doctor regime where thick dam training grass chamber domestic dictator educate sigh music spoken connected measure voice lemon pig comprise disturb appear greatly satisfied heat news curiosity top impress nor method reflect lesson recommend dual revenge thorough bus count broadband living riot prejudice target blonde excess company thereby tribe respond horror mere way proud shopping wise liver mortgage plastic gentleman eighteen terms worry melt');
      });

    });

  });
});


// #region Mocks

class TestMemberKey extends MemberKey {
  private constructor(key: CryptoKey) {
    super(key);
  }

  static async create() {
    const raw = new Uint8Array(32);
    raw.fill(0x55);
    const key = await crypto.subtle.importKey('raw', raw, MemberKey.KEY_DESIGNATION, true, MemberKey.KEY_USAGE);
    return new TestMemberKey(key);
  }
}

class TestUserKeys extends UserKeys {
  public constructor(keyPair: CryptoKeyPair) {
    super(keyPair);
  }
}

class TestRecoveryKey extends RecoveryKey {
  public constructor(readonly publicKey: CryptoKey, readonly privateKey?: CryptoKey) {
    super(publicKey, privateKey);
  }
}

// #endregion

import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { base64 } from 'rfc4648';
import { VaultMetadataJWEAutomaticAccessGrantDto } from '../../src/common/backend';
import { UserKeys, VaultMetadata } from '../../src/common/crypto';
import { JWEParser } from '../../src/common/jwe';

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

  describe('VaultMetadata', () => {
    // TODO review @sebi what else should we test?
    it('encryptWithMasterKey() and decryptWithMasterKey()', async () => {
      const uvfMasterKey = await crypto.subtle.generateKey(
        { name: 'AES-KW', length: 256 },
        true,
        ['wrapKey', 'unwrapKey']
      );
      const automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto = {
        "enabled": true,
        "maxWotDepth": -1
      }
      const orig = await VaultMetadata.create(automaticAccessGrant);
      expect(orig).to.be.not.null;
      const jwe: string = await orig.encryptWithMasterKey(uvfMasterKey);
      expect(jwe).to.be.not.null;
      const decrypted: VaultMetadata = await VaultMetadata.decryptWithMasterKey(jwe, uvfMasterKey);
      expect(JSON.stringify(decrypted.automaticAccessGrant)).to.eq(JSON.stringify(automaticAccessGrant));
      const decryptedRaw: any = await JWEParser.parse(jwe).decryptA256kw(uvfMasterKey);
      expect(decryptedRaw.fileFormat).to.eq("AES-256-GCM-32k");
      expect(decryptedRaw.latestFileKey).to.eq(orig.latestFileKey);
      expect(decryptedRaw.nameKey).to.eq(orig.nameKey);
      expect(decryptedRaw.kdf).to.eq("1STEP-HMAC-SHA512");
      expect(decryptedRaw['org.cryptomator.automaticAccessGrant']).to.deep.eq(automaticAccessGrant);
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

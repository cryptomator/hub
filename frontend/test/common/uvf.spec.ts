import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { VaultMetadata } from '../../src/common/uvf';

chaiUse(chaiAsPromised);

describe('Universal Vault Format', () => {

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };
  });

  describe('UVF Metadata', () => {
    // TODO review @sebi what else should we test?
    it('encrypt() and decryptWithMemberKey()', async () => {
      const vaultMemberKey = await crypto.subtle.generateKey(
        { name: 'AES-KW', length: 256 },
        false,
        ['wrapKey', 'unwrapKey']
      );
      const recoveryKey = await crypto.subtle.generateKey(
        { name: 'ECDH', namedCurve: 'P-384' },
        false,
        ['deriveKey']
      );

      const orig = await VaultMetadata.create({ enabled: true, maxWotDepth: -1 });
      expect(orig).to.be.not.null;
      expect(orig.seeds.get(orig.initialSeedId)).to.not.be.undefined
      expect(orig.seeds.get(orig.initialSeedId)!.length).to.eq(32)
      expect(orig.initialSeedId).to.eq(orig.latestSeedId)
      expect(orig.kdfSalt.length).to.eq(32)

      const uvfMetadata: string = await orig.encrypt(vaultMemberKey, recoveryKey.publicKey);
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
});

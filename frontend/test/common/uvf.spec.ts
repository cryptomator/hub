import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { JWEParser } from '../../src/common/jwe';
import { MetadataPayload, VaultMetadata, VaultMetadataJWEAutomaticAccessGrantDto } from '../../src/common/uvf';

chaiUse(chaiAsPromised);

describe('Universal Vault Format', () => {

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };
  });

  describe('Vault Metadata', () => {
    // TODO review @sebi what else should we test?
    it('encryptWithMasterKey() and decryptWithMasterKey()', async () => {
      const uvfMasterKey = await crypto.subtle.generateKey(
        { name: 'AES-KW', length: 256 },
        true,
        ['wrapKey', 'unwrapKey']
      );
      const automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto = {
        enabled: true,
        maxWotDepth: -1
      }
      const orig = await VaultMetadata.create(automaticAccessGrant);
      expect(orig).to.be.not.null;
      expect(orig.seeds.get(orig.initialSeedId)).to.not.be.undefined
      expect(orig.seeds.get(orig.initialSeedId)!.length).to.eq(32)
      expect(orig.initialSeedId).to.eq(orig.latestSeedId)
      expect(orig.kdfSalt.length).to.eq(32)
      const jwe: string = await orig.encryptWithMasterKey(uvfMasterKey);
      expect(jwe).to.be.not.null;
      const decrypted: VaultMetadata = await VaultMetadata.decryptWithMasterKey(jwe, uvfMasterKey);
      expect(decrypted.seeds).to.deep.eq(orig.seeds)
      expect(decrypted.initialSeedId).to.eq(orig.initialSeedId)
      expect(decrypted.latestSeedId).to.eq(orig.latestSeedId)
      expect(decrypted.automaticAccessGrant).to.deep.eq(automaticAccessGrant);
      const decryptedRaw: MetadataPayload = await JWEParser.parse(jwe).decryptA256kw(uvfMasterKey);
      expect(decryptedRaw.fileFormat).to.eq('AES-256-GCM-32k');
      expect(decryptedRaw.initialSeed).to.eq(decryptedRaw.latestSeed);
      expect(decryptedRaw.seeds[decryptedRaw.initialSeed]).to.be.not.empty;
      expect(decryptedRaw.kdf).to.eq('HKDF-SHA512');
      expect(decryptedRaw.kdfSalt).to.be.not.empty
      expect(decryptedRaw['org.cryptomator.automaticAccessGrant']).to.deep.eq(automaticAccessGrant);
    });
  });
});

import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { VaultMetadataJWEAutomaticAccessGrantDto } from '../../src/common/backend';
import { JWEParser } from '../../src/common/jwe';
import { VaultMetadata } from '../../src/common/uvf';

chaiUse(chaiAsPromised);

describe('Vault Format 8', () => {

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };
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
});

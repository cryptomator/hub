import { base64, base64url } from 'rfc4648';
import { beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { VaultDto } from '../../src/common/backend';
import { UserKeys } from '../../src/common/crypto';
import { JsonJWE } from '../../src/common/jwe';
import { MemberKey, RecoveryKey, UniversalVaultFormat, VaultMetadata } from '../../src/common/universalVaultFormat';

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

describe('UVF', () => {
  let alice: UserKeys;

  beforeAll(async () => {
    // prepare some test key pairs:
    const ecdhP384: EcKeyImportParams = { name: 'ECDH', namedCurve: 'P-384' };
    const ecdsaP384: EcKeyImportParams = { name: 'ECDSA', namedCurve: 'P-384' };
    const aliceEcdhPrv = crypto.subtle.importKey('jwk', alicePrivate, ecdhP384, true, ['deriveKey', 'deriveBits']);
    const aliceEcdhPub = crypto.subtle.importKey('jwk', alicePublic, ecdhP384, true, []);
    const aliceEcdsaPrv = crypto.subtle.importKey('jwk', alicePrivate, ecdsaP384, true, ['sign']);
    const aliceEcdsaPub = crypto.subtle.importKey('jwk', alicePublic, ecdsaP384, true, []);
    const aliceEcdh: CryptoKeyPair = { privateKey: await aliceEcdhPrv, publicKey: await aliceEcdhPub };
    const aliceEcdsa: CryptoKeyPair = { privateKey: await aliceEcdsaPrv, publicKey: await aliceEcdsaPub };
    alice = new TestUserKeys(aliceEcdh, aliceEcdsa);
  });

  describe('MemberKey', () => {
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
      await expect(decrypted.serializeKey()).resolves.toBe('VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVU=');
    });
  });

  describe('VaultMetadata', () => {
    it('create()', async () => {
      const orig = await VaultMetadata.create({ enabled: true, maxWotDepth: -1 });
      expect(orig).to.be.not.null;
      expect(orig.seeds.get(orig.initialSeedId)).to.not.be.undefined;
      expect(orig.seeds.get(orig.initialSeedId)!.length).to.eq(32);
      expect(orig.initialSeedId).to.eq(orig.latestSeedId);
      expect(orig.kdfSalt.length).to.eq(32);
    });

    describe('instance methods', () => {
      let original: VaultMetadata;

      beforeEach(async () => {
        // prepare some test metadata:
        original = await VaultMetadata.create({ enabled: true, maxWotDepth: -1 });
      });

      it('decrypt(encrypt(orig)) == orig', async () => {
        const dto: VaultDto = { id: '123', name: 'test', archived: false, creationTime: new Date() };
        const vaultMemberKey = await MemberKey.create();
        const recoveryKey = await RecoveryKey.create();

        const uvfFile: string = await original.encrypt('https://example.com/api/', dto, vaultMemberKey, recoveryKey);
        expect(uvfFile).to.be.not.null;
        const json = JSON.parse(uvfFile);
        expect(json).to.have.property('protected');
        expect(json).to.have.property('recipients');
        expect(json).to.have.property('iv');
        expect(json).to.have.property('ciphertext');
        expect(json).to.have.property('tag');

        const decrypted: VaultMetadata = await VaultMetadata.decryptWithMemberKey(uvfFile, vaultMemberKey);
        expect(decrypted.seeds).to.deep.eq(original.seeds);
        expect(decrypted.initialSeedId).to.eq(original.initialSeedId);
        expect(decrypted.latestSeedId).to.eq(original.latestSeedId);
        expect(decrypted.automaticAccessGrant).to.deep.eq(original.automaticAccessGrant);
        expect(decrypted.payload().fileFormat).to.eq('AES-256-GCM-32k');
        expect(decrypted.payload().nameFormat).to.eq('AES-SIV-512-B64URL');
        expect(decrypted.payload().kdf).to.eq('HKDF-SHA512');
      });
    });
  });

  describe('RecoveryKey', () => {
    it('create()', async () => {
      const recoveryKey = await RecoveryKey.create();
      expect(recoveryKey).to.be.not.null;
    });

    it('recover() succeeds for valid recovery key', async () => {
      const serialized = `cult hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf
      all baby dig war linear tour sleep humanity threat question neglect stance radar bank coup misery painter tragedy buddy
      compare winter national approval budget deep screen outdoor audience tear stream cure type ugly chamber supporter franchise
      accept sexy ad imply being drug doctor regime where thick dam training grass chamber domestic dictator educate sigh music spoken
      connected measure voice lemon pig comprise disturb appear greatly satisfied heat news curiosity top impress nor method reflect
      lesson recommend dual revenge thorough bus count broadband living riot prejudice target blonde excess company thereby tribe
      respond horror mere way proud shopping wise liver mortgage plastic gentleman eighteen terms worry melt`;

      const recoveryKey = await RecoveryKey.recover(serialized);

      await Promise.all([
        expect(recoveryKey.serializePublicKey()).resolves.toBe('{"kid":"org.cryptomator.hub.recoverykey.T-LR82IaI1_TGHwcyn1u8vAYakGPz4upg1lPnE0xBZQ","kty":"EC","crv":"P-384","x":"SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ","y":"hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW"}'),
        expect(recoveryKey.serializePrivateKey()).resolves.toBe('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=')
      ]);
    });

    it('recover() fails for invalid recovery key', async () => {
      const notInDict = RecoveryKey.recover('hallo bonjour');
      const invalidPadding = RecoveryKey.recover('cult hold all away buck do law relaxed other stimulus');
      const invalidCrc = RecoveryKey.recover(`wrong hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf
      all baby dig war linear tour sleep humanity threat question neglect stance radar bank coup misery painter tragedy buddy
      compare winter national approval budget deep screen outdoor audience tear stream cure type ugly chamber supporter franchise
      accept sexy ad imply being drug doctor regime where thick dam training grass chamber domestic dictator educate sigh music spoken
      connected measure voice lemon pig comprise disturb appear greatly satisfied heat news curiosity top impress nor method reflect
      lesson recommend dual revenge thorough bus count broadband living riot prejudice target blonde excess company thereby tribe
      respond horror mere way proud shopping wise liver mortgage plastic gentleman eighteen terms worry melt`);

      await Promise.all([
        expect(notInDict).rejects.toThrow(/Word not in dictionary/),
        expect(invalidPadding).rejects.toThrow(/Invalid padding/),
        expect(invalidCrc).rejects.toThrow(/Invalid recovery key checksum/),
      ]);
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
        expect(serialized).to.eq('{"kid":"org.cryptomator.hub.recoverykey.T-LR82IaI1_TGHwcyn1u8vAYakGPz4upg1lPnE0xBZQ","kty":"EC","crv":"P-384","x":"SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ","y":"hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW"}');
      });

      it('createRecoveryKey()', async () => {
        const result = await recoveryKey.createRecoveryKey();
        expect(result).to.eq('cult hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf all baby dig war linear tour sleep humanity threat question neglect stance radar bank coup misery painter tragedy buddy compare winter national approval budget deep screen outdoor audience tear stream cure type ugly chamber supporter franchise accept sexy ad imply being drug doctor regime where thick dam training grass chamber domestic dictator educate sigh music spoken connected measure voice lemon pig comprise disturb appear greatly satisfied heat news curiosity top impress nor method reflect lesson recommend dual revenge thorough bus count broadband living riot prejudice target blonde excess company thereby tribe respond horror mere way proud shopping wise liver mortgage plastic gentleman eighteen terms worry melt');
      });
    });
  });

  describe('UniversalVaultFormat', () => {
    it('create()', async () => {
      const uvf = await UniversalVaultFormat.create({ enabled: true, maxWotDepth: -1 });
      expect(uvf).to.be.not.null;
      expect(uvf.metadata).to.be.not.null;
      expect(uvf.memberKey).to.be.not.null;
      expect(uvf.recoveryKey).to.be.not.null;
    });

    it('decrypt()', async () => {
      const dto: VaultDto = {
        id: '123',
        name: 'test',
        archived: false,
        creationTime: new Date(),
        uvfMetadataFile: '{"protected":"eyJvcmlnaW4iOiJodHRwcy4vL2V4YW1wbGUuY29tL2FwaS8vdmF1bHRzLzEyMy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"vJ16vGF2Z3NcA7nXPnVrgDLzgxZ8RFtySgf0FsckcrTBfKDg4hAK0w"},{"header":{"kid":"org.cryptomator.hub.recoverykey.J7-F_hjMaygRKdqIoZrbxSqVSRFJ5aF8BXuOCoBBGjw","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"oQZ8e-e9UIOtbN50ySx5Xwik9ET3uu38Bzl6HdDR8uipOzdXIO8OUhVQMJWqEHjs","y":"cTP6OI_YfdCVPVWpGOA1SjQ6-4vUpE4a6QJx3JSw29DkOL70Rjl4GAcDc4Iw-VHY","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"m_tSjpBcNQK42pgqLA9YDZYa3WQwJZ94HhGIpwOcKDoJQ8lP8IKbFw"}],"iv":"XPY9W-TE__Hu2m53","ciphertext":"uoukmSAuFTq-20gD9Ayum8iH6ERrld6cNV_ZTyfHBcWeIIZKOdp3RWWEtzNagVnRy5ix0yAafJIa14aSLnsxFv-NhzW1BirU5YypIkvFIO4cPnjI54vzd8nEtPdpp6z2JOqKUvZQYN89Y2EGoXb33FmQAwVMNJt_xDn2Bcb1dvI0q0uKLUidsvFL87NHSA8KUVWjXmmFdjibfqhWuO9YtFVoYD2Oqso9TzQIRMnDt3aIcVAouTTE7bR9O8kj5nseNID2gQ2osKlJVVcUn-4Cn0bI2w_-SeAfAvnePWLmolF8q79_aOsMkow3zMGsQHFoVU3PWCHR374Z02Lnt0Mj5_aUu_k8R5L11xNZQ0EYY7XWWGoUjRif7HmRfTZoHbJwvnHWk5q6IuEEjd_zSa4_im7PpoEofZR2EcH7Zz_Llq00wPWT05ZD82aRo3VCRNs6A2s6Jd8hspnaYA","tag":"j-xKxc2aZ2EDHDm8CzNf8Tj4QIkruZauF0LeUwrhq6c"}',
        uvfKeySet: '{"keys": [{"kid":"org.cryptomator.hub.recoverykey.J7-F_hjMaygRKdqIoZrbxSqVSRFJ5aF8BXuOCoBBGjw","kty":"EC","crv":"P-384","x":"3ydUf9ZwzYc9RAT2X4nMnJIU2nGbwRbvLj0ve7-C6_i6LaBpy2EbUrfrOBYbEoAN","y":"CQ77rXdI5tg0pyPpTLWzke2l_dMt6k9FquZpilf-_35XlK6weIEdh-ialC-Tw8P0"}]}'
      };
      const accessToken = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4Ijoia3VWU3FSSEVYbC1DbzhLRkhQTDRtN1FSWTd6NkMxcHlvRWNFVkw3X0VXY3N6ZDZmSWxyWEFyZ29Fbl9yejU0ZSIsInkiOiJ5RlBUNjN1VWdTVVo0VUxYcUtSWl9LMjBOZy1kZUh3WkFyU29xLU91RTFEcHF2czY3THpGNlAtZXk2Ykl5T0o5IiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.23s1IkwjWpjpzUxr_wZjyXjPwM-D19m0ONQI_naq6bURT2DSHnwe7g.iuH5sI2eL9Qumb_a.TVjVWBOQJAR-9Pu_Ke702hjww9JUZzg9sLyhjAj2o7aYgJtixKw.iQq2B6qQr4ZddqS7-__fhTAF3CteL73IpbJZNBabWLE';
      const uvf = await UniversalVaultFormat.decrypt(dto, accessToken, alice);

      expect(uvf).to.be.not.null;
      expect(uvf.metadata).to.be.not.null;
      expect(uvf.metadata.initialSeedId).to.eq(473544690);
      expect(uvf.metadata.latestSeedId).to.eq(1075513622);
      expect(base64url.stringify(uvf.metadata.kdfSalt, { pad: false })).to.eq('NIlr89R7FhochyP4yuXZmDqCnQ0dBB3UZ2D-6oiIjr8');
      expect(base64url.stringify(uvf.metadata.initialSeed, { pad: false })).to.eq('ypeBEsobvcr6wjGzmiPcTaeG7_gUfE5yuYB3ha_uSLs');
      expect(base64url.stringify(uvf.metadata.latestSeed, { pad: false })).to.eq('Ln0sA6lQeuJl7PW1NWiFpTOTogKdJBOUmXJloaJa78Y');
      expect(uvf.memberKey).to.be.not.null;
      expect(uvf.recoveryKey).to.be.not.null;
      expect(uvf.recoveryKey.privateKey).to.be.undefined;
    });

    it('recover()', async () => {
      const vaultUvfFileContents = '{"protected":"eyJvcmlnaW4iOiJodHRwcy4vL2V4YW1wbGUuY29tL2FwaS8vdmF1bHRzLzEyMy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"J3Gd7wZzsy-ykWPboW9CqK6DzoDGwRiZCJ3d6fNYkpt0klqDRmYR0g"},{"header":{"kid":"org.cryptomator.hub.recoverykey.DGbyooUW5QlVWzTaF102-f59uRTX1kdqkQb_CMDq9gM","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"q4p-3TTjHZxAkf6Fa3rknf_oqBGRiAkXB78_UxElbv6DN0Ufc_2RSX2pYKb8gaAU","y":"czJGKQe_q12AMYROZfEUBbP6zMo0DjgjK_346_BjS9RQ531jf32Ryht4xIBP50ef","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"7D-irqonnK01xQiTNGmsgSRPBzLdKdGTtq2XGS6BONkHWFgESpOYeg"}],"iv":"FNxcOvj_BgQ4vRVN","ciphertext":"qyORqJsK_3aec14d6gnIuNrLmnhFggY0MBWkjqOUXsOhSgOKcamtLbLGhk1gqWfc_dcshzWnBxx7TB2K_TwfSKnF0mumPldFMQ6u_HQM-jemBDCsStBSdOQoYqYcwbLyEfvH39XO4Oremppsj1a-8bOIbga0gnbQQmuNXraQtZLtyut82Q7P521Ab_leIUo_laKHPk610SW3DyJySoroxFH40qEb0GfsUAdcHMwCNA2dhQCCOW4zFZsmV_PxnVaNOhUV8aVz-lmwW8PnNCLxMdveQDegnbSvban6O0Qjl1MRnf1_pnBEM2meWk6CTnuJYwWaKHgbG69geGliOB_wKRTAapw8gOyQ-gPZya2hMvK2gAJLPWXAAc6mDo_MfzHa7PbLQ70J1r_tsJLKLmTrvU683Let70KG1_UyHFi6m9aFfXgA5PfmfGFKeDZ3e_bqNzFJbf-ytZJq9A","tag":"FpTkkwkTujb3OosDC-ck97W4awBStimQ95NfTe0TRxQ"}';
      const recoveryKey = 'cult hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf all baby dig dip departure subsidy boring inner pioneer excellent chip do outer frighten carriage know sculpture copper downtown pretty universe twelve condition indirect fantasy extend excuse affair canvas anybody arrest kilometre notorious period online franchise accept sexy ad mouse away fatal tool leave whereas behind stake disease balance cook knock foreign design there sister kill fortune associate spelling lorry snake dive penalty martial affection inclusion heal clothes attribute drain devise civic debut buy nurse cost visual insertion site surprise relevant cost per apologize dinner terms see protect lottery worthy rational infect dog latest physician goods severe enjoyable acute concert problem primarily prey pen material melt';

      const uvf = await UniversalVaultFormat.recover(vaultUvfFileContents, recoveryKey);

      expect(uvf).to.be.not.null;
      expect(uvf.metadata).to.be.not.null;
      expect(uvf.metadata.initialSeedId).to.eq(473544690);
      expect(uvf.metadata.latestSeedId).to.eq(1075513622);
      expect(base64url.stringify(uvf.metadata.kdfSalt, { pad: false })).to.eq('NIlr89R7FhochyP4yuXZmDqCnQ0dBB3UZ2D-6oiIjr8');
      expect(base64url.stringify(uvf.metadata.initialSeed, { pad: false })).to.eq('ypeBEsobvcr6wjGzmiPcTaeG7_gUfE5yuYB3ha_uSLs');
      expect(base64url.stringify(uvf.metadata.latestSeed, { pad: false })).to.eq('Ln0sA6lQeuJl7PW1NWiFpTOTogKdJBOUmXJloaJa78Y');
      expect(uvf.memberKey).to.be.not.null;
      expect(uvf.recoveryKey).to.be.not.null;
      expect(uvf.recoveryKey.privateKey).to.be.not.null;
      expect(uvf.recoveryKey.publicKey).to.be.not.null;
    });

    describe('instance methods', () => {
      let uvf: UniversalVaultFormat;

      beforeAll(async () => {
        const json = `{
            "fileFormat": "AES-256-GCM-32k",
            "nameFormat": "AES-SIV-512-B64URL",
            "seeds": {
                "HDm38g": "ypeBEsobvcr6wjGzmiPcTaeG7_gUfE5yuYB3ha_uSLs",
                "gBryKw": "PiPoFgA5WUoziU9lZOGxNIu9egCI1CxKy3PurtWcAJ0",
                "QBsJFg": "Ln0sA6lQeuJl7PW1NWiFpTOTogKdJBOUmXJloaJa78Y"
            },
            "initialSeed": "HDm38g",
            "latestSeed": "QBsJFg",
            "kdf": "HKDF-SHA512",
            "kdfSalt": "NIlr89R7FhochyP4yuXZmDqCnQ0dBB3UZ2D-6oiIjr8",
            "org.example.customfield": 42
        }`;
        const metadata = await VaultMetadata.createFromJson(JSON.parse(json));
        uvf = await UniversalVaultFormat.forTesting(metadata);
      });

      it('encryptForUser() creates an access token', async () => {
        const token = await uvf.encryptForUser(alice.ecdhKeyPair.publicKey);
        expect(token).to.be.not.null;
      });

      it('create recovery key', async () => {
        const recoveryKey = await uvf.recoveryKey.createRecoveryKey();
        expect(recoveryKey).to.match(/^cult hold.+$/);
      });

      it('createMetadataFile() creates a vault.uvf file', async () => {
        const json = await uvf.createMetadataFile('https.//example.com/api/', { id: '123', name: 'test', archived: false, creationTime: new Date() });
        expect(json).to.be.not.null;
        const jwe = JSON.parse(json) as JsonJWE;
        expect(jwe.protected).to.not.be.empty;
        expect(jwe.recipients).to.have.lengthOf(2);
        expect(jwe.iv).to.not.be.empty;
        expect(jwe.ciphertext).to.not.be.empty;
        expect(jwe.tag).to.not.be.empty;
      });

      it('serializePublicKey() creates a JWK-encoded representation', async () => {
        const json = await uvf.recoveryKey.serializePublicKey();
        expect(json).to.be.not.null;
        const jwk = JSON.parse(json) as JsonWebKey & { kid: string };
        expect(jwk.kty).to.eq('EC');
        expect(jwk.crv).to.eq('P-384');
        expect(jwk.x).to.not.be.empty;
        expect(jwk.y).to.not.be.empty;
        expect(jwk.d).to.be.undefined;
        expect(jwk.kid).to.not.be.empty;
      });

      it('computeRootDirId() deterministically creates a dir ID', async () => {
        const dirId = await uvf.computeRootDirId();
        expect(dirId).to.have.a.lengthOf(32);
        expect(base64.stringify(dirId)).to.eq('5WEGzwKkAHPwVSjT2Brr3P3zLz7oMiNpMn/qBvht7eM=');
      });

      it('computeRootDirIdHash() creates a truncated hmac', async () => {
        const rootDirId = base64.parse('5WEGzwKkAHPwVSjT2Brr3P3zLz7oMiNpMn/qBvht7eM=').slice();
        const hash = await uvf.computeRootDirIdHash(rootDirId);
        expect(hash).to.have.a.lengthOf(32);
        expect(hash).to.eq('RZK7ZH7KBXULNEKBMGX3CU42PGUIAIX4');
      });

      it('encryptFile() creates some ciphertext', async () => {
        const rootDirId = base64.parse('5WEGzwKkAHPwVSjT2Brr3P3zLz7oMiNpMn/qBvht7eM=').slice();
        const fileContent = await uvf.encryptFile(rootDirId, uvf.metadata.initialSeedId);
        expect(fileContent).to.have.a.lengthOf(128);
        expect(fileContent.slice(0, 4)).to.eql(new Uint8Array([0x75, 0x76, 0x66, 0x01])); // magic bytes
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
  public constructor(ecdhKeyPair: CryptoKeyPair, ecdsaKeyPair: CryptoKeyPair) {
    super(ecdhKeyPair, ecdsaKeyPair);
  }
}

class TestRecoveryKey extends RecoveryKey {
  public constructor(readonly publicKey: CryptoKey, readonly privateKey?: CryptoKey) {
    super(publicKey, privateKey);
  }
}

// #endregion

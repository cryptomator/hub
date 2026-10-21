import { base64, base64urlnopad } from '@scure/base';
import { beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { VaultDto } from '../../src/common/backend';
import { UserKeys } from '../../src/common/crypto';
import { JsonJWE } from '../../src/common/jwe';
import { MemberKey, MetadataPayload, RecoveryKey, UniversalVaultFormat, UnsupportedVaultFormatError, VaultMetadata } from '../../src/common/universalVaultFormat';

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

      expect(encrypted).to.not.be.undefined;
    });

    it('load(userKeyPair.decryptAccessToken(...))', async () => {
      const jwe = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoicFotVXExTjNOVElRcHNpZC11UGZMaW95bVVGVFJLM1dkTXVkLWxDcGh5MjQ4bUlJelpDc3RPRzZLTGloZnBkZyIsInkiOiJzMnl6eF9Ca2QweFhIcENnTlJFOWJiQUIyQkNNTF80cWZwcFEza1N2LXhqcEROVWZZdmlxQS1xRERCYnZkNDdYIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.I_rXJagNrrCa9zISf0DZJLQbIZDxEpGxCyjFbNE0iZs6yFeVayNOGQ.7rASe4SqyKJJLHZ4.l6T2N_ATytZUyh1IZTIJJDY4dXCyQVsRB19QIIPrAi0QQiS4gl4.fnOtAJhdvPFFHVi6L5Ma_R8iL3IXq1_xAq2PvdEfx0A';

      const payload = await alice.decryptAccessToken(jwe);
      const decrypted = await MemberKey.load(payload.key);

      expect(decrypted).to.not.be.undefined;
      await expect(decrypted.serializeKey()).resolves.toBe('VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVU=');
    });
  });

  describe('VaultMetadata', () => {
    it('create()', async () => {
      const orig = VaultMetadata.create({ enabled: true, trustThreshold: 1 });
      expect(orig).to.not.be.undefined;
      expect(orig.seeds.get(orig.initialSeedId)).to.not.be.undefined;
      expect(orig.seeds.get(orig.initialSeedId)!.length).to.eq(32);
      expect(orig.initialSeedId).to.eq(orig.latestSeedId);
      expect(orig.kdfSalt.length).to.eq(32);
    });

    describe('createFromJson()', () => {
      const valid = VaultMetadata.create({ enabled: true, trustThreshold: 1 }).payload();
      const withField = (field: string, value: unknown) => ({ ...valid, [field]: value }) as unknown as MetadataPayload;

      it('accepts a valid payload', () => {
        const parsed = VaultMetadata.createFromJson(valid);

        expect(parsed.payload()).to.deep.eq(valid);
      });

      it('round-trips a seed ID with the most significant bit set', () => {
        const seed = base64urlnopad.encode(new Uint8Array(32));
        const payload = { ...valid, seeds: { '_____w': seed }, initialSeed: '_____w', latestSeed: '_____w' };

        const parsed = VaultMetadata.createFromJson(payload);

        expect(parsed.initialSeedId).to.eq(0xFFFFFFFF);
        expect(parsed.payload().seeds).to.deep.eq({ '_____w': seed });
        expect(parsed.payload().initialSeed).to.eq('_____w');
      });

      it('rejects unsupported fileFormat', () => {
        expect(() => VaultMetadata.createFromJson(withField('fileFormat', 'AES-256-GCM-64k'))).to.throw(UnsupportedVaultFormatError, 'Unsupported fileFormat: "AES-256-GCM-64k"');
        expect(() => VaultMetadata.createFromJson(withField('fileFormat', undefined))).to.throw(UnsupportedVaultFormatError);
      });

      it('rejects unsupported nameFormat', () => {
        expect(() => VaultMetadata.createFromJson(withField('nameFormat', 'AES-SIV-512-B32-CI'))).to.throw(UnsupportedVaultFormatError, 'Unsupported nameFormat: "AES-SIV-512-B32-CI"');
        expect(() => VaultMetadata.createFromJson(withField('nameFormat', undefined))).to.throw(UnsupportedVaultFormatError);
      });

      it('rejects unsupported kdf', () => {
        expect(() => VaultMetadata.createFromJson(withField('kdf', 'HKDF-SHA256'))).to.throw(UnsupportedVaultFormatError, 'Unsupported kdf: "HKDF-SHA256"');
        expect(() => VaultMetadata.createFromJson(withField('kdf', undefined))).to.throw(UnsupportedVaultFormatError);
      });

      it('rejects seeds that are not 32 bytes', () => {
        const shortSeed = base64urlnopad.encode(new Uint8Array(16));
        const payload = { ...valid, seeds: { ...valid.seeds, 'AAAAAA': shortSeed } };

        expect(() => VaultMetadata.createFromJson(payload)).to.throw('Malformed seed: AAAAAA');
      });

      it('rejects malformed seed IDs', () => {
        const payload = { ...valid, seeds: { ...valid.seeds, 'AAAA': base64urlnopad.encode(new Uint8Array(32)) } };

        expect(() => VaultMetadata.createFromJson(payload)).to.throw('Malformed seed ID');
        expect(() => VaultMetadata.createFromJson(withField('latestSeed', 'AAAA'))).to.throw('Malformed seed ID');
      });

      it('rejects kdfSalt that is not 32 bytes', () => {
        expect(() => VaultMetadata.createFromJson(withField('kdfSalt', base64urlnopad.encode(new Uint8Array(16))))).to.throw('Malformed kdfSalt');
      });

      it('rejects missing initialSeed or latestSeed', () => {
        expect(() => VaultMetadata.createFromJson(withField('initialSeed', 'AAAAAA'))).to.throw('Initial seed is missing');
        expect(() => VaultMetadata.createFromJson(withField('latestSeed', 'AAAAAA'))).to.throw('Latest seed is missing');
      });
    });

    describe('instance methods', () => {
      let original: VaultMetadata;

      beforeEach(async () => {
        // prepare some test metadata:
        original = VaultMetadata.create({ enabled: true, trustThreshold: 1 });
      });

      it('decrypt(encrypt(orig)) == orig', async () => {
        const dto: VaultDto = { id: '123', name: 'test', archived: false, creationTime: new Date(), requiredEmergencyKeyShares: 0, emergencyKeyShares: {} };
        const vaultMemberKey = await MemberKey.create();
        const recoveryKey = await RecoveryKey.create();

        const uvfFile: string = await original.encrypt('https://example.com/api/', dto, vaultMemberKey, recoveryKey);
        expect(uvfFile).to.not.be.undefined;
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
      expect(recoveryKey).to.not.be.undefined;
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
      const uvf = await UniversalVaultFormat.create({ enabled: true, trustThreshold: 1 });
      expect(uvf).to.not.be.undefined;
      expect(uvf.metadata).to.not.be.undefined;
      expect(uvf.memberKey).to.not.be.undefined;
      expect(uvf.recoveryKey).to.not.be.undefined;
    });

    it('decrypt()', async () => {
      const dto: VaultDto = {
        id: '123',
        name: 'test',
        archived: false,
        creationTime: new Date(),
        requiredEmergencyKeyShares: 0,
        emergencyKeyShares: {},
        uvfMetadataFile: '{"protected":"eyJjdHkiOiJqc29uIiwiY3JpdCI6WyJ1dmYuc3BlYy52ZXJzaW9uIl0sInV2Zi5zcGVjLnZlcnNpb24iOjEsImNsb3VkLmthdHRhLm9yaWdpbiI6Imh0dHBzOi8vZXhhbXBsZS5jb20vYXBpL3ZhdWx0cy8xMjMvdXZmL3ZhdWx0LnV2ZiIsImprdSI6Imp3a3MuanNvbiIsImVuYyI6IkEyNTZHQ00ifQ","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"Oi_vY3htg4QrzGNgq7PAPq3HtSnnn1_bAl2pW-uhmzyXR5XJcQbj_g"},{"header":{"kid":"org.cryptomator.hub.recoverykey.J7-F_hjMaygRKdqIoZrbxSqVSRFJ5aF8BXuOCoBBGjw","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"HuqGTpqSYTNXddwFiaRBlf5-VGU1ejmM6nv_udlF-KGTqVHUjNBEVhVAInC_XaAn","y":"ujqIpodt3NBTGv-QDILX6-6NdnB-qlXDB6ESkVz1qI2XqlHTpvGt76itucEY4eAJ","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"eS-HK8LKnb5YTNA02lGlkY3FbowbASHQfGei4a5r26alXBwyBFeSzA"}],"iv":"sfHxIQaWIqzCCW-y","ciphertext":"W_j9TpA0go6emGyzbiXc-i6frRHAplAI3O42Nx6bAvRTmkQvn2rOrDmfYZYBAmw69qZP-UMf7lXdrosMiXIt1cSfqcCPacl3PbvNDarij9xX0fKDxSjQm_VCT2AhNMAw2qcQk292CmkJGkpTwCrw8UletV-QCxM5wAHmLoqNmUdAe0Dfxd42VxoDDQ5hcyrtHDSFddT6x4xoBTL_ETJt5JjATrnuGla5NXjR-A_P_LsqClk-LDVuaowBefxZOmQDTgIh4ZKFyeqLfGu5E9y0ay2FzaV1qhHLbRRIuh4FZda_pPjy6uUwBLwTG0PGEUNSwonBn29Oo5hpX4itakKdzxKdAJarvTqSPFLSqjeDsxeGX4DrVSDhHYq5ZWZNA_0B_Cpb1Ob2xVpRDA2XfvoPYRkquJ3711RphoStfHLfTdmbuGq9axIK8xW9lwWjWDVdmAKjihhUZRekFQ","tag":"gY3kidDjJvqiFhD-LDylNMOJhpteOGX9hAcPf294PeA"}',
        uvfKeySet: '{"keys": [{"kid":"org.cryptomator.hub.recoverykey.J7-F_hjMaygRKdqIoZrbxSqVSRFJ5aF8BXuOCoBBGjw","kty":"EC","crv":"P-384","x":"3ydUf9ZwzYc9RAT2X4nMnJIU2nGbwRbvLj0ve7-C6_i6LaBpy2EbUrfrOBYbEoAN","y":"CQ77rXdI5tg0pyPpTLWzke2l_dMt6k9FquZpilf-_35XlK6weIEdh-ialC-Tw8P0"}]}'
      };
      const accessToken = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4Ijoia3VWU3FSSEVYbC1DbzhLRkhQTDRtN1FSWTd6NkMxcHlvRWNFVkw3X0VXY3N6ZDZmSWxyWEFyZ29Fbl9yejU0ZSIsInkiOiJ5RlBUNjN1VWdTVVo0VUxYcUtSWl9LMjBOZy1kZUh3WkFyU29xLU91RTFEcHF2czY3THpGNlAtZXk2Ykl5T0o5IiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.23s1IkwjWpjpzUxr_wZjyXjPwM-D19m0ONQI_naq6bURT2DSHnwe7g.iuH5sI2eL9Qumb_a.TVjVWBOQJAR-9Pu_Ke702hjww9JUZzg9sLyhjAj2o7aYgJtixKw.iQq2B6qQr4ZddqS7-__fhTAF3CteL73IpbJZNBabWLE';
      const uvf = await UniversalVaultFormat.decrypt(dto, accessToken, alice);

      expect(uvf).to.not.be.undefined;
      expect(uvf.metadata).to.not.be.undefined;
      expect(uvf.metadata.initialSeedId).to.eq(473544690);
      expect(uvf.metadata.latestSeedId).to.eq(1075513622);
      expect(base64urlnopad.encode(uvf.metadata.kdfSalt)).to.eq('NIlr89R7FhochyP4yuXZmDqCnQ0dBB3UZ2D-6oiIjr8');
      expect(base64urlnopad.encode(uvf.metadata.initialSeed)).to.eq('ypeBEsobvcr6wjGzmiPcTaeG7_gUfE5yuYB3ha_uSLs');
      expect(base64urlnopad.encode(uvf.metadata.latestSeed)).to.eq('Ln0sA6lQeuJl7PW1NWiFpTOTogKdJBOUmXJloaJa78Y');
      expect(uvf.memberKey).to.not.be.undefined;
      expect(uvf.recoveryKey).to.not.be.undefined;
      expect(uvf.recoveryKey.privateKey).to.be.undefined;
    });

    it('recover()', async () => {
      const vaultUvfFileContents = '{"protected":"eyJjdHkiOiJqc29uIiwiY3JpdCI6WyJ1dmYuc3BlYy52ZXJzaW9uIl0sInV2Zi5zcGVjLnZlcnNpb24iOjEsImNsb3VkLmthdHRhLm9yaWdpbiI6Imh0dHBzOi8vZXhhbXBsZS5jb20vYXBpL3ZhdWx0cy8xMjMvdXZmL3ZhdWx0LnV2ZiIsImprdSI6Imp3a3MuanNvbiIsImVuYyI6IkEyNTZHQ00ifQ","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"SZtfgIF3BHSSxOmBwpYgQ3Fx0m8HUNlh8i-DXJ1SrPV8N4HbpfV71w"},{"header":{"kid":"org.cryptomator.hub.recoverykey.DGbyooUW5QlVWzTaF102-f59uRTX1kdqkQb_CMDq9gM","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"GAu7yAb-bpioelnf2rq-gVWLBPvLsp2NBZ7Y7-PncF6nwJ6u7jjWpA36p-4kJM_L","y":"PwENMBSgYKMYkPsfmpERRKLXOFk3kc_4J5FrQRTl8yxOX88S9YsTJ-v4PfeFIPAE","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"jCHJArsVh6cigLlp-85H-19d8a4ChLZ5dpqZPUgVhNYeg7eG8O04Vw"}],"iv":"fWTR97bqlobFHGHe","ciphertext":"5J9HTmPyot0vE_l7XTBCfaLl30UnTLVh-ja7mTez6OO0ylhYsMl2_1CRmACsuzbFrgwfnVzAd3j_73M5rqW6YJprsft1MFcqoJWyQTteLNaI3s7JueQXkpAsAWjvf9JIuYF8ifXfo0zLc2GHsEXwaXb_d3Uwwn128c_gcNrOyh3m-luGJ1osWZtOf3KwPhSoeyKUYsfDb_eezO_bU_19nh9qd4Ds2eID4KlksQTydqz3q5OYVcv7yhX1fqhWBLYVn5V0UVY0glXLZziVA-qYBcOc4Prg6euQ9HZcLrHItjFozWajmJwfQMCM1ZS7zb4dYg3gwmnGsFjKn6J8hS3IgP7kXAtWpPtLXfVJ9EkiHtqr7URZipPuByziYlczQkvj3xT_Bdx2PYhsnj3Mmai0h1x9oIvIviPi05mzmdj_ef87lFyDDUDjx_ZyzC5DoNXX-Qn7YpeNpYuNyg","tag":"7zLMPl3ppUrHufOb6Om-5VrOn8l3UUSxi0mgamyPEjQ"}';
      const recoveryKey = 'cult hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf all baby dig dip departure subsidy boring inner pioneer excellent chip do outer frighten carriage know sculpture copper downtown pretty universe twelve condition indirect fantasy extend excuse affair canvas anybody arrest kilometre notorious period online franchise accept sexy ad mouse away fatal tool leave whereas behind stake disease balance cook knock foreign design there sister kill fortune associate spelling lorry snake dive penalty martial affection inclusion heal clothes attribute drain devise civic debut buy nurse cost visual insertion site surprise relevant cost per apologize dinner terms see protect lottery worthy rational infect dog latest physician goods severe enjoyable acute concert problem primarily prey pen material melt';

      const uvf = await UniversalVaultFormat.recover(vaultUvfFileContents, recoveryKey);

      expect(uvf).to.not.be.undefined;
      expect(uvf.metadata).to.not.be.undefined;
      expect(uvf.metadata.initialSeedId).to.eq(473544690);
      expect(uvf.metadata.latestSeedId).to.eq(1075513622);
      expect(base64urlnopad.encode(uvf.metadata.kdfSalt)).to.eq('NIlr89R7FhochyP4yuXZmDqCnQ0dBB3UZ2D-6oiIjr8');
      expect(base64urlnopad.encode(uvf.metadata.initialSeed)).to.eq('ypeBEsobvcr6wjGzmiPcTaeG7_gUfE5yuYB3ha_uSLs');
      expect(base64urlnopad.encode(uvf.metadata.latestSeed)).to.eq('Ln0sA6lQeuJl7PW1NWiFpTOTogKdJBOUmXJloaJa78Y');
      expect(uvf.memberKey).to.not.be.undefined;
      expect(uvf.recoveryKey).to.not.be.undefined;
      expect(uvf.recoveryKey.privateKey).to.not.be.undefined;
      expect(uvf.recoveryKey.publicKey).to.not.be.undefined;
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
        const metadata = VaultMetadata.createFromJson(JSON.parse(json));
        uvf = await UniversalVaultFormat.forTesting(metadata);
      });

      it('encryptForUser() creates an access token', async () => {
        const token = await uvf.encryptForUser(alice.ecdhKeyPair.publicKey);
        expect(token).to.not.be.undefined;
      });

      it('create recovery key', async () => {
        const recoveryKey = await uvf.recoveryKey.createRecoveryKey();
        expect(recoveryKey).to.match(/^cult hold.+$/);
      });

      it('createMetadataFile() creates a vault.uvf file', async () => {
        const json = await uvf.createMetadataFile('https.//example.com/api/', { id: '123', name: 'test', archived: false, creationTime: new Date(), requiredEmergencyKeyShares: 0, emergencyKeyShares: {} });
        expect(json).to.not.be.undefined;
        const jwe = JSON.parse(json) as JsonJWE;
        expect(jwe.protected).to.not.be.empty;
        expect(jwe.recipients).to.have.lengthOf(2);
        expect(jwe.iv).to.not.be.empty;
        expect(jwe.ciphertext).to.not.be.empty;
        expect(jwe.tag).to.not.be.empty;
      });

      it('serializePublicKey() creates a JWK-encoded representation', async () => {
        const json = await uvf.recoveryKey.serializePublicKey();
        expect(json).to.not.be.undefined;
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
        expect(base64.encode(dirId)).to.eq('5WEGzwKkAHPwVSjT2Brr3P3zLz7oMiNpMn/qBvht7eM=');
      });

      it('computeRootDirIdHash() creates a truncated hmac', async () => {
        const rootDirId = base64.decode('5WEGzwKkAHPwVSjT2Brr3P3zLz7oMiNpMn/qBvht7eM=') as Uint8Array<ArrayBuffer>;
        const hash = await uvf.computeRootDirIdHash(rootDirId);
        expect(hash).to.have.a.lengthOf(32);
        expect(hash).to.eq('RZK7ZH7KBXULNEKBMGX3CU42PGUIAIX4');
      });

      it('encryptFile() creates some ciphertext', async () => {
        const rootDirId = base64.decode('5WEGzwKkAHPwVSjT2Brr3P3zLz7oMiNpMn/qBvht7eM=') as Uint8Array<ArrayBuffer>;
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

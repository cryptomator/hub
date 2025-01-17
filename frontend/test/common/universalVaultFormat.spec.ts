import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
import { webcrypto } from 'node:crypto';
import { base64 } from 'rfc4648';
import { VaultDto } from '../../src/common/backend';
import { UserKeys } from '../../src/common/crypto';
import { JsonJWE } from '../../src/common/jwe';
import { MemberKey, RecoveryKey, UniversalVaultFormat, VaultMetadata } from '../../src/common/universalVaultFormat';

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

describe('UVF', () => {
  let alice: UserKeys;

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: webcrypto });
    // @ts-expect-error: incomplete 'window' type
    global.window = { crypto: global.crypto };

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
      return expect(decrypted.serializeKey()).to.eventually.eq('VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVU=');
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

      return Promise.all([
        expect(recoveryKey.serializePublicKey()).to.eventually.eq('{"kid":"org.cryptomator.hub.recoverykey.T-LR82IaI1_TGHwcyn1u8vAYakGPz4upg1lPnE0xBZQ","kty":"EC","crv":"P-384","x":"SzrRXmyI8VWFJg1dPUNbFcc9jZvjZEfH7ulKI1UkXAltd7RGWrcfFxqyGPcwu6AQ","y":"hHUag3OvDzEr0uUQND4PXHQTXP5IDGdYhJhL-WLKjnGjQAw0rNGy5V29-aV-yseW"}'),
        expect(recoveryKey.serializePrivateKey()).to.eventually.eq('MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDDCi4K1Ts3DgTz/ufkLX7EGMHjGpJv+WJmFgyzLwwaDFSfLpDw0Kgf3FKK+LAsV8r+hZANiAARLOtFebIjxVYUmDV09Q1sVxz2Nm+NkR8fu6UojVSRcCW13tEZatx8XGrIY9zC7oBCEdRqDc68PMSvS5RA0Pg9cdBNc/kgMZ1iEmEv5YsqOcaNADDSs0bLlXb35pX7Kx5Y=')
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

      return Promise.all([
        expect(notInDict).to.be.rejectedWith(Error, /Word not in dictionary/),
        expect(invalidPadding).to.be.rejectedWith(Error, /Invalid padding/),
        expect(invalidCrc).to.be.rejectedWith(Error, /Invalid recovery key checksum/),
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
        uvfMetadataFile: '{"protected":"eyJvcmlnaW4iOiJodHRwcy4vL2V4YW1wbGUuY29tL2FwaS8vdmF1bHRzLzEyMy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"6Jmq4Uje1Njp6UOZhMC8EQySIhb82OGqJ3LT_CNGtfS3PTVAanFTkw"},{"header":{"kid":"org.cryptomator.hub.recoverykey.gRQZJlBF8UOUry_viZdNFrIG02r12zKPfKPJjg1z0gc","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"ZIM3rAYg1CPMJgYD2-8M04n00tvUnXjx6QBjXMdZJZwozEjo6ns1aI3WVjGb71HC","y":"iYiX7Qs6JV8AxW-W9pRp_wFUqiwD22L7ytbr91eazmKDK0Uuk72K8xWdz0x3TZHB","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"UDe4WL6fkZCMhLQaNYMGvbRFGGNum8Nk7ahiaORnPi9BlUxPCCArkg"}],"iv":"DW6-c25BXTxJ-wF4","ciphertext":"5yhWvQwZPb6ss_b30pFxxpQPKC_-QIKgK1sdTQUUggl5yL6gzfFSOeERDjuD5WSFiLSisMi1BZwI-GPzclpG4SsjIMtOakfe_OcKQGBqpPAfJLiRUePg4CmptBTY205dbqFZfJvFK-jVxUP8oc7mYg3Z8kUovhUAeoIkyubaeiGcV49NLF-_UgV5smPouAxVBBroBpnJ76BDM0UfanfTzHZ_ekynwZDK7HFw4lJZ3xxrBkIbimB5NQU6cL8lviOGbaSQ8dAGCGcSuafKrAXlzOItCUdK4w5cBUTFJV6LILkKEmxy8atYNJdh-4VoJmOwv-LwGhwJyv20yrq5RrV-flgN4_jjFmh71ne_XchWy8MH7vdY8ShkT1KI6waFPcfU2PglEYQn6qaDBJ3igMsLUnNL1SRxrFY3ov6--mJdGlz-0eFny0Nz8Qy2bjxIOxCaKMRTQz41d1N7uiXcEa4","tag":"PZFk3Lw2XxZu2_ZooqV0lyI0ALRB8l6YuKDeH3jYBtQ"}',
        uvfKeySet: '{"keys": [{"kid":"org.cryptomator.hub.recoverykey.gRQZJlBF8UOUry_viZdNFrIG02r12zKPfKPJjg1z0gc","kty":"EC","crv":"P-384","x":"PKwD88lrI0LBwC0p5IKcHCaubRLNPCueNi_mfGrp83Y09MOIX3wIH63At0Lm897r","y":"d7TKPTkh4DURRrMGZp6Vig_cVAWjOa_nwurJ36Gp8SEiJbB6eK0uWxZL_tTESq9K"}]}'
      };
      const accessToken = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoiSDZ3M1VWbXJ1VTcwRnUwWHFqN0FLekNkZl95dVF1SXVHa0FvVEljaHBaUWNpV0JkbnRjOE93TGhTSU5xZFNGeiIsInkiOiIyWjlqbE5pX0lXa3lCbU1ycDhMbEttamg1WDUwWExGQTNsNzNVYVJ3bHRZSlpOVzdoTTVHTkZsX21XNmxBbWVlIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.1YGRThFLsu6XjoUhTkxzX6DTbyhEmjl2l07hjCl4qBynFv9hrZIpHg.3HMszd5g8-6FPrcX.ngw9_D6xpRKWNeALK2oW6d314JXnUYjYDhohAnRBjNezTdOgbLw.1E1tcGvv4wFfarSsL_atcCS47uDKjU7MT0C9wFBQIjk';
      const uvf = await UniversalVaultFormat.decrypt(dto, accessToken, alice);

      expect(uvf).to.be.not.null;
      expect(uvf.metadata).to.be.not.null;
      expect(uvf.metadata.initialSeedId).to.eq(4072093980);
      expect(uvf.metadata.latestSeedId).to.eq(369695552);
      expect(base64.stringify(uvf.metadata.kdfSalt)).to.eq('NIlr89R7FhochyP4yuXZmDqCnQ0dBB3UZ2D+6oiIjr8=');
      expect(base64.stringify(uvf.metadata.initialSeed)).to.eq('ypeBEsobvcr6wjGzmiPcTaeG7/gUfE5yuYB3ha/uSLs=');
      expect(base64.stringify(uvf.metadata.latestSeed)).to.eq('Ln0sA6lQeuJl7PW1NWiFpTOTogKdJBOUmXJloaJa78Y=');
      expect(uvf.memberKey).to.be.not.null;
      expect(uvf.recoveryKey).to.be.not.null;
      expect(uvf.recoveryKey.privateKey).to.be.undefined;
    });

    it('recover()', async () => {
      const vaultUvfFileContents = '{"protected":"eyJvcmlnaW4iOiJodHRwcy4vL2V4YW1wbGUuY29tL2FwaS8vdmF1bHRzLzEyMy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"6Jmq4Uje1Njp6UOZhMC8EQySIhb82OGqJ3LT_CNGtfS3PTVAanFTkw"},{"header":{"kid":"org.cryptomator.hub.recoverykey.gRQZJlBF8UOUry_viZdNFrIG02r12zKPfKPJjg1z0gc","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"ZIM3rAYg1CPMJgYD2-8M04n00tvUnXjx6QBjXMdZJZwozEjo6ns1aI3WVjGb71HC","y":"iYiX7Qs6JV8AxW-W9pRp_wFUqiwD22L7ytbr91eazmKDK0Uuk72K8xWdz0x3TZHB","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"UDe4WL6fkZCMhLQaNYMGvbRFGGNum8Nk7ahiaORnPi9BlUxPCCArkg"}],"iv":"DW6-c25BXTxJ-wF4","ciphertext":"5yhWvQwZPb6ss_b30pFxxpQPKC_-QIKgK1sdTQUUggl5yL6gzfFSOeERDjuD5WSFiLSisMi1BZwI-GPzclpG4SsjIMtOakfe_OcKQGBqpPAfJLiRUePg4CmptBTY205dbqFZfJvFK-jVxUP8oc7mYg3Z8kUovhUAeoIkyubaeiGcV49NLF-_UgV5smPouAxVBBroBpnJ76BDM0UfanfTzHZ_ekynwZDK7HFw4lJZ3xxrBkIbimB5NQU6cL8lviOGbaSQ8dAGCGcSuafKrAXlzOItCUdK4w5cBUTFJV6LILkKEmxy8atYNJdh-4VoJmOwv-LwGhwJyv20yrq5RrV-flgN4_jjFmh71ne_XchWy8MH7vdY8ShkT1KI6waFPcfU2PglEYQn6qaDBJ3igMsLUnNL1SRxrFY3ov6--mJdGlz-0eFny0Nz8Qy2bjxIOxCaKMRTQz41d1N7uiXcEa4","tag":"PZFk3Lw2XxZu2_ZooqV0lyI0ALRB8l6YuKDeH3jYBtQ"}';
      const recoveryKey = 'cult hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf all baby dig rip may southern bar super use emotional punk pathway who infection bet sporting colleague buffer neutral corporate stand mud desire rob mortgage actually tackle browser killing month minority company editorial escalate expense emission accept sexy ad guest symptom dirty friendly condemn ghost idea warn forever visual workout seat architect call tomorrow true situated bind branch critic heavily nature smoke birthday clinic rubber unique village tin glass female militant harmony embrace officer alive less pole core custody attend funding burst arms convince gap limb apology safety discovery firstly limited hearing nail oven balloon quickly uniform upcoming east tragedy wildlife image abolish nervous till melt';

      const uvf = await UniversalVaultFormat.recover(vaultUvfFileContents, recoveryKey);

      expect(uvf).to.be.not.null;
      expect(uvf.metadata).to.be.not.null;
      expect(uvf.metadata.initialSeedId).to.eq(4072093980);
      expect(uvf.metadata.latestSeedId).to.eq(369695552);
      expect(base64.stringify(uvf.metadata.kdfSalt)).to.eq('NIlr89R7FhochyP4yuXZmDqCnQ0dBB3UZ2D+6oiIjr8=');
      expect(base64.stringify(uvf.metadata.initialSeed)).to.eq('ypeBEsobvcr6wjGzmiPcTaeG7/gUfE5yuYB3ha/uSLs=');
      expect(base64.stringify(uvf.metadata.latestSeed)).to.eq('Ln0sA6lQeuJl7PW1NWiFpTOTogKdJBOUmXJloaJa78Y=');
      expect(uvf.memberKey).to.be.not.null;
      expect(uvf.recoveryKey).to.be.not.null;
      expect(uvf.recoveryKey.privateKey).to.be.not.null;
      expect(uvf.recoveryKey.publicKey).to.be.not.null;
    });

    describe('instance methods', () => {
      let uvf: UniversalVaultFormat;

      before(async () => {
        const json = `{
            "fileFormat": "AES-256-GCM-32k",
            "nameFormat": "AES-SIV-512-B64URL",
            "seeds": {
                "HDm38g": "ypeBEsobvcr6wjGzmiPcTaeG7/gUfE5yuYB3ha/uSLs=",
                "gBryKw": "PiPoFgA5WUoziU9lZOGxNIu9egCI1CxKy3PurtWcAJ0=",
                "QBsJFg": "Ln0sA6lQeuJl7PW1NWiFpTOTogKdJBOUmXJloaJa78Y="
            },
            "initialSeed": "HDm38g",
            "latestSeed": "QBsJFg",
            "kdf": "HKDF-SHA512",
            "kdfSalt": "NIlr89R7FhochyP4yuXZmDqCnQ0dBB3UZ2D+6oiIjr8=",
            "org.example.customfield": 42
        }`;
        const metadata = await VaultMetadata.createFromJson(JSON.parse(json));
        uvf = await UniversalVaultFormat.forTesting(metadata);
      });

      it('encryptForUser() creates an access token', async () => {
        const token = await uvf.encryptForUser(alice.ecdhKeyPair.publicKey);
        expect(token).to.be.not.null;
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
        const rootDirId = base64.parse('5WEGzwKkAHPwVSjT2Brr3P3zLz7oMiNpMn/qBvht7eM=');
        const hash = await uvf.computeRootDirIdHash(rootDirId);
        expect(hash).to.have.a.lengthOf(32);
        expect(hash).to.eq('RKHZLENL3PQIW6GZHE3KRRRGLFBHWHRU');
      });

      it('encryptFile() creates some ciphertext', async () => {
        const rootDirId = base64.parse('24UBEDeGu5taq7U4GqyA0MXUXb9HTYS6p3t9vvHGJAc=');
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

import { use as chaiUse, expect } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { before, describe } from 'mocha';
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
    Object.defineProperty(global, 'crypto', { value: require('node:crypto').webcrypto });
    // @ts-ignore: global not defined (but will be available within Node)
    global.window = { crypto: global.crypto };

    // prepare some test key pairs:
    const alicePrv = crypto.subtle.importKey('jwk', alicePrivate, UserKeys.KEY_DESIGNATION, true, UserKeys.KEY_USAGES);
    const alicePub = crypto.subtle.importKey('jwk', alicePublic, UserKeys.KEY_DESIGNATION, true, []);
    alice = new TestUserKeys({ privateKey: await alicePrv, publicKey: await alicePub });
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
      expect(orig.seeds.get(orig.initialSeedId)).to.not.be.undefined
      expect(orig.seeds.get(orig.initialSeedId)!.length).to.eq(32)
      expect(orig.initialSeedId).to.eq(orig.latestSeedId)
      expect(orig.kdfSalt.length).to.eq(32)
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
        expect(decrypted.seeds).to.deep.eq(original.seeds)
        expect(decrypted.initialSeedId).to.eq(original.initialSeedId)
        expect(decrypted.latestSeedId).to.eq(original.latestSeedId)
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
        uvfMetadataFile: '{"protected":"eyJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2FwaS92YXVsdHMvVE9ETy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"7FtABJ5BpSM9Ft8wUPfLQc-12WF57kX0tWtRWiVwA_N_gJBa9iwhzw"},{"header":{"kid":"org.cryptomator.hub.recoverykey.1h24rxLxIlNRPQAn5NBP0fL3VKNTmqS6NEnt2clI5ko","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"oNu46YFrgrGSvl98HyDD3_iPkfZBnpYgEHmPL3qbO4AdwBsycpIqcHwKhT8Lt7B8","y":"P80VgJFml85v_F2-aPYdgDQX_DPGZr1s_p8gWF4Idkp13QfKdhi32C7Zoy5kzPWO","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"QaJn0TP7mGAc5ukOpZ0gNAuBtCW7hPCkj8Jp4bhMftQfJefHNyqE7Q"}],"iv":"Wgif0WP21-MAwvWs","ciphertext":"-n5CePmmN99I4KqlnR64Fuu5b2Md9s4CGxLMm7KQqu65H0ug7Fs5HHnrx_gkpFiv1Mn-jwrkoEtiixyQcYX6UcoyT2dY1MkLQB7QU9mdMpZU3n19Q2sAx1-gfTCd7IzVXef7SEfuscdQL1QTKJW454Dy8L3WwPiDpUgt9ED7mMFdJ6lJ3_EFYstN0VFAVf_jwtIILmQrjkM_LI0FFKfqkOCH2nuE9xG8ihPH9X9OStllPp00G9_onYu9mrg-smiNNK2Ib19CZJ2E6mAp7F_LGiz6p203fsprj4XY9J6t8zl5Vpc61NmFvzvY4j3_5FpD_BmpVr8tyyVT9zqWn4vsBAHORQ1V_b9v68O7CekCebpQvpmzEPZwZN1Ma_T6oI7Ydn1rtBnDruVrpWm01RL8XpHnFbko","tag":"vPLd65IEcexmhGbYPM0cYI53H4Pp1OfTaAq_QGrneLM"}',
        uvfKeySet: '{"keys": [{"kid":"org.cryptomator.hub.recoverykey.1h24rxLxIlNRPQAn5NBP0fL3VKNTmqS6NEnt2clI5ko","kty":"EC","crv":"P-384","x":"DczdNhPQnpgP8FV6qa372LDLJF2w2bMKXzea5cjxslaMjM6w7NGqrF498LYHU-Jt","y":"vH9bc-Ow_O1EQmd6N5pKmDoPyE6ziKsrlpuck5aLXwV4fSMV_8Ro1a872j5ClsPe"}]}'
      };
      const accessToken = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoiUXZRWUpUd3dSVEg2MWRFS3ZoNDI4ZG9nN3pRTFFxY3I0NUhwZTRqZFQ5Qno2bjcyVzQ4dTJ3WXk0UXlyZ0kxciIsInkiOiJZS1RtQ04zZXNKNDJVbUpzLU44NTFKamsyUFVPUU0zZXpCTkJvZGk4RnRNUDlUeUhoXzc0aHpxTC1EYTZkMXlwIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.rdysEEQN0FidglDtK5yyaEpQtv4CsYLOQd__y7REkb_3BLP9nD4Blw.dFb9JOdveiw3LmIs.rSMkz8VoB_LspnvxvmRzCWNVLShTWfbzHfqe5lwrWwumYCdeRPM.xsS2tDUr2khJrLxHex8gZhBgO_CMA_PxFlR-ku3JiT8';
      const uvf = await UniversalVaultFormat.decrypt(dto, accessToken, alice);

      expect(uvf).to.be.not.null;
      expect(uvf.metadata).to.be.not.null;
      expect(uvf.metadata.initialSeedId).to.eq(2754894775);
      expect(uvf.metadata.latestSeedId).to.eq(2754894775);
      expect(base64.stringify(uvf.metadata.kdfSalt)).to.eq('HE4OP+2vyfLLURicF1XmdIIsWv0Zs6MobLKROUIEhQY=');
      expect(base64.stringify(uvf.metadata.initialSeed)).to.eq('fP4V4oAjsUw5DqackAvLzA0oP1kAQZ0f5YFZQviXSuU=');
      expect(base64.stringify(uvf.metadata.latestSeed)).to.eq('fP4V4oAjsUw5DqackAvLzA0oP1kAQZ0f5YFZQviXSuU=');
      expect(uvf.memberKey).to.be.not.null;
      expect(uvf.recoveryKey).to.be.not.null;
      expect(uvf.recoveryKey.privateKey).to.be.undefined;
    });

    it('recover()', async () => {
      const vaultUvfFileContents = '{"protected":"eyJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2FwaS92YXVsdHMvVE9ETy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"XLoNIWvDKQqaDurrGt7VK9s2aggSMir7fS4ZdBUxdTxceCOHndo4kA"},{"header":{"kid":"org.cryptomator.hub.recoverykey.v2nb-mGX4POKMWCQKOogMWTlAn7DDqEOjjEGCsPEeco","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"j6Retxx-L-rURQ4WNc8LvoqjbdPtGS6n9pCJgcm1U-NAWuWEvwJ_qi2tlrv_4w4p","y":"wS-Emo-Q9qdtkHMJiDfVDAaxhF2-nSkDRn2Eg9CbG0pVwGEpaDybx_YYJwIaYooO","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"iNGgybMqmiXn_lbKLMMTpg38i1f00O6Zj65d5nzsLw3hyzuylGWpvA"}],"iv":"Pfy90C9SSq2gJr6B","ciphertext":"ogYR1pZN9k97zEgO9Fj3ePQramtaUdHWq95geXD7FH1oB6T7fEOvdU2AEGWOcbIbQihn-eOqG2_5oTol16O_nQ4HcDOJ9w4R9EdpByuWG-kVNh_fpWeQjIuH4kO-Rtbf05JRVG2jexWopbIA8uHuoiOXSNpSYPTzTKirp2hU7w3sE01zycsu06HiasUX-tKZH_hbyiUEdTlFFLcvKpRwnYOQf6QMw0uY1IbUTX1cJY9LO5SpD8bZFZOd6hg_Qnsdcq52I8KkZyxocgqdW7P5OSUrv5z8DCLMPdByEpaz9cCOzQQvtZwHxJy82O4vDAh89QA_AzfK8J7TI5zJRlTGQgrNhiaVBC85fN3tMSv8sLfJs7rC_5LiVW5ZeqbQ52sAZQw0lfwgGpMmxsdMzPoVOLD8OxvX","tag":"3Jiv6kI4Qoso60T0dRv9vIlca-P4UFyHqh-TEZvargM"}';
      const recoveryKey = 'cult hold all away buck do law relaxed other stimulus all bank fit indulge dad any ear grey cult golf all baby dig adv convict seldom dancer funding refusal shop final ceremony brush fire stick pound seldom shower tobacco wealth secret dispose session intend host elite fasten compound pants original drug hurricane bat noble lovely half accept sexy ad mouth current sound tie freedom musical ought shatter minimum density broadcast locate argument explosive manager colour numerous biscuit die absurd jury sole steel pub listener tempt creative plug everybody power constant recall lord camera dawn elbow chairman sit organic united morning dry feedback marine keep charm face cloud giant pull eternal withdraw probable canal coal heal large less flight gig hockey pit weed identify object boom melt';

      const uvf = await UniversalVaultFormat.recover(vaultUvfFileContents, recoveryKey);

      expect(uvf).to.be.not.null;
      expect(uvf.metadata).to.be.not.null;
      expect(uvf.metadata.initialSeedId).to.eq(4157009252);
      expect(uvf.metadata.latestSeedId).to.eq(4157009252);
      expect(base64.stringify(uvf.metadata.kdfSalt)).to.eq('pNxWJ5R5TO0mbkmL5pv7M3tAi6Etoh/SK73Q0KvfKMY=');
      expect(base64.stringify(uvf.metadata.initialSeed!)).to.eq('p6zznin4zSGt7gH6T95/kZj6HndpyUdY+1QVfxR2k20=');
      expect(base64.stringify(uvf.metadata.latestSeed!)).to.eq('p6zznin4zSGt7gH6T95/kZj6HndpyUdY+1QVfxR2k20=');
      expect(uvf.memberKey).to.be.not.null;
      expect(uvf.recoveryKey).to.be.not.null;
      expect(uvf.recoveryKey.privateKey).to.be.not.null;
      expect(uvf.recoveryKey.publicKey).to.be.not.null;
    });

    describe('instance methods', () => {
      let uvf: UniversalVaultFormat;

      before(async () => {
        const dto: VaultDto = {
          id: '123',
          name: 'test',
          archived: false,
          creationTime: new Date(),
          uvfMetadataFile: '{"protected":"eyJvcmlnaW4iOiJodHRwczovL2V4YW1wbGUuY29tL2FwaS92YXVsdHMvVE9ETy91dmYvdmF1bHQudXZmIiwiamt1Ijoiandrcy5qc29uIiwiZW5jIjoiQTI1NkdDTSJ9","recipients":[{"header":{"kid":"org.cryptomator.hub.memberkey","alg":"A256KW"},"encrypted_key":"7FtABJ5BpSM9Ft8wUPfLQc-12WF57kX0tWtRWiVwA_N_gJBa9iwhzw"},{"header":{"kid":"org.cryptomator.hub.recoverykey.1h24rxLxIlNRPQAn5NBP0fL3VKNTmqS6NEnt2clI5ko","alg":"ECDH-ES+A256KW","epk":{"key_ops":[],"ext":true,"kty":"EC","x":"oNu46YFrgrGSvl98HyDD3_iPkfZBnpYgEHmPL3qbO4AdwBsycpIqcHwKhT8Lt7B8","y":"P80VgJFml85v_F2-aPYdgDQX_DPGZr1s_p8gWF4Idkp13QfKdhi32C7Zoy5kzPWO","crv":"P-384"},"apu":"","apv":""},"encrypted_key":"QaJn0TP7mGAc5ukOpZ0gNAuBtCW7hPCkj8Jp4bhMftQfJefHNyqE7Q"}],"iv":"Wgif0WP21-MAwvWs","ciphertext":"-n5CePmmN99I4KqlnR64Fuu5b2Md9s4CGxLMm7KQqu65H0ug7Fs5HHnrx_gkpFiv1Mn-jwrkoEtiixyQcYX6UcoyT2dY1MkLQB7QU9mdMpZU3n19Q2sAx1-gfTCd7IzVXef7SEfuscdQL1QTKJW454Dy8L3WwPiDpUgt9ED7mMFdJ6lJ3_EFYstN0VFAVf_jwtIILmQrjkM_LI0FFKfqkOCH2nuE9xG8ihPH9X9OStllPp00G9_onYu9mrg-smiNNK2Ib19CZJ2E6mAp7F_LGiz6p203fsprj4XY9J6t8zl5Vpc61NmFvzvY4j3_5FpD_BmpVr8tyyVT9zqWn4vsBAHORQ1V_b9v68O7CekCebpQvpmzEPZwZN1Ma_T6oI7Ydn1rtBnDruVrpWm01RL8XpHnFbko","tag":"vPLd65IEcexmhGbYPM0cYI53H4Pp1OfTaAq_QGrneLM"}',
          uvfKeySet: '{"keys": [{"kid":"org.cryptomator.hub.recoverykey.1h24rxLxIlNRPQAn5NBP0fL3VKNTmqS6NEnt2clI5ko","kty":"EC","crv":"P-384","x":"DczdNhPQnpgP8FV6qa372LDLJF2w2bMKXzea5cjxslaMjM6w7NGqrF498LYHU-Jt","y":"vH9bc-Ow_O1EQmd6N5pKmDoPyE6ziKsrlpuck5aLXwV4fSMV_8Ro1a872j5ClsPe"}]}'
        };
        const accessToken = 'eyJlbmMiOiJBMjU2R0NNIiwia2lkIjoib3JnLmNyeXB0b21hdG9yLmh1Yi51c2Vya2V5IiwiYWxnIjoiRUNESC1FUytBMjU2S1ciLCJlcGsiOnsia2V5X29wcyI6W10sImV4dCI6dHJ1ZSwia3R5IjoiRUMiLCJ4IjoiUXZRWUpUd3dSVEg2MWRFS3ZoNDI4ZG9nN3pRTFFxY3I0NUhwZTRqZFQ5Qno2bjcyVzQ4dTJ3WXk0UXlyZ0kxciIsInkiOiJZS1RtQ04zZXNKNDJVbUpzLU44NTFKamsyUFVPUU0zZXpCTkJvZGk4RnRNUDlUeUhoXzc0aHpxTC1EYTZkMXlwIiwiY3J2IjoiUC0zODQifSwiYXB1IjoiIiwiYXB2IjoiIn0.rdysEEQN0FidglDtK5yyaEpQtv4CsYLOQd__y7REkb_3BLP9nD4Blw.dFb9JOdveiw3LmIs.rSMkz8VoB_LspnvxvmRzCWNVLShTWfbzHfqe5lwrWwumYCdeRPM.xsS2tDUr2khJrLxHex8gZhBgO_CMA_PxFlR-ku3JiT8';
        uvf = await UniversalVaultFormat.decrypt(dto, accessToken, alice);
      });

      it('encryptForUser() creates an access token', async () => {
        const token = await uvf.encryptForUser(alice.keyPair.publicKey);
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

      it('computeRootDirIdHash() creates a hash', async () => {
        const hash = await uvf.computeRootDirIdHash();
        expect(hash).to.eq('6DYU3E5BTPAZ4DWEQPQK3AIHX2DXSPHG');
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

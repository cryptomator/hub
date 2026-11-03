import { base64 } from '@scure/base';
import { beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { ActivatedUser } from '../../src/common/backend';
import { EmergencyAccess, RecoveryProcess, RecoveryProcessContext } from '../../src/common/emergencyaccess';
import { JWE, Recipient } from '../../src/common/jwe';
import { UTF8 } from '../../src/common/util';

describe('Emergency Access', () => {
  const originalSecret = UTF8.encode('Hello World!');
  const vaultId = 'vault1';
  const processId = 'process1';
  const context: RecoveryProcessContext = { vaultId: vaultId, processId: processId };

  let alice: CryptoKeyPair, bob: CryptoKeyPair, carol: CryptoKeyPair, dave: CryptoKeyPair;
  let aliceDto: ActivatedUser, bobDto: ActivatedUser, carolDto: ActivatedUser, daveDto: ActivatedUser;

  beforeAll(async () => {
    // prepare some test key pairs:
    alice = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, false, ['deriveBits']);
    bob = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, false, ['deriveBits']);
    carol = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, false, ['deriveBits']);
    dave = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, false, ['deriveBits']);
    aliceDto = await createUserDto('alice', alice.publicKey);
    bobDto = await createUserDto('bob', bob.publicKey);
    carolDto = await createUserDto('carol', carol.publicKey);
    daveDto = await createUserDto('dave', dave.publicKey);
  });

  it('Split Secret, requiring any 3 of [alice, bob, carol, dave]', async () => {
    const shares = await EmergencyAccess.split(vaultId, originalSecret, 3, aliceDto, bobDto, carolDto, daveDto);

    expect(shares).to.have.property('alice');
    expect(shares).to.have.property('bob');
    expect(shares).to.have.property('carol');
    expect(shares).to.have.property('dave');
  });

  it('Start Recovery Process', async () => {
    const councilMembers = [aliceDto, bobDto, carolDto, daveDto];
    const recoveryProcess = await EmergencyAccess.startRecovery(processId, councilMembers);

    expect(recoveryProcess).to.have.property('recoveryPublicKey');
    expect(recoveryProcess).to.have.property('recoveryPrivateKeys');
    expect(recoveryProcess.recoveryPrivateKeys).to.have.property('alice');
    expect(recoveryProcess.recoveryPrivateKeys).to.have.property('bob');
    expect(recoveryProcess.recoveryPrivateKeys).to.have.property('carol');
    expect(recoveryProcess.recoveryPrivateKeys).to.have.property('dave');
  });

  describe('Finish Recovery', () => {
    let keyShares: Record<string, string>;
    let recoveryProcess: RecoveryProcess;

    beforeEach(async () => {
      // split some secret:
      const secret = 'Hello World!';
      const secretBytes = UTF8.encode(secret);
      keyShares = await EmergencyAccess.split(vaultId, secretBytes, 3, aliceDto, bobDto, carolDto, daveDto);

      // start recovery:
      const councilMembers = [aliceDto, bobDto, carolDto, daveDto];
      recoveryProcess = await EmergencyAccess.startRecovery(processId, councilMembers);
    });

    it('recover fails as Alice + Bob', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(keyShares['alice'], context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(keyShares['bob'], context, 'bob', bob.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob], recoveryProcess.recoveryPrivateKeys.alice, context, 'alice', alice.privateKey);

      expect(recovered).not.to.deep.eq(originalSecret);
    });

    it('recover as Alice + Bob + Carol', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(keyShares['alice'], context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(keyShares['bob'], context, 'bob', bob.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(keyShares['carol'], context, 'carol', carol.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob, recoveredCarol], recoveryProcess.recoveryPrivateKeys.alice, context, 'alice', alice.privateKey);

      expect(recovered).to.deep.eq(originalSecret);
    });

    it('recover as Alice + Carol + Dave', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(keyShares['alice'], context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(keyShares['carol'], context, 'carol', carol.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredDave = await EmergencyAccess.recoverShare(keyShares['dave'], context, 'dave', dave.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredCarol, recoveredDave], recoveryProcess.recoveryPrivateKeys.carol, context, 'carol', carol.privateKey);

      expect(recovered).to.deep.eq(originalSecret);
    });

    it('recover as Alice + Bob + Carol + Dave', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(keyShares['alice'], context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(keyShares['bob'], context, 'bob', bob.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(keyShares['carol'], context, 'carol', carol.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredDave = await EmergencyAccess.recoverShare(keyShares['dave'], context, 'dave', dave.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob, recoveredCarol, recoveredDave], recoveryProcess.recoveryPrivateKeys.dave, context, 'dave', dave.privateKey);

      expect(recovered).to.deep.eq(originalSecret);
    });
  });

  // regression tests for GHSL finding 21613, issue 1 (cross-vault decryption oracle):
  describe('Context Binding', () => {
    let recoveryProcess: RecoveryProcess;

    beforeEach(async () => {
      recoveryProcess = await EmergencyAccess.startRecovery(processId, [aliceDto, bobDto, carolDto]);
    });

    it('recoverShare rejects a key share belonging to a different vault', async () => {
      const foreignShares = await EmergencyAccess.split('otherVault', originalSecret, 2, aliceDto, bobDto, carolDto);

      await expect(EmergencyAccess.recoverShare(foreignShares['alice'], context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey))
        .rejects.toThrow(/not bound to expected context/);
    });

    it('recoverShare rejects a key share belonging to a different council member', async () => {
      const shares = await EmergencyAccess.split(vaultId, originalSecret, 2, aliceDto, bobDto, carolDto);

      await expect(EmergencyAccess.recoverShare(shares['bob'], context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey))
        .rejects.toThrow(/not bound to expected context/);
    });

    it('recoverShare rejects a legacy key share lacking context binding', async () => {
      const payload = { keyShare: base64.encode(originalSecret) };
      const legacyShare = await JWE.build(payload).withRecipients(Recipient.ecdhEs('', alice.publicKey)).toCompact();

      await expect(EmergencyAccess.recoverShare(legacyShare, context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey))
        .rejects.toThrow(/not bound to expected context/);
    });

    it('recoverShare rejects a foreign JWE whose ctx header was transplanted', async () => {
      // an attacker cannot forge the ctx header, as it is part of the GCM-authenticated protected header:
      const foreignShares = await EmergencyAccess.split('otherVault', originalSecret, 2, aliceDto, bobDto, carolDto);
      const legitShares = await EmergencyAccess.split(vaultId, originalSecret, 2, aliceDto, bobDto, carolDto);
      const [, ...foreignParts] = foreignShares['alice'].split('.');
      const [legitHeader] = legitShares['alice'].split('.');
      const tamperedShare = [legitHeader, ...foreignParts].join('.');

      await expect(EmergencyAccess.recoverShare(tamperedShare, context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey))
        .rejects.toThrow();
    });

    it('recoverShare refuses to forward payloads other than a key share', async () => {
      // even a correctly bound JWE must contain exactly a key share, nothing else (e.g. no private keys):
      const payload = { privateKey: 'top secret' };
      const jwe = await JWE.build(payload, { ctx: `hub:emergency-key-share:${vaultId}:alice`, crit: ['ctx'] }).withRecipients(Recipient.ecdhEs('', alice.publicKey)).toCompact();

      await expect(EmergencyAccess.recoverShare(jwe, context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey))
        .rejects.toThrow(/Unexpected key share payload/);
    });

    it('combineRecoveredShares rejects a process private key belonging to a different process', async () => {
      const shares = await EmergencyAccess.split(vaultId, originalSecret, 2, aliceDto, bobDto, carolDto);
      const recoveredAlice = await EmergencyAccess.recoverShare(shares['alice'], context, 'alice', alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(shares['bob'], context, 'bob', bob.privateKey, recoveryProcess.recoveryPublicKey);
      const otherProcess = await EmergencyAccess.startRecovery('otherProcess', [aliceDto, bobDto, carolDto]);

      await expect(EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob], otherProcess.recoveryPrivateKeys.alice, context, 'alice', alice.privateKey))
        .rejects.toThrow(/not bound to expected context/);
    });

    it('combineRecoveredShares rejects recovered shares belonging to a different process', async () => {
      const shares = await EmergencyAccess.split(vaultId, originalSecret, 2, aliceDto, bobDto, carolDto);
      const otherContext: RecoveryProcessContext = { vaultId: vaultId, processId: 'otherProcess' };
      const otherProcess = await EmergencyAccess.startRecovery('otherProcess', [aliceDto, bobDto, carolDto]);
      const recoveredAlice = await EmergencyAccess.recoverShare(shares['alice'], otherContext, 'alice', alice.privateKey, otherProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(shares['bob'], otherContext, 'bob', bob.privateKey, otherProcess.recoveryPublicKey);

      await expect(EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob], recoveryProcess.recoveryPrivateKeys.alice, context, 'alice', alice.privateKey))
        .rejects.toThrow(/not bound to expected context/);
    });
  });
});

/* ---------- MOCKS ---------- */

async function createUserDto(id: string, publicKey: CryptoKey): Promise<ActivatedUser> {
  const keyBytes = new Uint8Array(await crypto.subtle.exportKey('spki', publicKey));
  return {
    type: 'USER',
    id: id,
    name: `User ${id}`,
    email: '',
    enabled: true,
    devices: [],
    accessibleVaults: [],
    ecdhPublicKey: base64.encode(keyBytes),
    ecdsaPublicKey: ''
  };
}

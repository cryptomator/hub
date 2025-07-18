import { expect } from 'chai';
import { before, describe } from 'mocha';
import { webcrypto } from 'node:crypto';
import { EmergencyAccess, RecoveryProcess } from '../../src/common/emergencyaccess';
import { UTF8 } from '../../src/common/util';
import { base64 } from 'rfc4648';
import { ActivatedUser } from '../../src/common/backend';

describe('Emergency Access', () => {
  const originalSecret = UTF8.encode('Hello World!');

  let alice: CryptoKeyPair, bob: CryptoKeyPair, carol: CryptoKeyPair, dave: CryptoKeyPair;
  let aliceDto: ActivatedUser, bobDto: ActivatedUser, carolDto: ActivatedUser, daveDto: ActivatedUser;

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: webcrypto });
    // @ts-expect-error: incomplete 'window' type
    global.window = { crypto: global.crypto };

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
    const shares = await EmergencyAccess.split(originalSecret, 3, aliceDto, bobDto, carolDto, daveDto);

    expect(shares).to.have.property('alice');
    expect(shares).to.have.property('bob');
    expect(shares).to.have.property('carol');
    expect(shares).to.have.property('dave');
  });

  it('Start Recovery Process', async () => {
    const councilMembers = [aliceDto, bobDto, carolDto, daveDto];
    const recoveryProcess = await EmergencyAccess.startRecovery(councilMembers);

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
      keyShares = await EmergencyAccess.split(secretBytes, 3, aliceDto, bobDto, carolDto, daveDto);

      // start recovery:
      const councilMembers = [aliceDto, bobDto, carolDto, daveDto];
      recoveryProcess = await EmergencyAccess.startRecovery(councilMembers);
    });

    it('recover fails as Alice + Bob', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(keyShares['alice'], alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(keyShares['bob'], bob.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob], recoveryProcess.recoveryPrivateKeys.alice, alice.privateKey);

      expect(recovered).not.to.deep.eq(originalSecret);
    });

    it('recover as Alice + Bob + Carol', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(keyShares['alice'], alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(keyShares['bob'], bob.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(keyShares['carol'], carol.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob, recoveredCarol], recoveryProcess.recoveryPrivateKeys.alice, alice.privateKey);

      expect(recovered).to.deep.eq(originalSecret);
    });

    it('recover as Alice + Carol + Dave', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(keyShares['alice'], alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(keyShares['carol'], carol.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredDave = await EmergencyAccess.recoverShare(keyShares['dave'], dave.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredCarol, recoveredDave], recoveryProcess.recoveryPrivateKeys.carol, carol.privateKey);

      expect(recovered).to.deep.eq(originalSecret);
    });

    it('recover as Alice + Bob + Carol + Dave', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(keyShares['alice'], alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(keyShares['bob'], bob.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(keyShares['carol'], carol.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredDave = await EmergencyAccess.recoverShare(keyShares['dave'], dave.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob, recoveredCarol, recoveredDave], recoveryProcess.recoveryPrivateKeys.dave, dave.privateKey);

      expect(recovered).to.deep.eq(originalSecret);
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
    devices: [],
    accessibleVaults: [],
    ecdhPublicKey: base64.stringify(keyBytes),
    ecdsaPublicKey: ''
  };
}
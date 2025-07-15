import { expect } from 'chai';
import { before, describe } from 'mocha';
import { webcrypto } from 'node:crypto';
import { EmergencyAccess, RecoveryProcess } from '../../src/common/emergencyaccess';

const toUint8Array = (data: string) => new TextEncoder().encode(data);
const fromUint8Array = (data: AllowSharedBufferSource, options?: TextDecodeOptions) => new TextDecoder().decode(data, options);

describe('emergencyaccess', () => {
  const originalSecret = 'Hello World!';

  let alice: CryptoKeyPair, bob: CryptoKeyPair, carol: CryptoKeyPair, dave: CryptoKeyPair;

  before(async () => {
    // since this test runs on Node, we need to replace window.crypto:
    Object.defineProperty(global, 'crypto', { value: webcrypto });
    // @ts-expect-error: incomplete 'window' type
    global.window = { crypto: global.crypto };

    // prepare some test key pairs:
    alice = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, true, ['deriveKey', 'deriveBits']);
    bob = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, true, ['deriveKey', 'deriveBits']);
    carol = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, true, ['deriveKey', 'deriveBits']);
    dave = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, true, ['deriveKey', 'deriveBits']);
  });

  it('split()', async () => {
    const secretBytes = toUint8Array(originalSecret);
    const shares = await EmergencyAccess.split(secretBytes, 3, alice.publicKey, bob.publicKey, carol.publicKey, dave.publicKey);

    expect(shares).to.have.length(4);
  });

  it('startRecovery()', async () => {
    const councilMembers = {
      alice: alice.publicKey,
      bob: bob.publicKey,
      carol: carol.publicKey,
      dave: dave.publicKey
    };
    const recoveryProcess = await EmergencyAccess.startRecovery(councilMembers);

    expect(recoveryProcess).to.have.property('recoveryPublicKey');
    expect(recoveryProcess).to.have.property('recoveryPrivateKeys');
    expect(recoveryProcess.recoveryPrivateKeys).to.have.property('alice');
    expect(recoveryProcess.recoveryPrivateKeys).to.have.property('bob');
    expect(recoveryProcess.recoveryPrivateKeys).to.have.property('carol');
    expect(recoveryProcess.recoveryPrivateKeys).to.have.property('dave');
  });

  describe('Recovery', () => {
    let aliceShare: string, bobShare: string, carolShare: string, daveShare: string;
    let recoveryProcess: RecoveryProcess;

    beforeEach(async () => {
      // split some secret:
      const secret = 'Hello World!';
      const secretBytes = new TextEncoder().encode(secret);
      [aliceShare, bobShare, carolShare, daveShare] = await EmergencyAccess.split(secretBytes, 3, alice.publicKey, bob.publicKey, carol.publicKey, dave.publicKey);

      // start recovery:
      const councilMembers = {
        alice: alice.publicKey,
        bob: bob.publicKey,
        carol: carol.publicKey,
        dave: dave.publicKey
      };
      recoveryProcess = await EmergencyAccess.startRecovery(councilMembers);
    });

    it('recover as Alice + Bob + Carol', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(aliceShare, alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(bobShare, bob.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(carolShare, carol.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob, recoveredCarol], recoveryProcess.recoveryPrivateKeys.alice, alice.privateKey);

      expect(fromUint8Array(recovered)).to.eq(originalSecret);
    });

    it('recover as Alice + Carol + Dave', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(aliceShare, alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(carolShare, carol.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredDave = await EmergencyAccess.recoverShare(daveShare, dave.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredCarol, recoveredDave], recoveryProcess.recoveryPrivateKeys.carol, carol.privateKey);

      expect(fromUint8Array(recovered)).to.eq(originalSecret);
    });

    it('recover as Alice + Bob + Carol + Dave', async () => {
      const recoveredAlice = await EmergencyAccess.recoverShare(aliceShare, alice.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredBob = await EmergencyAccess.recoverShare(bobShare, bob.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredCarol = await EmergencyAccess.recoverShare(carolShare, carol.privateKey, recoveryProcess.recoveryPublicKey);
      const recoveredDave = await EmergencyAccess.recoverShare(daveShare, dave.privateKey, recoveryProcess.recoveryPublicKey);

      const recovered = await EmergencyAccess.combineRecoveredShares([recoveredAlice, recoveredBob, recoveredCarol, recoveredDave], recoveryProcess.recoveryPrivateKeys.dave, dave.privateKey);

      expect(fromUint8Array(recovered)).to.eq(originalSecret);
    });
  });
});
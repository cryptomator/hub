import { base64 } from '@scure/base';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import { asPublicKey, UserKeys } from '../../src/common/crypto';
import { TrustDto } from '../../src/common/backend';
import { JWT } from '../../src/common/jwt';
import wot, { SignedKeys } from '../../src/common/wot';

const mocked = vi.hoisted(() => ({
  meId: 'alice',
  aliceKeys: undefined as unknown as UserKeys
}));

vi.mock('../../src/common/auth', () => ({ default: Promise.resolve({}) }));
vi.mock('../../src/common/config', () => ({ default: {}, backendBaseURL: '/api/' }));
vi.mock('../../src/common/userdata', () => ({
  default: {
    get me() {
      return Promise.resolve({ id: mocked.meId });
    },
    decryptUserKeysWithBrowser: () => Promise.resolve(mocked.aliceKeys)
  }
}));

describe('Web of Trust', () => {
  let alice: UserKeys, bob: UserKeys, carol: UserKeys;
  let bobKeys: SignedKeys, carolKeys: SignedKeys;

  beforeAll(async () => {
    alice = await UserKeys.create(); // alice is "me" during these tests
    bob = await UserKeys.create();
    carol = await UserKeys.create();
    // UserKeys.create() generates the ECDSA public key without the 'verify' usage; re-import it the same way production code does:
    const aliceEcdsaVerifyKey = await asPublicKey(base64.decode(await alice.encodedEcdsaPublicKey()) as Uint8Array<ArrayBuffer>, UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES);
    mocked.aliceKeys = { ecdhKeyPair: alice.ecdhKeyPair, ecdsaKeyPair: { publicKey: aliceEcdsaVerifyKey, privateKey: alice.ecdsaKeyPair.privateKey } } as UserKeys;
    bobKeys = await publishedKeys(bob);
    carolKeys = await publishedKeys(carol);
  });

  async function publishedKeys(user: UserKeys): Promise<SignedKeys> {
    return {
      ecdhPublicKey: await user.encodedEcdhPublicKey(),
      ecdsaPublicKey: await user.encodedEcdsaPublicKey()
    };
  }

  describe('verify', () => {
    it('accepts a legitimate direct chain alice→bob', async () => {
      const chain = [await wot.createSignature(alice, 'alice', 'bob', bobKeys)];
      await expect(wot.verify(chain, 'bob', bobKeys)).resolves.toBeUndefined();
    });

    it('accepts a transitive chain alice→bob→carol with coherent identity claims', async () => {
      const chain = [
        await wot.createSignature(alice, 'alice', 'bob', bobKeys),
        await wot.createSignature(bob, 'bob', 'carol', carolKeys)
      ];
      await expect(wot.verify(chain, 'carol', carolKeys)).resolves.toBeUndefined();
    });

    it('rejects a chain whose signed keys do not match the alleged keys', async () => {
      const chain = [await wot.createSignature(alice, 'alice', 'bob', bobKeys)];
      await expect(wot.verify(chain, 'bob', carolKeys)).rejects.toThrow(/does not match signed public key/);
    });

    it('rejects a replayed chain: signature for bob presented as trust path for carol with substituted published keys', async () => {
      // a malicious server replaces carol's published keys with bob's and serves alice's genuine alice→bob signature as the chain for carol:
      const chain = [await wot.createSignature(alice, 'alice', 'bob', bobKeys)];
      await expect(wot.verify(chain, 'carol', bobKeys)).rejects.toThrow(/different subject/);
    });

    it('rejects a chain with discontinuous identity claims', async () => {
      // cryptographically linked (bob's key signs the second link), but bob's link claims to be issued by someone else:
      const chain = [
        await wot.createSignature(alice, 'alice', 'bob', bobKeys),
        await wot.createSignature(bob, 'mallory', 'carol', carolKeys)
      ];
      await expect(wot.verify(chain, 'carol', carolKeys)).rejects.toThrow(/unexpected issuer/);
    });

    it('rejects a chain whose first link was not issued by me', async () => {
      const chain = [await wot.createSignature(alice, 'not-alice', 'bob', bobKeys)];
      await expect(wot.verify(chain, 'bob', bobKeys)).rejects.toThrow(/unexpected issuer/);
    });

    it('rejects an empty chain', async () => {
      await expect(wot.verify([], 'bob', bobKeys)).rejects.toThrow(/Empty signature chain/);
    });

    it('rejects an intermediate link without a subject claim', async () => {
      const link1 = await JWT.build({ alg: 'ES384', typ: 'JWT', b64: true, iss: 'alice', iat: Math.floor(Date.now() / 1000) }, bobKeys, alice.ecdsaKeyPair.privateKey);
      const chain = [link1, await wot.createSignature(bob, 'bob', 'carol', carolKeys)];
      await expect(wot.verify(chain, 'carol', carolKeys)).rejects.toThrow(/lacks subject/);
    });
  });

  describe('computeTrustLevel', () => {
    let trusts: TrustDto[];

    beforeAll(async () => {
      trusts = [
        { trustedUserId: 'bob', signatureChain: [await wot.createSignature(alice, 'alice', 'bob', bobKeys)] },
        { trustedUserId: 'carol', signatureChain: [await wot.createSignature(alice, 'alice', 'bob', bobKeys), await wot.createSignature(bob, 'bob', 'carol', carolKeys)] }
      ];
    });

    it('returns 0 for myself, even without a trust entry', async () => {
      await expect(wot.computeTrustLevel({ id: 'alice' }, [])).resolves.toBe(0);
    });

    it('returns the chain length for a directly trusted user', async () => {
      await expect(wot.computeTrustLevel({ id: 'bob', ...bobKeys }, trusts)).resolves.toBe(1);
    });

    it('returns the chain length for a transitively trusted user', async () => {
      await expect(wot.computeTrustLevel({ id: 'carol', ...carolKeys }, trusts)).resolves.toBe(2);
    });

    it('returns -1 for a user without a trust entry', async () => {
      await expect(wot.computeTrustLevel({ id: 'dave', ...carolKeys }, trusts)).resolves.toBe(-1);
    });

    it('returns -1 for a user without published keys', async () => {
      await expect(wot.computeTrustLevel({ id: 'bob' }, trusts)).resolves.toBe(-1);
    });

    it('returns -1 if the published keys do not match the signed keys (key substitution)', async () => {
      await expect(wot.computeTrustLevel({ id: 'bob', ...carolKeys }, trusts)).resolves.toBe(-1);
    });

    it('returns -1 if the chain was issued for a different subject (replayed chain)', async () => {
      // bob's genuine chain served as the trust entry for carol, with carol's published keys substituted by bob's:
      const replayed = [{ trustedUserId: 'carol', signatureChain: trusts[0].signatureChain }];
      await expect(wot.computeTrustLevel({ id: 'carol', ...bobKeys }, replayed)).resolves.toBe(-1);
    });

    it('computes trust levels for multiple users in one batch', async () => {
      const levels = await wot.computeTrustLevels([
        { id: 'alice' },
        { id: 'bob', ...bobKeys },
        { id: 'carol', ...carolKeys },
        { id: 'dave' }
      ], trusts);
      expect(levels).toEqual(new Map([['alice', 0], ['bob', 1], ['carol', 2], ['dave', -1]]));
    });

    it('returns an empty map for an empty user list', async () => {
      await expect(wot.computeTrustLevels([], trusts)).resolves.toEqual(new Map());
    });
  });
});

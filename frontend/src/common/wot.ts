import { base64 } from '@scure/base';
import backend, { TrustDto, UserDto } from './backend';
import { UserKeys, asPublicKey, getJwkThumbprint } from './crypto';
import { JWT, JWTHeader } from './jwt';
import userdata from './userdata';

export type SignedKeys = {
  ecdhPublicKey: string;
  ecdsaPublicKey: string;
};

function deeplyEqual(a: SignedKeys, b: SignedKeys) {
  return a.ecdhPublicKey === b.ecdhPublicKey
    && a.ecdsaPublicKey === b.ecdsaPublicKey;
}

/**
 * Signs the public key of a user with my private key and sends the signature to the backend.
 * @param user The user whose keys to sign
 * @returns The new trust object created during the signing process
 */
async function sign(user: UserDto): Promise<TrustDto> {
  if (!user.ecdhPublicKey || !user.ecdsaPublicKey) {
    throw new Error('No public key to sign');
  }
  const toSign: SignedKeys = {
    ecdhPublicKey: user.ecdhPublicKey,
    ecdsaPublicKey: user.ecdsaPublicKey
  };
  const me = await userdata.me;
  const userKeys = await userdata.decryptUserKeysWithBrowser();
  const signature = await createSignature(userKeys, me.id, user.id, toSign);
  await backend.trust.trustUser(user.id, signature);
  const trust = await backend.trust.get(user.id);
  return trust!;
}

// visible for testing
async function createSignature(signer: UserKeys, iss: string, sub: string, signedKeys: SignedKeys): Promise<string> {
  return JWT.build({
    alg: 'ES384',
    typ: 'JWT',
    b64: true,
    iss: iss,
    sub: sub,
    iat: Math.floor(Date.now() / 1000)
  }, signedKeys, signer.ecdsaKeyPair.privateKey);
}

/**
 * Verifies a chain of signatures, where each signature signs the public key of the next signature.
 * @param signatureChain The signature chain, where the first element is signed by me
 * @param trustedUserId The id of the user whose keys the last signature in the chain is expected to attest
 * @param allegedSignedKey The public key that should be signed by the last signature in the chain
 */
async function verify(signatureChain: string[], trustedUserId: string, allegedSignedKey: SignedKeys) {
  const me = await userdata.me;
  const signerPublicKey = await userdata.decryptUserKeysWithBrowser().then(keys => keys.ecdsaKeyPair.publicKey);
  await verifyRescursive(signatureChain, signerPublicKey, me.id, trustedUserId, allegedSignedKey);
}

/**
 * Recursively verifies a chain of signatures, where each signature signs the public key of the next signature.
 * @param signatureChain The chain of signatures to verify
 * @param signerPublicKey A trusted public key to verify the first signature in the chain
 * @param expectedIssuer The user id the first signature in the chain must have been issued by (`iss` claim)
 * @param trustedUserId The user id the last signature in the chain must refer to (`sub` claim)
 * @param allegedSignedKey The public key that should be signed by the last signature in the chain
 * @throws Error if the signature chain is invalid
 */
async function verifyRescursive(signatureChain: string[], signerPublicKey: CryptoKey, expectedIssuer: string, trustedUserId: string, allegedSignedKey: SignedKeys) {
  if (signatureChain.length === 0) {
    throw new Error('Empty signature chain');
  }
  // get first element of signature chain:
  const [signature, ...remainingChain] = signatureChain;
  const [header, signedKeys] = await JWT.parse(signature, signerPublicKey) as [JWTHeader, SignedKeys];
  // in addition to the cryptographic linkage, the identity claims asserted by the signer must be coherent,
  // otherwise a signature issued for one user could be replayed as a trust path for a different user:
  if (header.iss !== expectedIssuer) {
    throw new Error('Signature issued by unexpected issuer');
  }
  if (remainingChain.length === 0) {
    // last element in chain should refer to the trusted user and match their signed public key
    if (header.sub !== trustedUserId) {
      throw new Error('Signature issued for a different subject');
    }
    if (!deeplyEqual(signedKeys, allegedSignedKey)) {
      throw new Error('Alleged public key does not match signed public key');
    }
  } else {
    // otherwise, the payload is an intermediate public key used to sign the next element
    if (typeof header.sub !== 'string') {
      throw new Error('Signature lacks subject');
    }
    const nextTrustedPublicKey = await asPublicKey(base64.decode(signedKeys.ecdsaPublicKey) as Uint8Array<ArrayBuffer>, UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES);
    await verifyRescursive(remainingChain, nextTrustedPublicKey, header.sub, trustedUserId, allegedSignedKey);
  }
}

export type TrustCheckableUser = Pick<UserDto, 'id' | 'ecdhPublicKey' | 'ecdsaPublicKey'>;

/**
 * Computes the trust level of a user based on a preloaded list of trusts.
 * @param user The user whose trust level to compute
 * @param trusts The current user's trust list (as returned by `backend.trust.listTrusted()`)
 * @returns `0` for myself, the length of the verified signature chain for a trusted user, or `-1` if the user is untrusted or verification fails
 */
async function computeTrustLevel(user: TrustCheckableUser, trusts: TrustDto[]): Promise<number> {
  const me = await userdata.me;
  if (me.id === user.id) {
    return 0; // Self
  }
  const trust = trusts.find(t => t.trustedUserId === user.id);
  if (trust && user.ecdhPublicKey && user.ecdsaPublicKey) {
    try {
      await verify(trust.signatureChain, user.id, { ecdhPublicKey: user.ecdhPublicKey, ecdsaPublicKey: user.ecdsaPublicKey });
      return trust.signatureChain.length;
    } catch (error) {
      console.error('WoT signature verification failed.', error);
      return -1; // Unverified
    }
  }
  return -1; // Unverified
}

/**
 * Batch variant of {@link computeTrustLevel}.
 * @param users The users whose trust levels to compute
 * @param trusts The current user's trust list (as returned by `backend.trust.listTrusted()`)
 * @returns A map of user id to trust level
 */
async function computeTrustLevels(users: TrustCheckableUser[], trusts: TrustDto[]): Promise<Map<string, number>> {
  const entries = await Promise.all(users.map(async user => [user.id, await computeTrustLevel(user, trusts)] as const));
  return new Map(entries);
}

/**
 * Creates a unique fingerprint for a user by hashing the concatenated thumbprints of their public keys.
 * @param user The user whose fingerprint to compute
 * @returns Hexadecimal representation of the fingerprint
 */
async function computeFingerprint(user: { ecdhPublicKey?: string; ecdsaPublicKey?: string }) {
  if (!user.ecdhPublicKey || !user.ecdsaPublicKey) {
    throw new Error('User has no public keys');
  }
  const ecdhPublicKey = await asPublicKey(base64.decode(user.ecdhPublicKey) as Uint8Array<ArrayBuffer>, UserKeys.ECDH_KEY_DESIGNATION);
  const ecdsaPublicKey = await asPublicKey(base64.decode(user.ecdsaPublicKey) as Uint8Array<ArrayBuffer>, UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES);
  const concatenatedThumbprints = new Uint8Array([
    ...await getJwkThumbprint(ecdhPublicKey),
    ...await getJwkThumbprint(ecdsaPublicKey)
  ]);
  const digest = await crypto.subtle.digest('SHA-256', concatenatedThumbprints);
  const digestBytes = Array.from(new Uint8Array(digest));
  const digestHexStr = digestBytes
    .map((b) => b.toString(16).padStart(2, '0').toUpperCase())
    .join('');
  return digestHexStr;
}

export default { sign, verify, computeTrustLevel, computeTrustLevels, computeFingerprint, createSignature };

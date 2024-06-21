import { base64 } from 'rfc4648';
import backend, { TrustDto, UserDto } from './backend';
import { UserKeys, asPublicKey, getJwkThumbprint } from './crypto';
import { JWT, JWTHeader } from './jwt';
import userdata from './userdata';

export type SignedKeys = {
  ecdhPublicKey: string;
  ecdsaPublicKey: string;
}

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
  const signature = await JWT.build({
    alg: 'ES384',
    typ: 'JWT',
    b64: true,
    iss: me.id,
    sub: user.id,
    iat: Math.floor(Date.now() / 1000)
  }, toSign, userKeys.ecdsaKeyPair.privateKey);
  await backend.trust.trustUser(user.id, signature);
  const trust = await backend.trust.get(user.id);
  return trust!;
}

/**
 * Verifies a chain of signatures, where each signature signs the public key of the next signature.
 * @param signatureChain The signature chain, where the first element is signed by me
 * @param allegedSignedKey The public key that should be signed by the last signature in the chain
 */
async function verify(signatureChain: string[], allegedSignedKey: SignedKeys) {
  let signerPublicKey = await userdata.decryptUserKeysWithBrowser().then(keys => keys.ecdsaKeyPair.publicKey);
  await verifyRescursive(signatureChain, signerPublicKey, allegedSignedKey);
}

/**
 * Recursively verifies a chain of signatures, where each signature signs the public key of the next signature.
 * @param signatureChain The chain of signatures to verify
 * @param signerPublicKey A trusted public key to verify the first signature in the chain
 * @param allegedSignedKey The public key that should be signed by the last signature in the chain
 * @throws Error if the signature chain is invalid
 */
async function verifyRescursive(signatureChain: string[], signerPublicKey: CryptoKey, allegedSignedKey: SignedKeys) {
  // get first element of signature chain:
  const [signature, ...remainingChain] = signatureChain;
  const [_, signedKeys] = await JWT.parse(signature, signerPublicKey) as [JWTHeader, SignedKeys];
  if (remainingChain.length === 0) {
    // last element in chain should match signed public key
    if (!deeplyEqual(signedKeys, allegedSignedKey)) {
      throw new Error('Alleged public key does not match signed public key');
    }
  } else {
    // otherwise, the payload is an intermediate public key used to sign the next element
    const nextTrustedPublicKey = await asPublicKey(base64.parse(signedKeys.ecdsaPublicKey), UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES);
    await verifyRescursive(remainingChain, nextTrustedPublicKey, allegedSignedKey);
  }
}

/**
 * Creates a unique fingerprint for a user by hashing the concatenated thumbprints of their public keys.
 * @param user The user whose fingerprint to compute
 * @returns Hexadecimal representation of the fingerprint
 */
async function computeFingerprint(user: UserDto) {
  if (!user.ecdhPublicKey || !user.ecdsaPublicKey) {
    throw new Error('User has no public keys');
  }
  const ecdhPublicKey = await asPublicKey(base64.parse(user.ecdhPublicKey), UserKeys.ECDH_KEY_DESIGNATION);
  const ecdsaPublicKey = await asPublicKey(base64.parse(user.ecdsaPublicKey), UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES);
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

export default { sign, verify, computeFingerprint };

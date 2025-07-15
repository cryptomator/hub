import { split, combine } from 'shamir-secret-sharing';
import { base64 } from 'rfc4648';
import { JWEBuilder, JWEParser } from './jwe';

type KeySharePayload = {
    keyShare: string; // base64 encoded share
}

type ProcessPrivateKeyPayload = {
    privateKey: JsonWebKey;
}

export type RecoveryProcess = {
    recoveryPublicKey: CryptoKey;
    recoveryPrivateKeys: Record<string, string>;
}

export class EmergencyAccess {
  /**
   * Splits a secret into [1..255] shares, where at least `k` shares are needed to reconstruct the secret.
   * The shares are encrypted for each recipient using ECDH-ES.
   * @param secret The secret to be split, as a Uint8Array.
   * @param k Minimum number of shares needed to reconstruct the secret.
   * @param recipients Array of CryptoKey objects representing the recipients.
   * @returns one JWE for each recipient (in the same order as the recipients).
   */
  public static async split(secret: Uint8Array, k: number, ...recipients: CryptoKey[]): Promise<string[]> {
    const n = recipients.length;
    let shares : Uint8Array[];
    if (k < 1) {
      throw new Error('Threshold k must be at least 1');
    } else if (n < k) {
      throw new Error('Not enough recipients provided for secret sharing');
    } else if (n > 255) {
      throw new Error('Too many recipients provided for secret sharing');
    } else if (k == 1) {
      // no splitting needed // TODO: should we keep this?
      shares = Array(n).fill(secret);
    } else {
      shares = await split(secret, n, k);
    }
    const jwes = [];
    for (let i = 0; i < n; i++) {
      const recipient = recipients[i];
      const payload: KeySharePayload = {
        keyShare: base64.stringify(shares[i])
      };
      const jwe = await JWEBuilder.ecdhEs(recipient).encrypt(payload);
      jwes.push(jwe);
    }
    return jwes;
  }

  /**
   * Starts a recovery process by generating a new key pair, whose private key is encrypted for each council member.
   * @param councilMembers The involved council members
   * @returns The recovery process, containing the public key and encrypted private keys for each council member.
   */
  public static async startRecovery(councilMembers: Record<string, CryptoKey>): Promise<RecoveryProcess> {
    // Generate a new key pair for the recovery process:
    const processKeyPair = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, true, ['deriveBits']);
    // Encrypt the process private key for each council member:
    const encryptedPrivateKeys = new Map<string, string>();
    const jwk = await crypto.subtle.exportKey('jwk', processKeyPair.privateKey);
    const payload: ProcessPrivateKeyPayload = { privateKey: jwk };
    for (const [memberId, memberKey] of Object.entries(councilMembers)) {
      const jwe = await JWEBuilder.ecdhEs(memberKey).encrypt(payload);
      encryptedPrivateKeys.set(memberId, jwe);
    }
    // return public key and encrypted private keys:
    return {
      recoveryPublicKey: processKeyPair.publicKey,
      recoveryPrivateKeys: Object.fromEntries(encryptedPrivateKeys)
    };
  }

  /**
   * Re-encrypts a council member's share for the recovery process.
   * @param share A JWE containing the key share, encrypted for the council member.
   * @param userPrivateKey The council member's private key.
   * @param recoveryProcessPublicKey The public key of the recovery process.
   * @returns A new JWE containing the key share, encrypted for the recovery process.
   */
  public static async recoverShare(share: string, userPrivateKey: CryptoKey, recoveryProcessPublicKey: CryptoKey): Promise<string> {
    const decrypted: KeySharePayload = await JWEParser.parse(share).decryptEcdhEs(userPrivateKey);
    return await JWEBuilder.ecdhEs(recoveryProcessPublicKey).encrypt(decrypted);
  }

  /**
   * Recombines the shares that the council members have recovered.
   * @param recoveredShares Sufficient recovered shares, e.g. a JWE whose recipient is the recovery process private key.
   * @param recoveryProcessPrivateKeyJwe a JWE containing the recovery process private key, encrypted for the user.
   * @param userPrivateKey The user's private key
   * @returns The combined secret as a Uint8Array.
   */
  public static async combineRecoveredShares(recoveredShares: string[], recoveryProcessPrivateKeyJwe: string, userPrivateKey: CryptoKey): Promise<Uint8Array> {
    const jwePayload = await JWEParser.parse(recoveryProcessPrivateKeyJwe).decryptEcdhEs<ProcessPrivateKeyPayload>(userPrivateKey);
    const recoveryProcessPrivateKey = await crypto.subtle.importKey('jwk', jwePayload.privateKey, { name: 'ECDH', namedCurve: 'P-384' }, false, ['deriveBits']);
    const decryptedShares = await Promise.all(recoveredShares.map(share => JWEParser.parse(share).decryptEcdhEs<KeySharePayload>(recoveryProcessPrivateKey)));
    const keyShares = decryptedShares.map(share => base64.parse(share.keyShare));
    return combine(keyShares);
  }
}
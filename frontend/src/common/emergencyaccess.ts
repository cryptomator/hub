import { split, combine } from 'shamir-secret-sharing';
import { base64 } from 'rfc4648';
import { JWEBuilder, JWEParser } from './jwe';
import { ActivatedUser } from './backend';
import { asPublicKey, UserKeys } from './crypto';

type KeySharePayload = {
    keyShare: string;
}

type ProcessPrivateKeyPayload = {
    privateKey: JsonWebKey;
}

export type RecoveryProcess = {
    recoveryPublicKey: string;
    recoveryPrivateKeys: Record<string, string>;
}

export class EmergencyAccess {
  private static readonly PROCESS_KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = { name: 'ECDH', namedCurve: 'P-384' };
  private static readonly PROCESS_KEY_USAGE: KeyUsage[] = ['deriveBits'];

  /**
   * Splits a secret into [1..255] shares, where at least `k` shares are needed to reconstruct the secret.
   * The shares are encrypted for each recipient using ECDH-ES.
   * @param secret The secret to be split, as a Uint8Array.
   * @param k Minimum number of shares needed to reconstruct the secret.
   * @param recipients Array of emergency access council members for whom to create key shares.
   * @returns one JWE for each recipient (in the same order as the recipients).
   */
  public static async split(secret: Uint8Array, k: number, ...recipients: ActivatedUser[]): Promise<Record<string, string>> {
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
    const result: Record<string, string> = {};
    for (let i = 0; i < n; i++) {
      const recipient = recipients[i];
      const payload: KeySharePayload = {
        keyShare: base64.stringify(shares[i])
      };
      const keyBytes = base64.parse(recipient.ecdhPublicKey);
      const key = await asPublicKey(keyBytes, UserKeys.ECDH_KEY_DESIGNATION);
      const jwe = await JWEBuilder.ecdhEs(key).encrypt(payload);
      result[recipient.id] = jwe;
    }
    return result;
  }

  /**
   * Starts a recovery process by generating a new key pair, whose private key is encrypted for each council member.
   * @param councilMembers The involved council members
   * @returns The recovery process, containing the public key and encrypted private keys for each council member.
   */
  public static async startRecovery(councilMembers: ActivatedUser[]): Promise<RecoveryProcess> {
    // Generate a new key pair for the recovery process:
    const processKeyPair = await crypto.subtle.generateKey(EmergencyAccess.PROCESS_KEY_DESIGNATION, true, EmergencyAccess.PROCESS_KEY_USAGE);
    // Encrypt the process private key for each council member:
    const encryptedPrivateKeys = new Map<string, string>();
    const jwk = await crypto.subtle.exportKey('jwk', processKeyPair.privateKey);
    const payload: ProcessPrivateKeyPayload = { privateKey: jwk };
    for (const member of councilMembers) {
      const keyBytes = base64.parse(member.ecdhPublicKey);
      const key = await asPublicKey(keyBytes, UserKeys.ECDH_KEY_DESIGNATION);
      const jwe = await JWEBuilder.ecdhEs(key).encrypt(payload);
      encryptedPrivateKeys.set(member.id, jwe);
    }
    // return public key and encrypted private keys:
    const publicKeyJwk = JSON.stringify(await crypto.subtle.exportKey('jwk', processKeyPair.publicKey)); // TODO: JWK? or SPKI?
    return {
      recoveryPublicKey: publicKeyJwk,
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
  public static async recoverShare(share: string, userPrivateKey: CryptoKey, recoveryProcessPublicKey: string): Promise<string> {
    const decrypted: KeySharePayload = await JWEParser.parse(share).decryptEcdhEs(userPrivateKey);
    const processPublicKeyJwk = JSON.parse(recoveryProcessPublicKey);
    const processPublicKey = await crypto.subtle.importKey('jwk', processPublicKeyJwk, EmergencyAccess.PROCESS_KEY_DESIGNATION, false, []);
    return await JWEBuilder.ecdhEs(processPublicKey).encrypt(decrypted);
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
    const recoveryProcessPrivateKey = await crypto.subtle.importKey('jwk', jwePayload.privateKey, EmergencyAccess.PROCESS_KEY_DESIGNATION, false, EmergencyAccess.PROCESS_KEY_USAGE);
    const decryptedShares = await Promise.all(recoveredShares.map(share => JWEParser.parse(share).decryptEcdhEs<KeySharePayload>(recoveryProcessPrivateKey)));
    const keyShares = decryptedShares.map(share => base64.parse(share.keyShare));
    return combine(keyShares);
  }
}
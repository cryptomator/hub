import { base64 } from '@scure/base';
import { combine, split } from 'shamir-secret-sharing';
import { ActivatedUser } from './backend';
import { asPublicKey, UserKeys } from './crypto';
import { JWE, Recipient } from './jwe';

type KeySharePayload = {
  keyShare: string;
};

type ProcessPrivateKeyPayload = {
  privateKey: JsonWebKey;
};

export type RecoveryProcess = {
  recoveryPublicKey: string;
  recoveryPrivateKeys: Record<string, string>;
};

/**
 * Identifies the recovery process a JWE belongs to.
 */
export type RecoveryProcessContext = {
  vaultId: string;
  processId: string;
};

export class EmergencyAccess {

  private static readonly PROCESS_KEY_DESIGNATION: EcKeyImportParams | EcKeyGenParams = { name: 'ECDH', namedCurve: 'P-384' };
  private static readonly PROCESS_KEY_USAGE: KeyUsage[] = ['deriveBits'];

  /**
   * Splits a secret into [1..255] shares, where at least `k` shares are needed to reconstruct the secret.
   * The shares are encrypted for each recipient using ECDH-ES, bound to the given vault via the critical `ctx` header parameter.
   * @param vaultId ID of the vault this secret belongs to.
   * @param secret The secret to be split, as a Uint8Array.
   * @param k Minimum number of shares needed to reconstruct the secret.
   * @param recipients Array of emergency access council members for whom to create key shares.
   * @returns one JWE for each recipient (in the same order as the recipients).
   */
  public static async split(vaultId: string, secret: Uint8Array, k: number, ...recipients: ActivatedUser[]): Promise<Record<string, string>> {
    const n = recipients.length;
    let shares: Uint8Array[];
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
        keyShare: base64.encode(shares[i])
      };
      const keyBytes = base64.decode(recipient.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
      const key = await asPublicKey(keyBytes, UserKeys.ECDH_KEY_DESIGNATION);
      const ctx = EmergencyAccess.keyShareContext(vaultId, recipient.id);
      result[recipient.id] = await JWE.build(payload, { ctx: ctx, crit: ['ctx'] }).withRecipients(Recipient.ecdhEs('', key)).toCompact();
    }
    return result;
  }

  /**
   * Starts a recovery process by generating a new key pair, whose private key is encrypted for each council member, bound to the given process via the critical `ctx` header parameter.
   * @param processId ID of the recovery process.
   * @param councilMembers The involved council members
   * @returns The recovery process, containing the public key and encrypted private keys for each council member.
   */
  public static async startRecovery(processId: string, councilMembers: ActivatedUser[]): Promise<RecoveryProcess> {
    // Generate a new key pair for the recovery process:
    const processKeyPair = await crypto.subtle.generateKey(EmergencyAccess.PROCESS_KEY_DESIGNATION, true, EmergencyAccess.PROCESS_KEY_USAGE);
    // Encrypt the process private key for each council member:
    const encryptedPrivateKeys = new Map<string, string>();
    const jwk = await crypto.subtle.exportKey('jwk', processKeyPair.privateKey);
    const payload: ProcessPrivateKeyPayload = { privateKey: jwk };
    for (const member of councilMembers) {
      const keyBytes = base64.decode(member.ecdhPublicKey) as Uint8Array<ArrayBuffer>;
      const key = await asPublicKey(keyBytes, UserKeys.ECDH_KEY_DESIGNATION);
      const ctx = EmergencyAccess.processKeyContext(processId, member.id);
      encryptedPrivateKeys.set(member.id, await JWE.build(payload, { ctx: ctx, crit: ['ctx'] }).withRecipients(Recipient.ecdhEs('', key)).toCompact());
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
   * The share JWE must be bound to the expected vault and council member via its `ctx` header parameter, otherwise it is rejected.
   * This prevents a malicious process starter from abusing approving council members as a decryption oracle for foreign ciphertexts.
   * @param share A JWE containing the key share, encrypted for the council member.
   * @param context The recovery process this share is contributed to.
   * @param memberId ID of the council member owning the share, i.e. the user calling this method.
   * @param userPrivateKey The council member's private key.
   * @param recoveryProcessPublicKey The public key of the recovery process.
   * @returns A new JWE containing the key share, encrypted for the recovery process.
   */
  public static async recoverShare(share: string, context: RecoveryProcessContext, memberId: string, userPrivateKey: CryptoKey, recoveryProcessPublicKey: string): Promise<string> {
    const expectedCtx = EmergencyAccess.keyShareContext(context.vaultId, memberId);
    const decrypted: unknown = await EmergencyAccess.decryptWithContext(share, Recipient.ecdhEs('', userPrivateKey), expectedCtx);
    if (!EmergencyAccess.isKeySharePayload(decrypted)) {
      throw new Error('Unexpected key share payload');
    }
    const processPublicKeyJwk = JSON.parse(recoveryProcessPublicKey);
    const processPublicKey = await crypto.subtle.importKey('jwk', processPublicKeyJwk, EmergencyAccess.PROCESS_KEY_DESIGNATION, false, []);
    const ctx = EmergencyAccess.recoveredShareContext(context);
    return JWE.build(decrypted, { ctx: ctx, crit: ['ctx'] }).withRecipients(Recipient.ecdhEs('', processPublicKey)).toCompact();
  }

  /**
   * Recombines the shares that the council members have recovered.
   * All involved JWEs must be bound to the expected recovery process via their `ctx` header parameter, otherwise they are rejected.
   * @param recoveredShares Sufficient recovered shares, e.g. a JWE whose recipient is the recovery process private key.
   * @param recoveryProcessPrivateKeyJwe a JWE containing the recovery process private key, encrypted for the user.
   * @param context The recovery process the shares were contributed to.
   * @param memberId ID of the council member owning the process private key JWE, i.e. the user calling this method.
   * @param userPrivateKey The user's private key
   * @returns The combined secret as a Uint8Array.
   */
  public static async combineRecoveredShares(recoveredShares: string[], recoveryProcessPrivateKeyJwe: string, context: RecoveryProcessContext, memberId: string, userPrivateKey: CryptoKey): Promise<Uint8Array> {
    const processKeyCtx = EmergencyAccess.processKeyContext(context.processId, memberId);
    const jwePayload: ProcessPrivateKeyPayload = await EmergencyAccess.decryptWithContext(recoveryProcessPrivateKeyJwe, Recipient.ecdhEs('', userPrivateKey), processKeyCtx);
    const recoveryProcessPrivateKey = await crypto.subtle.importKey('jwk', jwePayload.privateKey, EmergencyAccess.PROCESS_KEY_DESIGNATION, false, EmergencyAccess.PROCESS_KEY_USAGE);
    const recoveredShareCtx = EmergencyAccess.recoveredShareContext(context);
    const decryptedShares = await Promise.all(recoveredShares.map(share => EmergencyAccess.decryptWithContext<KeySharePayload>(share, Recipient.ecdhEs('', recoveryProcessPrivateKey), recoveredShareCtx)));
    const keyShares = decryptedShares.map(share => base64.decode(share.keyShare) as Uint8Array<ArrayBuffer>);
    return combine(keyShares);
  }

  // context strings binding a JWE to its purpose, scope and recipient via the critical `ctx` header parameter:

  private static keyShareContext(vaultId: string, memberId: string): string {
    return `hub:emergency-key-share:${vaultId}:${memberId}`;
  }

  private static processKeyContext(processId: string, memberId: string): string {
    return `hub:emergency-process-key:${processId}:${memberId}`;
  }

  private static recoveredShareContext(context: RecoveryProcessContext): string {
    return `hub:emergency-recovered-share:${context.processId}:${context.vaultId}`;
  }

  // decrypts a JWE after asserting that it was encrypted for the expected context
  private static async decryptWithContext<T>(jwe: string, recipient: Recipient, expectedCtx: string): Promise<T> {
    const encrypted = JWE.parseCompact(jwe);
    if (encrypted.header.ctx !== expectedCtx) {
      throw new Error(`JWE not bound to expected context ${expectedCtx}`);
    }
    return encrypted.decrypt(recipient, ['ctx']);
  }

  private static isKeySharePayload(payload: unknown): payload is KeySharePayload {
    return typeof payload === 'object' && payload !== null && Object.keys(payload).length === 1 && typeof (payload as KeySharePayload).keyShare === 'string';
  }

}

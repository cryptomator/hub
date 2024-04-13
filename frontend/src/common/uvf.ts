import * as miscreant from 'miscreant';
import { base32, base64 } from 'rfc4648';
import { VaultMetadataJWEAutomaticAccessGrantDto } from './backend';
import { JWEBuilder, JWEParser } from './jwe';

export class VaultMetadata {
  // a 256 bit = 32 byte file key for data encryption
  private static readonly RAWKEY_KEY_DESIGNATION: HmacImportParams | HmacKeyGenParams = {
    name: 'HMAC',
    hash: 'SHA-256',
    length: 256
  };

  readonly automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto;
  readonly keys: Record<string, CryptoKey>;
  readonly latestFileKey: string;
  readonly nameKey: string;

  protected constructor(automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto, keys: Record<string, CryptoKey>, latestFileKey: string, nameKey: string) {
    this.automaticAccessGrant = automaticAccessGrant;
    this.keys = keys;
    this.latestFileKey = latestFileKey;
    this.nameKey = nameKey;
  }

  /**
   * Creates new vault metadata with a new file key and name key
   * @returns new vault metadata
   */
  public static async create(automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto): Promise<VaultMetadata> {
    const fileKey = crypto.subtle.generateKey(
      VaultMetadata.RAWKEY_KEY_DESIGNATION,
      true,
      // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 is this correct?
      ['sign']
    );
    const nameKey = crypto.subtle.generateKey(
      VaultMetadata.RAWKEY_KEY_DESIGNATION,
      true,
      // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 is this correct?
      ['sign']
    );
    const fileKeyId = Array(4).fill(null).map(() => "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(Math.random() * 62)).join("")
    const nameKeyId = Array(4).fill(null).map(() => "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(Math.random() * 62)).join("")
    const keys: Record<string, CryptoKey> = {};
    keys[fileKeyId] = await fileKey;
    keys[nameKeyId] = await nameKey;
    return new VaultMetadata(automaticAccessGrant, keys, fileKeyId, nameKeyId);
  }

  /**
   * Decrypts the vault metadata using the vault masterkey
   * @param jwe JWE containing the vault key
   * @param masterKey the vault masterKey
   * @returns vault metadata
   */
  public static async decryptWithMasterKey(jwe: string, masterKey: CryptoKey): Promise<VaultMetadata> {
    const payload = await JWEParser.parse(jwe).decryptA256kw(masterKey);
    const keys: Record<string, string> = payload['keys'];
    const keysImported: Record<string, CryptoKey> = payload['keys'];
    for (const k in keys) {
      // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 is this correct?
      keysImported[k] = await crypto.subtle.importKey('raw', base64.parse(keys[k]), VaultMetadata.RAWKEY_KEY_DESIGNATION, true, ['sign']);
    }
    const latestFileKey = payload['latestFileKey']
    const nameKey = payload['nameKey']
    return new VaultMetadata(
      payload['org.cryptomator.automaticAccessGrant'],
      keysImported,
      latestFileKey,
      nameKey
    );
  }

  /**
   * Encrypts the vault metadata using the given vault masterKey
   * @param userPublicKey The recipient's public key (DER-encoded)
   * @returns a JWE containing this Masterkey
   */
  public async encryptWithMasterKey(masterKey: CryptoKey): Promise<string> {
    const keysExported: Record<string, string> = {};
    for (const k in this.keys) {
      keysExported[k] = base64.stringify(new Uint8Array(await crypto.subtle.exportKey('raw', this.keys[k])));
    }
    const payload = {
      fileFormat: "AES-256-GCM-32k",
      nameFormat: "AES-256-SIV",
      keys: keysExported,
      latestFileKey: this.latestFileKey,
      nameKey: this.nameKey,
      // TODO https://github.com/encryption-alliance/unified-vault-format/pull/21 finalize kdf
      kdf: "1STEP-HMAC-SHA512",
      'org.cryptomator.automaticAccessGrant': this.automaticAccessGrant
    }
    return JWEBuilder.a256kw(masterKey).encrypt(payload);
  }

  public async hashDirectoryId(cleartextDirectoryId: string): Promise<string> {
    const dirHash = new TextEncoder().encode(cleartextDirectoryId);
    // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! MUST NEVER BE RELEASED LIKE THIS
    // TODO https://github.com/encryption-alliance/unified-vault-format/pull/19 use rawFileKey,rawNameKey for rootDirHash for now - should depend on nameKey only!!
    const rawkey = new Uint8Array([...new Uint8Array(await crypto.subtle.exportKey('raw', this.keys[this.latestFileKey])), ...new Uint8Array(await crypto.subtle.exportKey('raw', this.keys[this.nameKey]))]);
    try {
      // miscreant lib requires mac key first and then the enc key
      const encKey = rawkey.subarray(0, rawkey.length / 2 | 0);
      const macKey = rawkey.subarray(rawkey.length / 2 | 0);
      const shiftedRawKey = new Uint8Array([...macKey, ...encKey]);
      const key = await miscreant.SIV.importKey(shiftedRawKey, 'AES-SIV');
      const ciphertext = await key.seal(dirHash, []);
      // hash is only used as deterministic scheme for the root dir
      const hash = await crypto.subtle.digest('SHA-1', ciphertext);
      return base32.stringify(new Uint8Array(hash));
    } finally {
      rawkey.fill(0x00);
    }
  }
}

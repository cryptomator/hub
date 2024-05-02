import JSZip from 'jszip';
import { base64, base64url } from 'rfc4648';
import { VaultDto } from './backend';
import { AccessTokenProducing, UserKeys, VaultTemplateProducing, getJwkThumbprint } from './crypto';
import { JWE, JWEHeader, JsonJWE, Recipient } from './jwe';

type MetadataPayload = {
  fileFormat: 'AES-256-GCM-32k';
  nameFormat: 'AES-SIV-512-B64URL'; // TODO verify after merging https://github.com/encryption-alliance/unified-vault-format/pull/24
  seeds: Record<string, string>;
  initialSeed: string;
  latestSeed: string;
  kdf: 'HKDF-SHA512';
  kdfSalt: string;
  'org.cryptomator.automaticAccessGrant': VaultMetadataJWEAutomaticAccessGrantDto;
}

type VaultMetadataJWEAutomaticAccessGrantDto = {
  enabled: boolean,
  maxWotDepth: number
}

type MemberKeyPayload = {
  key: string
}

// #region Member Key
/**
 * The AES Key Wrap Key used to encapsulate the UVF Vault Metadata CEK for a vault member.
 * This key is encrypted for each vault member individually, using the user's public key.
 */
export class MemberKey {
  public static readonly KEY_DESIGNATION: AesKeyGenParams | AesKeyAlgorithm = { name: 'AES-KW', length: 256 };

  public static readonly KEY_USAGE: KeyUsage[] = ['wrapKey', 'unwrapKey'];

  protected constructor(readonly key: CryptoKey) { }

  /**
   * Creates a new vault member key
   * @returns new key
   */
  public static async create(): Promise<MemberKey> {
    const key = await crypto.subtle.generateKey(MemberKey.KEY_DESIGNATION, true, MemberKey.KEY_USAGE);
    return new MemberKey(key);
  }

  /**
   * Decrypts the vault's member key using the member's private key
   * @param jwe JWE containing the encrypted member key
   * @param userPrivateKey The user's private key
   * @returns The masterkey
   */
  public static async decryptWithUserKey(jwe: string, userPrivateKey: CryptoKey): Promise<MemberKey> {
    let rawKey = new Uint8Array();
    try {
      const payload: MemberKeyPayload = await JWE.parseCompact(jwe).decrypt(Recipient.ecdhEs('org.cryptomator.hub.userkey', userPrivateKey));
      rawKey = base64.parse(payload.key);
      const memberKey = await crypto.subtle.importKey('raw', rawKey, MemberKey.KEY_DESIGNATION, true, MemberKey.KEY_USAGE);
      return new MemberKey(memberKey);
    } finally {
      rawKey.fill(0x00);
    }
  }

  /**
   * Encrypts this member key using the given public key
   * @param userPublicKey The user's public key (DER-encoded)
   * @returns a JWE containing this member key
   */
  public async encryptForUser(userPublicKey: CryptoKey | Uint8Array): Promise<string> {
    // TODO: remove Uint8Array support?
    if (userPublicKey instanceof Uint8Array) {
      userPublicKey = await crypto.subtle.importKey('spki', userPublicKey, UserKeys.KEY_DESIGNATION, false, []);
    }
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.key));
    try {
      const payload: MemberKeyPayload = {
        key: base64.stringify(rawkey),
      };
      const jwe = await JWE.build(payload).encrypt(Recipient.ecdhEs('org.cryptomator.hub.userkey', userPublicKey));
      return jwe.compactSerialization();
    } finally {
      rawkey.fill(0x00);
    }
  }

}
// #endregion

// #region Recovery Key
/**
 * The Recovery Key Pair used to encapsulate the UVF Vault Metadata CEK for recovery purposes.
 */
export class RecoveryKey {
  public static readonly KEY_DESIGNATION: EcKeyGenParams = { name: 'ECDH', namedCurve: 'P-384' };

  public static readonly KEY_USAGE: KeyUsage[] = ['deriveKey', 'deriveBits'];

  protected constructor(private publicKey: CryptoKey, private privateKey?: CryptoKey) { }

  /**
   * Creates a new vault member key
   * @returns new key
   */
  public static async create(): Promise<RecoveryKey> {
    const keypair = await crypto.subtle.generateKey(RecoveryKey.KEY_DESIGNATION, false, RecoveryKey.KEY_USAGE);
    return new RecoveryKey(keypair.publicKey, keypair.privateKey);
  }

  /**
   * Loads the public key of the recovery key pair.
   * @param publicKey the DER-encoded public key
   * @returns recovery key for encrypting vault metadata
   */
  public static async load(publicKey: CryptoKey | Uint8Array): Promise<RecoveryKey> {
    if (publicKey instanceof Uint8Array) {
      publicKey = await crypto.subtle.importKey('spki', publicKey, RecoveryKey.KEY_DESIGNATION, true, []);
    }
    return new RecoveryKey(publicKey);
  }

  /**
   * Restores the Recovery Key Pair
   * @param recoveryKey the encoded recovery key
   * @returns complete recovery key for decrypting vault metadata
   */
  public static recover(recoveryKey: string) {
    // TODO
  }

  /**
   * Encodes the private key
   * @returns private key in a human-readable encoding
   */
  public serializePrivateKey(): string {
    return 'TODO'; // TODO
  }

  /**
   * Encodes the public key
   * @returns public key in base64-encoded DER format
   */
  public async serializePublicKey(): Promise<string> {
    const bytes = await crypto.subtle.exportKey('spki', this.publicKey);
    return base64.stringify(new Uint8Array(bytes), { pad: true });
  }

}
// #endregion

// #region Vault metadata
/**
 * The UVF Metadata file
 */
export class VaultMetadata {

  private constructor(
    readonly automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto,
    readonly seeds: Map<number, Uint8Array>,
    readonly initialSeedId: number,
    readonly latestSeedId: number,
    readonly kdfSalt: Uint8Array) {
  }

  /**
   * Creates a new UVF vault
   * @param automaticAccessGrant Configuration instructing the client how to automatically deal with permission requests
   * @returns new vault
   */
  public static async create(automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto): Promise<VaultMetadata> {
    const initialSeedId = new Uint32Array(1);
    const initialSeedValue = new Uint8Array(32);
    const kdfSalt = new Uint8Array(32);
    crypto.getRandomValues(initialSeedId);
    crypto.getRandomValues(initialSeedValue);
    crypto.getRandomValues(kdfSalt);
    const initialSeedNo = initialSeedId[0];
    const seeds: Map<number, Uint8Array> = new Map<number, Uint8Array>();
    seeds.set(initialSeedNo, initialSeedValue);
    return new VaultMetadata(automaticAccessGrant, seeds, initialSeedNo, initialSeedNo, kdfSalt);
  }

  /**
   * Decrypts the vault metadata using the members' vault key
   * @param uvfMetadataFile contents of the `vault.uvf` file
   * @param memberKey the vault members' wrapping key
   * @returns Decrypted vault metadata
   */
  public static async decryptWithMemberKey(uvfMetadataFile: string, memberKey: MemberKey): Promise<VaultMetadata> {
    const json: JsonJWE = JSON.parse(uvfMetadataFile);
    const payload: MetadataPayload = await JWE.parseJson(json).decrypt(Recipient.a256kw('org.cryptomator.hub.memberkey', memberKey.key));
    const seeds = new Map<number, Uint8Array>();
    for (const key in payload.seeds) {
      const num = parseSeedId(key);
      const value = base64url.parse(payload.seeds[key], { loose: true });
      seeds.set(num, value);
    }
    const initialSeedId = parseSeedId(payload['initialSeed']);
    const latestSeedId = parseSeedId(payload['latestSeed']);
    const kdfSalt = base64url.parse(payload['kdfSalt'], { loose: true });
    return new VaultMetadata(
      payload['org.cryptomator.automaticAccessGrant'],
      seeds,
      initialSeedId,
      latestSeedId,
      kdfSalt
    );
  }

  /**
   * Encrypts the vault metadata
   * @param memberKey the vault members' AES wrapping key
   * @param recoveryKey the public part of the recovery EC key pair
   * @returns `vault.uvf` file contents
   */
  public async encrypt(memberKey: MemberKey, recoveryKey: RecoveryKey): Promise<string> {
    const recoveryKeyID = `org.cryptomator.hub.recoverykey.${await getJwkThumbprint(recoveryKey.publicKey)}`;
    const protectedHeader: JWEHeader = {
      jku: 'jku.jwks' // URL relative to /api/vaults/{vaultid}/
    };
    const jwe = await JWE.build(this.payload(), protectedHeader).encrypt(Recipient.a256kw('org.cryptomator.hub.memberkey', memberKey.key), Recipient.ecdhEs(recoveryKeyID, recoveryKey.publicKey));
    const json = jwe.jsonSerialization();
    return JSON.stringify(json);
  }

  public payload(): MetadataPayload {
    const encodedSeeds: Record<string, string> = {};
    for (const [key, value] of this.seeds) {
      const seedId = stringifySeedId(key);
      encodedSeeds[seedId] = base64url.stringify(value, { pad: false });
    }
    return {
      fileFormat: 'AES-256-GCM-32k',
      nameFormat: 'AES-SIV-512-B64URL',
      seeds: encodedSeeds,
      initialSeed: stringifySeedId(this.initialSeedId),
      latestSeed: stringifySeedId(this.latestSeedId),
      kdf: 'HKDF-SHA512',
      kdfSalt: base64url.stringify(this.kdfSalt, { pad: false }),
      'org.cryptomator.automaticAccessGrant': this.automaticAccessGrant
    };
  }

}
// #endregion
// #region Vault metadata

/**
 * A UVF-formatted Vault
 */
export class UniversalVaultFormat implements AccessTokenProducing, VaultTemplateProducing {
  private constructor(readonly metadata: VaultMetadata, readonly memberKey: MemberKey, readonly recoveryKey: RecoveryKey) { }

  public static async create(automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto): Promise<UniversalVaultFormat> {
    const metadata = await VaultMetadata.create(automaticAccessGrant);
    const memberKey = await MemberKey.create();
    const recoveryKey = await RecoveryKey.create();
    return new UniversalVaultFormat(metadata, memberKey, recoveryKey);
  }

  public static async decrypt(vault: VaultDto, memberKey: MemberKey): Promise<UniversalVaultFormat> {
    if (!vault.uvfMetadataFile || !vault.uvfRecoveryPublicKey) {
      throw new Error('Not a UVF vault.');
    }
    const metadata = await VaultMetadata.decryptWithMemberKey(vault.uvfMetadataFile, memberKey);
    const recoveryKey = await RecoveryKey.load(base64.parse(vault.uvfRecoveryPublicKey));
    return new UniversalVaultFormat(metadata, memberKey, recoveryKey);
  }

  public async createMetadataFile(): Promise<string> {
    return this.metadata.encrypt(this.memberKey, this.recoveryKey);
  }

  public async exportTemplate(vault: VaultDto): Promise<Blob> {
    const zip = new JSZip();
    zip.file('vault.uvf', this.createMetadataFile());
    // TODO: add root folder
    //zip.folder('d')?.folder(rootDirHash.substring(0, 2))?.folder(rootDirHash.substring(2));
    return zip.generateAsync({ type: 'blob' });
  }

  public async encryptForUser(userPublicKey: CryptoKey | Uint8Array): Promise<string> {
    return this.memberKey.encryptForUser(userPublicKey);
  }
}

/**
 * Parses the 4 byte seed id from its base64url-encoded form to a 32 bit integer.
 * @param encoded base64url-encoded seed ID
 * @returns a 32 bit number
 * @throws Error if the input is invalid
 */
function parseSeedId(encoded: string): number {
  const bytes = base64url.parse(encoded, { loose: true });
  if (bytes.length != 4) {
    throw new Error('Malformed seed ID');
  }
  return new Uint32Array(bytes.buffer)[0];
}

/**
 * Encodes a 32 bit integer denoting the 4 byte seed id as a base64url-encoded string.
 * @param id numeric seed ID
 * @returns a base6url-encoded seed ID
 */
function stringifySeedId(id: number): string {
  const ints = new Uint32Array([id]);
  const bytes = new Uint8Array(ints.buffer)
  return base64url.stringify(bytes, { pad: false });
}

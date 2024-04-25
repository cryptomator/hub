import { base64url } from 'rfc4648';
import { JWE, Recipient } from './jwe';

export type MetadataPayload = {
  fileFormat: 'AES-256-GCM-32k';
  nameFormat: 'AES-SIV-512-B64URL'; // TODO verify after merging https://github.com/encryption-alliance/unified-vault-format/pull/24
  seeds: Record<string, string>;
  initialSeed: string;
  latestSeed: string;
  kdf: 'HKDF-SHA512';
  kdfSalt: string;
  'org.cryptomator.automaticAccessGrant': VaultMetadataJWEAutomaticAccessGrantDto;
}

export type VaultMetadataJWEAutomaticAccessGrantDto = {
  enabled: boolean,
  maxWotDepth: number
}

export class VaultMetadata {

  readonly automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto;
  readonly seeds: Map<number, Uint8Array>;
  readonly initialSeedId: number;
  readonly latestSeedId: number;
  readonly kdfSalt: Uint8Array;

  protected constructor(automaticAccessGrant: VaultMetadataJWEAutomaticAccessGrantDto, seeds: Map<number, Uint8Array>, initialSeedId: number, latestSeedId: number, kdfSalt: Uint8Array) {
    this.automaticAccessGrant = automaticAccessGrant;
    this.seeds = seeds;
    this.initialSeedId = initialSeedId;
    this.latestSeedId = latestSeedId;
    this.kdfSalt = kdfSalt;
  }

  /**
   * Creates new vault metadata with a new file key and name key
   * @returns new vault metadata
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
   * Decrypts the vault metadata using the vault masterkey
   * @param jwe JWE containing the vault key
   * @param masterKey the vault masterKey
   * @returns vault metadata
   */
  public static async decryptWithMasterKey(jwe: string, masterKey: CryptoKey): Promise<VaultMetadata> {
    const payload = await JWE.parseCompact(jwe).decrypt(Recipient.a256kw('org.cryptomator.hub.masterkey', masterKey));
    const encodedSeeds: Record<string, string> = payload['seeds'];
    const seeds = new Map<number, Uint8Array>();
    for (const key in encodedSeeds) {
      const num = parseSeedId(key);
      const value = base64url.parse(encodedSeeds[key], { loose: true });
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
   * Encrypts the vault metadata using the given vault masterKey
   * @param userPublicKey The recipient's public key (DER-encoded)
   * @returns a JWE containing this Masterkey
   */
  public async encryptWithMasterKey(masterKey: CryptoKey): Promise<string> {
    const encodedSeeds: Record<string, string> = {};
    for (const [key, value] of this.seeds) {
      const seedId = stringifySeedId(key);
      encodedSeeds[seedId] = base64url.stringify(value, { pad: false });
    }
    const payload: MetadataPayload = {
      fileFormat: 'AES-256-GCM-32k',
      nameFormat: 'AES-SIV-512-B64URL',
      seeds: encodedSeeds,
      initialSeed: stringifySeedId(this.initialSeedId),
      latestSeed: stringifySeedId(this.latestSeedId),
      // TODO https://github.com/encryption-alliance/unified-vault-format/pull/21 finalize kdf
      kdf: 'HKDF-SHA512',
      kdfSalt: base64url.stringify(this.kdfSalt, { pad: false }),
      'org.cryptomator.automaticAccessGrant': this.automaticAccessGrant
    }
    const jwe = await JWE.build(payload).encrypt(Recipient.a256kw('org.cryptomator.hub.masterkey', masterKey));
    return jwe.compactSerialization();
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

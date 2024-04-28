import { base64url } from 'rfc4648';
import { getJwkThumbprint } from './crypto';
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

// const MEMBER_KEY_DESIGNATION: AesKeyGenParams = {
//   name: 'AES',
//   length: 256
// };

// const MEMBER_KEY_USAGE: KeyUsage[] = ['wrapKey', 'unwrapKey'];

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
    // const memberKey = await crypto.subtle.generateKey(MEMBER_KEY_DESIGNATION, false, MEMBER_KEY_USAGE);
    seeds.set(initialSeedNo, initialSeedValue);
    return new VaultMetadata(automaticAccessGrant, seeds, initialSeedNo, initialSeedNo, kdfSalt);
  }

  /**
   * Decrypts the vault metadata using the members' vault key
   * @param uvfMetadataFile contents of the `vault.uvf` file
   * @param memberKey the vault members' wrapping key
   * @returns Decrypted vault metadata
   */
  public static async decryptWithMemberKey(uvfMetadataFile: string, memberKey: CryptoKey): Promise<VaultMetadata> {
    const json: JsonJWE = JSON.parse(uvfMetadataFile);
    const payload: MetadataPayload = await JWE.parseJson(json).decrypt(Recipient.a256kw('org.cryptomator.hub.memberkey', memberKey));
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
  public async encrypt(memberKey: CryptoKey, recoveryKey: CryptoKey): Promise<string> {
    const recoveryKeyID = `org.cryptomator.hub.recoverykey.${await getJwkThumbprint(recoveryKey)}`;
    const protectedHeader: JWEHeader = {
      jku: 'jku.jwks' // URL relative to /api/vaults/{vaultid}/
    };
    const jwe = await JWE.build(this.payload(), protectedHeader).encrypt(Recipient.a256kw('org.cryptomator.hub.memberkey', memberKey), Recipient.ecdhEs(recoveryKeyID, recoveryKey)); // TODO
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

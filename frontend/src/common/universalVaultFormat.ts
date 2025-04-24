import JSZip from 'jszip';
import { base32, base64, base64url } from 'rfc4648';
import { VaultDto } from './backend';
import { AccessTokenPayload, AccessTokenProducing, JsonWebKeySet, OtherVaultMember, UserKeys, VaultTemplateProducing, getJwkThumbprintStr } from './crypto';
import { JWE, JWEHeader, JsonJWE, Recipient } from './jwe';
import { CRC32, wordEncoder } from './util';

type MetadataPayload = {
  fileFormat: 'AES-256-GCM-32k';
  nameFormat: 'AES-SIV-512-B64URL';
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

type UvfAccessTokenPayload = AccessTokenPayload & {
  /**
   * optional private key of the recovery key pair (PKCS8-encoded; only shared with vault owners)
   */
  recoveryKey?: string;
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
   * Creates a new vault member key
   * @param encodedKey base64-encoded raw 256 bit key (as retrieved from {@link AccessTokenPayload#key})
   * @returns new key
   */
  public static async load(encodedKey: string): Promise<MemberKey> {
    let rawKey: Uint8Array = new Uint8Array();
    try {
      rawKey = base64.parse(encodedKey);
      const memberKey = await crypto.subtle.importKey('raw', rawKey, MemberKey.KEY_DESIGNATION, true, MemberKey.KEY_USAGE);
      return new MemberKey(memberKey);
    } finally {
      rawKey.fill(0x00);
    }
  }

  /**
   * Encodes the key
   * @returns member key in base64-encoded raw format
   */
  public async serializeKey(): Promise<string> {
    const bytes = await crypto.subtle.exportKey('raw', this.key);
    return base64.stringify(new Uint8Array(bytes), { pad: true });
  }
}
// #endregion

// #region Recovery Key
/**
 * The Recovery Key Pair used to encapsulate the UVF Vault Metadata CEK for recovery purposes.
 */
export class RecoveryKey {
  public static readonly KEY_DESIGNATION: EcKeyGenParams = { name: 'ECDH', namedCurve: 'P-384' };

  public static readonly KEY_USAGES: KeyUsage[] = ['deriveKey', 'deriveBits'];

  protected constructor(readonly publicKey: CryptoKey, readonly privateKey?: CryptoKey) { }

  /**
   * Creates a new vault member key
   * @returns new key
   */
  public static async create(): Promise<RecoveryKey> {
    const keypair = await crypto.subtle.generateKey(RecoveryKey.KEY_DESIGNATION, true, RecoveryKey.KEY_USAGES);
    return new RecoveryKey(keypair.publicKey, keypair.privateKey);
  }

  /**
   * Imports the public key of the recovery key pair.
   * @param publicKey the DER-encoded public key
   * @param publicKey the PKCS8-encoded private key
   * @returns recovery key for encrypting vault metadata
   */
  public static async import(publicKey: CryptoKey | Uint8Array, privateKey?: CryptoKey | Uint8Array): Promise<RecoveryKey> {
    if (publicKey instanceof Uint8Array) {
      publicKey = await crypto.subtle.importKey('spki', publicKey, RecoveryKey.KEY_DESIGNATION, true, []);
    }
    if (privateKey instanceof Uint8Array) {
      privateKey = await crypto.subtle.importKey('pkcs8', privateKey, RecoveryKey.KEY_DESIGNATION, true, RecoveryKey.KEY_USAGES);
    }
    return new RecoveryKey(publicKey, privateKey);
  }

  /**
   * Restores the Recovery Key Pair
   * @param recoveryKey the encoded recovery key
   * @returns complete recovery key for decrypting vault metadata
   * @throws DecodeUvfRecoveryKeyError, if passing a malformed recovery key
   */
  public static async recover(recoveryKey: string): Promise<RecoveryKey> {
    // decode and check recovery key:
    let decoded;
    try {
      decoded = wordEncoder.decode(recoveryKey);
    } catch (error) {
      throw new DecodeUvfRecoveryKeyError(error instanceof Error ? error.message : 'Internal error. See console log for more info.');
    }

    const paddingLength = decoded[decoded.length - 1];
    if (paddingLength > 0x03) {
      throw new DecodeUvfRecoveryKeyError('Invalid padding');
    }
    const unpadded = decoded.subarray(0, -paddingLength);
    const checksum = unpadded.subarray(-2);
    const rawkey = unpadded.subarray(0, -2);
    const crc32 = CRC32.compute(rawkey);
    if (checksum[0] !== (crc32 & 0xFF)
      || checksum[1] !== (crc32 >> 8 & 0xFF)) {
      throw new DecodeUvfRecoveryKeyError('Invalid recovery key checksum.');
    }

    // construct new RecoveryKey from recovered key
    const privateKey = await crypto.subtle.importKey('pkcs8', rawkey, RecoveryKey.KEY_DESIGNATION, true, RecoveryKey.KEY_USAGES);
    const jwk = await crypto.subtle.exportKey('jwk', privateKey);
    delete jwk.d; // remove private part
    const publicKey = await crypto.subtle.importKey('jwk', jwk, RecoveryKey.KEY_DESIGNATION, true, []);
    return new RecoveryKey(publicKey, privateKey);
  }

  /**
   * Encodes the private key as a list of words
   * @returns private key in a human-readable encoding
   */
  public async createRecoveryKey(): Promise<string> {
    if (!this.privateKey) {
      throw new Error('Private key not available');
    }
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('pkcs8', this.privateKey));

    // add 16 bit checksum:
    const crc32 = CRC32.compute(rawkey);
    const checksum = new Uint8Array(2);
    checksum[0] = crc32 & 0xff;      // append the least significant byte of the crc
    checksum[1] = crc32 >> 8 & 0xff; // followed by the second-least significant byte
    const combined = new Uint8Array([...rawkey, ...checksum]);

    // add 1-3 bytes of padding:
    const numPaddingBytes = 3 - (combined.length % 3);
    const padding = new Uint8Array(numPaddingBytes);
    padding.fill(numPaddingBytes & 0xFF); // 01 or 02 02 or 03 03 03
    const padded = new Uint8Array([...combined, ...padding]);

    // encode using human-readable words:
    return wordEncoder.encodePadded(padded);
  }

  /**
   * Encodes the private key
   * @returns private key in base64-encoded DER format
   */
  public async serializePrivateKey(): Promise<string> {
    if (!this.privateKey) {
      throw new Error('Private key not available');
    }
    const bytes = await crypto.subtle.exportKey('pkcs8', this.privateKey);
    return base64.stringify(new Uint8Array(bytes), { pad: true });
  }

  /**
   * Encodes the public key
   * @returns public key in JWK format
   */
  public async serializePublicKey(): Promise<string> {
    const jwk = await crypto.subtle.exportKey('jwk', this.publicKey);
    const thumbprint = await getJwkThumbprintStr(jwk);
    return JSON.stringify({
      kid: `org.cryptomator.hub.recoverykey.${thumbprint}`,
      kty: jwk.kty,
      crv: jwk.crv,
      x: jwk.x,
      y: jwk.y
    });
  }
}

export class DecodeUvfRecoveryKeyError extends Error {
  constructor(message: string) {
    super(message);
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
    if (!seeds.has(initialSeedId)) {
      throw new Error('Initial seed is missing');
    }
    if (!seeds.has(latestSeedId)) {
      throw new Error('Latest seed is missing');
    }
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

  public get initialSeed(): Uint8Array {
    if (!this.seeds.has(this.initialSeedId)) {
      throw new Error('Illegal State');
    }
    return this.seeds.get(this.initialSeedId)!;
  }

  public get latestSeed(): Uint8Array {
    if (!this.seeds.has(this.latestSeedId)) {
      throw new Error('Illegal State');
    }
    return this.seeds.get(this.latestSeedId)!;
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
    return VaultMetadata.createFromJson(payload);
  }

  /**
   * Decrypts the vault metadata using the recovery key
   * @param uvfMetadataFile contents of the `vault.uvf` file
   * @param recoveryKey the vault's recovery key
   * @returns Decrypted vault metadata
   */
  public static async decryptWithRecoveryKey(uvfMetadataFile: string, recoveryKey: RecoveryKey): Promise<VaultMetadata> {
    if (!recoveryKey.privateKey) {
      throw new Error('Recovery key does not have a private key');
    }
    const recoveryKeyID = `org.cryptomator.hub.recoverykey.${await getJwkThumbprintStr(recoveryKey.publicKey)}`;
    const json: JsonJWE = JSON.parse(uvfMetadataFile);
    const payload: MetadataPayload = await JWE.parseJson(json).decrypt(Recipient.ecdhEs(recoveryKeyID, recoveryKey.privateKey));
    return VaultMetadata.createFromJson(payload);
  }

  public static async createFromJson(payload: MetadataPayload): Promise<VaultMetadata> {
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
   * @param apiURL absolute base URL of the API
   * @param vault the corresponding vault
   * @param memberKey the vault members' AES wrapping key
   * @param recoveryKey the public part of the recovery EC key pair
   * @returns `vault.uvf` file contents
   */
  public async encrypt(apiURL: string, vault: VaultDto, memberKey: MemberKey, recoveryKey: RecoveryKey): Promise<string> {
    const recoveryKeyID = `org.cryptomator.hub.recoverykey.${await getJwkThumbprintStr(recoveryKey.publicKey)}`;
    const protectedHeader: JWEHeader = {
      origin: `${apiURL}/vaults/${vault.id}/uvf/vault.uvf`,
      jku: 'jwks.json' // URL relative to origin
    };
    const jwe = await JWE.build(this.payload(), protectedHeader).encrypt(Recipient.a256kw('org.cryptomator.hub.memberkey', memberKey.key), Recipient.ecdhEs(recoveryKeyID, recoveryKey.publicKey));
    const json = jwe.jsonSerialization();
    return JSON.stringify(json);
  }

  public payload(): MetadataPayload {
    const encodedSeeds: Record<string, string> = {};
    for (const [key, value] of this.seeds) {
      const seedId = stringifySeedId(key);
      encodedSeeds[seedId] = base64url.stringify(value);
    }
    return {
      fileFormat: 'AES-256-GCM-32k',
      nameFormat: 'AES-SIV-512-B64URL',
      seeds: encodedSeeds,
      initialSeed: stringifySeedId(this.initialSeedId),
      latestSeed: stringifySeedId(this.latestSeedId),
      kdf: 'HKDF-SHA512',
      kdfSalt: base64url.stringify(this.kdfSalt),
      'org.cryptomator.automaticAccessGrant': this.automaticAccessGrant
    };
  }
}
// #endregion
// #region UVF

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

  public static async forTesting(metadata: VaultMetadata) {
    const memberKey = await MemberKey.create();
    const recoveryKey = await RecoveryKey.create();
    return new UniversalVaultFormat(metadata, memberKey, recoveryKey);
  }

  /**
   * Decrypts a UVF vault.
   * @param vault The vault to decrypt
   * @param accessToken The vault member's access token
   * @param userKeyPair THe vault member's key pair
   * @returns The decrypted vault
   */
  public static async decrypt(vault: VaultDto, accessToken: string, userKeyPair: UserKeys): Promise<UniversalVaultFormat> {
    if (!vault.uvfMetadataFile || !vault.uvfKeySet) {
      throw new Error('Not a UVF vault.');
    }
    const jwks = JSON.parse(vault.uvfKeySet) as JsonWebKeySet;
    const recoveryPublicKey = await this.getRecoveryPublicKeyFromJwks(jwks);
    const payload = await userKeyPair.decryptAccessToken(accessToken) as UvfAccessTokenPayload;
    const memberKey = await MemberKey.load(payload.key);
    const metadata = await VaultMetadata.decryptWithMemberKey(vault.uvfMetadataFile, memberKey);
    let recoveryKey: RecoveryKey;
    if (payload.recoveryKey) {
      recoveryKey = await RecoveryKey.import(recoveryPublicKey, base64.parse(payload.recoveryKey));
    } else {
      recoveryKey = await RecoveryKey.import(recoveryPublicKey);
    }
    return new UniversalVaultFormat(metadata, memberKey, recoveryKey);
  }

  /**
   * Recovery the `vault.uvf` file using the recovery key. After recovery, all access tokens need to be re-issued.
   * @param uvfMetadataFile contents of the `vault.uvf` file
   * @param recoveryKey the vault's recovery key encoded into human-readable words
   * @returns The recovered vault
   */
  public static async recover(uvfMetadataFile: string, recoveryKey: string): Promise<UniversalVaultFormat> {
    const recoveryKeyPair = await RecoveryKey.recover(recoveryKey);
    const metadata = await VaultMetadata.decryptWithRecoveryKey(uvfMetadataFile, recoveryKeyPair);
    const memberKey = await MemberKey.create();
    return new UniversalVaultFormat(metadata, memberKey, recoveryKeyPair);
  }

  private static async getRecoveryPublicKeyFromJwks(jwks: JsonWebKeySet): Promise<CryptoKey> {
    for (const key of jwks.keys) {
      if (key.kid?.startsWith('org.cryptomator.hub.recoverykey.')) {
        const thumbprint = await getJwkThumbprintStr(key as JsonWebKey);
        if (key.kid === `org.cryptomator.hub.recoverykey.${thumbprint}`) {
          return await crypto.subtle.importKey('jwk', key as JsonWebKey, RecoveryKey.KEY_DESIGNATION, true, []);
        }
      }
    }
    throw new Error('Recovery key not found in JWKS');
  }

  /**
   * Creates the `vault.uvf` file
   * @param apiURL absolute base URL of the API
   * @param vault the vault
   * @returns `vault.uvf` file contents
   */
  public async createMetadataFile(apiURL: string, vault: VaultDto): Promise<string> {
    return this.metadata.encrypt(apiURL, vault, this.memberKey, this.recoveryKey);
  }

  public async computeRootDirId(): Promise<Uint8Array> {
    const textencoder = new TextEncoder();
    const initialSeed = await crypto.subtle.importKey('raw', this.metadata.initialSeed, { name: 'HKDF' }, false, ['deriveBits']);
    const rootDirId = await crypto.subtle.deriveBits({ name: 'HKDF', hash: 'SHA-512', salt: this.metadata.kdfSalt, info: textencoder.encode('rootDirId') }, initialSeed, 256);
    return new Uint8Array(rootDirId);
  }

  public async computeRootDirIdHash(rootDirId: Uint8Array): Promise<string> {
    const textencoder = new TextEncoder();
    const initialSeed = await crypto.subtle.importKey('raw', this.metadata.initialSeed, { name: 'HKDF' }, false, ['deriveKey']);
    const hmacKey = await crypto.subtle.deriveKey({ name: 'HKDF', hash: 'SHA-512', salt: this.metadata.kdfSalt, info: textencoder.encode('hmac') }, initialSeed, { name: 'HMAC', hash: 'SHA-256', length: 256 }, false, ['sign']);
    const rootDirHash = await crypto.subtle.sign('HMAC', hmacKey, rootDirId);
    return base32.stringify(new Uint8Array(rootDirHash).slice(0, 20));
  }

  public async encryptFile(content: Uint8Array, seedId: number): Promise<Uint8Array> {
    const seed = this.metadata.seeds.get(seedId);
    if (!seed) {
      throw new Error('Seed not found');
    }
    if (content.length > 32 * 1024) {
      throw new Error('Only files up to 32k are supported.');
    }
    const textencoder = new TextEncoder();
    const fileKey = await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt']);

    // general header:
    const generalHeader = new ArrayBuffer(8);
    const view = new DataView(generalHeader);
    view.setUint32(0, 0x75766601); // magic bytes "uvf1"
    view.setUint32(4, seedId);

    // format-specific header:
    const initialSeed = await crypto.subtle.importKey('raw', this.metadata.initialSeed, { name: 'HKDF' }, false, ['deriveKey']);
    const headerKey = await crypto.subtle.deriveKey({ name: 'HKDF', hash: 'SHA-512', salt: this.metadata.kdfSalt, info: textencoder.encode('fileHeader') }, initialSeed, { name: 'AES-GCM', length: 256 }, false, ['wrapKey']);
    const headerNonce = new Uint8Array(12);
    crypto.getRandomValues(headerNonce);
    const encryptedFileKeyAndTag = await crypto.subtle.wrapKey('raw', fileKey, headerKey, { name: 'AES-GCM', iv: headerNonce, additionalData: generalHeader });

    // complete header:
    const header = new Uint8Array([...new Uint8Array(generalHeader), ...headerNonce, ...new Uint8Array(encryptedFileKeyAndTag)]);

    // encrypt chunk 0:
    const blockNonce = new Uint8Array(12);
    crypto.getRandomValues(blockNonce);
    const blockAd = new Uint8Array([0x00, 0x00, 0x00, 0x00, ...headerNonce]);
    const blockCiphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv: blockNonce, additionalData: blockAd }, fileKey, content);

    // result:
    return new Uint8Array([...header, ...blockNonce, ...new Uint8Array(blockCiphertext)]);
  }

  /** @inheritdoc */
  public async exportTemplate(apiURL: string, vault: VaultDto): Promise<Blob> {
    const rootDirId = await this.computeRootDirId();
    const rootDirHash = await this.computeRootDirIdHash(rootDirId);
    const dirFile = await this.encryptFile(rootDirId, this.metadata.initialSeedId);
    const zip = new JSZip();
    zip.file('vault.uvf', this.createMetadataFile(apiURL, vault));
    const rootDir = zip.folder('d')?.folder(rootDirHash.substring(0, 2))?.folder(rootDirHash.substring(2)); // TODO verify after merging https://github.com/encryption-alliance/unified-vault-format/pull/24
    rootDir?.file('dir.uvf', dirFile);
    return zip.generateAsync({ type: 'blob' });
  }

  /** @inheritdoc */
  public async encryptForUser(userPublicKey: CryptoKey | Uint8Array, isOwner?: boolean): Promise<string> {
    const payload: UvfAccessTokenPayload = {
      key: await this.memberKey.serializeKey(),
      recoveryKey: isOwner && this.recoveryKey.privateKey ? await this.recoveryKey.serializePrivateKey() : undefined
    };
    return OtherVaultMember.withPublicKey(userPublicKey).createAccessToken(payload);
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
  const bytes = new Uint8Array(ints.buffer);
  return base64url.stringify(bytes, { pad: false });
}

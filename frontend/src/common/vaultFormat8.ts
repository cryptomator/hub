import JSZip from 'jszip';
import * as miscreant from 'miscreant';
import { base32, base64, base64url } from 'rfc4648';
import { VaultDto } from './backend';
import config, { absFrontendBaseURL } from './config';
import { AccessTokenProducing, GCM_NONCE_LEN, OtherVaultMember, UnwrapKeyError, UserKeys, VaultTemplateProducing } from './crypto';
import { CRC32, wordEncoder } from './util';

interface VaultConfigPayload {
  jti: string
  format: number
  cipherCombo: string
  shorteningThreshold: number
}

interface VaultConfigHeaderHub {
  clientId: string
  authEndpoint: string
  tokenEndpoint: string
  authSuccessUrl: string
  authErrorUrl: string
  apiBaseUrl: string
  // deprecated:
  devicesResourceUrl: string
}


export class VaultFormat8 implements AccessTokenProducing, VaultTemplateProducing {
  // in this browser application, this 512 bit key is used
  // as a hmac key to sign the vault config.
  // however when used by cryptomator, it gets split into
  // a 256 bit encryption key and a 256 bit mac key
  private static readonly MASTERKEY_KEY_DESIGNATION: HmacImportParams | HmacKeyGenParams = {
    name: 'HMAC',
    hash: 'SHA-256',
    length: 512
  };

  readonly masterKey: CryptoKey;

  protected constructor(masterKey: CryptoKey) {
    this.masterKey = masterKey;
  }

  /**
   * Creates a new masterkey
   * @returns A new masterkey
   */
  public static async create(): Promise<VaultFormat8> {
    const key = crypto.subtle.generateKey(
      VaultFormat8.MASTERKEY_KEY_DESIGNATION,
      true,
      ['sign']
    );
    return new VaultFormat8(await key);
  }


  /**
   * Decrypts the vault's masterkey using the user's private key
   * @param jwe JWE containing the vault key
   * @param userKeyPair The current user's key pair
   * @returns The masterkey
   */
  public static async decryptWithUserKey(jwe: string, userKeyPair: UserKeys): Promise<VaultFormat8> {
    let rawKey = new Uint8Array();
    try {
      const payload = await userKeyPair.decryptAccessToken(jwe);
      rawKey = base64.parse(payload.key);
      const masterKey = crypto.subtle.importKey('raw', rawKey, VaultFormat8.MASTERKEY_KEY_DESIGNATION, true, ['sign']);
      return new VaultFormat8(await masterKey);
    } finally {
      rawKey.fill(0x00);
    }
  }

  /**
   * Unwraps keys protected by the legacy "Vault Admin Password".
   * @param vaultAdminPassword Vault Admin Password
   * @param wrappedMasterkey The wrapped masterkey
   * @param wrappedOwnerPrivateKey The wrapped owner private key
   * @param ownerPublicKey The owner public key
   * @param salt PBKDF2 Salt
   * @param iterations PBKDF2 Iterations
   * @returns The unwrapped key material.
   * @throws WrongPasswordError, if the wrong password is used
   * @deprecated Only used during "claim vault ownership" workflow for legacy vaults
   */
  public static async decryptWithAdminPassword(vaultAdminPassword: string, wrappedMasterkey: string, wrappedOwnerPrivateKey: string, ownerPublicKey: string, salt: string, iterations: number): Promise<[VaultFormat8, CryptoKeyPair]> {
    // pbkdf2:
    const encodedPw = new TextEncoder().encode(vaultAdminPassword);
    const pwKey = crypto.subtle.importKey('raw', encodedPw, 'PBKDF2', false, ['deriveKey']);
    const kek = crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        hash: 'SHA-256',
        salt: base64.parse(salt, { loose: true }),
        iterations: iterations
      },
      await pwKey,
      { name: 'AES-GCM', length: 256 },
      false,
      ['unwrapKey']
    );
    // unwrapping
    const decodedMasterKey = base64.parse(wrappedMasterkey, { loose: true });
    const decodedPrivateKey = base64.parse(wrappedOwnerPrivateKey, { loose: true });
    const decodedPublicKey = base64.parse(ownerPublicKey, { loose: true });
    try {
      const masterkey = crypto.subtle.unwrapKey(
        'raw',
        decodedMasterKey.slice(GCM_NONCE_LEN),
        await kek,
        { name: 'AES-GCM', iv: decodedMasterKey.slice(0, GCM_NONCE_LEN) },
        VaultFormat8.MASTERKEY_KEY_DESIGNATION,
        true,
        ['sign']
      );
      const privKey = crypto.subtle.unwrapKey(
        'pkcs8',
        decodedPrivateKey.slice(GCM_NONCE_LEN),
        await kek,
        { name: 'AES-GCM', iv: decodedPrivateKey.slice(0, GCM_NONCE_LEN) },
        { name: 'ECDSA', namedCurve: 'P-384' },
        false,
        ['sign']
      );
      const pubKey = crypto.subtle.importKey(
        'spki',
        decodedPublicKey,
        { name: 'ECDSA', namedCurve: 'P-384' },
        true,
        ['verify']
      );
      return [new VaultFormat8(await masterkey), { privateKey: await privKey, publicKey: await pubKey }];
    } catch (error) {
      throw new UnwrapKeyError(error);
    }
  }

  /**
   * Restore the master key from a given recovery key and verify the masterkey matches with the given vault config. On success, creates a new admin signature key pair.
   * @param vaultMetadataToken Content of the alleged matching vault metadata file vault.cryptomator
   * @param recoveryKey The recovery key
   * @returns The recovered master key
   * @throws Error, if passing a malformed recovery key or the recovery key does not match with the vault metadata
   */
  public static async recoverAndVerify(vaultMetadataToken: string, recoveryKey: string) {
    const vault = await this.recover(recoveryKey);

    const sigSeparatorIndex = vaultMetadataToken.lastIndexOf('.');
    const headerPlusPayload = vaultMetadataToken.slice(0, sigSeparatorIndex);
    const signature = vaultMetadataToken.slice(sigSeparatorIndex + 1, vaultMetadataToken.length);
    const message = new TextEncoder().encode(headerPlusPayload);
    var digest = await crypto.subtle.sign(
      VaultFormat8.MASTERKEY_KEY_DESIGNATION,
      vault.masterKey,
      message
    );
    const base64urlDigest = base64url.stringify(new Uint8Array(digest), { pad: false });
    if (!(signature === base64urlDigest)) {
      throw new Error('Recovery key does not match vault file.');
    }

    return vault;
  }

  /**
   * Restore the master key from a given recovery key, create a new admin signature key pair.
   * @param recoveryKey The recovery key
   * @returns The recovered master key
   * @throws DecodeVf8RecoveryKeyError, if passing a malformed recovery key
   */
  public static async recover(recoveryKey: string): Promise<VaultFormat8> {
    // decode and check recovery key:
    let decoded;
    try {
      decoded = wordEncoder.decode(recoveryKey);
    } catch (error) {
      throw new DecodeVf8RecoveryKeyError(error instanceof Error ? error.message : 'Internal error. See console log for more info.');
    }

    if (decoded.length !== 66) {
      throw new DecodeVf8RecoveryKeyError('Invalid recovery key length.');
    }
    const decodedKey = decoded.subarray(0, 64);
    const crc32 = CRC32.compute(decodedKey);
    if (decoded[64] !== (crc32 & 0xFF)
      || decoded[65] !== (crc32 >> 8 & 0xFF)) {
      throw new DecodeVf8RecoveryKeyError('Invalid recovery key checksum.');
    }

    // construct new VaultKeys from recovered key
    const key = crypto.subtle.importKey(
      'raw',
      decodedKey,
      VaultFormat8.MASTERKEY_KEY_DESIGNATION,
      true,
      ['sign']
    );
    return new VaultFormat8(await key);
  }

  /** @inheritdoc */
  public async exportTemplate(apiURL: string, vault: VaultDto): Promise<Blob> {
    const cfg = await config;

    const kid = `hub+${apiURL}vaults/${vault.id}`;

    const hubConfig: VaultConfigHeaderHub = {
      clientId: cfg.keycloakClientIdCryptomator,
      authEndpoint: cfg.keycloakAuthEndpoint,
      tokenEndpoint: cfg.keycloakTokenEndpoint,
      authSuccessUrl: `${absFrontendBaseURL}unlock-success?vault=${vault.id}`,
      authErrorUrl: `${absFrontendBaseURL}unlock-error?vault=${vault.id}`,
      apiBaseUrl: apiURL,
      devicesResourceUrl: `${apiURL}devices/`,
    };

    const jwtPayload: VaultConfigPayload = {
      jti: vault.id,
      format: 8,
      cipherCombo: 'SIV_GCM',
      shorteningThreshold: 220
    };

    const vaultConfigToken = await this.createVaultConfig(kid, hubConfig, jwtPayload);
    const rootDirHash = await this.hashDirectoryId('');

    const zip = new JSZip();
    zip.file('vault.cryptomator', vaultConfigToken);
    zip.folder('d')?.folder(rootDirHash.substring(0, 2))?.folder(rootDirHash.substring(2));
    return zip.generateAsync({ type: 'blob' });
  }

  private async createVaultConfig(kid: string, hubConfig: VaultConfigHeaderHub, payload: VaultConfigPayload): Promise<string> {
    const header = JSON.stringify({
      kid: kid,
      typ: 'jwt',
      alg: 'HS256',
      hub: hubConfig
    });
    const payloadJson = JSON.stringify(payload);
    const encoder = new TextEncoder();
    const unsignedToken = base64url.stringify(encoder.encode(header), { pad: false }) + '.' + base64url.stringify(encoder.encode(payloadJson), { pad: false });
    const encodedUnsignedToken = new TextEncoder().encode(unsignedToken);
    const signature = await crypto.subtle.sign(
      'HMAC',
      this.masterKey,
      encodedUnsignedToken
    );
    return unsignedToken + '.' + base64url.stringify(new Uint8Array(signature), { pad: false });
  }

  // visible for testing
  public async hashDirectoryId(cleartextDirectoryId: string): Promise<string> {
    const dirHash = new TextEncoder().encode(cleartextDirectoryId);
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.masterKey));
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

  /**
   * Encodes the key masterkey
   * @returns master key in base64-encoded raw format
   */
  public async serializeMasterKey(): Promise<string> {
    const bytes = await crypto.subtle.exportKey('raw', this.masterKey);
    return base64.stringify(new Uint8Array(bytes), { pad: true });
  }

  /** @inheritdoc */
  public async encryptForUser(userPublicKey: CryptoKey | Uint8Array): Promise<string> {
    return OtherVaultMember.withPublicKey(userPublicKey).createAccessToken({
      key: await this.serializeMasterKey(),
    });
  }

  /**
   * Encode masterkey for offline backup purposes, allowing re-importing the key for recovery purposes
   */
  public async createRecoveryKey(): Promise<string> {
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.masterKey));

    // add 16 bit checksum:
    const crc32 = CRC32.compute(rawkey);
    const checksum = new Uint8Array(2);
    checksum[0] = crc32 & 0xff;      // append the least significant byte of the crc
    checksum[1] = crc32 >> 8 & 0xff; // followed by the second-least significant byte
    const combined = new Uint8Array([...rawkey, ...checksum]);

    // encode using human-readable words:
    return wordEncoder.encodePadded(combined);
  }
}


export class DecodeVf8RecoveryKeyError extends Error {
  constructor(message: string) {
    super(message);
  }
}

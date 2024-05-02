import JSZip from 'jszip';
import * as miscreant from 'miscreant';
import { base32, base64, base64url } from 'rfc4648';
import { VaultDto } from './backend';
import config, { absBackendBaseURL, absFrontendBaseURL } from './config';
import { AccessTokenProducing, GCM_NONCE_LEN, UnwrapKeyError, UserKeys, VaultTemplateProducing } from './crypto';
import { JWE, Recipient } from './jwe';
import { CRC32, wordEncoder } from './util';

interface JWEPayload {
  key: string
}

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


export class VaultKeys implements AccessTokenProducing, VaultTemplateProducing {
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
  public static async create(): Promise<VaultKeys> {
    const key = crypto.subtle.generateKey(
      VaultKeys.MASTERKEY_KEY_DESIGNATION,
      true,
      ['sign']
    );
    return new VaultKeys(await key);
  }


  /**
   * Decrypts the vault's masterkey using the user's private key
   * @param jwe JWE containing the vault key
   * @param userPrivateKey The user's private key
   * @returns The masterkey
   */
  public static async decryptWithUserKey(jwe: string, userPrivateKey: CryptoKey): Promise<VaultKeys> {
    let rawKey = new Uint8Array();
    try {
      const payload: JWEPayload = await JWE.parseCompact(jwe).decrypt(Recipient.ecdhEs('org.cryptomator.hub.userkey', userPrivateKey));
      rawKey = base64.parse(payload.key);
      const masterKey = crypto.subtle.importKey('raw', rawKey, VaultKeys.MASTERKEY_KEY_DESIGNATION, true, ['sign']);
      return new VaultKeys(await masterKey);
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
  public static async decryptWithAdminPassword(vaultAdminPassword: string, wrappedMasterkey: string, wrappedOwnerPrivateKey: string, ownerPublicKey: string, salt: string, iterations: number): Promise<[VaultKeys, CryptoKeyPair]> {
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
        VaultKeys.MASTERKEY_KEY_DESIGNATION,
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
      return [new VaultKeys(await masterkey), { privateKey: await privKey, publicKey: await pubKey }];
    } catch (error) {
      throw new UnwrapKeyError(error);
    }
  }

  /**
   * Restore the master key from a given recovery key, create a new admin signature key pair.
   * @param recoveryKey The recovery key
   * @returns The recovered master key
   * @throws Error, if passing a malformed recovery key
   */
  public static async recover(recoveryKey: string): Promise<VaultKeys> {
    // decode and check recovery key:
    const decoded = wordEncoder.decode(recoveryKey);
    if (decoded.length !== 66) {
      throw new Error('Invalid recovery key length.');
    }
    const decodedKey = decoded.subarray(0, 64);
    const crc32 = CRC32.compute(decodedKey);
    if (decoded[64] !== (crc32 & 0xFF)
      || decoded[65] !== (crc32 >> 8 & 0xFF)) {
      throw new Error('Invalid recovery key checksum.');
    }

    // construct new VaultKeys from recovered key
    const key = crypto.subtle.importKey(
      'raw',
      decodedKey,
      VaultKeys.MASTERKEY_KEY_DESIGNATION,
      true,
      ['sign']
    );
    return new VaultKeys(await key);
  }

  public async exportTemplate(vault: VaultDto): Promise<Blob> {
    const cfg = await config;

    const kid = `hub+${absBackendBaseURL}vaults/${vault.id}`;

    const hubConfig: VaultConfigHeaderHub = {
      clientId: cfg.keycloakClientIdCryptomator,
      authEndpoint: cfg.keycloakAuthEndpoint,
      tokenEndpoint: cfg.keycloakTokenEndpoint,
      authSuccessUrl: `${absFrontendBaseURL}unlock-success?vault=${vault.id}`,
      authErrorUrl: `${absFrontendBaseURL}unlock-error?vault=${vault.id}`,
      apiBaseUrl: absBackendBaseURL,
      devicesResourceUrl: `${absBackendBaseURL}devices/`,
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
   * Encrypts this masterkey using the given public key
   * @param userPublicKey The recipient's public key (DER-encoded)
   * @returns a JWE containing this Masterkey
   */
  public async encryptForUser(userPublicKey: Uint8Array): Promise<string> {
    const publicKey = await crypto.subtle.importKey('spki', userPublicKey, UserKeys.KEY_DESIGNATION, false, []);
    const rawkey = new Uint8Array(await crypto.subtle.exportKey('raw', this.masterKey));
    try {
      const payload: JWEPayload = {
        key: base64.stringify(rawkey),
      };
      const jwe = await JWE.build(payload).encrypt(Recipient.ecdhEs('org.cryptomator.hub.userkey', publicKey));
      return jwe.compactSerialization();
    } finally {
      rawkey.fill(0x00);
    }
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

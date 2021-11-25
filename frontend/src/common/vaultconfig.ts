import JSZip from 'jszip';
import config from '../common/config';
import { Masterkey, VaultConfigHeaderHub, VaultConfigPayload } from '../common/crypto';

export class VaultConfig {

  readonly vaultConfigToken: string;
  private readonly rootDirHash: string;

  private constructor(vaultConfigToken: string, rootDirHash: string) {
    this.vaultConfigToken = vaultConfigToken;
    this.rootDirHash = rootDirHash;
  }

  public static async create(vaultId: string, masterkey: Masterkey): Promise<VaultConfig> {
    const kid = `hub+http://localhost:9090/vaults/${vaultId}`;

    const hubConfig: VaultConfigHeaderHub = {
      clientId: 'cryptomator-hub',
      authEndpoint: `${config.get().keycloakUrl}realms/cryptomator/protocol/openid-connect/auth`, // TODO: read full endpoint url from config
      tokenEndpoint: `${config.get().keycloakUrl}realms/cryptomator/protocol/openid-connect/token`,
      deviceRegistrationUrl: `${location.protocol}//${location.host}${import.meta.env.BASE_URL}#/devices/add`,
      authSuccessUrl: `${location.protocol}//${location.host}${import.meta.env.BASE_URL}#/unlock-success`,
      authErrorUrl: `${location.protocol}//${location.host}${import.meta.env.BASE_URL}#/unlock-error`
    };

    const jwtPayload: VaultConfigPayload = {
      jti: vaultId,
      format: 8,
      cipherCombo: 'SIV_GCM',
      shorteningThreshold: 220
    };

    const vaultConfigToken = await masterkey.createVaultConfig(kid, hubConfig, jwtPayload);
    const rootDirHash = await masterkey.hashDirectoryId('');
    return new VaultConfig(vaultConfigToken, rootDirHash);
  }

  public async exportTemplate(): Promise<Blob> {
    const zip = new JSZip();
    zip.file('vault.cryptomator', this.vaultConfigToken);
    zip.folder('d')?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2));
    return await zip.generateAsync({ type: 'blob' });
  }

}

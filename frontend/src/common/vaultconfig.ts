import JSZip from 'jszip';
import config, { backendBaseURL, frontendBaseURL } from '../common/config';
import { Masterkey, VaultConfigHeaderHub, VaultConfigPayload } from '../common/crypto';

export class VaultConfig {

  readonly vaultConfigToken: string;
  private readonly rootDirHash: string;

  private constructor(vaultConfigToken: string, rootDirHash: string) {
    this.vaultConfigToken = vaultConfigToken;
    this.rootDirHash = rootDirHash;
  }

  public static async create(vaultId: string, masterkey: Masterkey): Promise<VaultConfig> {
    const cfg = config.get();

    const kid = `hub+${backendBaseURL}/vaults/${vaultId}`;

    const hubConfig: VaultConfigHeaderHub = {
      clientId: cfg.keycloakClientId,
      authEndpoint: `${cfg.keycloakUrl}/realms/${cfg.keycloakRealm}/protocol/openid-connect/auth`, // TODO: read from config
      tokenEndpoint: `${cfg.keycloakUrl}/realms/${cfg.keycloakRealm}/protocol/openid-connect/token`, // TODO: read from config
      devicesResourceUrl: `${backendBaseURL}/devices/`,
      authSuccessUrl: `${frontendBaseURL}/unlock-success?vault=${vaultId}`,
      authErrorUrl: `${frontendBaseURL}/unlock-error?vault=${vaultId}`
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
    return zip.generateAsync({ type: 'blob' });
  }

}

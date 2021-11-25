import JSZip from 'jszip';
import config from '../common/config';
import { Masterkey, VaultConfigHeaderHub, VaultConfigPayload } from '../common/crypto';

export async function createVaultConfig(vaultId: string, masterkey: Masterkey): Promise<string> {
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

  return await masterkey.createVaultConfig(kid, hubConfig, jwtPayload);
}


export async function createVaultTemplate(rootDirHash: string, vaultConfigToken: string): Promise<Blob> {
  const zip = new JSZip();
  zip.file('vault.cryptomator', vaultConfigToken);
  zip.folder('d')?.folder(rootDirHash.substring(0, 2))?.folder(rootDirHash.substring(2));
  return await zip.generateAsync({ type: 'blob' });

}

import backend, { VaultDto } from './backend';
import { AccessTokenProducing } from './crypto';
import { UniversalVaultFormat } from './universalVaultFormat';
import { VaultFormat8 } from './vaultFormat8';
import userdata from './userdata';

/**
 * Fetches the current user's access token for the given vault and decrypts it into the vault keys, transparently
 * handling both the Universal Vault Format and the legacy Vault Format 8 (chosen by the presence of a UVF metadata
 * file on the vault). The returned object can wrap the vault key for other users via
 * {@link AccessTokenProducing#encryptForUser}.
 *
 * Requires the current user to be a member of the vault (otherwise the access token request is rejected) and to have
 * unlocked their user keys in this browser.
 *
 * @param vault the vault whose keys to unwrap
 * @returns the decrypted vault keys
 */
export async function unwrapVaultKeys(vault: VaultDto & { uvfMetadataFile: string }): Promise<UniversalVaultFormat>;
export async function unwrapVaultKeys(vault: VaultDto & { uvfMetadataFile: undefined }): Promise<VaultFormat8>;
export async function unwrapVaultKeys(vault: VaultDto): Promise<AccessTokenProducing>;
export async function unwrapVaultKeys(vault: VaultDto): Promise<AccessTokenProducing> {
  const deviceId = await (await userdata.browserKeys)?.id();
  const accessToken = await backend.vaults.accessToken(vault.id, deviceId, true);
  const userKeys = await userdata.decryptUserKeysWithBrowser();
  if (vault.uvfMetadataFile) {
    return UniversalVaultFormat.decrypt(vault, accessToken, userKeys);
  } else {
    return VaultFormat8.decryptWithUserKey(accessToken, userKeys);
  }
}

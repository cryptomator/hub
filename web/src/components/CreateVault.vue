<template>
  <h1>Create new vault</h1>
  <div v-if="state === State.Processing">
    <input v-model="vaultName" type="text" placeholder="Vault Name"/>
    <input v-model="password" type="password" placeholder="Masterpwassword"/>
    
    <button type="button" @click="createVault()">createVault</button>
  </div>
  <div v-else-if="state === State.Created">
    <h2>Vault created!</h2>

    <button type="button" @click="createVaultFolder()">Download zipped vault folder</button>
  </div>
</template>

<script lang="ts">
import backend from '../common/backend'
import { uuid } from '../common/util'
import { Masterkey, VaultConfigHeaderHub, VaultConfigPayload } from '../common/crypto'
import { defineComponent } from 'vue'
import JSZip from 'jszip';
import { saveAs } from 'file-saver';

enum State {
  Processing,
  Created
}

export default defineComponent({
  data: () => ({
    State,
    state: State.Processing as State,
    password: '' as string,
    vaultName: '' as string,
    token: '' as string,
    rootDirHash: '' as string,
  }),

  methods: {
    async createVault() {
      const masterkey = await Masterkey.create();
      const vaultId = uuid();
      const kid = `hub+http://localhost:9090/vaults/${vaultId}`

      const hubConfig: VaultConfigHeaderHub = {
        clientId: 'cryptomator-hub',
        authEndpoint: 'http://localhost:8080/auth/realms/cryptomator/protocol/openid-connect/auth',
        tokenEndpoint: 'http://localhost:8080/auth/realms/cryptomator/protocol/openid-connect/token',
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

      this.token = await masterkey.createVaultConfig(kid, hubConfig, jwtPayload);
      const wrapped = await masterkey.wrap(this.password);
      backend.vaults.createVault(vaultId, this.vaultName, wrapped.encrypted, wrapped.iterations, wrapped.salt).then(() => {
        masterkey.hashDirectoryId("").then(rootDirHash => {
          this.rootDirHash = rootDirHash
          this.state = State.Created
        })
      })
    },
    async createVaultFolder() {
      if(this.state === State.Created) {
        const zip = new JSZip()
        zip.file("vault.cryptomator", this.token)
        zip.folder("d")?.folder(this.rootDirHash.substring(0, 2))?.folder(this.rootDirHash.substring(2))
        zip.generateAsync({type: "blob"}).then((blob) => {
          saveAs(blob, `${this.vaultName}.zip`);
        })
      }
    }
  }
})
</script>

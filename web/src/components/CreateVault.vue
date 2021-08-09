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
import { Masterkey } from '../common/crypto'
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
  }),

  methods: {
    async createVault() {
      const masterkey = await Masterkey.create();
      const vaultId = uuid();
      const hubUrl = `hub/${location.protocol}//${location.hostname}:${location.port}/vault/${vaultId}`
      this.token = await masterkey.createVaultConfig(vaultId, hubUrl);
      const wrapped = await masterkey.wrap(this.$data.password);
      backend.vaults.createVault(vaultId, this.$data.vaultName, wrapped.encrypted, wrapped.iterations, wrapped.salt).then(() => {
        this.$data.state = State.Created
      })
    },
    async createVaultFolder() {
      if(this.$data.state === State.Created) {
        const zip = new JSZip()
        zip.file("vault.cryptomator", this.token)
        zip.folder("d")
        zip.generateAsync({type: "blob"}).then((blob) => {
          saveAs(blob, `${this.$data.vaultName}.zip`);
        })
      }
    }
  }
})
</script>

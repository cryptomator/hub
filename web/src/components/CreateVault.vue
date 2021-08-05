<template>
  <h1>Create new vault</h1>

  <input v-model="vaultName" type="text" placeholder="Vault Name"/>
  <input v-model="password" type="password" placeholder="Masterpwassword"/>

  <button type="button" @click="createVault()">createVault</button>
</template>

<script lang="ts">
import backend from '../common/backend'
import { uuid } from '../common/util'
import { Masterkey } from '../common/crypto'
import { defineComponent } from 'vue'

export default defineComponent({
  data: () => ({
    password: '' as string,
    vaultName: '' as string,
  }),

  methods: {
    async createVault() {
      const masterkey = await Masterkey.create();
      const vaultId = uuid();
      const hubUrl = 'hub+' + location.protocol + '//' + location.hostname + ':' + location.port + '/vault/' + vaultId
      const token = await masterkey.createVaultConfig(vaultId, hubUrl);
      console.log("vault config: ", token);
      const wrapped = await masterkey.wrap(this.$data.password);
      backend.vaults.createVault(vaultId, this.$data.vaultName, wrapped.encrypted, wrapped.iterations, wrapped.salt).then(() => {
        alert("Vault created")
      })
    }
  }
})
</script>

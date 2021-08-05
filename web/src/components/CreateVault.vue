<template>
  <h1>Create new vault</h1>

  <input v-model="vaultName" type="text" placeholder="Vault Name"/>
  <input v-model="password" type="password" placeholder="Masterpwassword"/>

  <button type="button" @click="createVault()">createVault</button>
</template>

<script lang="ts">
import backend from '../common/backend'
import { uuid } from '../common/util'
import { Vault } from '../common/vault'
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'Other',

  data: () => ({
    password: '' as string,
    vaultName: '' as string,
  }),

  methods: {
    createVault() {
      const vault = Vault.create();
      const vaultId = uuid();
      const hubUrl = 'hub+' + location.protocol + '//' + location.hostname + ':' + location.port + '/vault/' + vaultId

      vault.createVaultConfig(vaultId, hubUrl).then(token => {
        // const div = document.querySelector<HTMLDivElement>('#jwt')!
        // div.innerHTML = `<b>jwt</b>: <code>${token}</code>`
        console.log("vault config: ", token);
      }).catch(e => {
        console.log("error creating vault config: ", e);
      });
      vault.encryptMasterkey(this.$data.password).then(masterkey => {
        backend.vaults.createVault(vaultId, this.$data.vaultName, masterkey.encrypted, masterkey.iterations, masterkey.salt)
      });
    }
  }
})
</script>

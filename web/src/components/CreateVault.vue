<template>
  <h1>Create new vault</h1>

  <input v-model="vaultName" type="text" placeholder="Vault Name"/>
  <input v-model="password" type="password" placeholder="Masterpwassword"/>

  <button type="button" @click="createVault()">createVault</button>
</template>

<script lang="ts">
import auth from '../common/auth'
import { Vault } from '../common/vault'
import { Spi } from '../common/spi'
import { ref, defineComponent } from 'vue'

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

export default defineComponent({
  name: 'Other',

  data: vm => ({
    password: "",
    vaultName: "",
  }),

  methods: {
    doLogin() {
      auth.loginIfRequired()
    },

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
        new Spi().createVault(vaultId, this.$data.vaultName, masterkey.encrypted, masterkey.iterations, masterkey.salt)
      });
    }
  }
})
</script>

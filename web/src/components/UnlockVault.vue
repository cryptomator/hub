<template>
  <h1>Unlock vault</h1>
</template>

<script lang="ts">
import backend from '../common/backend'
import { defineComponent } from 'vue'
import { AxiosError } from 'axios';

export default defineComponent({
  name: 'Other',
  props: {
    vaultId: {
      type: String,
      default: null
    },
    deviceId: {
      type: String,
      default: null
    },
    deviceKey: {
        type: String,
        default: null
    },
    redirectTo: {
        type: String,
        default: null
    }
  },
  mounted() {
    backend.devices.getDevice(this.deviceId).then(device => {
      if(device.data.publicKey === this.deviceKey) {
        this.getKeyForThisVault()
      } else {
        // FIXME the fingerprint of the key is equal but the public key not, may don't check this?
      }
    }).catch((reason: AxiosError) => {
      if (reason.response?.status === 404) {
        this.createDevice()
      } else {
        console.log(reason.message)
      }
  })
  },
  methods: {
    async createDevice() {
      await backend.devices.createDevice(this.deviceId, "name", this.deviceKey)
      this.getKeyForThisVault()
    },
    async getKeyForThisVault() {
     // const publicKey = await backend.vaults.unlock(this.vaultId, this.deviceId)
     // window.location.href = this.redirectTo
    }
  }
})
</script>

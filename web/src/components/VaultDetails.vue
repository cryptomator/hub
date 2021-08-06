<template>
  <h1>Vault Details</h1>

  <ul>
    <li v-for="device in devices" :key="device.name">{{ device.name }}</li>
  </ul>

  <button @click="giveAccess()">give access</button>
</template>

<script lang="ts">
import backend, { DeviceDto } from '../common/backend'
import { Base64Url } from '../common/util'
import { Masterkey } from '../common/crypto'
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'VaultDetails',
  data: () => ({
    devices: [] as DeviceDto[]
  }),

  mounted() {
    backend.devices.listAll().then(devices => {
      this.$data.devices = devices;
    })
  },

  methods: {
    async giveAccess() {
      const masterkey = await Masterkey.create();
      for (const device of this.$data.devices) {
        const publicKey = Base64Url.decode(device.publicKey);
        console.log(publicKey);
      }
    }
  }
})
</script>

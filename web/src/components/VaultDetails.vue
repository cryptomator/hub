<template>
  <div v-if="vault == null">
    Loading...
  </div>
  <div v-else>
    <h1>Vault Details for {{ vault?.name }}</h1>

    <ul>
      <li v-for="device in devices" :key="device.name">{{ device.name }}</li>
    </ul>

    <input v-model="password" type="password" placeholder="Masterpassword"/>
    <button @click="giveAccess()">give access (see console.log)</button>
  </div>
</template>

<script lang="ts">
import backend, { DeviceDto, VaultDto } from '../common/backend'
import { Base64Url } from '../common/util'
import { Masterkey, WrappedMasterkey } from '../common/crypto'
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'VaultDetails',
  props: {
    vaultId: {
      type: String,
      default: null
    }
  },
  data: () => ({
    password: '' as string,
    devices: [] as DeviceDto[],
    vault: null as VaultDto | null,
  }),

  mounted() {
    backend.vaults.get(this.vaultId).then(vault => {
      this.$data.vault = vault.data;
    });
    backend.devices.listAll().then(devices => {
      this.$data.devices = devices;
    });
  },

  methods: {
    async giveAccess() {
      try {
        const vaultDto = this.$data.vault!;
        const wrappedKey = new WrappedMasterkey(vaultDto.masterkey, vaultDto.salt, vaultDto.iterations);
        const masterkey = await Masterkey.unwrap(this.$data.password, wrappedKey);
        for (const device of this.$data.devices) {
          const publicKey = Base64Url.decode(device.publicKey);
          const deviceSpecificKey = await masterkey.encryptForDevice(publicKey);
          await backend.vaults.grantAccess(this.vaultId, device.id, deviceSpecificKey.encrypted, deviceSpecificKey.publicKey);
        }
      } catch (error) {
        console.error('granting access permissions failed.', error);
      }
    }
  }
})
</script>

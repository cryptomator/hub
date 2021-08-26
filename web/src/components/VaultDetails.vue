<template>
  <div v-if="vault == null">
    Loading...
  </div>
  <div v-else>
    <h1>Vault Details for {{ vault?.name }}</h1>

    <h2>Modify access list</h2>
    <ul>
      <li v-for="user in users" :key="user.name">👤 {{ user.name }} <button @click="revokeUserAccess(user.id)">⛔</button> <button @click="giveUserAccess(user)">✅</button>
        <ul v-if="user.devices.length > 0">
          <li v-for="device in user.devices" :key="device.name">📱 {{ device.name }} <button v-if="device.vaultsAccessTo.length > 0 && !device.vaultsAccessTo.includes(vault.id)" @click="revokeDeviceAccess(device.id)">⛔</button> <button v-else @click="giveDeviceAccess(device)">✅</button></li>
        </ul>
      </li>
    </ul>

    <input v-model="password" type="password" placeholder="Masterpassword"/>
  </div>
</template>

<script lang="ts">
import backend, { DeviceDto, UserDto, VaultDto } from '../common/backend'
import { base64url } from "rfc4648";
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
    users: [] as UserDto[],
    vault: null as VaultDto | null,
  }),

  mounted() {
    backend.vaults.get(this.vaultId).then(vault => {
      this.vault = vault.data;
    });
    backend.users.listAllUsersIncludingDevices().then(users => {
      this.users = users
    })
  },

  methods: {
    async giveUserAccess(user: UserDto) {
      for(const device of user.devices) {
        this.giveDeviceAccess(device)
      }
    },

    async giveDeviceAccess(device: DeviceDto) {
      try {
        const vaultDto = this.vault!;
        const wrappedKey = new WrappedMasterkey(vaultDto.masterkey, vaultDto.salt, vaultDto.iterations);
        const masterkey = await Masterkey.unwrap(this.password, wrappedKey);
        const publicKey = base64url.parse(device.publicKey);
        const deviceSpecificKey = await masterkey.encryptForDevice(publicKey);
        await backend.vaults.grantAccess(this.vaultId, device.id, deviceSpecificKey.encrypted, deviceSpecificKey.publicKey);
      } catch (error) {
        console.error('granting access permissions failed.', error);
      }
    },

    async revokeUserAccess(userId: string) {
      try {
        backend.vaults.revokeUserAccess(this.vaultId, userId)
      } catch (error) {
        console.error('revoking access permissions failed.', error);
      }
    },

    async revokeDeviceAccess(deviceId: string) {
      try {
        backend.vaults.revokeDeviceAccess(this.vaultId, deviceId)
      } catch (error) {
        console.error('revoking access permissions failed.', error);
      }
    }
  }
})
</script>

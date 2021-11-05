<template>
  <div v-if="vault == null && errorCode == Error.None">
    Loading...
  </div>
  <div v-else-if="errorCode == Error.NotFound">
    <h1>Vault not found</h1>
  </div>
  <div v-else>
    <h1>{{ t('vault_details_title') }} {{ vault?.name }}</h1>

    <h2>Modify access list</h2>
    <ul>
      <li v-for="user in users" :key="user.name">
        👤 {{ user.name }} <button @click="revokeUserAccess(user.id)">⛔</button> <button @click="giveUserAccess(user)">✅</button>
        <ul v-if="user.devices.length > 0">
          <li v-for="device in user.devices" :key="device.name">📱 {{ device.name }} <button @click="revokeDeviceAccess(device.id)">⛔</button> <button @click="giveDeviceAccess(device)">✅</button></li>
        </ul>
      </li>
    </ul>

    <input v-model="password" type="password" placeholder="Masterpassword"/>
  </div>
</template>

<script lang="ts">
import backend, { DeviceDto, UserDto, VaultDto } from '../common/backend';
import { base64url } from 'rfc4648';
import { Masterkey, WrappedMasterkey } from '../common/crypto';
import { defineComponent } from 'vue';
import { AxiosError } from 'axios';
import { useI18n } from 'vue-i18n';

enum Error {
  None,
  NotFound
}

export default defineComponent({
  name: 'VaultDetails',
  props: {
    vaultId: {
      type: String,
      default: null
    }
  },
  setup() {
    const { t } = useI18n({
      useScope: 'global'
    });
    return { t };
  },
  data: () => ({
    Error,
    errorCode: Error.None as Error,
    password: '' as string,
    users: [] as UserDto[],
    vault: null as VaultDto | null
  }),

  mounted() {
    backend.vaults.get(this.vaultId).then(vault => {
      this.vault = vault.data;
    }).catch((error: AxiosError) => {
      if (error.response?.status === 404) {
        this.errorCode = Error.NotFound;
      }
    });
    backend.users.listAllUsersIncludingDevices().then(users => {
      this.users = users;
    });
  },

  methods: {
    async giveUserAccess(user: UserDto) {
      for(const device of user.devices) {
        this.giveDeviceAccess(device);
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
        backend.vaults.revokeUserAccess(this.vaultId, userId);
      } catch (error) {
        console.error('revoking access permissions failed.', error);
      }
    },

    async revokeDeviceAccess(deviceId: string) {
      try {
        backend.vaults.revokeDeviceAccess(this.vaultId, deviceId);
      } catch (error) {
        console.error('revoking access permissions failed.', error);
      }
    }
  }
});
</script>

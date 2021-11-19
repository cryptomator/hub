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

<script setup lang="ts">
import axios from 'axios';
import { base64url } from 'rfc4648';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { DeviceDto, UserDto, VaultDto } from '../common/backend';
import { Masterkey, WrappedMasterkey } from '../common/crypto';

enum Error {
  None,
  NotFound
}

const props = defineProps<{
  vaultId: string
}>();

const { t } = useI18n({ useScope: 'global' });
const errorCode = ref(Error.None);
const password = ref('');
const users = ref<UserDto[]>([]);
const vault = ref<VaultDto | null>(null);

onMounted(async () => {
  try {
    vault.value = await backend.vaults.get(props.vaultId);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      errorCode.value = Error.NotFound;
    }
  }
  users.value = await backend.users.listAllUsersIncludingDevices();
});

async function giveUserAccess(user: UserDto) {
  for (const device of user.devices) {
    giveDeviceAccess(device);
  }
}

async function giveDeviceAccess(device: DeviceDto) {
  try {
    const vaultDto = vault.value!;
    const wrappedKey = new WrappedMasterkey(vaultDto.masterkey, vaultDto.salt, vaultDto.iterations);
    const masterkey = await Masterkey.unwrap(password.value, wrappedKey);
    const publicKey = base64url.parse(device.publicKey);
    const deviceSpecificKey = await masterkey.encryptForDevice(publicKey);
    await backend.vaults.grantAccess(props.vaultId, device.id, deviceSpecificKey.encrypted, deviceSpecificKey.publicKey);
  } catch (error) {
    console.error('granting access permissions failed.', error);
  }
}

async function revokeUserAccess(userId: string) {
  try {
    backend.vaults.revokeUserAccess(props.vaultId, userId);
  } catch (error) {
    console.error('revoking access permissions failed.', error);
  }
}

async function revokeDeviceAccess(deviceId: string) {
  try {
    backend.vaults.revokeDeviceAccess(props.vaultId, deviceId);
  } catch (error) {
    console.error('revoking access permissions failed.', error);
  }
}
</script>

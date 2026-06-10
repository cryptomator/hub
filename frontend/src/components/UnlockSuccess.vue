<template>
  <AppShell v-if="showSidebar" :me="me!">
    <UnlockSuccessContent :status="status" :error="onFetchError" :retry="fetchData" :has-browser-keys="hasBrowserKeys" />
  </AppShell>

  <div v-else>
    <SimpleNavigationBar v-if="me" :me="me" />
    <UnlockSuccessContent :status="status" :error="onFetchError" :retry="fetchData" :has-browser-keys="hasBrowserKeys" />
  </div>
</template>

<script setup lang="ts">
import { ComputedRef, computed, onMounted, ref } from 'vue';
import backend, { UserDto, VaultDto } from '../common/backend';
import userdata from '../common/userdata';
import AppShell from './AppShell.vue';
import SimpleNavigationBar from './SimpleNavigationBar.vue';
import UnlockSuccessContent, { UnlockStatus } from './UnlockSuccessContent.vue';

const props = defineProps<{
  vaultId: string
  deviceId: string
}>();

const accountState : ComputedRef<AccountState> = computed(() => {
  if (!me.value?.setupCode) {
    return AccountState.RequiresSetup;
  } else {
    return AccountState.Ready;
  }
});

const deviceState : ComputedRef<DeviceState> = computed(() => {
  const foundDevice = me.value?.devices.find(d => d.id === props.deviceId);
  if (!foundDevice) {
    return DeviceState.NoSuchDevice;
  } else {
    return DeviceState.Validated;
  }
});

const vaultAccess : ComputedRef<VaultAccess> = computed(() => {
  const vault = accessibleVaults.value?.find(v => v.id === props.vaultId);
  if (!vault) return VaultAccess.Denied;
  if (vault.archived) return VaultAccess.Archived;
  return VaultAccess.Allowed;
});

enum AccountState {
  RequiresSetup,
  Ready
}

enum DeviceState {
  NoSuchDevice,
  Validated
}

enum VaultAccess {
  Allowed,
  Archived,
  Denied
}

const me = ref<UserDto>();
const hasBrowserKeys = ref<boolean>(false);
const accessibleVaults = ref<VaultDto[]>();
const onFetchError = ref<Error>();

// Only the fully set-up state shows the full app navigation; everything else uses the minimal nav.
const showSidebar = computed(() => me.value !== undefined && accountState.value === AccountState.Ready && hasBrowserKeys.value);

const status : ComputedRef<UnlockStatus> = computed(() => {
  if (me.value === undefined) {
    return onFetchError.value ? 'error' : 'loading';
  }
  if (accountState.value === AccountState.RequiresSetup) {
    return 'accountSetup';
  }
  if (deviceState.value === DeviceState.NoSuchDevice) {
    return 'deviceSetup';
  }
  if (vaultAccess.value === VaultAccess.Archived) {
    return 'archived';
  }
  if (vaultAccess.value === VaultAccess.Denied) {
    return 'denied';
  }
  return 'allowed';
});

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = undefined;
  try {
    me.value = await userdata.me;
    hasBrowserKeys.value = await userdata.browserKeys.then(keys => keys !== undefined);
    accessibleVaults.value = await backend.vaults.listAccessible();
  } catch (error) {
    console.error('Retrieving user information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>

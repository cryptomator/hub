<template>
  <NavigationBar v-if="accountState == AccountState.Ready && browserKeys" :me="me!"/>
  <SimpleNavigationBar v-else-if="me" :me="me"/>

  <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8 flex justify-center">
    <div v-if="me == null">
      <div v-if="onFetchError == null">
        {{ t('common.loading') }}
      </div>
      <div v-else>
        <FetchError :error="onFetchError" :retry="fetchData"/>
      </div>
    </div>

    <div v-else class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
      <div class="flex justify-center mb-3 sm:mb-5">
        <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
      </div>

      <!-- TODO: localize -->

      <!-- ACCOUNT SETUP -->
      <div v-if="accountState == AccountState.RequiresSetup" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          Setup Required
        </h1>
        <p class="my-3">
          To continue, please follow a few simple steps get your account set up.
        </p>
        <router-link to="/app/setup" class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">Complete Setup</router-link>
      </div>

      <!-- DEVICE SETUP -->
      <div v-else-if="deviceState == DeviceState.NoSuchDevice" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          New Device
        </h1>
        <p class="my-3">
          Please enter your account key in Cryptomator to authorize it.
        </p>
        <router-link v-if="browserKeys" to="/app/profile" class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">View Account Key in my Profile</router-link>
      </div>

      <!-- NO VAULT ACCESS -->
      <div v-else-if="vaultAccess == VaultAccess.Denied" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          No access to this Vault
        </h1>
        <p class="mt-2">
          Please contact the vault owner to add your account to the vault members.
        </p>
      </div>

      <!-- SUCCESS -->
      <div v-else class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          Vault unlocked successfully
        </h1>
        <p class="mt-2">
          You may now close this browser tab and return to Cryptomator.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ComputedRef, computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto, VaultDto } from '../common/backend';
import { BrowserKeys } from '../common/crypto';
import FetchError from './FetchError.vue';
import NavigationBar from './NavigationBar.vue';
import SimpleNavigationBar from './SimpleNavigationBar.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vaultId: string
  deviceId: string
}>();

const accountState : ComputedRef<AccountState> = computed(() => {
  const publicKey = me.value?.publicKey;
  if (!publicKey) {
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
  return accessibleVaults.value?.find(v => v.id === props.vaultId)
    ? VaultAccess.Allowed
    : VaultAccess.Denied;
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
  Denied
}

const me = ref<UserDto>();
const browserKeys = ref<boolean>(false);
const accessibleVaults = ref<VaultDto[]>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me(true);
    browserKeys.value = await BrowserKeys.load(me.value.id) != null;
    accessibleVaults.value = await backend.vaults.listAccessible();
  } catch (error) {
    console.error('Retrieving user information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>

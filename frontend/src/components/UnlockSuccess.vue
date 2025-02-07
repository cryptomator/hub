<template>
  <NavigationBar v-if="accountState == AccountState.Ready && hasBrowserKeys" :me="me!"/>
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

    <div v-else class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
      <div class="flex justify-center mb-3 sm:mb-5">
        <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
      </div>

      <!-- ACCOUNT SETUP -->
      <div v-if="accountState == AccountState.RequiresSetup" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlockSuccess.accountSetup.title') }}
        </h1>
        <p class="my-3">
          {{ t('unlockSuccess.accountSetup.description') }}
        </p>
        <router-link to="/app/setup" class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-xs text-white bg-primary focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">{{ t('unlockSuccess.accountSetup.goToSetup') }}</router-link>
      </div>

      <!-- DEVICE SETUP -->
      <div v-else-if="deviceState == DeviceState.NoSuchDevice" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlockSuccess.deviceSetup.title') }}
        </h1>
        <p class="my-3">
          {{ t('unlockSuccess.deviceSetup.description') }}
        </p>
        <router-link v-if="hasBrowserKeys" to="/app/profile" class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-xs text-white bg-primary focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">
          {{ t('unlockSuccess.deviceSetup.goToProfile') }}
        </router-link>
      </div>

      <!-- NO VAULT ACCESS -->
      <div v-else-if="vaultAccess == VaultAccess.Denied" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlockSuccess.noVaultAccess.title') }}
        </h1>
        <p class="mt-2">
          {{ t('unlockSuccess.noVaultAccess.descritpion') }}
        </p>
      </div>

      <!-- SUCCESS -->
      <div v-else class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlockSuccess.title') }}
        </h1>
        <p class="mt-2">
          {{ t('unlockSuccess.descritpion') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ComputedRef, computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto, VaultDto } from '../common/backend';
import userdata from '../common/userdata';
import FetchError from './FetchError.vue';
import NavigationBar from './NavigationBar.vue';
import SimpleNavigationBar from './SimpleNavigationBar.vue';

const { t } = useI18n({ useScope: 'global' });

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
const hasBrowserKeys = ref<boolean>(false);
const accessibleVaults = ref<VaultDto[]>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
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

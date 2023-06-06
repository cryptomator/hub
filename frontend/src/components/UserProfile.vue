<template>
  <div v-if="me == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else>
    <h1 class="sr-only">Your Profile</h1>
    <div class="grid grid-cols-1 items-start gap-4 lg:grid-cols-4 lg:gap-8">
      <div class="grid grid-cols-1 gap-4">
        <div class="text-center">
          <img class="h-32 w-32 rounded-full bg-white mx-auto" :src="me.pictureUrl" alt="" />
          <p class="mt-3 font-semibold">{{ me.name }}</p>
          <p v-if="me.email != null" class="text-sm text-gray-500">{{ me.email }}</p>
        </div>
        <div class="mt-2 flex flex-col gap-2">
          <button type="button" class="inline-flex items-center justify-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openKeycloakUserAccount()">
            <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('userProfile.actions.manageAccount') }}
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-8 lg:col-span-3">
        <ManageRecoveryCode />
        <DeviceList />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowTopRightOnSquareIcon } from '@heroicons/vue/24/solid';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import config from '../common/config';
import DeviceList from './DeviceList.vue';
import FetchError from './FetchError.vue';
import ManageRecoveryCode from './ManageRecoveryCode.vue';

const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const keycloakUserAccountURL = ref<string>();
const onFetchError = ref<Error | null>();

onMounted(async () => {
  let cfg = config.get();
  keycloakUserAccountURL.value = `${cfg.keycloakUrl}/realms/${cfg.keycloakRealm}/account`;
  await fetchData();
});

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me(true);
  } catch (error) {
    console.error('Retrieving user information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function openKeycloakUserAccount() {
  window.open(keycloakUserAccountURL.value, '_blank');
}
</script>

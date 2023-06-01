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
    <div class="grid grid-cols-1 items-start gap-4 lg:grid-cols-3 lg:gap-8">
      <div class="grid grid-cols-1 gap-4">
        <div class="text-center">
          <img class="h-32 w-32 rounded-full bg-white mx-auto" :src="me.pictureUrl" alt="" />
          <p class="mt-3 font-semibold">{{ me.name }}</p>
          <p v-if="me.email != null" class="text-sm text-gray-500">{{ me.email }}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-8 lg:col-span-2">
        <ManageRecoveryCode />
        <DeviceList />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import DeviceList from './DeviceList.vue';
import FetchError from './FetchError.vue';
import ManageRecoveryCode from './ManageRecoveryCode.vue';

const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me(true);
  } catch (error) {
    console.error('Retrieving user information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>

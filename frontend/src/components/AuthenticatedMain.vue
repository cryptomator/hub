<template>
  <div v-if="me === undefined">
    <!--TODO: beautify loading screen -->
    <div v-if="!onFetchError">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData" />
    </div>
  </div>

  <AppShell v-else :me="me">
    <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
      <GlobalBanners />
      <router-view />
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../common/backend';
import userdata from '../common/userdata';
import AppShell from './AppShell.vue';
import FetchError from './FetchError.vue';
import GlobalBanners from './GlobalBanners.vue';

const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const onFetchError = ref<Error>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = undefined;
  try {
    me.value = await userdata.me;
  } catch (error) {
    console.error('Retrieving logged in user failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>

<template>
  <div v-if="me == null">
    <!--TODO: beautify loading screen -->
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else>
    <Navbar :me="me"/>

    <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
      <router-view></router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import backend, { UserDto } from '../common/backend';
import Navbar from './Navbar.vue';
import FetchError from './FetchError.vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me(true, true);
  } catch (err) {
    console.error('Retrieving logged in user failed.', err);
    onFetchError.value = err instanceof Error ? err : new Error('Unknown Error');
  }
}
</script>

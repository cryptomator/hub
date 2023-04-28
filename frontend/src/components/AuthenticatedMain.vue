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
    <NavigationBar :me="me"/>

    <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
      <router-view></router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import FetchError from './FetchError.vue';
import NavigationBar from './NavigationBar.vue';

const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    me.value = await backend.users.me();
  } catch (error) {
    console.error('Retrieving logged in user failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>

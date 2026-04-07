<template>
  <div v-if="me === undefined">
    <!--TODO: beautify loading screen -->
    <div v-if="!onFetchError">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else class="flex flex-col h-screen">
    <NavigationBar :me="me" @open-menu="mobileOpen = true"/>
    <div class="flex flex-1 overflow-hidden">
      <SideBar :mobile-open="mobileOpen" @close="mobileOpen = false"/>
      <main class="flex-1 overflow-y-auto">
        <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
          <router-view></router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../common/backend';
import userdata from '../common/userdata';
import FetchError from './FetchError.vue';
import NavigationBar from './NavigationBar.vue';
import SideBar from './SideBar.vue';

const { t } = useI18n({ useScope: 'global' });

const me = ref<UserDto>();
const onFetchError = ref<Error>();
const mobileOpen = ref(false);

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

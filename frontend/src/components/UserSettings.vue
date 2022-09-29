<template>
  <div class="pb-5 border-b border-gray-200">
    <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
      {{ t('userSettings.title') }}
    </h2>
  </div>

  <div class="space-y-6 mt-5">
    <form class="mt-5" novalidate>
      <div class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">{{ t('userSettings.general.title') }}</h3>
            </div>
            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-3">
                  <label for="language" class="block text-sm font-medium text-gray-700">{{ t('userSettings.general.language.title') }}</label>
                  <select v-model="$i18n.locale" class="mt-1 block w-full py-2 px-3 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary sm:text-sm">
                    <option v-for="locale in Locale" :key="locale" :value="locale">
                      {{ t(`locale.${locale}`) }}
                    </option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>

    <div class="shadow sm:rounded-lg sm:overflow-hidden">
      <div class="bg-white px-4 py-5 sm:p-6">
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <div class="md:col-span-1">
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ t('userSettings.serverInfo.title') }}
            </h3>
            <p class="mt-1 text-sm text-gray-500">
              {{ t('userSettings.serverInfo.description') }}
            </p>
          </div>
          <div class="mt-5 md:mt-0 md:col-span-2">
            <div v-if="version == null">
              <div v-if="onFetchError == null">
                {{ t('common.loading') }}
              </div>
              <div v-else>
                <FetchError :error="onFetchError" :retry="fetchData"/>
              </div>
            </div>
            <div v-else class="grid grid-cols-6 gap-6">
              <div class="col-span-6 sm:col-span-3">
                <label for="hubVersion" class="block text-sm font-medium text-gray-700">{{ t('userSettings.serverInfo.hubVersion.title') }}</label>
                <input id="hubVersion" v-model="version.hubVersion" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              </div>
              <div class="col-span-6 sm:col-span-3">
                <label for="keycloakVersion" class="block text-sm font-medium text-gray-700">{{ t('userSettings.serverInfo.keycloakVersion.title') }}</label>
                <input id="keycloakVersion" v-model="version.keycloakVersion" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { VersionDto } from '../common/backend';
import { Locale } from '../i18n';
import FetchError from './FetchError.vue';

const { t } = useI18n({ useScope: 'global' });

const version = ref<VersionDto>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    let versionInstalled = backend.version.get();
    version.value = await versionInstalled;
  } catch (error) {
    console.error('Retrieving version information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>

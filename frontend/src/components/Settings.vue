<template>
  <div class="pb-5 border-b border-gray-200">
    <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
      {{ t('settings.title') }}
    </h2>
  </div>

  <div class="space-y-6 mt-5">
    <form class="mt-5" novalidate>
      <div class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">{{ t('settings.general.title') }}</h3>
            </div>
            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-3">
                  <label for="language" class="block text-sm font-medium text-gray-700">{{ t('settings.general.language.title') }}</label>
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
              {{ t('settings.version.title') }}
            </h3>
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
                <label class="block text-sm font-medium text-gray-700">{{ t('settings.version.hub.title') }}</label>
                {{ version.hubVersion }}
              </div>
              <div class="col-span-6 sm:col-span-3">
                <label class="block text-sm font-medium text-gray-700">{{ t('settings.version.keycloak.title') }}</label>
                {{ version.keycloakVersion }}
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

const { t } = useI18n({ useScope: 'global' });

const version = ref<VersionDto>();
const onFetchError = ref<Error | null>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    version.value = await backend.version.get();
  } catch (err) {
    console.error('Retrieving version failed.', err);
    onFetchError.value = err instanceof Error ? err : new Error('Unknown Error');
  }
}


</script>

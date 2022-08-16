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
            <p class="mt-1 text-sm text-gray-500">
              {{ t('settings.version.description') }}
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
                <label for="hubVersion" class="block text-sm font-medium text-gray-700">{{ t('settings.version.hub.title') }}</label>
                <input id="hubVersion" v-model="version.hubVersion" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />

                <p v-if="errorOnFetchingUpdates" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <ExclamationIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('settings.update.fetchingUpdatesFailed.description') }}
                </p>
                <p v-else-if="!stableUpdateExists && !betaUpdateExists" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('settings.update.upToDate.description') }}
                </p>
                <p v-else-if="stableUpdateExists" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <ExclamationIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('settings.update.updateExists.description', [latestVersion?.stable]) }}
                </p>
                <p v-else-if="betaUpdateExists && isBeta" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <ExclamationIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('settings.update.updateExists.description', [latestVersion?.beta]) }}
                </p>
                <p v-else-if="betaUpdateExists && !isBeta" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <InformationCircleIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('settings.update.updateExists.description', [latestVersion?.beta]) }}
                </p>
              </div>
              <div class="col-span-6 sm:col-span-3">
                <label for="keycloakVersion" class="block text-sm font-medium text-gray-700">{{ t('settings.version.keycloak.title') }}</label>
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
import { CheckIcon, ExclamationIcon, InformationCircleIcon } from '@heroicons/vue/solid';
import semver from 'semver';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { VersionDto } from '../common/backend';
import { FetchUpdateError, LatestVersionDto, updateChecker } from '../common/updatecheck';
import { Locale } from '../i18n';
import FetchError from './FetchError.vue';

const { t } = useI18n({ useScope: 'global' });

const version = ref<VersionDto>();
const latestVersion = ref<LatestVersionDto>();
const onFetchError = ref<Error | null>();
const errorOnFetchingUpdates = ref<boolean>(false);

const isBeta = computed(() => semver.prerelease(version.value?.hubVersion ?? '0.1.0') != null);
const stableUpdateExists = computed(() => {
  if (version.value && latestVersion.value) {
    return semver.lt(version.value?.hubVersion , latestVersion.value.stable ?? '0.1.0');
  }
  return false;
});
const betaUpdateExists = computed(() => {
  if (version.value && latestVersion.value) {
    return semver.lt(version.value?.hubVersion , latestVersion.value.beta ?? '0.1.0-beta1');
  }
  return false;
});

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    let versionInstalled = backend.version.get();
    let versionAvailable = versionInstalled.then(versionDto => updateChecker.get(versionDto.hubVersion));
    version.value = await versionInstalled;
    latestVersion.value = await versionAvailable;
  } catch (err) {
    if (err instanceof FetchUpdateError) {
      errorOnFetchingUpdates.value = true;
    } else {
      console.error('Retrieving version information failed.', err);
      onFetchError.value = err instanceof Error ? err : new Error('Unknown Error');
    }
  }
}

</script>

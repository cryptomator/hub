<template>
  <div v-if="admin == null || version == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else>
    <div class="pb-5 border-b border-gray-200">
      <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
        {{ t('admin.title') }}
      </h2>
    </div>

    <div class="space-y-6 mt-5">
      <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6">
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <div class="md:col-span-1">
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ t('admin.serverInfo.title') }}
            </h3>
            <p class="mt-1 text-sm text-gray-500">
              {{ t('admin.serverInfo.description') }}
            </p>
          </div>
          <div class="mt-5 md:mt-0 md:col-span-2">
            <div class="grid grid-cols-6 gap-6">
              <div class="col-span-6 sm:col-span-4">
                <label for="hubId" class="block text-sm font-medium text-gray-700">{{ t('admin.serverInfo.hubId.title') }}</label>
                <input id="hubId" v-model="admin.hubId" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              </div>
              <div class="col-span-6 sm:col-span-3">
                <label for="hubVersion" class="block text-sm font-medium text-gray-700">{{ t('admin.serverInfo.hubVersion.title') }}</label>
                <input id="hubVersion" v-model="version.hubVersion" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />

                <p v-if="errorOnFetchingUpdates" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.serverInfo.hubVersion.description.fetchingUpdatesFailed') }}
                </p>
                <p v-else-if="!stableUpdateExists && !betaUpdateExists" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.serverInfo.hubVersion.description.upToDate') }}
                </p>
                <p v-else-if="stableUpdateExists" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.serverInfo.hubVersion.description.updateExists', [latestVersion?.stable]) }}
                </p>
                <p v-else-if="betaUpdateExists && isBeta" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.serverInfo.hubVersion.description.updateExists', [latestVersion?.beta]) }}
                </p>
                <p v-else-if="betaUpdateExists && !isBeta" id="version-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <InformationCircleIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.serverInfo.hubVersion.description.updateExists', [latestVersion?.beta]) }}
                </p>
              </div>
              <div class="col-span-6 sm:col-span-3">
                <label for="keycloakVersion" class="block text-sm font-medium text-gray-700">{{ t('admin.serverInfo.keycloakVersion.title') }}</label>
                <input id="keycloakVersion" v-model="version.keycloakVersion" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
                <p id="keycloakAdminRealmURL" class="inline-flex mt-2 text-sm">
                  <LinkIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                  <a role="button" :href="keycloakAdminRealmURL" target="_blank" class="underline text-gray-500 hover:text-gray-900">{{ $t('admin.serverInfo.keycloakVersion.description') }}</a>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="admin.hasLicense && remainingSeats != null" class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6">
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <div class="md:col-span-1">
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ t('admin.licenseInfo.title') }}
            </h3>
            <p class="mt-1 text-sm text-gray-500">
              {{ t('admin.licenseInfo.description') }}
            </p>
          </div>
          <div class="mt-5 md:mt-0 md:col-span-2">
            <div class="grid grid-cols-6 gap-6">
              <div class="col-span-6 sm:col-span-3">
                <label for="email" class="block text-sm font-medium text-gray-700">{{ t('admin.licenseInfo.email.title') }}</label>
                <input id="email" v-model="admin.email" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              </div>

              <div class="col-span-6 sm:col-span-3">
                <label for="seats" class="block text-sm font-medium text-gray-700">{{ t('admin.licenseInfo.seats.title') }}</label>
                <input id="seats" v-model="admin.licensedSeats" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="seats-description" readonly />
                <p v-if="remainingSeats > 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.licenseInfo.seats.description.enoughSeats', [remainingSeats]) }}
                </p>
                <p v-else-if="remainingSeats == 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.licenseInfo.seats.description.zeroSeats') }}
                </p>
                <p v-else id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <XMarkIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.licenseInfo.seats.description.undercutSeats', [numberOfExceededSeats]) }}
                </p>
              </div>

              <div class="col-span-6 sm:col-span-3">
                <label for="issuedAt" class="block text-sm font-medium text-gray-700">{{ t('admin.licenseInfo.issuedAt.title') }}</label>
                <input id="issuedAt" :value="d(admin.issuedAt, 'short')" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              </div>

              <div class="col-span-6 sm:col-span-3">
                <label for="expiresAt" class="block text-sm font-medium text-gray-700">{{ t('admin.licenseInfo.expiresAt.title') }}</label>
                <input id="expiresAt" :value="d(admin.expiresAt, 'short')" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="expiresAt-description" readonly />
                <p v-if="now < admin.expiresAt" id="expiresAt-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.licenseInfo.expiresAt.description.valid') }}
                </p>
                <p v-else id="expiresAt-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <XMarkIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.licenseInfo.expiresAt.description.expired') }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="admin.hasLicense" class="flex justify-end items-center">
        <button type="button" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="manageSubscription()">
          <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
          {{ t('admin.licenseInfo.manageSubscription') }}
        </button>
      </div>

      <div v-if="!admin.hasLicense && remainingSeats != null" class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6">
        <div class="md:grid md:grid-cols-3 md:gap-6">
          <div class="md:col-span-1">
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ t('admin.licenseInfo.title') }}
            </h3>
            <p v-if="!admin.managedInstance" class="mt-1 text-sm text-gray-500">
              {{ t('admin.licenseInfo.selfHostedNoLicense.description') }}
            </p>
            <p v-else class="mt-1 text-sm text-gray-500">
              {{ t('admin.licenseInfo.managedNoLicense.description') }}
            </p>
          </div>
          <div class="mt-5 md:mt-0 md:col-span-2">
            <div class="grid grid-cols-6 gap-6">
              <div class="col-span-6 sm:col-span-3">
                <label for="licenseType" class="block text-sm font-medium text-gray-700">{{ t('admin.licenseInfo.type.title') }}</label>
                <input v-if="!admin.managedInstance" id="licenseType" value="Community License" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
                <input v-else id="licenseType" value="Managed" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
              </div>

              <div class="col-span-6 sm:col-span-3">
                <label for="seats" class="block text-sm font-medium text-gray-700">{{ t('admin.licenseInfo.seats.title') }}</label>
                <input id="seats" v-model="admin.licensedSeats" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="seats-description" readonly />
                <p v-if="remainingSeats > 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.licenseInfo.seats.description.enoughSeats', [remainingSeats]) }}
                </p>
                <p v-else-if="remainingSeats == 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.licenseInfo.seats.description.zeroSeats') }}
                </p>
                <p v-else id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                  <XMarkIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                  {{ t('admin.licenseInfo.seats.description.undercutSeats', [numberOfExceededSeats]) }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!admin.hasLicense" class="flex justify-end items-center">
        <button type="button" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="manageSubscription()">
          <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
          {{ t('admin.licenseInfo.getLicense') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowTopRightOnSquareIcon, CheckIcon, ExclamationTriangleIcon, InformationCircleIcon, LinkIcon, XMarkIcon } from '@heroicons/vue/20/solid';
import semver from 'semver';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { BillingDto, VersionDto } from '../common/backend';
import config, { absFrontendBaseURL } from '../common/config';
import { FetchUpdateError, LatestVersionDto, updateChecker } from '../common/updatecheck';
import { Locale } from '../i18n/index';
import FetchError from './FetchError.vue';

const { t, d, locale, fallbackLocale } = useI18n({ useScope: 'global' });

const props = defineProps<{
  token?: string
}>();

const version = ref<VersionDto>();
const latestVersion = ref<LatestVersionDto>();
const admin = ref<BillingDto>();
const now = ref<Date>(new Date());
const keycloakAdminRealmURL = ref<string>();
const onFetchError = ref<Error | null>();
const errorOnFetchingUpdates = ref<boolean>(false);

const isBeta = computed(() => {
  if (version.value && semver.valid(version.value.hubVersion)) {
    return semver.prerelease(version.value.hubVersion ?? '0.1.0') != null;
  }
  return false;
});
const stableUpdateExists = computed(() => {
  if (version.value && semver.valid(version.value.hubVersion) && latestVersion.value?.stable) {
    return semver.lt(version.value.hubVersion, latestVersion.value.stable ?? '0.1.0');
  }
  return false;
});
const betaUpdateExists = computed(() => {
  if (version.value && semver.valid(version.value.hubVersion) && latestVersion.value?.beta) {
    return semver.lt(version.value.hubVersion, latestVersion.value.beta ?? '0.1.0-beta1');
  }
  return false;
});

const remainingSeats = computed(() => admin.value ? admin.value.licensedSeats - admin.value.usedSeats : undefined);
const numberOfExceededSeats = computed(() => {
  if (remainingSeats.value === undefined) {
    return undefined;
  }
  return remainingSeats.value < 0 ? Math.abs(remainingSeats.value) : 0;
});

onMounted(async () => {
  let cfg = config.get();
  keycloakAdminRealmURL.value = `${cfg.keycloakUrl}/admin/${cfg.keycloakRealm}/console`;
  if (props.token) {
    await setToken(props.token);
  }
  await fetchData();
});

async function setToken(token: string) {
  try {
    await backend.billing.setToken(token);
  } catch (error) {
    console.error('Setting token failed.', error);
  }
}

async function fetchData() {
  try {
    let versionDto = backend.version.get();
    let versionAvailable = versionDto.then(versionDto => updateChecker.get(versionDto.hubVersion));
    admin.value = await backend.billing.get();
    version.value = await versionDto;
    latestVersion.value = await versionAvailable;
  } catch (error) {
    if (error instanceof FetchUpdateError) {
      errorOnFetchingUpdates.value = true;
    } else {
      console.error('Retrieving server information failed.', error);
      onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  }
}

function manageSubscription() {
  const returnUrl = `${absFrontendBaseURL}admin`;
  const supportedLanguages = [Locale.EN, Locale.DE];
  const supportedLanguagePathComponents = Object.fromEntries(supportedLanguages.map(lang => [lang, lang == Locale.EN ? '' : `${lang}/`]));
  const languagePathComponent = supportedLanguagePathComponents[(locale.value as string).split('-')[0]] ?? supportedLanguagePathComponents[fallbackLocale.value as string] ?? '';
  window.open(`https://cryptomator.org/${languagePathComponent}hub/billing/?hub_id=${admin.value?.hubId}&return_url=${encodeURIComponent(returnUrl)}`, '_self');
}
</script>

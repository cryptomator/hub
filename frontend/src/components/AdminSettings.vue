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
      <div class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
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
                  <p id="keycloakURL" class="inline-flex mt-2 text-sm">
                    <LinkIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                    <a role="button" :href="keycloakURL" target="_blank" class="underline text-gray-500 hover:text-gray-900">{{ $t('admin.serverInfo.keycloakVersion.description') }}</a>
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="admin.hasLicense" class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
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
                  <input id="seats" v-model="admin.totalSeats" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="seats-description" readonly />
                  <p v-if="admin.remainingSeats > 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('admin.licenseInfo.seats.description.enoughSeats', [admin.remainingSeats]) }}
                  </p>
                  <p v-else-if="admin.remainingSeats == 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('admin.licenseInfo.seats.description.zeroSeats') }}
                  </p>
                  <p v-else id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <XMarkIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('admin.licenseInfo.seats.description.undercutSeats') }}
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

        <div class="flex justify-end items-center px-4 py-3 bg-gray-50 sm:px-6">
          <button type="button" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="manageSubscription()">
            <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('admin.licenseInfo.manageSubscription') }}
          </button>
        </div>
      </div>

      <div v-else-if="!admin.hasLicense" class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">
                {{ t('admin.licenseInfo.title') }}
              </h3>
              <p class="mt-1 text-sm text-gray-500">
                {{ t('admin.licenseInfo.communityLicense.description') }}
              </p>
            </div>
            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-3">
                  <label for="licenseType" class="block text-sm font-medium text-gray-700">{{ t('admin.licenseInfo.communityLicense.type.title') }}</label>
                  <input id="licenseType" value="Community License" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
                </div>

                <div class="col-span-6 sm:col-span-3">
                  <label for="seats" class="block text-sm font-medium text-gray-700">{{ t('admin.licenseInfo.seats.title') }}</label>
                  <input id="seats" v-model="admin.totalSeats" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="seats-description" readonly />
                  <p v-if="admin.remainingSeats > 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('admin.licenseInfo.seats.description.enoughSeats', [admin.remainingSeats]) }}
                  </p>
                  <p v-else-if="admin.remainingSeats == 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <ExclamationTriangleIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('admin.licenseInfo.seats.description.zeroSeats') }}
                  </p>
                  <p v-else id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <XMarkIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('admin.licenseInfo.seats.description.undercutSeats') }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end items-center px-4 py-3 bg-gray-50 sm:px-6">
          <button type="button" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="manageSubscription()">
            <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('admin.licenseInfo.communityLicense.upgradeLicense') }}
          </button>
        </div>
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
import FetchError from './FetchError.vue';

const { t, d, locale } = useI18n({ useScope: 'global' });

const props = defineProps<{
  token?: string
}>();

const version = ref<VersionDto>();
const latestVersion = ref<LatestVersionDto>();
const admin = ref<BillingDto>();
const now = ref<Date>(new Date());
const keycloakURL = ref<string>();
const onFetchError = ref<Error | null>();
const errorOnFetchingUpdates = ref<boolean>(false);

const isBeta = computed(() => semver.prerelease(version.value?.hubVersion ?? '0.1.0') != null);
const stableUpdateExists = computed(() => {
  if (version.value && latestVersion.value?.stable) {
    return semver.lt(version.value?.hubVersion , latestVersion.value.stable ?? '0.1.0');
  }
  return false;
});
const betaUpdateExists = computed(() => {
  if (version.value && latestVersion.value?.beta) {
    return semver.lt(version.value?.hubVersion , latestVersion.value.beta ?? '0.1.0-beta1');
  }
  return false;
});

onMounted(async () => {
  keycloakURL.value = config.get().keycloakUrl;
  if (props.token) {
    await setToken(props.token);
  }
  await fetchData();
});

async function setToken(token: string) {
  try {
    await backend.billing.setToken(token);
  } catch (err) {
    console.error('Setting token failed.', err);
  }
}

async function fetchData() {
  try {
    let versionDto = backend.version.get();
    let versionAvailable = versionDto.then(versionDto => updateChecker.get(versionDto.hubVersion));
    let adminDto = backend.billing.get();
    admin.value = await adminDto;
    version.value = await versionDto;
    latestVersion.value = await versionAvailable;
  } catch (err) {
    if (err instanceof FetchUpdateError) {
      errorOnFetchingUpdates.value = true;
    } else {
      console.error('Retrieving server information failed.', err);
      onFetchError.value = err instanceof Error ? err : new Error('Unknown Error');
    }
  }
}

function manageSubscription() {
  const returnUrl = `${absFrontendBaseURL}admin`;
  const languagePathComponent = locale.value == 'en' ? '' : `${locale.value}/`;
  window.open(`https://cryptomator.org/${languagePathComponent}hub/billing/?hub_id=${admin.value?.hubId}&return_url=${encodeURIComponent(returnUrl)}`, '_self');
}
</script>

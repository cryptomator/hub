<template>
  <SimpleNavigationBar v-if="me !== undefined" :me="me" />

  <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
    <div v-if="state == State.Preparing">
      <div v-if="!onFetchError" class="text-center">
        {{ t('common.loading') }}
      </div>
      <div v-else>
        <FetchError :error="onFetchError" :retry="fetchData" />
      </div>
    </div>

    <div v-else-if="state == State.NonAdmin" class="flex justify-center">
      <div class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
        <div class="flex justify-center">
          <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
        </div>
        <div class="mt-3 sm:mt-5">
          <h3 class="text-lg leading-6 font-medium text-gray-900">
            {{ t('setupLicense.nonAdmin.title') }}
          </h3>
          <div class="mt-2">
            <p class="text-sm text-gray-500">
              {{ t('setupLicense.nonAdmin.description') }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="state == State.GetLicense" class="sm:w-8/12 mx-auto">
      <section class="w-full bg-white p-4 sm:p-6 shadow-sm sm:rounded-lg flex flex-col items-center">
        <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
        <h1 class="text-lg leading-6 font-medium text-gray-900 my-3 sm:my-5">{{ t('setupLicense.title') }}</h1>
        <p v-if="onSessionError" class="mb-3 text-sm text-red-900">{{ t('setupLicense.session.error') }}</p>

        <form class="flex flex-col items-center" @submit.prevent="getTrialLicense()">
          <p class="mt-1 text-sm text-gray-500">{{ t('setupLicense.trial.description') }}</p>
          <altcha-widget class="mt-3" :challenge="challengeUrl" auto="onsubmit" type="native" display="invisible" @statechange="onAltchaStateChange" />
          <button type="submit" :disabled="processing || altchaVerifying" class="mt-3 inline-flex cursor-pointer items-center justify-center rounded-md border border-transparent bg-primary px-20 py-2 text-sm font-medium text-white shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
            <span v-if="processing || altchaVerifying" class="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" aria-hidden="true" />
            {{ t('setupLicense.trial.submit') }}
          </button>
          <p v-if="onTrialError" class="mt-2 text-sm text-red-900">{{ t('setupLicense.trial.error') }}</p>
        </form>
      </section>

      <details class="mt-5 sm:mt-6">
        <summary class="cursor-pointer text-center text-sm font-medium text-gray-500 hover:text-gray-700">{{ t('setupLicense.moreOptions') }}</summary>
        <div class="mt-5 sm:mt-6 grid grid-cols-1 gap-6 md:grid-cols-2">
          <section class="flex flex-col bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
            <h4 class="text-base font-medium text-gray-900">{{ t('setupLicense.buy.title') }}</h4>
            <p class="mt-1 text-sm text-gray-500">{{ t('setupLicense.buy.description') }}</p>
            <div class="mt-auto pt-3">
              <a :href="storeUrl" rel="noopener" class="inline-flex w-full items-center justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-sm font-medium text-white shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2">
                {{ t('setupLicense.buy.submit') }}
                <ArrowTopRightOnSquareIcon class="ml-2 -mr-0.5 h-4 w-4" aria-hidden="true" />
              </a>
            </div>
          </section>

          <section class="flex flex-col bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6">
            <h4 class="text-base font-medium text-gray-900">{{ t('setupLicense.community.title') }}</h4>
            <p class="mt-1 text-sm text-gray-500">{{ t('setupLicense.community.description') }}</p>
            <div class="mt-auto pt-3">
              <a :href="communityUrl" rel="noopener" class="inline-flex w-full items-center justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-sm font-medium text-white shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2">
                {{ t('setupLicense.community.submit') }}
                <ArrowTopRightOnSquareIcon class="ml-2 -mr-0.5 h-4 w-4" aria-hidden="true" />
              </a>
            </div>
          </section>

          <section class="flex flex-col bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 md:col-span-2">
            <form class="flex grow flex-col" @submit.prevent="applyExistingLicense()">
              <h4 class="text-base font-medium text-gray-900">{{ t('setupLicense.existing.title') }}</h4>
              <p class="mt-1 text-sm text-gray-500">{{ t('setupLicense.existing.description') }}</p>
              <textarea id="licenseToken" v-model="licenseToken" rows="3" name="licenseToken" :aria-label="t('setupLicense.existing.title')" class="mt-3 block w-full rounded-md border-gray-300 font-mono text-xs shadow-xs focus:border-primary focus:ring-primary" required />
              <div class="mt-auto pt-3">
                <button type="submit" :disabled="processing || licenseToken.trim() == ''" class="inline-flex w-full cursor-pointer items-center justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-sm font-medium text-white shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
                  {{ t('setupLicense.existing.submit') }}
                </button>
                <p v-if="onApplyError" class="mt-2 text-sm text-red-900">{{ t('setupLicense.existing.error', [billing?.hubId]) }}</p>
              </div>
            </form>
          </section>
        </div>
      </details>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowTopRightOnSquareIcon } from '@heroicons/vue/20/solid';
import 'altcha/altcha.css';
import 'altcha/external'; // CSP-friendly variant without inlined workers, requires registering the algorithms below
import ShaWorker from 'altcha/workers/sha?worker';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import authPromise from '../common/auth';
import backend, { BillingDto, UserDto } from '../common/backend';
import config, { absFrontendBaseURL } from '../common/config';
import { requestTrialLicense } from '../common/trial';
import userdata from '../common/userdata';
import router from '../router';
import FetchError from './FetchError.vue';
import SimpleNavigationBar from './SimpleNavigationBar.vue';

$altcha.algorithms.set('SHA-256', () => new ShaWorker()); // the license API's challenges use SHA-256 exclusively

enum State {
  Preparing,
  NonAdmin,
  GetLicense
}

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  session?: string
}>();

const state = ref(State.Preparing);
const me = ref<UserDto>();
const billing = ref<BillingDto>();
const processing = ref(false);
const licenseToken = ref('');
const altchaPayload = ref('');
const altchaVerifying = ref(false);
const onFetchError = ref<Error>();
const onSessionError = ref<Error>();
const onTrialError = ref<Error>();
const onApplyError = ref<Error>();

const challengeUrl = computed(() => `${config.get().licenseApiUrl}/hub/challenge`);

function onAltchaStateChange(event: Event) {
  const { state, payload } = (event as CustomEvent<{ payload?: string, state: string }>).detail;
  altchaVerifying.value = state === 'verifying';
  altchaPayload.value = state === 'verified' && payload ? payload : '';
  if (state === 'error') {
    console.error('Altcha verification failed.');
    onTrialError.value = new Error('Altcha verification failed');
  }
}

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = undefined;
  try {
    if (!config.get().licenseSetupRequired) {
      await router.push('/app/vaults'); // license has been set up in the meantime
      return;
    }
    const auth = await authPromise;
    me.value = await userdata.me;
    if (!auth.hasRole('admin')) {
      state.value = State.NonAdmin;
      return;
    }
    billing.value = await backend.billing.get();
    if (props.session) {
      try {
        await backend.license.refresh(props.session); // license bought or registered in the store, redirected back with a session id
        await proceed();
        return;
      } catch (error) {
        console.error('Applying license from store session failed.', error);
        onSessionError.value = error instanceof Error ? error : new Error('Unknown Error');
      }
    }
    state.value = State.GetLicense;
  } catch (error) {
    console.error('Retrieving setup information failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

const storeUrl = computed(() => billing.value ? `${config.get().billingUrl}?${licenseStoreQuery(billing.value.hubId)}` : '');

const communityUrl = computed(() => billing.value ? `${new URL('/hub/register/', config.get().billingUrl)}?${licenseStoreQuery(billing.value.hubId)}` : '');

function licenseStoreQuery(hubId: string): string {
  const returnUrl = `${absFrontendBaseURL}setup-license`;
  return `hub_id=${encodeURIComponent(hubId)}&return_url=${encodeURIComponent(returnUrl)}&token_transfer=session`;
}

async function getTrialLicense() {
  onTrialError.value = undefined;
  if (!altchaPayload.value) {
    return; // first submit attempt: the altcha widget intercepts it, solves the challenge and re-submits the form once verified
  }
  try {
    processing.value = true;
    const trial = await requestTrialLicense(config.get().licenseApiUrl, altchaPayload.value); // browser talks to the license API directly, works without outbound internet access on the Hub server
    await backend.license.installTrial(trial.hubId, trial.licenseKey);
    await proceed();
  } catch (error) {
    console.error('Retrieving trial license failed.', error);
    onTrialError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}

async function applyExistingLicense() {
  await applyLicense(() => backend.billing.setToken(licenseToken.value.trim()));
}

async function applyLicense(action: () => Promise<void>) {
  onApplyError.value = undefined;
  try {
    processing.value = true;
    await action();
    await proceed();
  } catch (error) {
    console.error('Applying license failed.', error);
    onApplyError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}

async function proceed() {
  await config.reload();
  await router.push('/app/vaults');
}
</script>

<template>
  <div v-if="billing == null">
    {{ t('common.loading') }}
  </div>

  <div v-else>
    <div class="pb-5 border-b border-gray-200">
      <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
        {{ t('billing.title') }}
      </h2>
    </div>

    <div class="space-y-6 mt-5">
      <div class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">
                {{ t('billing.serverInfo.title') }}
              </h3>
              <p class="mt-1 text-sm text-gray-500">
                {{ t('billing.serverInfo.description') }}
              </p>
            </div>
            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-4">
                  <label for="hubId" class="block text-sm font-medium text-gray-700">{{ t('billing.serverInfo.hubId.title') }}</label>
                  <input id="hubId" v-model="billing.hubId" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="billing.hasLicense" class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">
                {{ t('billing.licenseInfo.title') }}
              </h3>
              <p class="mt-1 text-sm text-gray-500">
                {{ t('billing.licenseInfo.description') }}
              </p>
            </div>
            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-3">
                  <label for="email" class="block text-sm font-medium text-gray-700">{{ t('billing.licenseInfo.email.title') }}</label>
                  <input id="email" v-model="billing.email" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
                </div>

                <div class="col-span-6 sm:col-span-3">
                  <label for="seats" class="block text-sm font-medium text-gray-700">{{ t('billing.licenseInfo.seats.title') }}</label>
                  <input id="seats" v-model="billing.totalSeats" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="seats-description" readonly />
                  <p v-if="billing.remainingSeats > 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('billing.licenseInfo.seats.description.enoughSeats', [billing.remainingSeats]) }}
                  </p>
                  <p v-else-if="billing.remainingSeats == 0" id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <ExclamationIcon class="shrink-0 text-orange-500 mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('billing.licenseInfo.seats.description.zeroSeats') }}
                  </p>
                  <p v-else id="seats-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <XIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('billing.licenseInfo.seats.description.undercutSeats') }}
                  </p>
                </div>

                <div class="col-span-6 sm:col-span-3">
                  <label for="issuedAt" class="block text-sm font-medium text-gray-700">{{ t('billing.licenseInfo.issuedAt.title') }}</label>
                  <input id="issuedAt" :value="d(billing.issuedAt, 'short')" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
                </div>

                <div class="col-span-6 sm:col-span-3">
                  <label for="expiresAt" class="block text-sm font-medium text-gray-700">{{ t('billing.licenseInfo.expiresAt.title') }}</label>
                  <input id="expiresAt" :value="d(billing.expiresAt, 'short')" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" aria-describedby="expiresAt-description" readonly />
                  <p v-if="now < billing.expiresAt" id="expiresAt-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <CheckIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('billing.licenseInfo.expiresAt.description.valid') }}
                  </p>
                  <p v-else id="expiresAt-description" class="inline-flex mt-2 text-sm text-gray-500">
                    <XIcon class="shrink-0 text-red-500 mr-1 h-5 w-5" aria-hidden="true" />
                    {{ t('billing.licenseInfo.expiresAt.description.expired') }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end items-center px-4 py-3 bg-gray-50 sm:px-6">
          <button type="button" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="manageSubscription()">
            <ExternalLinkIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('billing.licenseInfo.manageSubscription') }}
          </button>
        </div>
      </div>

      <div v-else class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white text-center px-4 py-5 sm:p-6">
          <!-- <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m-9 1V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
          </svg> -->
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('billing.licenseInfo.empty.title') }}</h3>
          <p class="mt-1 text-sm text-gray-500">{{ t('billing.licenseInfo.empty.description') }}</p>
          <div class="mt-6">
            <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="manageSubscription()">
              <ExternalLinkIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
              {{ t('billing.licenseInfo.getLicense') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckIcon, ExclamationIcon, ExternalLinkIcon, XIcon } from '@heroicons/vue/solid';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { BillingDto } from '../common/backend';
import { frontendBaseURL } from '../common/config';

const { t, d } = useI18n({ useScope: 'global' });

const props = defineProps<{
  token?: string
}>();

const billing = ref<BillingDto>();
const now = ref<Date>(new Date());

onMounted(async () => {
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
    billing.value = await backend.billing.get();
  } catch (err) {
    console.error('Retrieving billing information failed.', err);
  }
}

function manageSubscription() {
  const returnUrl = `${frontendBaseURL}/billing`;
  window.open(`http://localhost:1313/hub/billing/?hub_id=${billing.value?.hubId}&return_url=${encodeURIComponent(returnUrl)}`, '_self'); // TODO: use real url
}
</script>

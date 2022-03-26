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

    <form class="mt-5" novalidate @submit.prevent="manageSubscription()">
      <div class="shadow sm:rounded-lg sm:overflow-hidden">
        <div class="bg-white px-4 py-5 sm:p-6">
          <div class="md:grid md:grid-cols-3 md:gap-6">
            <div class="md:col-span-1">
              <h3 class="text-lg font-medium leading-6 text-gray-900">{{ t('billing.serverInfo.title') }}</h3>
            </div>
            <div class="mt-5 md:mt-0 md:col-span-2">
              <div class="grid grid-cols-6 gap-6">
                <div class="col-span-6 sm:col-span-3">
                  <label for="hubId" class="block text-sm font-medium text-gray-700">{{ t('billing.serverInfo.hubId.title') }}</label>
                  <input id="hubId" v-model="billing.hub_id" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" readonly />
                </div>

                <div class="col-span-6">
                  <label for="token" class="block text-sm font-medium text-gray-700">{{ t('billing.serverInfo.token.title') }}</label>
                  <textarea id="token" v-model="billing.token" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md bg-gray-200" rows="6" readonly></textarea>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end items-center px-4 py-3 bg-gray-50 sm:px-6">
          <button type="submit" class="flex-none inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed">
            {{ t('billing.submit') }}
          </button>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { BillingDto } from '../common/backend';
import { frontendBaseURL } from '../common/config';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  token: string
}>();

const billing = ref<BillingDto>();

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
  window.open(`http://localhost:1313/hub/billing/?hub_id=${billing.value?.hub_id}&return_url=${encodeURIComponent(returnUrl)}`, '_self'); // TODO: use real url
}
</script>

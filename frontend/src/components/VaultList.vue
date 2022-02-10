<template>
  <div v-if="vaults == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <div v-else-if="vaults.length == 0" class="text-center">
    <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
      <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m-9 1V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
    </svg>
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('vaultList.empty.title') }}</h3>
    <p class="mt-1 text-sm text-gray-500">{{ t('vaultList.empty.description') }}</p>
    <div class="mt-6">
      <router-link to="/vaults/create" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">
        <PlusIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
        {{ t('vaultList.createVault') }}
      </router-link>
    </div>
  </div>

  <div v-else>
    <div class="pb-5 border-b border-gray-200 flex items-center whitespace-nowrap">
      <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">{{ t('vaultList.title') }}</h2>
      <div class="flex-none flex items-center ml-auto pl-4 sm:pl-6">
        <router-link to="/vaults/create" class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary">
          <PlusIcon class="-ml-0.5 mr-2 h-4 w-4" aria-hidden="true" />
          {{ t('vaultList.createVault') }}
        </router-link>
      </div>
    </div>

    <div class="mt-5 bg-white shadow overflow-hidden sm:rounded-md">
      <ul role="list" class="divide-y divide-gray-200">
        <li v-for="vault in vaults" :key="vault.masterkey">
          <a role="button" tabindex="0" class="block hover:bg-gray-50" :class="selectedVault == vault ? 'bg-gray-50' : ''" @click="onVaultClick(vault)">
            <div class="px-4 py-4 flex items-center sm:px-6">
              <div class="min-w-0 flex-1 sm:flex sm:items-center sm:justify-between">
                <p class="text-sm font-medium text-primary truncate">{{ vault.name }}</p>
                <div class="mt-4 shrink-0 sm:mt-0 sm:ml-5">
                  <div class="flex overflow-hidden -space-x-1">
                    <!-- <img v-for="member in vault.members" :key="member.id" class="inline-block h-6 w-6 rounded-full ring-2 ring-white" :src="member.pictureUrl" :alt="member.name" /> -->
                  </div>
                </div>
              </div>
              <div class="ml-5 shrink-0">
                <ChevronRightIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
              </div>
            </div>
          </a>
        </li>
      </ul>
    </div>

    <SlideOver v-if="selectedVault != null" ref="vaultDetailsSlideOver" :title="selectedVault.name" @close="selectedVault = null">
      <VaultDetails :vault-id="selectedVault.id"></VaultDetails>
    </SlideOver>
  </div>
</template>

<script setup lang="ts">
import { ChevronRightIcon, PlusIcon } from '@heroicons/vue/solid';
import { nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { VaultDto } from '../common/backend';
import SlideOver from './SlideOver.vue';
import VaultDetails from './VaultDetails.vue';
import FetchError from './FetchError.vue';

const { t } = useI18n({ useScope: 'global' });

const vaultDetailsSlideOver = ref<typeof SlideOver>();
const onFetchError = ref<Error | null>();

const vaults = ref<VaultDto[]>();
const selectedVault = ref<VaultDto | null>(null);

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    vaults.value = await backend.vaults.listSharedOrOwned();
  } catch (error) {
    console.error('Retrieving vault list failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}


function onVaultClick(vault: VaultDto) {
  selectedVault.value = vault;
  nextTick(() => vaultDetailsSlideOver.value?.show());
}
</script>

<template>
  <div class="flex items-center mb-3 whitespace-nowrap">
    <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">Vaults</h2>
    <div class="flex-none flex items-center ml-auto pl-4 sm:pl-6">
      <router-link to="/vaults/create" tag="button" class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
        <PlusIcon class="-ml-0.5 mr-2 h-4 w-4" aria-hidden="true" />
        Create Vault
      </router-link>
    </div>
  </div>
  <div class="bg-white shadow overflow-hidden sm:rounded-md">
    <ul role="list" class="divide-y divide-gray-200">
      <li v-for="vault in vaults" :key="vault.masterkey">
        <a href="#" class="block hover:bg-gray-50" :class="selectedVault == vault ? 'bg-gray-50' : ''" @click="onVaultClick(vault)">
          <div class="px-4 py-4 flex items-center sm:px-6">
            <div class="min-w-0 flex-1 sm:flex sm:items-center sm:justify-between">
              <p class="text-sm font-medium text-indigo-600 truncate">{{ vault.name }}</p>
              <div class="mt-4 flex-shrink-0 sm:mt-0 sm:ml-5">
                <div class="flex overflow-hidden -space-x-1">
                  <img key="driesvincent@example.com" class="inline-block h-6 w-6 rounded-full ring-2 ring-white" src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80" alt="Dries Vincent" />
                  <img key="lindsaywalton@example.com" class="inline-block h-6 w-6 rounded-full ring-2 ring-white" src="https://images.unsplash.com/photo-1517841905240-472988babdf9?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80" alt="Lindsay Walton" />
                  <img key="courtneyhenry@example.com" class="inline-block h-6 w-6 rounded-full ring-2 ring-white" src="https://images.unsplash.com/photo-1438761681033-6461ffad8d80?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80" alt="Courtney Henry" />
                  <img key="tomcook@example.com" class="inline-block h-6 w-6 rounded-full ring-2 ring-white" src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80" alt="Tom Cook" />
                  <!-- <img v-for="applicant in position.applicants" :key="applicant.email" class="inline-block h-6 w-6 rounded-full ring-2 ring-white" :src="applicant.imageUrl" :alt="applicant.name" /> -->
                </div>
              </div>
            </div>
            <div class="ml-5 flex-shrink-0">
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
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import backend, { VaultDto } from '../common/backend';
import { ChevronRightIcon, PlusIcon } from '@heroicons/vue/solid';
import SlideOver from './SlideOver.vue';
import VaultDetails from './VaultDetails.vue';

export default defineComponent({
  name: 'VaultList',
  components: {
    ChevronRightIcon,
    PlusIcon,
    SlideOver,
    VaultDetails,
  },
  data: () => ({
    Error,
    vaults: [] as VaultDto[],
    selectedVault: null as VaultDto | null,
  }),
  async mounted() {
    this.vaults = await backend.vaults.listAll();
  },
  methods: {
    onVaultClick(vault: VaultDto) {
      this.selectedVault = vault;
      this.$nextTick(() => (this.$refs.vaultDetailsSlideOver as typeof SlideOver).show());
    }
  }
})
</script>

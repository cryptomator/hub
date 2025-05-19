<template>
  <section v-if="props.visible" class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
    <div class="bg-gray-50 px-6 py-4 border-b border-gray-200">
      <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
        {{ t('nav.vaults') }}
      </h3>
    </div>

    <!-- Search bar -->
    <div class="px-6 py-3 border-b border-gray-200">
      <input id="vaultSearch" v-model="vaultQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
    </div>

    <!-- Vault list -->
    <div class="py-0">
      <ul class="divide-y divide-gray-200 bg-white">
        <li v-for="vault in paginatedVaults" :key="vault.id" class="py-2 px-6">
          <div class="text-sm font-medium text-gray-900 truncate">{{ vault.name }}</div>
          <div v-if="vault.description" class="text-sm text-gray-500 truncate">{{ vault.description }}</div>
        </li>
        <li v-if="!filteredVaults.length" class="flex items-center justify-center py-4 px-6 w-full text-sm text-gray-500">
          {{ t(vaultQuery ? 'common.nothingFound' : 'common.none') }}
        </li>
      </ul>
    </div>

    <!-- Pagination -->
    <div v-if="showPaginationVault" class="bg-gray-50 border-t border-gray-200">
      <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
        <div class="hidden sm:block">
          <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
            <span class="font-medium">{{ paginationBeginVault }}</span>
            <span class="font-medium">{{ paginationEndVault }}</span>
          </i18n-t>
        </div>
        <div class="flex flex-1 justify-between sm:justify-end space-x-3">
          <button v-if="currentPageVault > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPageVault">
            {{ t('common.previous') }}
          </button>
          <button v-if="hasNextPageVault" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPageVault">
            {{ t('common.next') }}
          </button>
        </div>
      </nav>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
const { t } = useI18n({ useScope: 'global' });

interface Vault {
  id: string;
  name: string;
  description?: string;
}

const props = defineProps<{
  vaults: Vault[];
  pageSize: number;
  visible: boolean; 
}>();

// Vaults – search & pagination
const pageSizeVault = ref(props.pageSize);
const currentPageVault = ref(0);
const vaultQuery = ref('');

const filteredVaults = computed(() => {
  const q = vaultQuery.value.trim().toLowerCase();
  return props.vaults
    .filter(v => !q || v.name.toLowerCase().includes(q) || (v.description && v.description.toLowerCase().includes(q)))
    .sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
});

const showPaginationVault = computed(() => filteredVaults.value.length > pageSizeVault.value);

const paginatedVaults = computed(() =>
  filteredVaults.value.slice(currentPageVault.value * pageSizeVault.value, (currentPageVault.value + 1) * pageSizeVault.value)
);

const hasNextPageVault = computed(() => (currentPageVault.value + 1) * pageSizeVault.value < filteredVaults.value.length);

const paginationBeginVault = computed(() => (filteredVaults.value.length ? currentPageVault.value * pageSizeVault.value + 1 : 0));
const paginationEndVault = computed(() => Math.min((currentPageVault.value + 1) * pageSizeVault.value, filteredVaults.value.length));

function showNextPageVault() {
  if (hasNextPageVault.value) currentPageVault.value++;
}
function showPreviousPageVault() {
  if (currentPageVault.value > 0) currentPageVault.value--;
}

watch(() => [filteredVaults.value.length, vaultQuery.value], () => (currentPageVault.value = 0));
</script>
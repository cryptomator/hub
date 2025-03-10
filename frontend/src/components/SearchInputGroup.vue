<template>
  <div class="flex rounded-md shadow-xs">
    <div class="relative flex items-stretch grow focus-within:z-10">
      <Combobox as="div" class="w-full" @update:model-value="item => selectedItem = item">
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <UsersIcon v-if="selectedItem == null" class="h-5 w-5 text-gray-400" aria-hidden="true" />
            <img v-else :src="selectedItem.pictureUrl ?? ''" alt="" class="w-5 h-5 rounded-full" >
          </div>

          <ComboboxInput v-if="selectedItem == null" v-focus class="w-full h-10 rounded-l-md border border-gray-300 bg-white py-2 px-10 shadow-xs focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary sm:text-sm disabled:bg-primary-l2" placeholder="John Doe" @change="query = $event.target.value"/>
          <div v-else class="w-full h-10 rounded-l-md border border-gray-300 bg-primary-l2 py-2 px-10 flex items-center justify-between shadow-xs sm:text-sm">
            <span class="truncate">{{ selectedItem.name }}</span>
            <span v-if="selectedItem.type === 'GROUP'" class="text-gray-500 text-xs italic mr-6">
              {{ selectedItem.memberSize }} {{ t('vaultDetails.sharedWith.members') }}
            </span>
          </div>
        </div>

        <ComboboxOptions v-if="selectedItem == null && filteredItems.length > 0" class="absolute z-10 mt-1 max-h-56 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black/5 focus:outline-hidden sm:text-sm">
          <ComboboxOption v-for="item in filteredItems" :key="item.id" :value="item" class="relative cursor-default select-none py-2 pl-3 pr-9 ui-not-active:text-gray-900 ui-active:bg-primary ui-active:text-white">
            <div class="flex items-center">
              <img :src="item.pictureUrl ?? ''" alt="" class="h-6 w-6 shrink-0 rounded-full" >
              <span class="ml-3 truncate">{{ item.name }}</span>
              <span v-if="item.type === 'GROUP'" class="ml-auto text-xs text-gray-500 italic">
                {{ item.memberSize }} {{ t('vaultDetails.sharedWith.members') }}
              </span>
            </div>
          </ComboboxOption>
        </ComboboxOptions>
      </Combobox>

      <button v-if="selectedItem != null" type="button" class="absolute inset-y-0 right-0 pr-3 flex items-center" @click="reset()">
        <XCircleIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
      </button>
    </div>

    <button ref="actionButton" :disabled="selectedItem == null" type="button" class="-ml-px relative inline-flex items-center space-x-2 px-4 py-2 border border-transparent text-sm font-medium rounded-r-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary focus:border-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="onAction()">
      {{ actionTitle }}
    </button>
  </div>
</template>

<script setup lang="ts" generic="T extends Item">
import { Combobox, ComboboxInput, ComboboxOption, ComboboxOptions } from '@headlessui/vue';
import { UsersIcon, XCircleIcon } from '@heroicons/vue/24/solid';
import { computed, nextTick, ref, shallowRef, watch } from 'vue';
import { debounce } from '../common/util';
import { useI18n } from 'vue-i18n';

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type?: string;
  memberSize?: number;
}

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  actionTitle: string
  onSearch: (query: string) => Promise<T[]>
}>();

const emit = defineEmits<{
  action: [item: T]
}>();

const vFocus = {
  mounted: (el: HTMLElement) => {
    el.focus();
  }
};

const actionButton = ref<HTMLButtonElement>();
const selectedItem = shallowRef<T | null>(null);
watch(selectedItem, (value) => {
  if (value != null) {
    nextTick(() => actionButton.value?.focus());
  }
});

const query = ref('');
const searchResult = shallowRef<T[]>([]);
const debouncedSearch = debounce(async () => {
  if (query.value != '') {
    const result = await props.onSearch(query.value);
    if (query.value != '') {
      searchResult.value = result;
    }
  }
});
watch(query, async (newQuery) => {
  if (newQuery == '') {
    searchResult.value = [];
    debouncedSearch.cancel();
  } else {
    debouncedSearch();
  }
});

const filteredItems = computed(() => {
  if (searchResult.value.length == 0) {
    return [];
  } else {
    return searchResult.value.filter((item) => item.name.toLowerCase().includes(query.value.toLowerCase()));
  }
});

function onAction() {
  if (selectedItem.value) {
    emit('action', selectedItem.value);
    reset();
  }
}

function reset() {
  selectedItem.value = null;
  query.value = '';
}
</script>

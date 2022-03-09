<template>
  <div class="flex rounded-md shadow-sm">
    <div class="relative flex items-stretch flex-grow focus-within:z-10">
      <Combobox as="div" class="w-full" @update:model-value="item => selectedItem = item">
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <UsersIcon v-if="selectedItem == null" class="h-5 w-5 text-gray-400" aria-hidden="true" />
            <img v-else :src="selectedItem.pictureUrl" alt="" class="w-5 h-5 rounded-full" />
          </div>

          <ComboboxInput v-if="selectedItem == null" v-focus class="w-full h-10 rounded-l-md border border-gray-300 bg-white py-2 px-10 shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary sm:text-sm disabled:bg-primary-l2" placeholder="John Doe" @change="query = $event.target.value" />
          <input v-else v-model="selectedItem.name" class="w-full h-10 rounded-l-md border border-gray-300 bg-primary-l2 py-2 px-10 shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary sm:text-sm" readonly />
        </div>

        <ComboboxOptions v-if="selectedItem == null && filteredItems.length > 0" class="absolute z-10 mt-1 max-h-56 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm">
          <ComboboxOption v-for="item in filteredItems" :key="item.id" v-slot="{ active }" :value="item" as="template">
            <li :class="['relative cursor-default select-none py-2 pl-3 pr-9', active ? 'bg-primary text-white' : 'text-gray-900']">
              <div class="flex items-center">
                <img :src="item.pictureUrl" alt="" class="h-6 w-6 shrink-0 rounded-full" />
                <span class="ml-3 truncate">{{ item.name }}</span>
              </div>
            </li>
          </ComboboxOption>
        </ComboboxOptions>
      </Combobox>

      <button v-if="selectedItem != null" type="button" class="absolute inset-y-0 right-0 pr-3 flex items-center" @click="selectedItem = null">
        <XCircleIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
      </button>
    </div>

    <button ref="actionButton" :disabled="selectedItem == null" type="button" class="-ml-px relative inline-flex items-center space-x-2 px-4 py-2 border border-transparent text-sm font-medium rounded-r-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary focus:border-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="onAction()">
      {{ actionTitle }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { Combobox, ComboboxInput, ComboboxOption, ComboboxOptions } from '@headlessui/vue';
import { UsersIcon, XCircleIcon } from '@heroicons/vue/solid';
import { computed, nextTick, ref, watch } from 'vue';

interface Item {
  id: string;
  name: string;
  pictureUrl: string;
}

const props = defineProps<{
  actionTitle: string
  items: Item[]
}>();

const emit = defineEmits<{
  (e: 'action', id: string): void
}>();

const vFocus = {
  mounted: (el: HTMLElement) => {
    el.focus();
  }
};

const actionButton = ref<HTMLButtonElement>();

const query = ref('');
const selectedItem = ref<Item | null>(null);
watch(selectedItem, (value) => {
  if (value != null) {
    nextTick(() => actionButton.value?.focus());
  }
});

const filteredItems = computed(() => {
  if (query.value.length == 0) {
    return [];
  } else {
    return props.items.filter((item) => item.name.toLowerCase().includes(query.value.toLowerCase()));
  }
});

function onAction() {
  if (selectedItem.value) {
    emit('action', selectedItem.value.id);
    selectedItem.value = null;
  }
}
</script>

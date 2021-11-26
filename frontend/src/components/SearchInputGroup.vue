<template>
  <div class="flex rounded-md shadow-sm">
    <div class="relative flex items-stretch flex-grow focus-within:z-10">
      <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
        <UsersIcon v-if="selectedItem == null" class="h-5 w-5 text-gray-400" aria-hidden="true" />
        <img v-else :src="selectedItem.pictureUrl" alt="" class="w-5 h-5 rounded-full" />
      </div>

      <Listbox v-model="selectedItem" as="div" class="w-full">
        <ListboxButton class="w-full" @focus="searchInput?.focus()">
          <input id="searchInput" ref="searchInput" v-model="searchTerm" v-focus :disabled="selectedItem != null" type="text" name="searchInput" autocomplete="off" class="focus:ring-primary focus:border-primary block w-full rounded-none rounded-l-md pl-10 sm:text-sm border-gray-300 disabled:bg-primary-l2" placeholder="John Doe" @focus="searchInputFocus = true" @blur="onSearchInputBlur()" />
        </ListboxButton>
        <div v-show="listboxOptionsOpen">
          <ListboxOptions ref="listboxOptions" class="absolute z-10 mt-1 w-full bg-white shadow-lg max-h-60 rounded-md py-1 text-base ring-1 ring-black ring-opacity-5 overflow-auto focus:outline-none sm:text-sm" static @focus="listboxOptionsFocus = true" @blur="onListboxOptionsBlur()">
            <ListboxOption v-for="item in filteredItems" :key="item.id" v-slot="{ active }" as="template" :value="item">
              <li :class="[active ? 'text-white bg-primary' : 'text-gray-900', {'hover:text-white hover:bg-primary': !listboxOptionsFocus}, 'cursor-default select-none relative py-2 pl-3 pr-9']" @focus="listboxOptionFocus = true" @blur="listboxOptionFocus = false">
                <div class="flex items-center">
                  <img :src="item.pictureUrl" alt="" class="flex-shrink-0 h-6 w-6 rounded-full" />
                  <span class="ml-3 block truncate">{{ item.name }}</span>
                </div>
              </li>
            </ListboxOption>
          </ListboxOptions>
        </div>
      </Listbox>

      <button v-if="selectedItem != null" type="button" class="absolute inset-y-0 right-0 pr-3 flex items-center" @click="reset()">
        <XCircleIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
      </button>
    </div>

    <button ref="actionButton" :disabled="selectedItem == null" type="button" class="-ml-px relative inline-flex items-center space-x-2 px-4 py-2 border border-transparent text-sm font-medium rounded-r-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary focus:border-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="onAction()">
      {{ actionTitle }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { UsersIcon, XCircleIcon } from '@heroicons/vue/solid';
import { computed, nextTick, ref, Ref, watch } from 'vue';

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

const searchInput = ref<HTMLInputElement>();
const listboxOptions = ref<HTMLElement>();
const actionButton = ref<HTMLButtonElement>();

const searchInputFocus = ref(false);
const listboxOptionsFocus = ref(false);
const listboxOptionFocus = ref(false);
const searchTerm = ref('');
const selectedItem = ref<Item | null>(null);
watch(selectedItem, (value) => {
  if (value != null) {
    searchTerm.value = value.name;
    nextTick(() => actionButton.value?.focus());
  }
});

const listboxOptionsOpen = computed(() => {
  if (selectedItem.value == null) {
    return (searchInputFocus.value || listboxOptionsFocus.value || listboxOptionFocus.value) && filteredItems.value.length > 0;
  } else {
    return false;
  }
});
const filteredItems = computed(() => {
  if (searchTerm.value.length == 0) {
    return [];
  } else {
    return props.items.filter((item) => item.name.toLowerCase().includes(searchTerm.value.toLowerCase()));
  }
});

function delayBlur(el: HTMLElement | undefined, focus: Ref<boolean>) {
  setTimeout(() => {
    if (document.activeElement != el) {
      focus.value = false;
    }
  }, 500);
}

function onSearchInputBlur() {
  delayBlur(searchInput.value, searchInputFocus);
}

function onListboxOptionsBlur() {
  delayBlur(listboxOptions.value, listboxOptionsFocus);
}

function reset() {
  selectedItem.value = null;
  searchTerm.value = '';
  nextTick(() => searchInput.value?.focus());
}

function onAction() {
  if (selectedItem.value) {
    emit('action', selectedItem.value.id);
    reset();
  }
}
</script>

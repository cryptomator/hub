<template>
  <div>
    <div class="flex rounded-md shadow-sm">
      <div class="relative flex items-stretch flex-grow focus-within:z-10">
        <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <UsersIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
        </div>
        <input id="searchTerm" type="text" name="searchTerm" class="focus:ring-primary focus:border-primary block w-full rounded-none rounded-l-md pl-10 sm:text-sm border-gray-300" placeholder="John Doe" />
      </div>
      <button type="button" class="-ml-px relative inline-flex items-center space-x-2 px-4 py-2 border border-transparent text-sm font-medium rounded-r-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary" @click="onAction()">
        {{ actionTitle }}
      </button>
    </div>
    <Listbox v-show="open" v-model="selectedItem" as="div" class="mt-1 relative">
      <transition leave-active-class="transition ease-in duration-100" leave-from-class="opacity-100" leave-to-class="opacity-0">
        <ListboxOptions class="absolute z-10 mt-1 w-full bg-white shadow-lg max-h-60 rounded-md py-1 text-base ring-1 ring-black ring-opacity-5 overflow-auto focus:outline-none sm:text-sm" static>
          <ListboxOption v-for="item in items" :key="item.id" v-slot="{ active, selected }" as="template" :value="item">
            <li :class="[active ? 'text-white bg-primary' : 'text-gray-900', 'cursor-default select-none relative py-2 pl-3 pr-9']">
              <span :class="[selected ? 'font-semibold' : 'font-normal', 'block truncate']">
                {{ item.name }}
              </span>

              <span v-if="selected" :class="[active ? 'text-white' : 'text-primary', 'absolute inset-y-0 right-0 flex items-center pr-4']">
                <CheckIcon class="h-5 w-5" aria-hidden="true" />
              </span>
            </li>
          </ListboxOption>
        </ListboxOptions>
      </transition>
    </Listbox>
  </div>
</template>

<script setup lang="ts">
import { Listbox, ListboxOption, ListboxOptions } from '@headlessui/vue';
import { CheckIcon, UsersIcon } from '@heroicons/vue/solid';
import { ref } from 'vue';

interface Item {
  id: string;
  name: string;
}

defineProps<{
  actionTitle: string
  items: Item[]
}>();
const emit = defineEmits<{
  (e: 'action', id: string): void
}>();

const open = ref(true);
const selectedItem = ref<Item | null>(null);

function onAction() {
  if (selectedItem.value) {
    emit('action', selectedItem.value.id);
  }
}
</script>

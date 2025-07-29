<template>
  <div class="relative w-full">
    <div
      :class="[
        'flex items-center flex-wrap min-h-[42px] rounded-md',
        { 
          'px-2 py-1 shadow-xs border border-gray-300 focus-within:ring-1 focus-within:ring-primary cursor-text': inputVisible 
        }
      ]"
      @click="focusInput"
    >
      <!-- Pills -->
      <button
        v-for="(user, index) in selectedUsers"
        :key="user.id"
        tabindex="-1"
        class="inline-flex items-center text-sm rounded-full px-2 py-1 mr-1 mb-1 border transition-colors shadow-sm"
        :class="{
          'bg-white text-gray-800': selectedPillIndex !== index,
          'bg-white ring-2 ring-primary': selectedPillIndex === index
        }"
        @click="onPillClick($event, user)"
      >
        <img :src="user.pictureUrl" class="w-4 h-4 rounded-full mr-1" />
        {{ user.name }}
        <span class="ml-1 trust-details">
          <TrustDetails
            :trusted-user="user as UserDto"
            :trusts="trusts"
            @trust-changed="refreshTrusts"
          />
        </span>
        <div v-if="inputVisible" class="ml-1 text-gray-500 hover:text-red-600">&times;</div>
      </button>
      <!-- Combobox -->
      <Combobox v-show="inputVisible" as="div" class="flex-1 relative" @update:model-value="onSelect">
        <ComboboxInput v-if="inputVisible" as="template">
          <input
            ref="inputEl"
            v-model="query"
            autocomplete="off"
            class="w-full min-w-[60px] h-9 border-none focus:ring-0 text-sm px-1 placeholder-gray-400"
            :class="{
              'caret-transparent': selectedPillIndex !== null,
              'caret-black': selectedPillIndex === null
            }"
            :placeholder="props.selectedUsers.length === 0 ? t('recoveryDialog.searchUser') : ''"
            @keydown="onKeyDown"
            @blur="onBlur"
          />
        </ComboboxInput>
      </Combobox>
    </div>
    <!-- DROPDOWN -->
    <div
      v-if="inputVisible && query && filteredUsers.length > 0"
      class="absolute z-10 mt-1 w-full rounded-md border border-gray-300 bg-white shadow-lg ring-1 ring-black/5 focus:outline-none sm:text-sm"
    >
      <div
        v-for="(user, index) in filteredUsers"
        :key="user.id"
        :class="[
          'cursor-pointer select-none py-2 px-3 flex items-center',
          (hoveredIndex === index || (hoveredIndex === null && activeIndex === index))
            ? 'bg-primary text-white'
            : 'hover:bg-primary'
        ]"
        @click="onSelect(user as T)"
        @mouseenter="hoveredIndex = index"
        @mouseleave="hoveredIndex = null"
      >
        <img :src="user.pictureUrl" alt="" class="h-5 w-5 rounded-full mr-2" />
        {{ user.name }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends Item">
import backend, { TrustDto, UserDto } from '../common/backend';
import { ref, computed, watch, nextTick, onMounted } from 'vue';
import { Combobox, ComboboxInput } from '@headlessui/vue';
import { useI18n } from 'vue-i18n';
import TrustDetails from './TrustDetails.vue';

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type: 'USER';
  memberSize?: number;

  email?: string;
  devices?: unknown[];
  accessibleVaults?: unknown[];
}

const trusts = ref<TrustDto[]>([]);

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  selectedUsers: T[];
  onSearch: (query: string) => Promise<T[]>;
  inputVisible: boolean;
}>();

const emit = defineEmits<{
  action: [item: T];
  remove: [item: T];
}>();

const inputVisible = computed(() => props.inputVisible !== false);

const query = ref('');
const searchResults = ref<T[]>([]);
const inputEl = ref<HTMLInputElement | null>(null);

const focusInput = () => {
  selectedPillIndex.value = null;
  nextTick(() => {
    inputEl.value?.focus();
  });
};

const activeIndex = ref(0);
const hoveredIndex = ref<number | null>(null);
const selectedPillIndex = ref<number | null>(null);

const filteredUsers = computed(() => {
  return searchResults.value.filter(
    (u) => !props.selectedUsers.some((sel) => sel.id === u.id)
  );
});

async function refreshTrusts() {
  trusts.value = await backend.trust.listTrusted();
}

function onPillClick(event: MouseEvent, user: T) {
  const target = event.target as HTMLElement;
  if (target.closest('.trust-details')) {
    return;
  }
  if (inputVisible.value) {
    removeUser(user);
  }
}

watch(query, async (newQuery) => {
  if (newQuery.trim() === '') {
    searchResults.value = [];
  } else {
    searchResults.value = await props.onSearch(newQuery);
  }
});

watch(query, async (newQuery) => {
  if (newQuery.trim() === '') {
    searchResults.value = [];
  } else {
    searchResults.value = await props.onSearch(newQuery);
  }
});

onMounted(async () => {
  await refreshTrusts();
});

function onSelect(user: T) {
  emit('action', user);
  query.value = '';
  searchResults.value = [];
  activeIndex.value = 0;
  nextTick(() => inputEl.value?.focus());
}

function removeUser(user: T) {
  emit('remove', user);
}

function onBlur() {
  selectedPillIndex.value = null;
}

function onKeyDown(e: KeyboardEvent) {
  const userCount = props.selectedUsers.length;

  if (e.key === 'Backspace') {
    if (query.value === '' && selectedPillIndex.value === null && userCount > 0) {
      selectedPillIndex.value = userCount - 1;
      e.preventDefault();
    } else if (selectedPillIndex.value !== null) {
      const user = props.selectedUsers[selectedPillIndex.value];
      removeUser(user);
      selectedPillIndex.value = selectedPillIndex.value == 0 ? (props.selectedUsers.length == 1 ? null : 0) : selectedPillIndex.value - 1;
      e.preventDefault();
    }
  } else if (e.key === 'ArrowLeft') {
    if (query.value === '' && userCount > 0) {
      if (selectedPillIndex.value === null) {
        selectedPillIndex.value = userCount - 1;
      } else if (selectedPillIndex.value > 0) {
        selectedPillIndex.value--;
      }
      e.preventDefault();
    }
  } else if (e.key === 'ArrowRight') {
    if (selectedPillIndex.value !== null) {
      if (selectedPillIndex.value < userCount - 1) {
        selectedPillIndex.value++;
      } else {
        selectedPillIndex.value = null;
        nextTick(() => inputEl.value?.focus());
      }
      e.preventDefault();
    }
  } else if (e.key === 'ArrowDown') {
    e.preventDefault();
    hoveredIndex.value = null;
    if (filteredUsers.value.length > 0) {
      activeIndex.value = (activeIndex.value + 1) % filteredUsers.value.length;
    }
  } else if (e.key === 'ArrowUp') {
    e.preventDefault();
    hoveredIndex.value = null;
    if (filteredUsers.value.length > 0) {
      activeIndex.value = (activeIndex.value - 1 + filteredUsers.value.length) % filteredUsers.value.length;
    }
  } else if (e.key === 'Enter' || e.key === 'Tab') {
    if (activeIndex.value >= 0 && filteredUsers.value[activeIndex.value]) {
      e.preventDefault();
      onSelect(filteredUsers.value[activeIndex.value] as T);
    }
  } else {
    selectedPillIndex.value = null;
  }
}

</script>

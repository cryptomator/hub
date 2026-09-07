<template>
  <div class="relative w-full">
    <div
      :class="[
        'flex items-center flex-wrap min-h-[54px] rounded-md px-2 py-1 shadow-xs border', 
        inputVisible ? 'focus-within:ring-1 bg-white' : 'bg-gray-100 cursor-not-allowed',
        props.hasError
          ? (inputVisible ? 'border-red-300 text-red-900 focus-within:ring-red-500 focus-within:border-red-500' : 'border-red-300/60 text-red-900')
          : (inputVisible ? 'border-gray-300 focus-within:ring-primary' : 'border-gray-300/60')
      ]"
      @click="focusInput"
    >
      <!-- Pills -->
      <button
        v-for="(user, index) in selectedUsers"
        :key="user.id"
        tabindex="-1"
        :disabled="!inputVisible"
        class="inline-flex items-center text-sm text-gray-800 rounded-full px-2 py-1 mt-1 mb-1 mr-1 border transition-colors shadow-sm gap-1"
        :class="{
          'bg-white': selectedPillIndex !== index,
          'bg-white ring-2 ring-primary': selectedPillIndex === index,
          'cursor-not-allowed': !inputVisible
        }"
        @click="onPillClick($event, user)"
      >
        <img :src="user.pictureUrl" class="w-4 h-4 rounded-full" :class="{ 'opacity-60': !inputVisible }" alt="" />
        <span :class="{ 'opacity-60': !inputVisible }">{{ user.name }}</span>
        <span v-if="user.type === 'USER'" class="trust-details">
          <TrustDetails
            :trusted-user="user as UserDto"
            :trusts="trusts"
            :dimmed="!inputVisible"
            @trust-changed="refreshTrusts"
          />
        </span>
        <span v-else class="trust-details" :class="{ 'opacity-60': !inputVisible }">
          <span class="inline-flex items-center bg-gray-50 ring-1 ring-inset ring-gray-500/10 mx-1 px-2 p-0.5 rounded-full">
            {{ user.memberSize }}
          </span>
        </span>
        <div v-if="inputVisible" class="text-gray-500 hover:text-red-600">&times;</div>
      </button>
      <!-- Combobox -->
      <Combobox :disabled="!inputVisible" @update:model-value="onSelect">
        <div class="flex-1 relative">
          <ComboboxInput :id="props.inputId" as="template">
            <input
              :id="props.inputId /* Just to silence SonarQube warnings. Gets overwritten by ComboboxInput's :id */"
              ref="inputEl"
              v-model="query"
              autocomplete="off"
              class="w-full min-w-[60px] h-9 border-none focus:ring-0 text-sm px-1 placeholder-gray-400"
              :class="{
                'caret-transparent': selectedPillIndex !== undefined,
                'caret-black': selectedPillIndex === undefined,
                'hidden': !inputVisible
              }"
              :readonly="!inputVisible"
              :tabindex="inputVisible ? undefined : -1"
              :aria-hidden="!inputVisible || undefined"
              :placeholder="props.placeholder || t('common.search.placeholder')"
              @keydown="onKeyDown"
              @blur="onBlur"
            />
          </ComboboxInput>
        </div>
      </Combobox>
      <div v-if="props.hasError" class="absolute left-1/2 -translate-x-1/2 -top-2 transform -translate-y-full">
        <div class="bg-red-50 border border-red-300 text-red-900 px-2 py-1 rounded shadow-sm text-sm hyphens-auto">
          {{ props.errorMessage || t('common.unexpectedError') }}
          <div class="absolute bottom-0 left-1/2 transform translate-y-1/2 rotate-45 w-2 h-2 bg-red-50 border-r border-b border-red-300"></div>
        </div>
      </div>
    </div>
    <!-- DROPDOWN -->
    <div
      v-if="showDropdown"
      class="absolute z-10 mt-1 w-full rounded-md border border-gray-300 bg-white shadow-lg ring-1 ring-black/5 focus:outline-none sm:text-sm"
    >
      <template v-if="filteredUsers.length > 0">
        <div
          v-for="(user, index) in filteredUsers"
          :key="user.id"
          :class="[
            'cursor-pointer select-none py-2 px-3 flex items-center',
            (hoveredIndex === index || (hoveredIndex === undefined && activeIndex === index))
              ? 'bg-primary text-white'
              : 'hover:bg-primary'
          ]"
          @click="onSelect(user as T)"
          @mouseenter="hoveredIndex = index"
          @mouseleave="hoveredIndex = undefined"
        >
          <img :src="user.pictureUrl" alt="" class="h-5 w-5 rounded-full mr-2" />
          {{ user.name }}
          <span v-if="user.type === 'GROUP'" class="ml-1 trust-details">
            <span class="inline-flex items-center bg-gray-50 ring-1 ring-inset ring-gray-500/10 mx-1 px-2 p-0.5 rounded-full focus:outline-hidden focus:ring-primary text-black">
              {{ user.memberSize }}
            </span>
          </span>
        </div>
      </template>
      <div v-else-if="searching" class="select-none py-2 px-3 text-gray-500">
        {{ t('common.search.searching') }}
      </div>
      <div v-else class="select-none py-2 px-3 text-gray-500">
        {{ props.noResultsText || t('common.search.empty') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends AuthorityDto">
import backend, { AuthorityDto, TrustDto, UserDto } from '../common/backend';
import { ref, computed, watch, nextTick, onMounted } from 'vue';
import { Combobox, ComboboxInput } from '@headlessui/vue';
import { useI18n } from 'vue-i18n';
import TrustDetails from './TrustDetails.vue';

export type MultiUserSelectExpose = {
  focus: () => Promise<void> | void;
};

const trusts = ref<TrustDto[]>([]);

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  selectedUsers: T[];
  onSearch: (query: string) => Promise<T[]>;
  inputVisible: boolean;
  hasError?: boolean;
  errorMessage?: string;
  placeholder?: string;
  inputId?: string;
  noResultsText?: string;
}>();

const emit = defineEmits<{
  action: [item: T];
  remove: [item: T];
}>();

const query = ref('');
const searchResults = ref<T[]>([]);
const searching = ref(false);
let searchSeq = 0;

const inputEl = ref<HTMLInputElement>();

async function focus() {
  selectedPillIndex.value = undefined;
  await nextTick();

  if (props.inputVisible) inputEl.value?.focus();
}

defineExpose<MultiUserSelectExpose>({
  focus
});

const focusInput = () => {
  if (!props.inputVisible) return;
  selectedPillIndex.value = undefined;
  nextTick(() => {
    inputEl.value?.focus();
  });
};

const activeIndex = ref(0);
const hoveredIndex = ref<number>();
const selectedPillIndex = ref<number>();

const filteredUsers = computed(() => {
  return searchResults.value.filter(
    (u) => !props.selectedUsers.some((sel) => sel.id === u.id)
  );
});

const showDropdown = computed(() => props.inputVisible && query.value.trim().length > 0);

async function refreshTrusts() {
  trusts.value = await backend.trust.listTrusted();
}

function onPillClick(event: MouseEvent, user: T) {
  const target = event.target as HTMLElement;
  if (target.closest('.trust-details')) {
    return;
  }
  if (props.inputVisible) {
    removeUser(user);
  }
}

watch(query, async (newQuery) => {
  const seq = ++searchSeq;
  if (newQuery.trim() === '') {
    searchResults.value = [];
    searching.value = false;
    return;
  }
  searching.value = true;
  try {
    const results = await props.onSearch(newQuery);
    if (seq === searchSeq) {
      searchResults.value = results;
    }
  } catch (error) {
    console.error('User search failed.', error);
    if (seq === searchSeq) {
      searchResults.value = [];
    }
  } finally {
    if (seq === searchSeq) {
      searching.value = false;
    }
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
  selectedPillIndex.value = undefined;
}

function onKeyDown(e: KeyboardEvent) {
  if (!props.inputVisible) return;
  const userCount = props.selectedUsers.length;

  if (e.key === 'Backspace') {
    if (query.value === '' && selectedPillIndex.value === undefined && userCount > 0) {
      selectedPillIndex.value = userCount - 1;
      e.preventDefault();
    } else if (selectedPillIndex.value !== undefined) {
      const user = props.selectedUsers[selectedPillIndex.value];
      removeUser(user);
      selectedPillIndex.value = selectedPillIndex.value == 0 ? (props.selectedUsers.length === 1 ? undefined : 0) : selectedPillIndex.value - 1;
      e.preventDefault();
    }
  } else if (e.key === 'ArrowLeft') {
    if (query.value === '' && userCount > 0) {
      if (selectedPillIndex.value === undefined) {
        selectedPillIndex.value = userCount - 1;
      } else if (selectedPillIndex.value > 0) {
        selectedPillIndex.value--;
      }
      e.preventDefault();
    }
  } else if (e.key === 'ArrowRight') {
    if (selectedPillIndex.value !== undefined) {
      if (selectedPillIndex.value < userCount - 1) {
        selectedPillIndex.value++;
      } else {
        selectedPillIndex.value = undefined;
        nextTick(() => inputEl.value?.focus());
      }
      e.preventDefault();
    }
  } else if (e.key === 'ArrowDown') {
    e.preventDefault();
    hoveredIndex.value = undefined;
    if (filteredUsers.value.length > 0) {
      activeIndex.value = (activeIndex.value + 1) % filteredUsers.value.length;
    }
  } else if (e.key === 'ArrowUp') {
    e.preventDefault();
    hoveredIndex.value = undefined;
    if (filteredUsers.value.length > 0) {
      activeIndex.value = (activeIndex.value - 1 + filteredUsers.value.length) % filteredUsers.value.length;
    }
  } else if (e.key === 'Enter' || e.key === 'Tab') {
    if (activeIndex.value >= 0 && filteredUsers.value[activeIndex.value]) {
      e.preventDefault();
      onSelect(filteredUsers.value[activeIndex.value] as T);
    }
  } else {
    selectedPillIndex.value = undefined;
  }
}

</script>

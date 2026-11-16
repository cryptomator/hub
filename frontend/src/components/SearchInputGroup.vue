<template>
  <div class="flex rounded-md shadow-xs">
    <div class="relative flex items-stretch grow focus-within:z-10" @keydown.capture="onKeydown" @pointerdown.capture="onPointer($event, true)" @pointermove.capture="onPointer($event, false)">
      <Combobox v-slot="{ activeOption }" as="div" class="w-full" @update:model-value="value => selectedItem = value">
        <HighlightReporter :option="activeOption" @change="onHighlight" />
        <div class="relative">
          <div v-if="showIcon" class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <UsersIcon v-if="selectedItem === undefined" class="h-5 w-5 text-gray-400" aria-hidden="true" />
            <img v-else :src="selectedItem.pictureUrl ?? ''" alt="" class="w-5 h-5 rounded-full" />
          </div>

          <ComboboxInput v-if="selectedItem === undefined" :id="inputId" ref="comboboxInput" v-focus="autoFocus" :disabled="disabled" :class="fieldShapeClass" class="w-full h-10 border border-gray-300 bg-white py-2 shadow-xs focus:border-primary focus:outline-hidden focus:ring-1 focus:ring-primary sm:text-sm disabled:bg-primary-l2" :placeholder="placeHolder" @change="query = $event.target.value" />
          <div v-else :class="fieldShapeClass" class="w-full h-10 border border-gray-300 bg-primary-l2 py-2 flex items-center justify-between shadow-xs sm:text-sm">
            <span class="truncate">{{ selectedItem.name }}</span>
            <span v-if="selectedItem.type === 'GROUP'" class="ml-3 text-gray-500 text-xs italic whitespace-nowrap">{{ t('common.xMembers', [selectedItem.memberSize]) }}</span>
          </div>
        </div>

        <ComboboxOptions v-if="selectedItem === undefined && filteredItems.length > 0" class="absolute z-10 mt-1 max-h-56 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black/5 focus:outline-hidden sm:text-sm">
          <ComboboxOption v-for="item in filteredItems" :key="item.id" :value="item" class="relative cursor-default select-none py-2 pl-3 pr-9 ui-not-active:text-gray-900 ui-active:bg-primary ui-active:text-white">
            <div class="flex items-center">
              <img v-if="item.pictureUrl" :src="item.pictureUrl" class="w-6 h-6 shrink-0 rounded-full" alt="group icon" />
              <div v-else class="w-6 h-6 flex items-center rounded-full rounded-full border border-gray-300 bg-white justify-center overflow-hidden">
                <UserGroupIcon class="w-4 h-4 shrink-0 text-gray-400" aria-hidden="true" />
              </div>
              <span class="ml-3 truncate">{{ item.name }}</span>
              <span v-if="item.type === 'GROUP'" class="ml-3 text-gray-500 text-xs italic whitespace-nowrap ui-active:text-white">{{ t('common.xMembers', [item.memberSize]) }}</span>
            </div>
          </ComboboxOption>
        </ComboboxOptions>
      </Combobox>

      <button v-if="selectedItem !== undefined" :disabled="disabled" type="button" class="absolute inset-y-0 right-0 pr-3 flex items-center disabled:cursor-not-allowed disabled:opacity-50" @click="reset()">
        <XCircleIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
      </button>
    </div>

    <button v-if="hasActionButton" ref="actionButton" :disabled="selectedItem === undefined || disabled" type="button" class="-ml-px relative inline-flex items-center space-x-2 px-4 py-2 border border-transparent text-sm font-medium rounded-r-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary focus:border-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" @click="onAction()">
      {{ actionTitle }}
    </button>
  </div>
</template>

<script setup lang="ts" generic="T extends Item">
import { Combobox, ComboboxInput, ComboboxOption, ComboboxOptions } from '@headlessui/vue';
import { UsersIcon, UserGroupIcon, XCircleIcon } from '@heroicons/vue/24/solid';
import { computed, defineComponent, nextTick, ref, shallowRef, watch } from 'vue';
import type { PropType } from 'vue';
import { useI18n } from 'vue-i18n';
import { debounce } from '../common/util';

export type Item = {
  id: string;
  name: string;
  pictureUrl?: string;
  type?: string;
  memberSize?: number;
};

export type SearchInputGroupExpose = {
  focusTarget?: HTMLElement;
};

const { t } = useI18n({ useScope: 'global' });

const props = withDefaults(defineProps<{
  actionTitle?: string
  placeHolder: string
  onSearch: (query: string) => Promise<T[]>
  disabled?: boolean
  autoFocus?: boolean
  showIcon?: boolean
  inputId?: string
}>(), {
  actionTitle: undefined,
  disabled: false,
  autoFocus: true,
  showIcon: true,
  inputId: undefined,
});

const emit = defineEmits<{
  action: [item: T]
  highlight: [item: T | undefined]
}>();

const vFocus = {
  mounted: (el: HTMLElement, binding: { value?: boolean }) => {
    if (binding.value !== false) {
      el.focus();
    }
  }
};

const comboboxInput = ref<InstanceType<typeof ComboboxInput>>();
const actionButton = ref<HTMLButtonElement>();
defineExpose({
  focusTarget: computed<HTMLElement | undefined>(() => comboboxInput.value?.$el ?? actionButton.value)
});

const hasActionButton = computed(() => !!props.actionTitle);
const fieldShapeClass = computed(() => [hasActionButton.value ? 'rounded-l-md' : 'rounded-md', props.showIcon ? 'px-10' : 'pl-3 pr-10']);

// Without an action button, a selection acts immediately. Headless UI also selects the active option on Tab and blur,
// and activates the first result on its own, so only a click on a result or Enter after moving the highlight counts.
let userMovedHighlight = false;
let explicitPick = false;

function onKeydown(event: KeyboardEvent) {
  if (['ArrowDown', 'ArrowUp', 'Home', 'End', 'PageDown', 'PageUp'].includes(event.key)) {
    userMovedHighlight = true;
  } else if (event.key === 'Enter') {
    explicitPick = userMovedHighlight;
  } else if (event.key === 'Tab') {
    explicitPick = false;
  }
}

function onPointer(event: PointerEvent, isPress: boolean) {
  const onOption = (event.target as Element).closest('[role="option"]') !== null;
  if (onOption) {
    userMovedHighlight = true;
  }
  if (isPress) {
    explicitPick = onOption;
  }
}

function onHighlight(option: T | undefined) {
  emit('highlight', userMovedHighlight ? option : undefined);
}

const selectedItem = shallowRef<T>();
watch(selectedItem, (value) => {
  const wasExplicit = explicitPick;
  explicitPick = false;
  if (value === undefined) {
    return;
  }
  if (hasActionButton.value) {
    nextTick(() => actionButton.value?.focus());
  } else if (wasExplicit) {
    onAction();
  } else {
    reset();
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
  userMovedHighlight = false;
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
  selectedItem.value = undefined;
  query.value = '';
  emit('highlight', undefined);
}

// reports the option the user is pointing at (hover or arrow keys), so a parent can preview it
const HighlightReporter = defineComponent({
  props: { option: { type: Object as PropType<T | null>, default: null } },
  emits: { change: (_option: T | undefined) => true },
  setup(props, { emit: emitChange }) {
    watch(() => props.option, option => emitChange('change', option ?? undefined));
    return () => null;
  }
});
</script>

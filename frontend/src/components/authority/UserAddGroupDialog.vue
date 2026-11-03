<template>
  <HubDialog v-model:open="open" :title="t('user.addGroups.title', [user.name])" :busy="processing" :error="onAddGroupsError && t('common.unexpectedError', [onAddGroupsError.message])" :initial-focus="searchInputGroup?.focusTarget" @submit="addGroups()">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('user.addGroups.description', [user.name]) }}
    </DialogDescription>
    <div class="mt-5">
      <SearchInputGroup ref="searchInputGroup" :action-title="t('common.select')" :place-holder="t('user.addGroups.searchLabel')" :on-search="searchGroup" :disabled="processing" @action="selectGroup" />
    </div>
    <div class="mt-5">
      <p v-if="newGroups.length > 0" class="text-sm font-medium text-gray-700">
        {{ t('user.addGroups.selected', [newGroups.length]) }}
      </p>
      <TransitionGroup tag="ul" class="max-h-64 divide-y divide-gray-200 overflow-y-auto" :class="{ 'mt-2 border-t border-gray-200': newGroups.length > 0 }" enter-active-class="transition-all duration-250 ease-out" enter-from-class="bg-green-50 opacity-0 scale-95" enter-to-class="bg-white opacity-100 scale-100" leave-active-class="transition-all duration-250 ease-in" leave-from-class="bg-white opacity-100 scale-100" leave-to-class="bg-red-50 opacity-0 scale-95">
        <li v-for="group in newGroups" :key="group.id" class="flex items-center justify-between gap-3 py-2 transform transition">
          <div class="flex min-w-0 items-center" :title="group.name">
            <img :src="group.pictureUrl" class="h-8 w-8 shrink-0 rounded-full border border-gray-300 bg-white object-cover" alt="" />
            <p class="ml-4 truncate text-sm font-medium text-gray-900">
              {{ group.name }}
            </p>
          </div>
          <button type="button" :disabled="processing" class="shrink-0 rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-500 focus:outline-hidden focus:ring-2 focus:ring-primary disabled:cursor-not-allowed disabled:opacity-50" @click="deselectGroup(group.id)">
            <span class="sr-only">{{ t('common.remove') }}</span>
            <XMarkIcon class="h-5 w-5" aria-hidden="true" />
          </button>
        </li>
      </TransitionGroup>
      <p v-if="newGroups.length === 0" class="rounded-md border border-dashed border-gray-300 px-4 py-6 text-center text-sm text-gray-500">
        {{ t('user.addGroups.empty') }}
      </p>
    </div>

    <template #footer>
      <button type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing || newGroups.length === 0" :class="primaryButton">
        {{ newGroups.length > 0 ? t('user.addGroups.submitCount', [newGroups.length]) : t('user.addGroups.submit') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { XMarkIcon } from '@heroicons/vue/20/solid';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { GroupDto, UserDto } from '../../common/backend';
import { primaryButton, secondaryButton } from '../dialogButtons';
import HubDialog from '../HubDialog.vue';
import SearchInputGroup from '../SearchInputGroup.vue';
import type { SearchInputGroupExpose } from '../SearchInputGroup.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const searchInputGroup = ref<SearchInputGroupExpose>();
const newGroups = ref<GroupDto[]>([]);
const onAddGroupsError = ref<Error>();

const props = defineProps<{
  user: UserDto
  groups: GroupDto[]
}>();

const emit = defineEmits<{
  saved: [added: GroupDto[]]
}>();

defineExpose({
  show
});

function show() {
  newGroups.value = [];
  onAddGroupsError.value = undefined;
  open.value = true;
}

function isKnown(id: string) {
  return props.groups.some(g => g.id === id);
}

async function searchGroup(query: string): Promise<GroupDto[]> {
  if (!query.trim()) {
    return [];
  }
  try {
    const results = await backend.authorities.search(query, true);
    return results
      .filter((g): g is GroupDto => g.type === 'GROUP')
      .filter(g => !isKnown(g.id) && !newGroups.value.some(n => n.id === g.id))
      .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
  } catch (error) {
    console.error('Searching groups failed.', error);
    return [];
  }
}

function selectGroup(group: GroupDto) {
  if (isKnown(group.id) || newGroups.value.some(g => g.id === group.id)) {
    return;
  }
  newGroups.value.unshift(group);
}

function deselectGroup(id: string) {
  newGroups.value = newGroups.value.filter(g => g.id !== id);
}

async function addGroups() {
  if (processing.value || !open.value || newGroups.value.length === 0) {
    return;
  }
  onAddGroupsError.value = undefined;
  const added: GroupDto[] = [];
  try {
    processing.value = true;
    for (const group of newGroups.value) {
      await backend.groups.addMember(group.id, props.user.id);
      added.push(group);
    }
    open.value = false;
  } catch (error) {
    console.error('Adding user to groups failed.', error);
    onAddGroupsError.value = error instanceof Error ? error : new Error('Unknown Error');
    // groups added before the failure are saved already, so only the rest stays selected for a retry:
    const addedIds = new Set(added.map(group => group.id));
    newGroups.value = newGroups.value.filter(group => !addedIds.has(group.id));
  } finally {
    processing.value = false;
    if (added.length > 0) {
      emit('saved', added);
    }
  }
}
</script>

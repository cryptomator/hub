<template>
  <HubDialog v-model:open="open" :title="t('group.addMembers.title', [group.name])" :busy="processing" :error="onAddMembersError && t('common.unexpectedError', [onAddMembersError.message])" :initial-focus="searchInputGroup?.focusTarget" @submit="addMembers()">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('group.addMembers.description') }}
    </DialogDescription>
    <div class="mt-5">
      <SearchInputGroup ref="searchInputGroup" :action-title="t('common.select')" :place-holder="t('group.addMembers.searchLabel')" :on-search="searchUser" :disabled="processing" @action="selectUser" />
    </div>
    <div class="mt-5">
      <p v-if="newMembers.length > 0" class="text-sm font-medium text-gray-700">
        {{ t('group.addMembers.selected', [newMembers.length]) }}
      </p>
      <TransitionGroup tag="ul" class="max-h-64 divide-y divide-gray-200 overflow-y-auto" :class="{ 'mt-2 border-t border-gray-200': newMembers.length > 0 }" enter-active-class="transition-all duration-250 ease-out" enter-from-class="bg-green-50 opacity-0 scale-95" enter-to-class="bg-white opacity-100 scale-100" leave-active-class="transition-all duration-250 ease-in" leave-from-class="bg-white opacity-100 scale-100" leave-to-class="bg-red-50 opacity-0 scale-95">
        <li v-for="member in newMembers" :key="member.id" class="flex items-center justify-between gap-3 py-2 transform transition">
          <div class="flex min-w-0 items-center" :title="member.name">
            <img :src="member.pictureUrl" class="h-8 w-8 shrink-0 rounded-full border border-gray-300 object-cover" alt="" />
            <p class="ml-4 truncate text-sm font-medium text-gray-900">
              {{ member.name }}
            </p>
          </div>
          <button type="button" :disabled="processing" class="shrink-0 rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-500 focus:outline-hidden focus:ring-2 focus:ring-primary disabled:cursor-not-allowed disabled:opacity-50" @click="deselectMember(member.id)">
            <span class="sr-only">{{ t('common.remove') }}</span>
            <XMarkIcon class="h-5 w-5" aria-hidden="true" />
          </button>
        </li>
      </TransitionGroup>
      <p v-if="newMembers.length === 0" class="rounded-md border border-dashed border-gray-300 px-4 py-6 text-center text-sm text-gray-500">
        {{ t('group.addMembers.empty') }}
      </p>
    </div>

    <template #footer>
      <button type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing || newMembers.length === 0" :class="primaryButton">
        {{ newMembers.length > 0 ? t('group.addMembers.submitCount', [newMembers.length]) : t('group.addMembers.submit') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { XMarkIcon } from '@heroicons/vue/20/solid';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AuthorityDto, GroupDto, UserDto } from '../../common/backend';
import { primaryButton, secondaryButton } from '../dialogButtons';
import HubDialog from '../HubDialog.vue';
import SearchInputGroup from '../SearchInputGroup.vue';
import type { SearchInputGroupExpose } from '../SearchInputGroup.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const searchInputGroup = ref<SearchInputGroupExpose>();
const newMembers = ref<AuthorityDto[]>([]);
const onAddMembersError = ref<Error>();

const props = defineProps<{
  group: GroupDto
  members: AuthorityDto[]
}>();

const emit = defineEmits<{
  saved: [added: AuthorityDto[]]
}>();

defineExpose({
  show
});

function show() {
  newMembers.value = [];
  onAddMembersError.value = undefined;
  open.value = true;
}

function isKnown(id: string) {
  return props.members.some(u => u.id === id);
}

async function searchUser(query: string): Promise<UserDto[]> {
  if (!query.trim()) {
    return [];
  }
  try {
    const results = await backend.authorities.search(query);
    return results
      .filter((u): u is UserDto => u.type === 'USER')
      .filter(u => !isKnown(u.id) && !newMembers.value.some(n => n.id === u.id))
      .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
  } catch (error) {
    console.error('Searching users failed.', error);
    return [];
  }
}

function selectUser(user: UserDto) {
  if (isKnown(user.id) || newMembers.value.some(u => u.id === user.id)) {
    return;
  }
  newMembers.value.unshift(user);
}

function deselectMember(id: string) {
  newMembers.value = newMembers.value.filter(u => u.id !== id);
}

async function addMembers() {
  if (processing.value || !open.value || newMembers.value.length === 0) {
    return;
  }
  onAddMembersError.value = undefined;
  try {
    processing.value = true;
    for (const member of newMembers.value) {
      await backend.groups.addMember(props.group.id, member.id);
    }
    emit('saved', [...newMembers.value]);
    open.value = false;
  } catch (error) {
    console.error('Adding members failed.', error);
    onAddMembersError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

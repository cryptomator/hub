<template>
  <TransitionRoot as="template" :show="open">
    <Dialog as="div" class="fixed inset-0 z-10 overflow-y-auto" @close="open = false">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in  duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75" @click.stop />
      </TransitionChild>
      <div class="flex min-h-full items-end justify-center p-4 sm:items-center sm:p-0">
        <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
          <DialogPanel class="relative flex w-full h-[32rem] transform flex-col overflow-hidden rounded-lg bg-white shadow-xl transition-all sm:my-8 sm:max-w-lg">
            <form novalidate class="flex flex-1 min-h-0 flex-col p-4" @submit.prevent="onSubmit">
              <div class="flex flex-1 min-h-0 flex-col overflow-hidden bg-white p-1 mb-4">
                <DialogTitle class="text-lg font-medium text-gray-900">
                  {{ t('group.addMembers.title') }}
                </DialogTitle>
                <div class="flex flex-col p-1 mt-4">
                  <SearchInputGroup :action-title="t('common.add')" :place-holder="t('group.addMembers.searchLabel')" :on-search="searchUser" @action="addUser" />
                  <p v-if="onAddUserError" class="mt-1 text-sm text-red-900 text-right">
                    {{ t('common.unexpectedError', [onAddUserError.message]) }}
                  </p>
                </div>
                <div ref="scrollContainer" class="mt-4 flex flex-1 min-h-0 flex-col overflow-y-auto">
                  <TransitionGroup tag="ul" class="flex-1 min-h-0 divide-y divide-gray-200" enter-active-class="transition-all duration-250 ease-out" enter-from-class="bg-green-50 opacity-0 scale-95" enter-to-class="bg-white opacity-100 scale-100" leave-active-class="transition-all duration-250 ease-in" leave-from-class="bg-white opacity-100 scale-100" leave-to-class="bg-red-50 opacity-0 scale-95">
                    <li v-for="member in sortedNewMembers" :key="member.id" class="flex flex-col py-2 border-b border-gray-200 border-l-4 border-transparent mx-1 last:border-b-0 transform transition">
                      <div class="flex items-center justify-between">
                        <div class="flex items-center w-full" :title="member.name">
                          <img :src="member.pictureUrl" class="w-8 h-8 rounded-full border border-gray-300 object-cover" alt="user icon" />
                          <p class="ml-4 text-sm font-medium truncate">
                            {{ member.name }}
                          </p>
                        </div>
                        <button type="button" class="cursor-pointer text-red-600 hover:text-red-900" :title="t('common.remove')" @click="removeTempMember(member.id)">{{ t('common.remove') }}</button>
                      </div>
                    </li>
                  </TransitionGroup>
                </div>
              </div>
              <div class="flex-shrink-0 bg-gray-50 -m-4 py-3 px-6 sm:flex sm:flex-row-reverse sm:space-x-4 sm:space-x-reverse">
                <button type="submit" :disabled="isSaving" class="w-full sm:w-auto inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed">
                  {{ t('common.save') }}
                </button>
                <button type="button" :disabled="isSaving" class="mt-3 sm:mt-0 w-full sm:w-auto inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed" @click="open = false">
                  {{ t('common.cancel') }}
                </button>
                <span class="mt-3 sm:mt-0 flex items-center text-sm text-gray-600 sm:mr-auto">
                  {{ selectedCount }} {{ t('group.addMembers.selectedUser') }}
                </span>
              </div>
            </form>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import SearchInputGroup from '../SearchInputGroup.vue';
import backend, { AuthorityDto, UserDto } from '../../common/backend';

const props = defineProps<{ groupId: string; members: AuthorityDto[] }>();
const emit  = defineEmits<{ saved: [added: AuthorityDto[]] }>();

const { t } = useI18n({ useScope: 'global' });
const open = ref(false);
const newMembers = ref<AuthorityDto[]>([]);
const onAddUserError = ref<Error>();
const isSaving = ref(false);

const selectedCount = computed(() => newMembers.value.length);
const sortedNewMembers = computed(() => [...newMembers.value].reverse());

function isKnown(id: string) {
  return props.members.some(u => u.id === id);
}

async function searchUser(query: string): Promise<UserDto[]> {
  if (!query.trim()) return [];

  try {
    const results = await backend.authorities.search(query);
    // Filter to only users (not groups) and exclude existing members
    return results
      .filter((a): a is UserDto => a.type === 'USER')
      .filter(u => !isKnown(u.id) && !newMembers.value.some(n => n.id === u.id))
      .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
  } catch (error) {
    console.error('Search failed:', error);
    return [];
  }
}

function addUser(user: UserDto) {
  try {
    if (isKnown(user.id) || newMembers.value.some(u => u.id === user.id)) {
      return;
    }
    newMembers.value.push(user);
  } catch (e) { onAddUserError.value = e as Error; }
}

function removeTempMember(id: string) {
  newMembers.value = newMembers.value.filter(u => u.id !== id);
}

async function onSubmit() {
  if (newMembers.value.length === 0) {
    open.value = false;
    return;
  }

  isSaving.value = true;
  onAddUserError.value = undefined;

  try {
    // Add each member to the group via API
    for (const member of newMembers.value) {
      await backend.groups.addMember(props.groupId, member.id);
    }
    emit('saved', [...newMembers.value]);
    open.value = false;
  } catch (error) {
    console.error('Adding members failed:', error);
    onAddUserError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    isSaving.value = false;
  }
}

function show() {
  newMembers.value = [];
  open.value = true;
}

defineExpose({ show });
</script>
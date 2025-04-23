<template>
  <TransitionRoot as="template" :show="open">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75 transition-opacity" @click.stop />
      </TransitionChild>
      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
            <DialogPanel class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
              <form novalidate @submit.prevent="onSubmit">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                    {{ t('accessGroupDialog.title') }}
                  </DialogTitle>
                  <div class="mt-4 space-y-4">
                    <div class="mt-6">
                      <h3 class="font-medium text-gray-900">
                        {{ t('editGroupDialog.members.title') }}
                      </h3>
                      <ul class="mt-2 border-t border-b border-gray-200 divide-y divide-gray-200">
                        <template v-for="member in groupMembers" :key="member.id">
                          <li class="py-3 flex flex-col">
                            <div class="flex justify-between items-center">
                              <div class="flex items-center whitespace-nowrap w-full" :title="member.name">
                                <img :src="member.pictureUrl" alt="" class="w-8 h-8 rounded-full" />
                                <p class="w-full ml-4 text-sm font-medium text-gray-900 truncate">
                                  {{ member.name }}
                                </p>
                              </div>
                              <button type="button" class="ml-3 text-red-600 hover:text-red-800" @click="removeGroupMember(member.id)">
                                {{ t('common.remove') }}
                              </button>
                            </div>
                          </li>
                        </template>
                        <li class="py-2 flex flex-col">
                          <div v-if="!addingUser" class="flex justify-between items-center">
                            <button type="button" class="group -ml-1 bg-white p-1 rounded-md flex items-center focus:outline-hidden focus:ring-2 focus:ring-primary" @click="addingUser = true">
                              <span class="w-8 h-8 rounded-full border-2 border-dashed border-gray-300 flex items-center justify-center text-gray-400">
                                <PlusSmallIcon class="h-5 w-5" aria-hidden="true" />
                              </span>
                              <span class="ml-4 text-sm font-medium text-primary group-hover:text-primary-l1">
                                {{ t('editGroupDialog.addMember') }}
                              </span>
                            </button>
                          </div>
                          <SearchInputGroup v-else :action-title="t('common.add')" :on-search="searchUser" @action="addUser" />
                          <div v-if="onAddUserError" class="mt-1">
                            <p class="text-sm text-red-900 text-right">
                              {{ t('common.unexpectedError', [onAddUserError.message]) }}
                            </p>
                          </div>
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm">
                    {{ t('common.save') }}
                  </button>
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                </div>
              </form>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import SearchInputGroup from './SearchInputGroup.vue';
import { PlusSmallIcon } from '@heroicons/vue/24/solid';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);

type User = { id: string; name: string; pictureUrl: string };
const groupMembers = ref<User[]>([]);
const addingUser = ref(false);
const onAddUserError = ref<Error | null>(null);

async function fetchGroupMembersExample(): Promise<User[]> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve([
        { id: '1', name: 'Julian', pictureUrl: 'https://skymatic.de/img/team-julian.jpg' },
        { id: '2', name: 'Armin', pictureUrl: 'https://skymatic.de/img/team-armin.jpg' },
        { id: '3', name: 'Tobi', pictureUrl: 'https://skymatic.de/img/team-tobias.jpg' }
      ]);
    }, 300);
  });
}

async function show() {
  groupMembers.value = await fetchGroupMembersExample();
  open.value = true;
}

function onSubmit() {
  console.log('Gespeicherte Daten:', {
    groupMembers: groupMembers.value
  });
  open.value = false;
}

async function searchUser(query: string): Promise<User[]> {
  if (query.trim().length === 0) return [];
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve([{ id: '4', name: 'Sebi', pictureUrl: 'https://skymatic.de/img/team-sebastian.jpg' }]);
    }, 300);
});
}

async function addUser(user: User) {
  onAddUserError.value = null;
  try {
    groupMembers.value.push(user);
    addingUser.value = false;
  } catch (error) {
    onAddUserError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function removeGroupMember(memberId: string) {
  groupMembers.value = groupMembers.value.filter(member => member.id !== memberId);
}

defineExpose({
  show,
});
</script>

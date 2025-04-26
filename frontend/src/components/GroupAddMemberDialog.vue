<template>
  <TransitionRoot as="template" :show="open">
    <Dialog as="div" class="fixed inset-0 z-10 overflow-y-auto">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in  duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75" @click.stop />
      </TransitionChild>
      <div class="flex min-h-full items-end justify-center p-4 sm:items-center sm:p-0">
        <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
          <DialogPanel class="relative flex w-full max-h-[80vh] min-h-[30rem] transform flex-col overflow-hidden rounded-lg bg-white shadow-xl transition-all sm:my-8 sm:max-w-lg">
            <form novalidate class="flex flex-1 min-h-0 flex-col" @submit.prevent="onSubmit">
              <div class="flex flex-1 min-h-0 flex-col overflow-hidden bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <DialogTitle class="text-lg font-medium text-gray-900">
                  Add Members
                </DialogTitle>
                <div class="mt-6 flex flex-1 min-h-0 flex-col overflow-y-auto">
                  <h3 class="font-medium text-gray-900">
                    Search by username, full name or email address
                  </h3>
                  <ul class="mt-2 flex-1 min-h-0 divide-y divide-gray-200">
                    <li class="py-2 flex flex-col p-1">
                      <SearchInputGroup :action-title="t('common.add')" :on-search="searchUser" @action="addUser"/>
                      <p v-if="onAddUserError" class="mt-1 text-sm text-red-900 text-right">
                        {{ t('common.unexpectedError', [onAddUserError.message]) }}
                      </p>
                    </li>
                    <template v-for="member in sortedNewMembers" :key="member.id" >
                      <li class="py-3 flex flex-col">
                        <div class="flex items-center justify-between">
                          <div class="flex items-center w-full" :title="member.name">
                            <img :src="member.userPicture" class="w-8 h-8 rounded-full" />
                            <p class="ml-4 text-sm font-medium truncate">{{ member.name }}</p>
                          </div>
                          <button type="button" class="ml-3 text-red-600 hover:text-red-800" @click="removeTempMember(member.id)">
                            {{ t('common.remove') }}
                          </button>
                        </div>
                      </li>
                    </template>
                  </ul>
                </div>
              </div>
              <div class="flex-shrink-0 bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:space-x-4 sm:space-x-reverse sm:px-6">
                <button type="submit" class="w-full sm:w-auto inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">
                  {{ t('common.save') }}
                </button>
                <button type="button" class="mt-3 sm:mt-0 w-full sm:w-auto inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="open = false">
                  {{ t('common.cancel') }}
                </button>
                <span class="mt-3 sm:mt-0 flex items-center text-sm text-gray-600 sm:mr-auto">
                  {{ selectedCount }} Member(s) selected
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
import SearchInputGroup from './SearchInputGroup.vue';
import { PlusSmallIcon } from '@heroicons/vue/24/solid';

type User = { id: string; name: string; userPicture: string; role: string };

const props = defineProps<{ members: User[] }>();
const emit  = defineEmits<{ saved: (added: User[]) => void }>();

const { t } = useI18n({ useScope: 'global' });
const open = ref(false);
const addingUser = ref(false);
const newMembers = ref<User[]>([]);
const onAddUserError = ref<Error | null>(null);  
const selectedCount = computed(() => newMembers.value.length);

const sortedNewMembers = computed(() =>
  [...newMembers.value].sort((a, b) =>
    a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }),
  )
);

function isKnown(id: string) {
  return props.members.some(u => u.id === id);
}

function show() {
  newMembers.value = [];
  open.value = true;
}

async function searchUser(query: string): Promise<User[]> {
  if (!query.trim()) return [];

  const results: User[] = await new Promise(resolve =>
    setTimeout(() => resolve([
      { id: '26', name: 'Nikolai Ivanov', userPicture: 'https://i.pravatar.cc/50?u=nikolaiivanov', role: 'admin' },
      { id: '27', name: 'Grace Kim', userPicture: 'https://i.pravatar.cc/50?u=gracekim', role: 'admin' },
      { id: '28', name: 'Pedro Martins', userPicture: 'https://i.pravatar.cc/50?u=pedromartins', role: 'admin' },
      { id: '29', name: 'Miriam Ndulu', userPicture: 'https://i.pravatar.cc/50?u=miriamndulu', role: 'admin' },
      { id: '30', name: 'Thomas Berg', userPicture: 'https://i.pravatar.cc/50?u=thomasberg', role: 'admin' },
      { id: '31', name: 'Shiho Nakamura', userPicture: 'https://i.pravatar.cc/50?u=shihonakamura', role: 'admin' },
      { id: '32', name: 'Abdul Rahman', userPicture: 'https://i.pravatar.cc/50?u=abdulrahman', role: 'admin' },
      { id: '33', name: 'Lucia Caruso', userPicture: 'https://i.pravatar.cc/50?u=luciacaruso', role: 'admin' },
      { id: '34', name: 'Andrei Popescu', userPicture: 'https://i.pravatar.cc/50?u=andreipopescu', role: 'admin' },
      { id: '35', name: 'Maya Patel', userPicture: 'https://i.pravatar.cc/50?u=mayapatel', role: 'admin' },
      { id: '36', name: 'Joon Park', userPicture: 'https://i.pravatar.cc/50?u=joonpark', role: 'admin' },
      { id: '37', name: 'Selma Öztürk', userPicture: 'https://i.pravatar.cc/50?u=selmaozturk', role: 'admin' },
      { id: '38', name: 'Luka Kovačić', userPicture: 'https://i.pravatar.cc/50?u=lukakovacic', role: 'admin' },
      { id: '39', name: 'Clara Jensen', userPicture: 'https://i.pravatar.cc/50?u=clarajensen', role: 'admin' },
      { id: '40', name: 'Igor Petrescu', userPicture: 'https://i.pravatar.cc/50?u=igorpetrescu', role: 'admin' },
      { id: '41', name: 'Leila Haddad', userPicture: 'https://i.pravatar.cc/50?u=leilahaddad', role: 'admin' },
      { id: '42', name: 'Mateusz Nowak', userPicture: 'https://i.pravatar.cc/50?u=mateusznowak', role: 'admin' },
      { id: '43', name: 'Sienna Brown', userPicture: 'https://i.pravatar.cc/50?u=siennabrown', role: 'admin' },
      { id: '44', name: 'Rashid Aliyev', userPicture: 'https://i.pravatar.cc/50?u=rashidaliyev', role: 'admin' },
      { id: '45', name: 'Emily Thompson', userPicture: 'https://i.pravatar.cc/50?u=emilythompson', role: 'admin' },
      { id: '46', name: 'Sergei Kuznetsov', userPicture: 'https://i.pravatar.cc/50?u=sergeikuznetsov', role: 'admin' },
      { id: '47', name: 'Chloe Wilson', userPicture: 'https://i.pravatar.cc/50?u=chloewilson', role: 'admin' },
      { id: '48', name: 'Omar Farouk', userPicture: 'https://i.pravatar.cc/50?u=omarfarouk', role: 'admin' },
      { id: '49', name: 'Camille Laurent', userPicture: 'https://i.pravatar.cc/50?u=camillelaurent', role: 'admin' },
      { id: '50', name: 'Jonas Sørensen', userPicture: 'https://i.pravatar.cc/50?u=jonassorensen', role: 'admin' },
      { id: '51', name: 'Zoya Ahmed', userPicture: 'https://i.pravatar.cc/50?u=zoyaahmed', role: 'admin' }
    ]), 300)
  );

  return results
    .filter(u => !isKnown(u.id) && !newMembers.value.some(n => n.id === u.id))
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))
    .map(u => ({ ...u, pictureUrl: u.userPicture }));
}

function addUser(user: User) {
  try {
    if (isKnown(user.id) || newMembers.value.some(u => u.id === user.id)) {
      addingUser.value = false;
      return;
    }
    newMembers.value.push(user);
  } catch (e) { onAddUserError.value = e as Error; }
  finally { addingUser.value = false; }
}

function removeTempMember(id: string) {
  newMembers.value = newMembers.value.filter(u => u.id !== id);
}

function onSubmit() {
  emit('saved', [...newMembers.value]);
  open.value = false;
}

defineExpose({ show });
</script>
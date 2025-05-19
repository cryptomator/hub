<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>
  <div v-else>
    <div class="flex flex-row items-center justify-between gap-3 pb-5 w-full">
      <!-- Headline -->
      <h2 id="title" class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
        Headline
      </h2>

      <div class="flex gap-3 items-center flex-shrink-0">
        <!-- Edit-Button -->
        <button class="bg-primary py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showGroupEdit">
          {{ t('common.edit') }}
        </button>

        <!-- Menu -->
        <Menu as="div" class="relative inline-block text-left">
          <MenuButton class="group p-1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2">
            <span class="sr-only">Open options menu</span>
            <EllipsisVerticalIcon class="h-5 w-5 text-gray-400 group-hover:text-gray-500" aria-hidden="true" />
          </MenuButton>

          <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 translate-y-1 scale-95" enter-to-class="transform opacity-100 translate-y-0 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 translate-y-0 scale-100" leave-to-class="transform opacity-0 translate-y-1 scale-95">
            <MenuItems class="absolute right-0 mt-2 z-10 w-48 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black/5 focus:outline-hidden">
              <div class="py-1">
                <MenuItem v-slot="{ active }">
                  <div :class="[ active ? 'bg-gray-100 text-red-900' : 'text-red-700', 'cursor-pointer block px-4 py-2 text-sm']">
                    {{ t('common.remove') }}
                  </div>
                </MenuItem>
              </div>
            </MenuItems>
          </transition>
        </Menu>
      </div>
    </div>
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
      <section class="lg:col-start-1 grid gap-6">
        <!-- Group Info -->
        <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
          <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
              {{ t('group.detail.info') }}
            </h3>
          </div>

          <div class="px-6 py-6">
            <div class="flex flex-col items-center justify-center h-full text-center">
              <img :src="group.picture" :alt="t('group.edit.profileImage')" class="w-48 h-48 rounded-full object-cover border border-gray-300 mb-4" />
              <h2 class="text-xl font-semibold text-gray-900">{{ group.name }}</h2>
            </div>

            <div class="divide-y divide-gray-100">
              <div class="flex justify-between py-3">
                <dt class="text-sm text-gray-500">{{ t('group.detail.roles') }}</dt>
                <dd class="flex flex-wrap justify-end gap-2">
                  <template v-if="group.roles?.length">
                    <span v-for="role in sortedRoles" :key="role.id" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20 capitalize">
                      {{ role.name }}
                    </span>
                  </template>
                  <span v-else class="text-sm text-gray-500">
                    {{ t('common.none') }}
                  </span>
                </dd>
              </div>
              <div class="py-3 flex justify-between">
                <dt class="text-sm text-gray-500">{{ t('group.detail.createdOn') }}</dt>
                <dd class="text-sm text-gray-900 font-medium">{{ d(group.createdAt, 'long') }}</dd>
              </div>
            </div>
          </div>
        </section>
        <!-- Vaults -->
        <AuthorityVaultList :vaults="group.vaults" :page-size="10" :visible="true" />
      </section>
      <section class="lg:col-start-2 grid gap-6">
        <!-- Members -->
        <AuthorityGroupMemberList :group="group" :members="group.users" :page-size="10" :on-saved="onMembersSaved" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import AuthorityVaultList from './AuthorityVaultList.vue';
import AuthorityGroupMemberList from './AuthorityGroupMemberList.vue';

const router = useRouter();

interface User {
  id: string;
  name: string;
  userPicture?: string;
  role?: string;
  username: string;
  email?: string;
}
interface Role {
  id: string;
  name: string;
}

interface Vault {
  id: string;
  name: string;
  description: string;
}

interface DetailGroup {
  id: string;
  name: string;
  picture?: string;
  createdAt: string;
  roles: Role[];
  users: User[];
  vaults: Vault[];
  description?: string;
  memberSize: number;
}

function showGroupEdit() {
  router.push(`/app/authority/group/${group.value.id}/edit`);
}

const props = defineProps<{ id: string }>();
const { t, d } = useI18n({ useScope: 'global' });

const group = ref<DetailGroup>({
  id: props.id,
  name: 'Frontend‑Team',
  picture: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iIzAwNUU3MSIgb3BhY2l0eT0iMS4wMCIvPjxwYXRoIGZpbGw9IiNmNmY2ZjYiIGQ9Ik0yNSAyNUwyNSAwTDUwIDBaTTUwIDBMNzUgMEw3NSAyNVpNNzUgNzVMNzUgMTAwTDUwIDEwMFpNNTAgMTAwTDI1IDEwMEwyNSA3NVpNMCA1MEwwIDI1TDI1IDI1Wk03NSAyNUwxMDAgMjVMMTAwIDUwWk0xMDAgNTBMMTAwIDc1TDc1IDc1Wk0yNSA3NUwwIDc1TDAgNTBaIi8+PHBhdGggZmlsbD0iI2NlZWNmMiIgZD0iTTI1IDI1TDAgMjVMMCAwWk03NSAyNUw3NSAwTDEwMCAwWk03NSA3NUwxMDAgNzVMMTAwIDEwMFpNMjUgNzVMMjUgMTAwTDAgMTAwWiIvPjxwYXRoIGZpbGw9IiNhYmRmZTkiIGQ9Ik0yNSAyNUw1MCAyNUw1MCA1MEwyNSA1MFpNMzEuMyA0MC42TDQwLjYgNTBMNTAgNDAuNkw0MC42IDMxLjNaTTc1IDI1TDc1IDUwTDUwIDUwTDUwIDI1Wk01OS40IDMxLjNMNTAgNDAuNkw1OS40IDUwTDY4LjggNDAuNlpNNzUgNzVMNTAgNzVMNTAgNTBMNzUgNTBaTTY4LjggNTkuNEw1OS40IDUwTDUwIDU5LjRMNTkuNCA2OC44Wk0yNSA3NUwyNSA1MEw1MCA1MEw1MCA3NVpNNDAuNiA2OC44TDUwIDU5LjRMNDAuNiA1MEwzMS4zIDU5LjRaIi8+PC9zdmc+',
  createdAt: '2017-02-15T13:12:00Z',
  description: 'Das Frontend-Team ist für die Entwicklung der Benutzeroberfläche verantwortlich.',
  roles: [
    { id: '1', name: 'User' },
    { id: '2', name: 'Create-Vault' }
  ],
  users: [
    { id: '1', name: 'Anna Marie Schmidtson', username: 'anna.schmidtson', email: 'anna.schmidtson@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=anna', role: 'admin' },
    { id: '2', name: 'Liu Wei', username: 'liu.wei', email: 'liu.wei@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=liuwei', role: 'admin' },
    { id: '3', name: 'Carlos Gómez', username: 'carlos.gomez', email: 'carlos.gomez@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=carlosgomez', role: 'admin' },
    { id: '4', name: 'Fatima Al-Hassan', username: 'fatima.alhassan', email: 'fatima.alhassan@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=fatimaalhassan', role: 'admin' },
    { id: '5', name: 'Giulia Rossi', username: 'giulia.rossi', email: 'giulia.rossi@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=giuliarossi', role: 'admin' },
    { id: '6', name: 'Noah Johansson', username: 'noah.johansson', email: 'noah.johansson@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=noahjohansson', role: 'admin' },
    { id: '7', name: 'Aisha Khan', username: 'aisha.khan', email: 'aisha.khan@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=aishakhan', role: 'admin' },
    { id: '8', name: 'Hiroshi Tanaka', username: 'hiroshi.tanaka', email: 'hiroshi.tanaka@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=hiroshitanaka', role: 'admin' },
    { id: '9', name: 'Elena Petrov', username: 'elena.petrov', email: 'elena.petrov@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=elenapetrov', role: 'admin' },
    { id: '10', name: 'Samuel Osei', username: 'samuel.osei', email: 'samuel.osei@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=samuelosei', role: 'admin' },
    { id: '11', name: 'Marie Dubois', username: 'marie.dubois', email: 'marie.dubois@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=mariedubois', role: 'admin' },
    { id: '12', name: 'Javier Morales', username: 'javier.morales', email: 'javier.morales@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=javiermorales', role: 'admin' },
    { id: '13', name: 'Sofia Almeida', username: 'sofia.almeida', email: 'sofia.almeida@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=sofiaalmeida', role: 'admin' },
    { id: '14', name: 'Chen Mei', username: 'chen.mei', email: 'chen.mei@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=chenmei', role: 'admin' },
    { id: '15', name: 'Michael O\'Connor', username: 'michael.oconnor', email: 'michael.oconnor@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=michaeloconnor', role: 'admin' },
    { id: '16', name: '', username: 'zanele.dlamini', email: 'zanele.dlamini@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=zaneledlamini', role: 'admin' },
    { id: '17', name: 'Anna Kovár', username: 'anna.kovar', email: 'anna.kovar@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=annakovar', role: 'admin' },
    { id: '18', name: '', username: 'timur.iskanderov', email: 'timur.iskanderov@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=timuriskanderov', role: 'admin' },
    { id: '19', name: 'Lara Müller', username: 'lara.mueller', email: 'lara.mueller@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=laramuller', role: 'admin' },
    { id: '20', name: '', username: 'ahmed.nasser', email: 'ahmed.nasser@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=ahmednasser', role: 'admin' },
    { id: '21', name: '', username: 'isabella.costa', email: 'isabella.costa@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=isabellacosta', role: 'admin' },
    { id: '22', name: 'Oliver Smith', username: 'oliver.smith', email: 'oliver.smith@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=oliversmith', role: 'admin' },
    { id: '23', name: 'Yuki Sato', username: 'yuki.sato', email: 'yuki.sato@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=yukisato', role: 'admin' },
    { id: '24', name: 'Priya Reddy', username: 'priya.reddy', email: 'priya.reddy@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=priyareddy', role: 'admin' },
    { id: '25', name: 'Juanita Rivera', username: 'juanita.rivera', email: 'juanita.rivera@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=juanitarivera', role: 'admin' }
  ],
  vaults: [
    { id: 'v1', name: 'HR', description: '...' },
    { id: 'v2', name: 'Finances', description: '...' },
    { id: 'v3', name: 'Products', description: '...' },
    { id: 'v4', name: 'Tax', description: '...' },
    { id: 'v5', name: 'Orga', description: '...' },
    { id: 'v6', name: 'Sales', description: '...' },
  ],
  memberSize: 25
});

function onMembersSaved(newMembers: User[]) {
  const ids = new Set(group.value.users.map(u => u.id));
  newMembers.forEach(u => { if (!ids.has(u.id)) group.value.users.push(u); });
  group.value.users.sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
}

const loading = ref(false);

const sortedRoles = computed(() =>
  [...group.value.roles].sort((a, b) => a.name.localeCompare(b.name))
);

</script>
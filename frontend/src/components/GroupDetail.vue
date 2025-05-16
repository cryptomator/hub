<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>

  <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
    <!-- Group Info -->
    <section class="order-1 lg:col-start-1 lg:row-start-1 bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
      <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
        <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
          {{ t('group.detail.info') }}
        </h3>
        <button class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showGroupEdit">
          <PencilIcon class="h-4 w-4 text-gray-500" aria-hidden="true" />
          {{ t('group.detail.edit') }}
        </button>
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
    <!-- Users -->
    <section class="order-2 lg:col-start-2 lg:row-start-1 lg:row-span-2 bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
      <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
        <div class="flex items-baseline gap-1">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('group.detail.members') }}
          </h3>
          <span class="text-xs text-gray-500">{{ group.users.length }}</span>
        </div>
        <button class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openAccessDialog">
          <PlusIcon class="h-4 w-4 text-gray-500" aria-hidden="true" />
          {{ t('group.members.add') }}
        </button>
      </div>

      <!-- Search bar -->
      <div class="px-6 py-3 border-b border-gray-200">
        <input id="memberSearch" v-model="userQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
      </div>

      <!-- User table -->
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-300" aria-describedby="usersTitle">
          <tbody class="divide-y divide-gray-200 bg-white">
            <tr v-for="user in paginatedUsers" :key="user.id + user.name">
              <td class="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 flex items-center gap-3 sm:pl-6">
                <img :src="user.userPicture" class="w-8 h-8 rounded-full object-cover border border-gray-300" />
                <div class="flex flex-col truncate">
                  <span class="font-medium truncate">{{ user.name }}</span>
                  <span class="text-xs text-gray-500 truncate">{{ user.username }}</span>
                </div>
              </td>

              <td class="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                <button type="button" class="inline-flex items-center gap-2 px-2.5 py-1.5 rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-red-500" :title="t('common.remove')" @click="showDeleteDialog(user)">
                  <TrashIcon class="h-4 w-4 text-white" aria-hidden="true" />
                </button>
              </td>
            </tr>
            <tr v-if="!filteredUsers.length">
              <td colspan="3" class="py-4 px-4 text-sm text-center text-gray-500">
                {{ t(userQuery ? 'group.members.search.empty' : 'common.none') }}
              </td>
            </tr>
          </tbody>

          <!-- USERS – Pagination ----------------------------->
          <tfoot v-if="showPaginationUsers" class="bg-gray-50">
            <tr>
              <td colspan="3">
                <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
                  <div class="hidden sm:block">
                    <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
                      <span class="font-medium">{{ paginationBegin }}</span>
                      <span class="font-medium">{{ paginationEnd }}</span>
                    </i18n-t>
                  </div>
                  <div class="flex flex-1 justify-end space-x-3">
                    <button v-if="currentPage > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPage">
                      {{ t('common.previous') }}
                    </button>
                    <button v-if="hasNextPage" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPage">
                      {{ t('common.next') }}
                    </button>
                  </div>
                </nav>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
    <!-- Vaults -->
    <section class="order-3 lg:col-start-1 lg:row-start-2 bg-white bg-white bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
      <div class="bg-gray-50 px-6 py-4 border-b border-gray-200">
        <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
          {{ t('nav.vaults') }}
        </h3>
      </div>

      <!-- Search bar -->
      <div class="px-6 py-3 border-b border-gray-200">
        <input id="vaultSearch" v-model="vaultQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
      </div>

      <!-- Vault list -->
      <div class="py-0">
        <ul class="divide-y divide-gray-200 bg-white">
          <li v-for="vault in paginatedVaults" :key="vault.id" class="py-2 px-6">
            <div class="text-sm font-medium text-primary truncate">{{ vault.name }}</div>
            <div class="text-sm text-gray-500 truncate">{{ vault.description }}</div>
          </li>
          <li v-if="!filteredVaults.length" class="text-sm text-gray-500">
            {{ t(vaultQuery ? 'common.nothingFound' : 'common.none') }}
          </li>
        </ul>
      </div>

      <!-- Pagination -->
      <div v-if="showPaginationVault" class="bg-gray-50 border-t border-gray-200">
        <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
          <div class="hidden sm:block">
            <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
              <span class="font-medium">{{ paginationBeginVault }}</span>
              <span class="font-medium">{{ paginationEndVault }}</span>
            </i18n-t>
          </div>
          <div class="flex flex-1 justify-between sm:justify-end">
            <button v-if="currentPageVault > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPageVault">
              {{ t('common.previous') }}
            </button>
            <button v-if="hasNextPageVault" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPageVault">
              {{ t('common.next') }}
            </button>
          </div>
        </nav>
      </div>
    </section>
  </div>

  <!-- Dialogs -->
  <GroupAddMemberDialog ref="addMemberDialog" :members="group.users" @saved="onMembersSaved" />
  <GroupMemberRemoveDialog v-if="deletingGroupMember != null" ref="deleteGroupMemberDialog" :group="deletingGroupMember" @close="deletingGroupMember = null" @delete="onGroupMemberDeleted" />
</template>

<script setup lang="ts">
import { PlusIcon } from '@heroicons/vue/20/solid';
import { PencilIcon } from '@heroicons/vue/24/outline';
import { TrashIcon } from '@heroicons/vue/24/solid';
import { computed, nextTick, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import type { UserDto } from '../common/backend';
import GroupAddMemberDialog from './GroupAddMemberDialog.vue';
import GroupMemberRemoveDialog from './GroupMemberRemoveDialog.vue';

const router = useRouter();

interface User {
  id: string;
  name: string;
  userPicture?: string;
  role?: string;
  username?: string;
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

const deleteGroupMemberDialog = ref<typeof GroupMemberRemoveDialog>();
const deletingGroupMember = ref<UserDto | null>(null);

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

function showDeleteDialog(u: UserDto) {
  deletingGroupMember.value = u;
  nextTick(() => deleteGroupMemberDialog.value?.show());
}
function onGroupMemberDeleted() {
  deletingGroupMember.value = null;
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
    { id: '16', name: 'Zanele Dlamini', username: 'zanele.dlamini', email: 'zanele.dlamini@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=zaneledlamini', role: 'admin' },
    { id: '17', name: 'Anna Kovár', username: 'anna.kovar', email: 'anna.kovar@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=annakovar', role: 'admin' },
    { id: '18', name: 'Timur Iskanderov', username: 'timur.iskanderov', email: 'timur.iskanderov@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=timuriskanderov', role: 'admin' },
    { id: '19', name: 'Lara Müller', username: 'lara.mueller', email: 'lara.mueller@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=laramuller', role: 'admin' },
    { id: '20', name: 'Ahmed Nasser', username: 'ahmed.nasser', email: 'ahmed.nasser@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=ahmednasser', role: 'admin' },
    { id: '21', name: 'Isabella Costa', username: 'isabella.costa', email: 'isabella.costa@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=isabellacosta', role: 'admin' },
    { id: '22', name: 'Oliver Smith', username: 'oliver.smith', email: 'oliver.smith@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=oliversmith', role: 'admin' },
    { id: '23', name: 'Yuki Sato', username: 'yuki.sato', email: 'yuki.sato@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=yukisato', role: 'admin' },
    { id: '24', name: 'Priya Reddy', username: 'priya.reddy', email: 'priya.reddy@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=priyareddy', role: 'admin' },
    { id: '25', name: 'Juanita Rivera', username: 'juanita.rivera', email: 'juanita.rivera@skymatic.de', userPicture: 'https://i.pravatar.cc/50?u=juanitarivera', role: 'admin' }
  ],
  vaults: [
    { id: 'v1', name: 'HR', description: '...' },
    { id: 'v2', name: 'Finances', description: '...' },
    { id: 'v2', name: 'Products', description: '...' },
    { id: 'v3', name: 'Tax', description: '...' },
    { id: 'v4', name: 'Orga', description: '...' },
    { id: 'v5', name: 'Sales', description: '...' },
  ],
  memberSize: 25
});

const addMemberDialog = ref<InstanceType<typeof GroupAddMemberDialog> | null>(null);

function openAccessDialog() {
  addMemberDialog.value?.show();
}

function onMembersSaved(newMembers: User[]) {
  const ids = new Set(group.value.users.map(u => u.id));
  newMembers.forEach(u => { if (!ids.has(u.id)) group.value.users.push(u); });
  group.value.users.sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
}

const loading = ref(false);

const sortedRoles = computed(() =>
  [...group.value.roles].sort((a, b) => a.name.localeCompare(b.name))
);

// --- USERS pagination & search ------------------------------------------------
const pageSize = ref(10);
const currentPage = ref(0);
const userQuery = ref('');

const showPaginationUsers = computed(
  () => filteredUsers.value.length > pageSize.value
);

const filteredUsers = computed(() => {
  const q = userQuery.value.trim().toLowerCase();
  return [...group.value.users]
    .filter(u => {
      if (!q) return true;
      const nameMatch = u.name.toLowerCase().includes(q);
      const usernameMatch = u.username?.toLowerCase().includes(q);
      return nameMatch || usernameMatch;
    })
    .sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
});

const paginatedUsers = computed(() =>
  filteredUsers.value.slice(currentPage.value * pageSize.value, currentPage.value * pageSize.value + pageSize.value)
);

const hasNextPage = computed(
  () => (currentPage.value + 1) * pageSize.value < filteredUsers.value.length
);

const paginationBegin = computed(() =>
  filteredUsers.value.length ? currentPage.value * pageSize.value + 1 : 0,
);
const paginationEnd = computed(() =>
  Math.min((currentPage.value + 1) * pageSize.value, filteredUsers.value.length),
);

function showNextPage() {
  if (hasNextPage.value) currentPage.value += 1;
}
function showPreviousPage() {
  if (currentPage.value > 0) currentPage.value -= 1;
}

watch(() => [filteredUsers.value.length, userQuery.value], () => (currentPage.value = 0));

// --- VAULTS pagination & search ----------------------------------------------
const pageSizeVault = pageSize;

const showPaginationVault = computed(
  () => filteredVaults.value.length > pageSizeVault.value
);

const currentPageVault = ref(0);
const vaultQuery = ref('');

const filteredVaults = computed(() => {
  const q = vaultQuery.value.trim().toLowerCase();
  return [...group.value.vaults]
    .filter(v => {
      if (!q) return true;
      const nameMatch = v.name.toLowerCase().includes(q);
      const descMatch = v.description ? v.description.toLowerCase().includes(q) : false;
      return nameMatch || descMatch;
    })
    .sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
});

const paginatedVaults = computed(() =>
  filteredVaults.value.slice(currentPageVault.value * pageSizeVault.value, currentPageVault.value * pageSizeVault.value + pageSizeVault.value)
);

const hasNextPageVault = computed(() =>
  (currentPageVault.value + 1) * pageSizeVault.value < filteredVaults.value.length,
);

const paginationBeginVault = computed(() =>
  filteredVaults.value.length ? currentPageVault.value * pageSizeVault.value + 1 : 0,
);
const paginationEndVault = computed(() =>
  Math.min((currentPageVault.value + 1) * pageSizeVault.value, filteredVaults.value.length),
);

function showNextPageVault() {
  if (hasNextPageVault.value) currentPageVault.value += 1;
}
function showPreviousPageVault() {
  if (currentPageVault.value > 0) currentPageVault.value -= 1;
}

watch(() => [filteredVaults.value.length, vaultQuery.value], () => (currentPageVault.value = 0));
</script>
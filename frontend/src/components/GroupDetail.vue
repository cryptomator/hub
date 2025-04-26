<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>

  <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6 p-6 items-start">
    <div class="flex flex-col gap-6">
      <!-- Group Info -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('group.detail.info') }}
          </h3>
          <button class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openEditDialog">
            <PencilIcon class="h-4 w-4 text-gray-500" aria-hidden="true" />
            {{ t('group.detail.edit') }}
          </button>
        </div>

        <div class="px-6 py-6">
          <div class="flex gap-6 items-start mb-6">
            <img :src="group.picture" alt="Group" class="w-20 h-20 rounded-full object-cover border border-gray-300"/>
            <div>
              <h2 class="text-xl font-semibold text-gray-900">{{ group.name }}</h2>
              <p v-if="group.description" class="text-sm text-gray-500 mt-1 whitespace-pre-wrap">
                {{ group.description }}
              </p>
            </div>
          </div>

          <div class="divide-y divide-gray-100">
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('group.detail.name') }}</dt>
              <dd class="text-sm text-gray-900 font-medium">{{ group.name }}</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('group.detail.visibility') }}</dt>
              <dd class="text-sm text-gray-900 font-medium">Private</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('group.detail.createdOn') }}</dt>
              <dd class="text-sm text-gray-900 font-medium">{{ d(group.createdAt, 'long') }}</dd>
            </div>
            <div class="py-3 flex justify-between">
              <dt class="text-sm text-gray-500">{{ t('group.detail.roles') }}</dt>
              <dd class="flex flex-wrap justify-end gap-2">
                <span v-for="role in ['create-vault', 'admin']" :key="role" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20 capitalize">
                  {{ role }}
                </span>
              </dd>
            </div>
          </div>
        </div>
      </section>

      <!-- Vaults -->
      <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
        <div class="bg-gray-50 px-6 py-4 border-b border-gray-200">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
            {{ t('group.detail.vaults') }}
          </h3>
        </div>
        <div class="px-6 py-6">
          <ul class="divide-y divide-gray-100">
            <li v-for="vault in group.vaults" :key="vault.id" class="py-2">
              <div class="text-sm font-medium text-primary truncate">{{ vault.name }}</div>
              <div class="text-sm text-gray-500 truncate">{{ vault.path }}</div>
            </li>
            <li v-if="!group.vaults?.length" class="text-sm text-gray-500">{{ t('common.none') }}</li>
          </ul>
        </div>
      </section>
    </div>

    <!-- Users -->
    <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
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
                <span class="truncate">{{ user.name }}</span>
              </td>

              <td class="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                <button type="button" class="inline-flex items-center gap-2 px-2.5 py-1.5 rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-red-500" :title="t('common.remove')" @click="showDeleteDialog(user)">
                  <TrashIcon class="h-4 w-4 text-white" aria-hidden="true" />
                  {{ t('common.remove') }}
                </button>
              </td>
            </tr>
            <tr v-if="!filteredUsers.length">
              <td colspan="3" class="py-4 px-4 text-sm text-center text-gray-500">
                {{ t(userQuery ? 'group.members.search.empty' : 'common.none') }}
              </td>
            </tr>
          </tbody>

          <tfoot v-if="filteredUsers.length" class="bg-gray-50" >
            <tr>
              <td colspan="3">
                <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
                  <div class="hidden sm:block">
                    <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
                      <span class="font-medium">{{ paginationBegin }}</span>
                      <span class="font-medium">{{ paginationEnd }}</span>
                    </i18n-t>
                  </div>
                  <div class="flex flex-1 justify-between sm:justify-end">
                    <button type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0 disabled:opacity-50 disabled:hover:bg-white disabled:cursor-not-allowed" :disabled="currentPage === 0" @click="showPreviousPage">
                      {{ t('common.previous') }}
                    </button>
                    <button type="button" class="relative ml-3 inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0 disabled:opacity-50 disabled:hover:bg-white disabled:cursor-not-allowed" :disabled="!hasNextPage" @click="showNextPage">
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
  </div>

  <!-- Dialogs -->
  <GroupEditDialog ref="editGroupDialog" />
  <GroupAddMemberDialog ref="addMemberDialog" :members="group.users" @saved="onMembersSaved" />
  <GroupMemberRemoveDialog v-if="deletingGroupMember != null" ref="deleteGroupMemberDialog" :group="deletingGroupMember" @close="deletingGroupMember = null" @delete="onGroupMemberDeleted" />
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { PencilIcon } from '@heroicons/vue/24/outline';
import { PlusIcon } from '@heroicons/vue/20/solid';
import { TrashIcon } from '@heroicons/vue/24/solid';
import GroupEditDialog from './GroupEditDialog.vue';
import GroupAddMemberDialog from './GroupAddMemberDialog.vue';
import GroupMemberRemoveDialog from './GroupMemberRemoveDialog.vue';
import type { UserDto } from '../common/backend';

interface User {
  id: string;
  name: string;
  userPicture?: string;
  role?: string;
}

interface Vault {
  id: string;
  name: string;
  path: string;
}

const deleteGroupMemberDialog = ref<typeof GroupMemberRemoveDialog>();
const deletingGroupMember     = ref<UserDto | null>(null);

interface DetailGroup {
  id: string;
  name: string;
  picture?: string;
  createdAt: string;
  users: User[];
  vaults: Vault[];
  description?: string;
}

function showDeleteDialog(u: UserDto) {
  deletingGroupMember.value = u;
  nextTick(() => deleteGroupMemberDialog.value?.show());
}
function onGroupMemberDeleted() {
  deletingGroupMember.value = null;
}

const props  = defineProps<{ id: string }>();
const { t, d } = useI18n({ useScope: 'global' });
const editGroupDialog = ref<InstanceType<typeof GroupEditDialog> | null>(null);

const group = ref<DetailGroup>({
  id: props.id,
  name: 'Frontend‑Team',
  picture: 'https://i.pravatar.cc/150?u=group',
  createdAt: '2017-02-15T13:12:00Z',
  description: 'Das Frontend-Team ist für die Entwicklung der Benutzeroberfläche verantwortlich.',
  users: [
    { id: '1', name: 'Anna Marie Schmidtson', userPicture: 'https://i.pravatar.cc/50?u=anna', role: 'admin' },
    { id: '2',  name: 'Liu Wei', userPicture: 'https://i.pravatar.cc/50?u=liuwei', role: 'admin' },
    { id: '3',  name: 'Carlos Gómez', userPicture: 'https://i.pravatar.cc/50?u=carlosgomez', role: 'admin' },
    { id: '4',  name: 'Fatima Al-Hassan', userPicture: 'https://i.pravatar.cc/50?u=fatimaalhassan', role: 'admin' },
    { id: '5',  name: 'Giulia Rossi', userPicture: 'https://i.pravatar.cc/50?u=giuliarossi', role: 'admin' },
    { id: '6',  name: 'Noah Johansson', userPicture: 'https://i.pravatar.cc/50?u=noahjohansson', role: 'admin' },
    { id: '7',  name: 'Aisha Khan', userPicture: 'https://i.pravatar.cc/50?u=aishakhan', role: 'admin' },
    { id: '8',  name: 'Hiroshi Tanaka', userPicture: 'https://i.pravatar.cc/50?u=hiroshitanaka', role: 'admin' },
    { id: '9',  name: 'Elena Petrov', userPicture: 'https://i.pravatar.cc/50?u=elenapetrov', role: 'admin' },
    { id: '10', name: 'Samuel Osei', userPicture: 'https://i.pravatar.cc/50?u=samuelosei', role: 'admin' },
    { id: '11', name: 'Marie Dubois', userPicture: 'https://i.pravatar.cc/50?u=mariedubois', role: 'admin' },
    { id: '12', name: 'Javier Morales', userPicture: 'https://i.pravatar.cc/50?u=javiermorales', role: 'admin' },
    { id: '13', name: 'Sofia Almeida', userPicture: 'https://i.pravatar.cc/50?u=sofiaalmeida', role: 'admin' },
    { id: '14', name: 'Chen Mei', userPicture: 'https://i.pravatar.cc/50?u=chenmei', role: 'admin' },
    { id: '15', name: 'Michael O\'Connor', userPicture: 'https://i.pravatar.cc/50?u=michaeloconnor', role: 'admin' },
    { id: '16', name: 'Zanele Dlamini', userPicture: 'https://i.pravatar.cc/50?u=zaneledlamini', role: 'admin' },
    { id: '17', name: 'Anna Kovár', userPicture: 'https://i.pravatar.cc/50?u=annakovar', role: 'admin' },
    { id: '18', name: 'Timur Iskanderov', userPicture: 'https://i.pravatar.cc/50?u=timuriskanderov', role: 'admin' },
    { id: '19', name: 'Lara Müller', userPicture: 'https://i.pravatar.cc/50?u=laramuller', role: 'admin' },
    { id: '20', name: 'Ahmed Nasser', userPicture: 'https://i.pravatar.cc/50?u=ahmednasser', role: 'admin' },
    { id: '21', name: 'Isabella Costa', userPicture: 'https://i.pravatar.cc/50?u=isabellacosta', role: 'admin' },
    { id: '22', name: 'Oliver Smith', userPicture: 'https://i.pravatar.cc/50?u=oliversmith', role: 'admin' },
    { id: '23', name: 'Yuki Sato', userPicture: 'https://i.pravatar.cc/50?u=yukisato', role: 'admin' },
    { id: '24', name: 'Priya Reddy', userPicture: 'https://i.pravatar.cc/50?u=priyareddy', role: 'admin' },
    { id: '25', name: 'Juanita Rivera', userPicture: 'https://i.pravatar.cc/50?u=juanitarivera', role: 'admin' }
  ],
  vaults: [
    { id: 'v1', name: 'cryptobot-os', path: 'cryptomator/cryptobot-os.git' },
    { id: 'v2', name: 'ios', path: 'cryptomator/ios.git' },
    { id: 'v2', name: 'ios', path: 'cryptomator/ios.git' },
    { id: 'v3', name: 'android', path: 'cryptomator/android.git' },
    { id: 'v4', name: 'web', path: 'cryptomator/web.git' },
    { id: 'v5', name: 'desktop', path: 'cryptomator/desktop.git' },
    { id: 'v6', name: 'vaults', path: 'cryptomator/vaults.git' },
  ]
});

function openEditDialog() {
  editGroupDialog.value?.show();
}
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
const pageSize = ref(10);
const currentPage = ref(0);
const userQuery = ref('');

const filteredUsers = computed(() => {
  const q = userQuery.value.trim().toLowerCase();
  return [...group.value.users]
    .filter(u => !q || u.name.toLowerCase().includes(q))
    .sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
});

const paginatedUsers = computed(() =>
  filteredUsers.value.slice(currentPage.value * pageSize.value,currentPage.value * pageSize.value + pageSize.value)
);

const hasNextPage = computed(
  () => (currentPage.value + 1) * pageSize.value < filteredUsers.value.length,
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

watch(() => [filteredUsers.value.length, userQuery.value],() => (currentPage.value = 0));
</script>

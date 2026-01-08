<template>
  <div v-if="loading" class="text-center p-8 text-gray-500 text-sm">
    {{ t('common.loading') }}
  </div>

  <FetchError v-else-if="onFetchError" :error="onFetchError" :retry="fetchData" />

  <div v-else class="flex flex-col">
    <h2 class="text-2xl font-bold leading-9 text-gray-900 sm:text-3xl sm:truncate mb-4">
      {{ t('users.title') }}
    </h2>
    <!-- Searchbar + Createbutton -->
    <div class="flex flex-wrap sm:flex-nowrap justify-between items-center gap-3 mb-4">
      <input v-model="query" type="text" :placeholder="t('userList.search.placeholder')" class="flex-1 focus:ring-primary focus:border-primary shadow-xs text-sm border-gray-300 rounded-md"/>
      <button type="button" class="bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showCreateUser()">{{ t('userList.create.button') }}</button>
    </div>
    
    <!-- Mobile Card Layout (visible on small screens) -->
    <div v-if="paginatedUsers.length > 0" class="block md:hidden space-y-3">
      <div v-for="user in paginatedUsers" :key="user.id" class="bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow cursor-pointer" @click="router.push(`users/${user.id}`)">
        <div class="px-4 py-4">
          <div class="flex items-start justify-between mb-4">
            <div class="flex items-center min-w-0 flex-1" :title="user.name">
              <img :src="user.pictureUrl" :alt="t('userList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300 flex-shrink-0" />
              <div class="ml-3 min-w-0 flex-1">
                <p class="text-sm font-medium text-gray-900 truncate leading-tight">{{ user.name }}</p>
                <p class="text-xs text-gray-500 truncate">{{ user.firstName || user.lastName ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() : user.email }}</p>
              </div>
            </div>
            <Menu v-if="user.id !== currentUserId" as="div" class="relative inline-block shrink-0 text-left">
              <MenuButton class="group p-1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2" @click.stop>
                <span class="sr-only">Open options menu</span>
                <EllipsisVerticalIcon class="h-5 w-5 text-gray-400 group-hover:text-gray-500" aria-hidden="true" />
              </MenuButton>
              <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
                <MenuItems class="absolute right-0 mt-2 z-10 w-48 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black/5 focus:outline-hidden">
                  <div class="py-1">
                    <MenuItem>
                      <button type="button" class="text-red-700 hover:text-red-900 block w-full px-4 py-2 text-sm text-left hover:bg-gray-50" @click.stop="showDeleteUserDialog(user)">{{ t('common.remove') }}</button>
                    </MenuItem>
                  </div>
                </MenuItems>
              </transition>
            </Menu>
          </div>

          <!-- Stats section -->
          <div class="mb-3 ml-13 text-xs text-gray-600">
            <span>{{ t('userList.vaults.count') }}: {{ user.accessibleVaultCount ?? 0 }}</span>
            <span class="mx-2">|</span>
            <span>{{ t('userList.groups.count') }}: {{ user.groupsCount ?? 0 }}</span>
            <span class="mx-2">|</span>
            <span>{{ t('userList.device.count') }}: {{ user.devicesCount ?? 0 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Desktop Table Layout (visible on medium screens and up) -->
    <div v-if="paginatedUsers.length > 0" class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8 hidden md:block">
      <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
        <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
          <table class="min-w-full divide-y divide-gray-200" aria-describedby="userListTitle">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 w-2/5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('userList.userName') }}</th>
                <th class="px-6 py-3 w-1/8 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">{{ t('userList.vaults.count') }}</th>
                <th class="px-6 py-3 w-1/8 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">{{ t('userList.groups.count') }}</th>
                <th class="px-6 py-3 w-1/8 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">{{ t('userList.device.count') }}</th>
                <th class="px-3 py-3 w-auto text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap"></th>
              </tr>
            </thead>

            <tbody class="bg-white divide-y divide-gray-200">
              <template v-for="user in paginatedUsers" :key="user.id">
                <tr>
                  <td class="pr-8 pl-6 py-4 text-sm font-medium text-gray-900">
                    <div class="flex items-center gap-3 max-w-sm">
                      <img :src="user.pictureUrl" :alt="t('userList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300"/>
                      <div class="flex flex-col min-w-0 flex-1">
                        <button type="button" class="truncate block hover:underline cursor-pointer text-left" :title="user.name" @click="router.push(`users/${user.id}`)">{{ user.name }}</button>
                        <span class="text-xs text-gray-500 truncate" :title="user.firstName || user.lastName ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() : user.email">{{ user.firstName || user.lastName ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() : user.email }}</span>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.accessibleVaultCount ?? 0 }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.groupsCount ?? 0 }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.devicesCount ?? 0 }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div v-if="user.id !== currentUserId" class="flex justify-end gap-3">
                      <div class="cursor-pointer text-sm font-medium text-red-700 hover:text-red-900" @click="showDeleteUserDialog(user)">{{ t('common.remove') }}</div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>

            <!-- Pagination -->
            <tfoot v-if="showPagination" class="bg-gray-50">
              <tr>
                <td colspan="5">
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
      </div>
    </div>

    <!-- Mobile Pagination -->
    <nav v-if="showPagination" class="flex items-center justify-between px-4 py-3 md:hidden" :aria-label="t('common.pagination')">
      <div class="flex flex-1 justify-between space-x-3">
        <button v-if="currentPage > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPage">
          {{ t('common.previous') }}
        </button>
        <div class="flex-1"></div>
        <button v-if="hasNextPage" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPage">
          {{ t('common.next') }}
        </button>
      </div>
    </nav>

    <!-- Empty State for Search Results -->
    <div v-if="query !== '' && sortedUsers.length === 0" class="mt-3 text-center">
      <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
        <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.75 15.75l-2.489-2.489m0 0a3.375 3.375 0 10-4.773-4.773 3.375 3.375 0 004.774 4.774zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('userList.filter.result.empty.title') }}</h3>
      <p class="mt-1 text-sm text-gray-500">{{ t('userList.filter.result.empty.description') }}</p>
    </div>
  </div>

  <!-- Delete Dialog -->
  <UserDeleteDialog v-if="deletingUser" ref="deleteUserDialog" :user="deletingUser" @close="deletingUser = undefined" @delete="onUserDeleted"/>
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter, useRoute } from 'vue-router';
import backend from '../../common/backend';
import userdata from '../../common/userdata';
import UserDeleteDialog from './UserDeleteDialog.vue';
import FetchError from '../FetchError.vue';

const router = useRouter();
const route = useRoute();

import { UserDtoWithCounts } from '../../common/backend';

const { t } = useI18n({ useScope: 'global' });

const PAGE_SIZE = 20;

const users = ref<UserDtoWithCounts[]>([]);
const loading = ref(true);
const onFetchError = ref<Error>();
const deleteUserDialog = ref<typeof UserDeleteDialog>();
const deletingUser = ref<UserDtoWithCounts>();
const query = ref('');
const currentUserId = ref<string>('');
const currentPage = ref(0);

const showDeleteUserDialog = (user: UserDtoWithCounts) => {
  deletingUser.value = user;
  nextTick(() => deleteUserDialog.value?.show());
};

const onUserDeleted = (deletedUser: { id: string }) => {
  users.value = users.value.filter((u: UserDtoWithCounts) => u.id !== deletedUser.id);
  deletingUser.value = undefined;
};

function showCreateUser() {
  router.push('/app/users/create');
}

onMounted(async () => {
  const me = await userdata.me;
  currentUserId.value = me.id;
  fetchData();
});

watch(() => route.path, (newPath) => {
  if (newPath === '/app/users') {
    fetchData();
  }
});

async function fetchData() {
  loading.value = true;
  onFetchError.value = undefined;
  try {
    users.value = await backend.users.listAll();
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    loading.value = false;
  }
}

const filteredUsers = computed(() =>
  query.value === ''
    ? users.value
    : users.value.filter((u: UserDtoWithCounts) =>
      u.name.toLowerCase().includes(query.value.toLowerCase())
    )
);

const sortedUsers = computed(() =>
  filteredUsers.value.toSorted((a, b) => {
    if (a.id === currentUserId.value) return -1;
    if (b.id === currentUserId.value) return 1;
    return a.name.localeCompare(b.name);
  })
);

const paginatedUsers = computed(() =>
  sortedUsers.value.slice(currentPage.value * PAGE_SIZE, (currentPage.value + 1) * PAGE_SIZE)
);

const showPagination = computed(() => sortedUsers.value.length > PAGE_SIZE);
const hasNextPage = computed(() => (currentPage.value + 1) * PAGE_SIZE < sortedUsers.value.length);
const paginationBegin = computed(() => sortedUsers.value.length ? currentPage.value * PAGE_SIZE + 1 : 0);
const paginationEnd = computed(() => Math.min((currentPage.value + 1) * PAGE_SIZE, sortedUsers.value.length));

function showNextPage() {
  if (hasNextPage.value) currentPage.value += 1;
}

function showPreviousPage() {
  if (currentPage.value > 0) currentPage.value -= 1;
}

watch(() => [filteredUsers.value.length, query.value], () => (currentPage.value = 0));
</script>

<template>
  <div v-if="onFetchError == null">
    <div v-if="users.length === 0">
      {{ t('common.loading') }}
    </div>

    <div v-else class="mt-4 flex flex-col">
      <!-- Searchbar + Createbutton -->
      <div class="flex flex-wrap sm:flex-nowrap justify-between items-center gap-3 mb-4">
        <input v-model="query" type="text" :placeholder="t('userList.search.placeholder')" class="flex-1 focus:ring-primary focus:border-primary shadow-xs text-sm border-gray-300 rounded-md"/>
        <button type="button" class="bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showCreateUser()">{{ t('userList.create.button') }}</button>
      </div>
      
      <!-- Mobile Card Layout (visible on small screens) -->
      <div class="block md:hidden space-y-3">
        <div v-for="user in sortedUsers" :key="user.id" class="bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow cursor-pointer" @click="router.push(`authority/user/${user.id}`)">
          <div class="px-4 py-4">
            <div class="flex items-start justify-between mb-4">
              <div class="flex items-center min-w-0 flex-1" :title="user.name">
                <img :src="user.userPicture" :alt="t('userList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300 flex-shrink-0" />
                <div class="ml-3 min-w-0 flex-1">
                  <p class="text-sm font-medium text-gray-900 truncate leading-tight">{{ user.name }}</p>
                  <p class="text-xs text-gray-500 truncate">{{ user.email }}</p>
                </div>
              </div>
              <Menu as="div" class="relative inline-block shrink-0 text-left">
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
              <span>{{ t('userList.groups.count') }}: {{ user.groups?.length ?? 0 }}</span>
              <span class="mx-2">|</span>
              <span>{{ t('userList.device.count') }}: {{ user.devices?.length ?? 0 }}</span>
              <span class="mx-2">|</span>
              <span>{{ t('userList.vaults.count') }}: {{ user.vaults?.length ?? 0 }}</span>
            </div>
            
            <!-- Creation date -->
            <div class="ml-13 text-xs text-gray-500">{{ t('userList.user.created') }}: {{ d(new Date(user.creationTime), 'short') }}</div>
          </div>
        </div>
      </div>

      <!-- Desktop Table Layout (visible on medium screens and up) -->
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8 hidden md:block">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="userListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 w-2/5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('userList.userName') }}</th>
                  <th class="px-6 py-3 w-1/8 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">{{ t('userList.groups.count') }}</th>
                  <th class="px-6 py-3 w-1/8 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">{{ t('userList.device.count') }}</th>
                  <th class="px-6 py-3 w-1/8 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">{{ t('userList.vaults.count') }}</th>
                  <th class="px-6 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">{{ t('userList.user.created') }}</th>
                  <th class="px-3 py-3 w-auto text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap"></th>
                </tr>
              </thead>

              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="user in sortedUsers" :key="user.id">
                  <tr>
                    <td class="pr-8 pl-6 py-4 text-sm font-medium text-gray-900">
                      <div class="flex items-center gap-3 max-w-sm">
                        <img :src="user.userPicture" :alt="t('userList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300"/>
                        <div class="flex flex-col min-w-0 flex-1">
                          <button type="button" class="truncate block hover:underline text-left" :title="user.name" @click="router.push(`authority/user/${user.id}`)">{{ user.name }}</button>
                          <span class="text-xs text-gray-500 truncate" :title="user.email">{{ user.email }}</span>
                        </div>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.groups?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.devices?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.vaults?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ d(new Date(user.creationTime), 'long') }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div class="flex justify-end gap-3">
                        <div class="cursor-pointer text-sm font-medium text-red-700 hover:text-red-900" @click="showDeleteUserDialog(user)">{{ t('common.remove') }}</div>
                      </div>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-else>
    <FetchError :error="onFetchError" :retry="fetchData" />
  </div>

  <!-- Delete Dialog -->
  <DeleteUserDialog v-if="deletingUser != null" ref="deleteUserDialog" :user="deletingUser" @close="deletingUser = null" @delete="onUserDeleted"/>
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import DeleteUserDialog from './DeleteUserDialog.vue';
import FetchError from './FetchError.vue';

const router = useRouter();

interface UserDto {
  id: string;
  name: string;
  email: string;
  userPicture?: string;
  language?: string;
  devices?: { id: string }[];
  groups?: string[];
  vaults?: { id: string }[];
  accessibleVaults?: string[];
  creationTime: string;
  type?: 'USER';
}

const { t, d } = useI18n({ useScope: 'global' });

const users = ref<UserDto[]>([]);
const onFetchError = ref<Error | null>(null);
const deleteUserDialog = ref<typeof DeleteUserDialog>();
const deletingUser = ref<UserDto | null>(null);
const query = ref('');

const showDeleteUserDialog = (user: UserDto) => {
  deletingUser.value = user;
  nextTick(() => deleteUserDialog.value?.show());
};

const onUserDeleted = (deletedUser: UserDto) => {
  users.value = users.value.filter(u => u.id !== deletedUser.id);
  deletingUser.value = null;
};

function showCreateUser() {
  router.push('/app/authority/user/create');
}

onMounted(() => {
  fetchData();
});

async function fetchData() {
  try {
    // TODO: Replace with actual API call
    // This is temporary mock data for development purposes
    await new Promise((resolve) => setTimeout(resolve, 500));
    users.value = [
      {
        id: '1',
        name: 'Anna Marie Schmidtson',
        email: 'anna@example.com',
        userPicture: 'https://i.pravatar.cc/150?u=anna',
        groups: ['Admin', 'Support'],
        devices: [{ id: 'd1' }, { id: 'd2' }],
        vaults: [{ id: 'v1' }],
        accessibleVaults: [],
        creationTime: '2023-05-01T10:00:00Z',
        type: 'USER',
      },
      {
        id: '2',
        name: 'Abdelmajid Achhoud',
        email: 'majid@example.com',
        userPicture: 'https://i.pravatar.cc/150?u=majid',
        groups: [],
        devices: [{ id: 'd3' }],
        vaults: [{ id: 'v2' }, { id: 'v3' }],
        accessibleVaults: [],
        creationTime: '2024-01-10T15:30:00Z',
        type: 'USER',
      },
      {
        id: '3',
        name: 'Dr. Alexander von der Heide',
        email: 'dralex@example.com',
        userPicture: 'https://i.pravatar.cc/150?u=alex',
        groups: [],
        devices: [{ id: 'd4' }],
        vaults: [{ id: 'v4' }],
        accessibleVaults: [],
        creationTime: '2024-03-10T09:00:00Z',
        type: 'USER',
      },
    ];
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

const filteredUsers = computed(() =>
  query.value === ''
    ? users.value
    : users.value.filter((u) =>
      u.name.toLowerCase().includes(query.value.toLowerCase())
    )
);

const sortedUsers = computed(() =>
  filteredUsers.value.slice().sort((a, b) =>
    new Date(b.creationTime).getTime() - new Date(a.creationTime).getTime()
  )
);
</script>

<template>
  <div v-if="onFetchError == null">
    <div v-if="users.length === 0">
      {{ t('common.loading') }}
    </div>

    <div v-else class="mt-4 flex flex-col">
      <!-- Searchbar + Createbutton -->
      <div class="flex flex-wrap sm:flex-nowrap justify-between items-center gap-3 mb-4">
        <input v-model="query" type="text" :placeholder="t('userList.search.placeholder')" class="flex-1 focus:ring-primary focus:border-primary shadow-xs text-sm border-gray-300 rounded-md"/>
        <button type="button" class="bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showCreateUser()">
          {{ t('createUserDialog.button') }}
        </button>
      </div>
      <div class="border-b border-gray-200 mb-6"></div>
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="userListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 w-1/3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('userList.userName') }}
                  </th>
                  <th class="px-6 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('userList.groups.count') }}
                  </th>
                  <th class="px-6 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('userList.device.count') }}
                  </th>
                  <th class="px-6 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('userList.vaults.count') }}
                  </th>
                  <th class="px-6 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('userList.user.created') }}
                  </th>
                  <th class="px-3 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap"></th>
                </tr>
              </thead>

              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="user in sortedUsers" :key="user.id">
                  <tr>
                    <td class="px-6 py-4 text-sm font-medium text-gray-900">
                      <div class="flex items-center gap-3 max-w-xs">
                        <img :src="user.userPicture" alt="Profilbild" class="w-10 h-10 rounded-full object-cover border border-gray-300"/>
                        <button type="button" class="truncate block hover:underline" :title="user.name" @click="router.push(`authority/user/${user.id}`)"> {{ user.name }} </button>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.groups?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.devices?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.vaults?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ d(new Date(user.creationTime), 'long') }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div class="flex justify-end gap-3">
                        <button type="button" class="inline-flex items-center gap-2 px-2.5 py-1.5 rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showDeleteUserDialog(user)">
                          <TrashIcon class="h-4 w-4 text-white" aria-hidden="true" />
                          {{ t('userList.delete.user.button') }}
                        </button>
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
import { TrashIcon } from '@heroicons/vue/24/solid';
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

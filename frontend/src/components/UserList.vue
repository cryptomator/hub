<template>
  <div v-if="onFetchError == null">
    <div v-if="users.length === 0">
      {{ t('common.loading') }}
    </div>

    <div v-else class="mt-4 flex flex-col">
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="userListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('userList.userName') }}
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('userList.groups.count') }}
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('userList.device.count') }}
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('userList.vaults.count') }}
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                    {{ t('userList.user.created') }}
                  </th>
                  <th class="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap"></th>
                </tr>
              </thead>

              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="user in sortedUsers" :key="user.id">
                  <tr>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 flex items-center gap-3 w-auto">
                      <img :src="user.avatarUrl" alt="Profilbild" class="w-10 h-10 rounded-full object-cover border border-gray-300"/>
                      <span>{{ user.name }}</span>
                    </td>                    
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.groups?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.devices?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.vaults?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ d(new Date(user.creationTime), 'short') }}</td>
                    <td class="px-12 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div class="flex justify-end gap-2">
                        <button class="text-red-600 hover:text-red-900">Delete</button>
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
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import FetchError from './FetchError.vue';

// Dummy data for demonstration purposes
interface UserDto {
  id: string;
  name: string;
  avatarUrl: string;
  groups?: string[];
  devices?: { id: string }[];
  vaults?: { id: string }[];
  creationTime: string;
}

const { t, d } = useI18n({ useScope: 'global' });

const users = ref<UserDto[]>([]);
const onFetchError = ref<Error | null>(null);

onMounted(() => {
  fetchData();
});

// Simulate an API call with Dummy Data to fetch users
async function fetchData() {
  try {
    await new Promise((resolve) => setTimeout(resolve, 500));
    users.value = [
      {
        id: '1',
        name: 'Anna Schmidt',
        avatarUrl: 'https://i.pravatar.cc/150?u=anna',
        groups: ['Admin', 'Support'],
        devices: [{ id: 'd1' }, { id: 'd2' }],
        vaults: [{ id: 'v1' }],
        creationTime: '2023-05-01T10:00:00Z',
      },
      {
        id: '2',
        name: 'Max Mustermann',
        avatarUrl: 'https://i.pravatar.cc/150?u=max',
        groups: [],
        devices: [{ id: 'd3' }],
        vaults: [{ id: 'v2' }, { id: 'v3' }],
        creationTime: '2024-01-10T15:30:00Z',
      },
      {
        id: '2',
        name: 'Max Mustermann',
        avatarUrl: 'https://i.pravatar.cc/150?u=max',
        groups: [],
        devices: [{ id: 'd3' }],
        vaults: [{ id: 'v2' }, { id: 'v3' }],
        creationTime: '2024-01-10T15:30:00Z',
      },
    ];
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

const sortedUsers = computed(() =>
  users.value.slice().sort((a, b) => new Date(b.creationTime).getTime() - new Date(a.creationTime).getTime())
);
</script>

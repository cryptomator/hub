<template>
  <div v-if="onFetchError == null">
    <div v-if="groups.length === 0">
      {{ t('common.loading') }}
    </div>

    <div v-else class="flex flex-col">
      <h2 class="text-2xl font-bold leading-9 text-gray-900 sm:text-3xl sm:truncate mb-4">
        {{ t('groups.title') }}
      </h2>
      <!-- Searchbar + Create button -->
      <div class="flex flex-wrap sm:flex-nowrap justify-between items-center gap-3 mb-4">
        <input v-model="query" type="text" :placeholder="t('groupList.search.placeholder')" class="flex-1 focus:ring-primary focus:border-primary shadow-xs text-sm border-gray-300 rounded-md"/>
        <button type="button" class="bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showCreateGroup()">{{ t('groupList.create.button') }}</button>
      </div>
      
      <!-- Mobile Card Layout (visible on small screens) -->
      <div class="block md:hidden space-y-3">
        <div v-for="group in sortedGroups" :key="group.id" class="bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow cursor-pointer" @click="router.push(`/app/groups/${group.id}`)">
          <div class="px-4 py-4">
            <div class="flex items-start justify-between mb-4">
              <div class="flex items-center min-w-0 flex-1" :title="group.name">
                <img :src="group.groupPicture" :alt="t('groupList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300 flex-shrink-0" />
                <div class="ml-3 min-w-0 flex-1">
                  <p class="text-sm font-medium text-gray-900 truncate leading-tight">{{ group.name }}</p>
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
                        <button type="button" class="text-red-700 hover:text-red-900 block w-full px-4 py-2 text-sm text-left hover:bg-gray-50" @click.stop="showDeleteGroupDialog(group)">{{ t('common.remove') }}</button>
                      </MenuItem>
                    </div>
                  </MenuItems>
                </transition>
              </Menu>
            </div>
            
            <!-- Stats section -->
            <div class="mb-3 ml-13 text-xs text-gray-600">
              <span>{{ t('groupList.members.count') }}: {{ group.members?.length ?? 0 }}</span>
              <span class="mx-2">|</span>
              <span>{{ t('groupList.vaults.count') }}: {{ group.vaults?.length ?? 0 }}</span>
            </div>
            
            <!-- Creation date -->
            <div class="ml-13 text-xs text-gray-500">{{ t('groupList.group.created') }}: {{ d(new Date(group.creationTime), 'short') }}</div>
          </div>
        </div>
      </div>

      <!-- Desktop Table Layout (visible on medium screens and up) -->
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8 hidden md:block">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="groupListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 w-1/4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('groupList.name') }}</th>
                  <th class="px-4 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('groupList.members.count') }}</th>
                  <th class="px-4 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('groupList.vaults.count') }}</th>
                  <th class="px-4 py-3 w-1/6 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">{{ t('groupList.group.created') }}</th>
                  <th class="px-4 py-3 w-auto text-right text-xs font-medium text-gray-500 uppercase tracking-wider"></th>
                </tr>
              </thead>

              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="group in sortedGroups" :key="group.name">
                  <tr>
                    <td class="px-6 py-4 text-sm font-medium text-gray-900">
                      <div class="flex items-center gap-3 max-w-xs">
                        <img :src="group.groupPicture" :alt="t('groupList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300"/>
                        <button type="button" class="truncate block hover:underline" :title="group.name" @click="router.push(`/app/groups/${group.id}`)"> {{ group.name }} </button>
                      </div>
                    </td>                
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ group.members?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ group.vaults?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ d(new Date(group.creationTime), 'long') }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div class="cursor-pointer text-sm font-medium text-red-700 hover:text-red-900" @click="showDeleteGroupDialog(group)">{{ t('common.remove') }}</div>
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
  <GroupDeleteDialog v-if="deletingGroup != null" ref="deleteGroupDialog" :group="deletingGroup" @close="deletingGroup = null" @delete="onGroupDeleted"/>
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import GroupDeleteDialog from './GroupDeleteDialog.vue';
import FetchError from '../FetchError.vue';

const router = useRouter();

interface GroupDto {
  id: string;
  name: string;
  groupPicture: string;
  members: string[];
  vaults?: { id: string }[];
  creationTime: string;
}

const { t, d } = useI18n({ useScope: 'global' });

const groups = ref<GroupDto[]>([]);
const onFetchError = ref<Error | null>(null);
const deleteGroupDialog = ref<typeof GroupDeleteDialog>();
const deletingGroup = ref<GroupDto | null>(null);
const query = ref('');

function showDeleteGroupDialog(group: GroupDto) {
  deletingGroup.value = group;
  nextTick(() => deleteGroupDialog.value?.show());
}

function onGroupDeleted(deletedGroup: GroupDto) {
  groups.value = groups.value.filter(g => g.id !== deletedGroup.id);
  deletingGroup.value = null;
}

function showCreateGroup() {
  router.push('/app/groups/create');
}

onMounted(() => {
  fetchData();
});

async function fetchData() {
  try {
    // TODO: Replace with actual API call
    // This is temporary mock data for development purposes
    await new Promise((resolve) => setTimeout(resolve, 500));
    groups.value = [
      {
        id: '1',
        name: 'Sales',
        groupPicture: 'https://i.pravatar.cc/150?u=anna',
        members: ['user1', 'user2'],
        vaults: [{ id: 'v1' }, { id: 'v2' }],
        creationTime: '2023-08-15T14:30:00Z',
      },
      {
        id: '2',
        name: 'Research & Development',
        groupPicture: 'https://i.pravatar.cc/150?u=max',
        members: ['user1', 'user2'],
        vaults: [{ id: 'v3' }],
        creationTime: '2024-02-20T09:15:00Z',
      },
      {
        id: '3',
        name: 'Marketing',
        groupPicture: 'https://i.pravatar.cc/150?u=max',
        members: ['user1', 'user2'],
        vaults: [],
        creationTime: '2024-01-10T16:45:00Z',
      },
    ];
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

const filteredGroups = computed(() =>
  query.value === ''
    ? groups.value
    : groups.value.filter((u) =>
      u.name.toLowerCase().includes(query.value.toLowerCase())
    )
);

const sortedGroups = computed(() =>
  filteredGroups.value.slice().sort((a, b) =>
    new Date(b.creationTime).getTime() - new Date(a.creationTime).getTime()
  )
);
</script>

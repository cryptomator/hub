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
      <div v-if="sortedGroups.length > 0" class="block md:hidden space-y-3">
        <div v-for="group in sortedGroups" :key="group.id" class="bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow cursor-pointer" @click="router.push(`/app/groups/${group.id}`)">
          <div class="px-4 py-4">
            <div class="flex items-start justify-between mb-4">
              <div class="flex items-center min-w-0 flex-1" :title="group.name">
                <img :src="group.pictureUrl" :alt="t('groupList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300 flex-shrink-0" />
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
            <div class="ml-13 text-xs text-gray-600 space-x-3">
              <span>{{ t('groupList.members.count') }}: {{ group.memberSize ?? 0 }}</span>
              <span>{{ t('groupList.vaults.count') }}: {{ group.vaultCount ?? 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Desktop Table Layout (visible on medium screens and up) -->
      <div v-if="sortedGroups.length > 0" class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8 hidden md:block">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="groupListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 w-2/5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('groupList.name') }}</th>
                  <th class="px-4 py-3 w-1/5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('groupList.members.count') }}</th>
                  <th class="px-4 py-3 w-1/5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{{ t('groupList.vaults.count') }}</th>
                  <th class="px-4 py-3 w-auto text-right text-xs font-medium text-gray-500 uppercase tracking-wider"></th>
                </tr>
              </thead>

              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="group in sortedGroups" :key="group.id">
                  <tr>
                    <td class="px-6 py-4 text-sm font-medium text-gray-900">
                      <div class="flex items-center gap-3 max-w-xs">
                        <img :src="group.pictureUrl" :alt="t('groupList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300"/>
                        <button type="button" class="truncate block hover:underline" :title="group.name" @click="router.push(`/app/groups/${group.id}`)"> {{ group.name }} </button>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ group.memberSize ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ group.vaultCount ?? 0 }}</td>
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

      <!-- Empty State when no groups exist -->
      <div v-else-if="query === '' && sortedGroups.length == 0" class="mt-3 text-center">
        <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('groupList.empty.title') }}</h3>
        <p class="mt-1 text-sm text-gray-500">{{ t('groupList.empty.description') }}</p>
      </div>

      <!-- Empty State for Search Results -->
      <div v-else-if="query !== '' && sortedGroups.length == 0" class="mt-3 text-center">
        <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.75 15.75l-2.489-2.489m0 0a3.375 3.375 0 10-4.773-4.773 3.375 3.375 0 004.774 4.774zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('groupList.filter.result.empty.title') }}</h3>
        <p class="mt-1 text-sm text-gray-500">{{ t('groupList.filter.result.empty.description') }}</p>
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
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter, useRoute } from 'vue-router';
import backend, { GroupDto } from '../../common/backend';
import GroupDeleteDialog from './GroupDeleteDialog.vue';
import FetchError from '../FetchError.vue';

const router = useRouter();
const route = useRoute();

const { t } = useI18n({ useScope: 'global' });

const groups = ref<GroupDto[]>([]);
const onFetchError = ref<Error | null>(null);
const deleteGroupDialog = ref<typeof GroupDeleteDialog>();
const deletingGroup = ref<GroupDto | null>(null);
const query = ref('');

function showDeleteGroupDialog(group: GroupDto) {
  deletingGroup.value = group;
  nextTick(() => deleteGroupDialog.value?.show());
}

function onGroupDeleted(deletedGroupId: string) {
  groups.value = groups.value.filter(g => g.id !== deletedGroupId);
  deletingGroup.value = null;
}

function showCreateGroup() {
  router.push('/app/groups/create');
}

onMounted(() => {
  fetchData();
});

watch(() => route.path, (newPath) => {
  if (newPath === '/app/groups') {
    fetchData();
  }
});

async function fetchData() {
  try {
    groups.value = await backend.groups.listAll();
  } catch (error) {
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

const filteredGroups = computed(() =>
  query.value === ''
    ? groups.value
    : groups.value.filter((g) =>
      g.name.toLowerCase().includes(query.value.toLowerCase())
    )
);

const sortedGroups = computed(() =>
  filteredGroups.value.slice().sort((a, b) =>
    a.name.localeCompare(b.name)
  )
);
</script>

<template>
  <div v-if="onFetchError == null">
    <div v-if="groups.length === 0">
      {{ t('common.loading') }}
    </div>

    <div v-else class="mt-4 flex flex-col">
      <!-- Searchbar + Create button -->
      <div class="flex flex-wrap sm:flex-nowrap justify-between items-center gap-3 mb-4">
        <input v-model="query" type="text" :placeholder="t('groupList.search.placeholder')" class="flex-1 focus:ring-primary focus:border-primary shadow-xs text-sm border-gray-300 rounded-md"/>
        <button type="button" class="bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showCreateGroup()">
          {{ t('groupList.create.button') }}
        </button>
      </div>
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="groupListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 w-1/4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('groupList.name') }}
                  </th>
                  <th class="px-4 py-3 w-1/4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('groupList.members.count') }}
                  </th>
                  <th class="px-4 py-3 w-1/4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('groupList.vaults.count') }}
                  </th>
                  <th class="px-4 py-3 w-auto text-right text-xs font-medium text-gray-500 uppercase tracking-wider"></th>
                </tr>
              </thead>

              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="group in sortedGroups" :key="group.name">
                  <tr>
                    <td class="px-6 py-4 text-sm font-medium text-gray-900">
                      <div class="flex items-center gap-3 max-w-xs">
                        <img :src="group.groupPicture" :alt="t('groupList.profileImage')" class="w-10 h-10 rounded-full object-cover border border-gray-300"/>
                        <button type="button" class="truncate block hover:underline" :title="group.name" @click="router.push(`/app/authority/group/${group.id}`)"> {{ group.name }} </button>
                      </div>
                    </td>                
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ group.members?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ group.vaults?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div class="cursor-pointer text-sm font-medium text-red-700 hover:text-red-900" @click="showDeleteGroupDialog(group)">
                        {{ t('common.remove') }}
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
  <DeleteGroupDialog v-if="deletingGroup != null" ref="deleteGroupDialog" :group="deletingGroup" @close="deletingGroup = null" @delete="onGroupDeleted"/>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import DeleteGroupDialog from './DeleteGroupDialog.vue';
import FetchError from './FetchError.vue';

const router = useRouter();

interface GroupDto {
  id: string;
  name: string;
  groupPicture: string;
  members: string[];
  vaults?: { id: string }[];
}

const { t } = useI18n({ useScope: 'global' });

const groups = ref<GroupDto[]>([]);
const onFetchError = ref<Error | null>(null);
const deleteGroupDialog = ref<typeof DeleteGroupDialog>();
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
  router.push('/app/authority/group/create');
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
      },
      {
        id: '2',
        name: 'Research & Development',
        groupPicture: 'https://i.pravatar.cc/150?u=max',
        members: ['user1', 'user2'],
        vaults: [{ id: 'v3' }],
      },
      {
        id: '3',
        name: 'Marketing',
        groupPicture: 'https://i.pravatar.cc/150?u=max',
        members: ['user1', 'user2'],
        vaults: [],
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
  filteredGroups.value.slice().sort((a, b) => a.name.localeCompare(b.name))
);
</script>

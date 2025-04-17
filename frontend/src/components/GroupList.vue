<template>
  <div v-if="onFetchError == null">
    <div v-if="groups.length === 0">
      {{ t('common.loading') }}
    </div>

    <div v-else class="mt-4 flex flex-col">
      <!-- Searchbar + Createbutton -->
      <div class="flex flex-wrap sm:flex-nowrap justify-between items-center gap-3 mb-4">
        <input v-model="query" type="text" :placeholder="t('groupList.search.placeholder')" class="flex-1 focus:ring-primary focus:border-primary shadow-xs text-sm border-gray-300 rounded-md"/>
        <button type="button" class="bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showCreateGroupDialog()">
          {{ t('createGroupDialog.button') }}
        </button>
      </div>
      <div class="border-b border-gray-200 mb-6"></div>
      <div class="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
        <div class="py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8">
          <div class="shadow-sm overflow-hidden border-b border-gray-200 sm:rounded-lg">
            <table class="min-w-full divide-y divide-gray-200" aria-describedby="groupListTitle">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 w-1/3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('groupList.name') }}
                  </th>
                  <th class="px-4 py-3 w-2/3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {{ t('groupList.members.count') }}
                  </th>
                  <th class="px-4 py-3 w-auto text-right text-xs font-medium text-gray-500 uppercase tracking-wider"></th>
                </tr>
              </thead>

              <tbody class="bg-white divide-y divide-gray-200">
                <template v-for="group in sortedGroups" :key="group.name">
                  <tr>
                    <td class="px-6 py-4 text-sm font-medium text-gray-900">
                      <div class="flex items-center gap-3 max-w-xs">
                        <img :src="group.groupPicture" alt="Gruppenbild" class="w-10 h-10 rounded-full object-cover border border-gray-300"/>
                        <span class="truncate block" :title="group.name">{{ group.name }}</span>
                      </div>
                    </td>                
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ group.members?.length ?? 0 }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div class="flex justify-end gap-3">
                        <button type="button" class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showEditGroupDialog()">
                          <PencilIcon class="h-4 w-4 text-gray-500" aria-hidden="true" />
                          {{ t('groupList.edit.group.button') }}
                        </button>
                        <button type="button" class="inline-flex items-center gap-2 px-2.5 py-1.5 rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="showDeleteDialog(group)">
                          <TrashIcon class="h-4 w-4 text-white" aria-hidden="true" />
                          {{ t('groupList.delete.group.button') }}
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
  <DeleteGroupDialog v-if="deletingGroup != null" ref="deleteGroupDialog" :group="deletingGroup" @close="deletingGroup = null" @delete="onGroupDeleted"/>
  <GroupEditDialog ref="editGroupDialog" />
  <GroupCreateDialog ref="createGroupDialog" />
</template>

<script setup lang="ts">
import { PencilIcon, TrashIcon } from '@heroicons/vue/24/solid';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import DeleteGroupDialog from './DeleteGroupDialog.vue';
import GroupCreateDialog from './GroupCreateDialog.vue';
import GroupEditDialog from './GroupEditDialog.vue';

import FetchError from './FetchError.vue';

interface GroupDto {
  name: string;
  groupPicture: string;
  members: string[];
}

const { t } = useI18n({ useScope: 'global' });

const groups = ref<GroupDto[]>([]);
const onFetchError = ref<Error | null>(null);
const deleteGroupDialog = ref<typeof DeleteGroupDialog>();
const deletingGroup = ref<GroupDto | null>(null);
const editGroupDialog = ref<typeof GroupEditDialog>();
const createGroupDialog = ref<typeof GroupCreateDialog>();
const query = ref('');

function showDeleteDialog(group: GroupDto) {
  deletingGroup.value = group;
  nextTick(() => deleteGroupDialog.value?.show());
}

function onGroupDeleted(deletedGroup: GroupDto) {
  groups.value = groups.value.filter(g => g.name !== deletedGroup.name);
  deletingGroup.value = null;
}

function showEditGroupDialog() {
  editGroupDialog.value?.show();
}

function showCreateGroupDialog() {
  createGroupDialog.value?.show();
}

onMounted(() => {
  fetchData();
});

async function fetchData() {
  try {
    await new Promise((resolve) => setTimeout(resolve, 500));
    groups.value = [
      {
        name: 'Sales',
        groupPicture: 'https://i.pravatar.cc/150?u=anna',
        members: ['user1', 'user2'],
      },
      {
        name: 'Research & Development',
        groupPicture: 'https://i.pravatar.cc/150?u=max',
        members: ['user1', 'user2'],
      },
      {
        name: 'Marketing',
        groupPicture: 'https://i.pravatar.cc/150?u=max',
        members: ['user1', 'user2'],
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

<template>
  <section class="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden">
    <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
      <div class="flex items-baseline gap-1">
        <h3 id="groupsTitle" class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
          {{ t('user.detail.groups') }}
        </h3>
        <span class="text-xs text-gray-500">{{ groups.length }}</span>
      </div>
      <button class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openAddGroupDialog">
        {{ t('user.detail.groups.join') }}
      </button>
    </div>

    <!-- Search bar -->
    <div class="px-6 py-3 border-b border-gray-200">
      <input id="groupSearch" v-model="groupQuery" :placeholder="t('common.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-xs text-sm border-gray-300 rounded-md disabled:bg-gray-200" />
    </div>

    <!-- Group table -->
    <div class="overflow-x-auto">
      <table class="min-w-full divide-y divide-gray-300" aria-describedby="groupsTitle">
        <tbody class="divide-y divide-gray-200 bg-white">
          <tr v-for="group in paginatedGroups" :key="group.id">
            <td class="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 flex items-center gap-3 sm:pl-6">
              <div class="w-8 h-8 rounded-full border border-gray-300 bg-white flex items-center justify-center overflow-hidden">
                <img v-if="group.userPicture" :src="group.userPicture" class="w-full h-full object-cover" alt="group icon" />
                <UserGroupIcon v-else class="w-5 h-5 text-gray-400" aria-hidden="true" />
              </div>
              <span class="truncate">{{ group.name }}</span>
            </td>

            <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
              <a tabindex="0" class="cursor-pointer text-red-600 hover:text-red-900" :title="t('common.leave')" @click="showDeleteDialog(group)">{{ t('common.leave') }}</a>
            </td>
          </tr>
          <tr v-if="!filteredGroups.length">
            <td colspan="2" class="py-4 px-4 text-sm text-center text-gray-500">
              {{ t(groupQuery ? 'group.members.search.empty' : 'common.none') }}
            </td>
          </tr>
        </tbody>

        <!-- GROUP pagination -->
        <tfoot v-if="showPaginationGroup" class="bg-gray-50">
          <tr>
            <td colspan="2">
              <nav class="flex items-center justify-between px-4 py-3 sm:px-6" :aria-label="t('common.pagination')">
                <div class="hidden sm:block">
                  <i18n-t keypath="auditLog.pagination.showing" scope="global" tag="p" class="text-sm text-gray-700">
                    <span class="font-medium">{{ paginationBeginGroup }}</span>
                    <span class="font-medium">{{ paginationEndGroup }}</span>
                  </i18n-t>
                </div>
                <div class="flex flex-1 justify-end space-x-3">
                  <button v-if="currentPageGroup > 0" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showPreviousPageGroup">
                    {{ t('common.previous') }}
                  </button>
                  <button v-if="hasNextPageGroup" type="button" class="relative inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-offset-0" @click="showNextPageGroup">
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
  <UserAddGroupDialog ref="addGroupDialog" :groups="user.groups" @saved="props.onSaved" />
  <UserGroupRemoveDialog ref="deleteGroupMemberDialog" :group="deletingGroup" @close="deletingGroup = null" @delete="onGroupRemoved"/>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import UserAddGroupDialog from './UserAddGroupDialog.vue';
import UserGroupRemoveDialog from './UserGroupRemoveDialog.vue';
import { GroupDto } from '../common/backend';
import { UserGroupIcon } from '@heroicons/vue/20/solid'; 
const { t } = useI18n({ useScope: 'global' });
interface Group {
  id: string;
  name: string;
  userPicture?: string;
}
interface DetailUser {
  firstName?: string;
  lastName?: string;
  username: string;
  roles: string[];
  password: string;
  email: string;
  userPicture?: string;
  creationTime: string;
  groups: Group[];
  vaults: Vault[];
  devices: Device[];
  legacyDevices: Device[];
}
interface Vault {
  id: string;
  name: string;
  description?: string;
}

interface Device {
  id: string;
  name: string;
  type: 'DESKTOP' | 'MOBILE' | 'TABLET';
  creationTime: string;
  lastAccessTime?: string;
  lastIpAddress?: string;
}

const props = defineProps<{
  user: DetailUser;
  groups: Group[];
  pageSize: number;
  onSaved: (groups: Group[]) => void;
}>();

const deletingGroup = ref<Group | null>(null);
const deleteGroupMemberDialog = ref<InstanceType<typeof UserGroupRemoveDialog> | null>(null);

function showDeleteDialog(g: Group) {
  deletingGroup.value = g;
  nextTick(() => {
    deleteGroupMemberDialog.value?.show();
  });
}

function onGroupRemoved(removed: GroupDto) {
  props.groups.filter(g => g.id !== removed.id);
  deletingGroup.value = null;
}

// ---------------------------------------------------------------------------
// GROUP dialog actions
// ---------------------------------------------------------------------------
const addGroupDialog = ref<InstanceType<typeof UserAddGroupDialog> | null>(null);

function openAddGroupDialog() {
  addGroupDialog.value?.show();
}

// ---------------------------------------------------------------------------
// GROUPS – search & pagination (member-list style)
// ---------------------------------------------------------------------------
const pageSizeGroup = ref(10);
const currentPageGroup = ref(0);
const groupQuery = ref('');

const filteredGroups = computed(() => {
  const q = groupQuery.value.trim().toLowerCase();
  return (props.groups ?? [])
    .filter((g) => !q || g.name.toLowerCase().includes(q))
    .sort((a, b) => a.name.localeCompare(b.name, 'de', { sensitivity: 'base' }));
});

const showPaginationGroup = computed(() => filteredGroups.value.length > pageSizeGroup.value);

const paginatedGroups = computed(() =>
  filteredGroups.value.slice(currentPageGroup.value * pageSizeGroup.value, (currentPageGroup.value + 1) * pageSizeGroup.value)
);

const hasNextPageGroup = computed(() => (currentPageGroup.value + 1) * pageSizeGroup.value < filteredGroups.value.length);

const paginationBeginGroup = computed(() => (filteredGroups.value.length ? currentPageGroup.value * pageSizeGroup.value + 1 : 0));
const paginationEndGroup = computed(() => Math.min((currentPageGroup.value + 1) * pageSizeGroup.value, filteredGroups.value.length));

function showNextPageGroup() {
  if (hasNextPageGroup.value) currentPageGroup.value++;
}
function showPreviousPageGroup() {
  if (currentPageGroup.value > 0) currentPageGroup.value--;
}

watch(() => [filteredGroups.value.length, groupQuery.value], () => (currentPageGroup.value = 0));

</script>
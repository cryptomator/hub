<template>
  <section class="bg-white rounded-lg shadow-sm overflow-hidden">
    <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
      <div class="flex items-baseline gap-1">
        <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">
          {{ t('group.detail.members') }}
        </h3>
        <span class="text-xs text-gray-500">{{ group.users.length }}</span>
      </div>
      <button class="inline-flex items-center gap-2 px-2.5 py-1.5 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openAccessDialog">
        {{ t('common.add') }}
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
            <td class="whitespace-nowrap h-17 py-4 pl-4 pr-3 text-sm font-medium text-gray-900 flex items-center gap-3 sm:pl-6">
              <img :src="user.userPicture" class="w-8 h-8 rounded-full object-cover border border-gray-300" />
              <div class="flex flex-col truncate">
                <span class="font-medium truncate">
                  <template v-if="user.name">
                    {{ user.name }}
                  </template>
                  <template v-else>
                    {{ user.username }}
                  </template>
                </span>
                <span v-if="user.name" class="text-xs text-gray-500 truncate">{{ user.username }}</span>
              </div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
              <a tabindex="0" class="cursor-pointer text-red-600 hover:text-red-900" :title="t('common.leave')" @click="showDeleteDialog(user)">{{ t('common.remove') }}</a>
            </td>
          </tr>
          <tr v-if="!filteredUsers.length">
            <td colspan="3" class="py-4 px-4 text-sm text-center text-gray-500">
              {{ t(userQuery ? 'group.members.search.empty' : 'common.none') }}
            </td>
          </tr>
        </tbody>

        <!-- USERS – Pagination ----------------------------->
        <tfoot v-if="showPaginationUsers" class="bg-gray-50">
          <tr>
            <td colspan="3">
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
  </section>
  <GroupAddMemberDialog ref="addMemberDialog" :members="group.users" @saved="props.onSaved" />
  <GroupMemberRemoveDialog v-if="deletingGroupMember" ref="deleteGroupMemberDialog" :member="deletingGroupMember" :group-id="group.id" @close="deletingGroupMember = null" @delete="onGroupMemberDeleted" />
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../common/backend';
import GroupAddMemberDialog from './GroupAddMemberDialog.vue';
import GroupMemberRemoveDialog from './GroupMemberRemoveDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  group: DetailGroup;
  members: UserDto[];
  pageSize: number;
  onSaved: (users: UserDto[]) => void;
}>();

interface DetailGroup {
  id: string;
  name: string;
  picture?: string;
  createdAt: string;
  roles: Role[];
  users: User[];
  vaults: Vault[];
  description?: string;
  memberSize: number;
}
interface User {
  id: string;
  name: string;
  userPicture?: string;
  role?: string;
  username: string;
  email?: string;
}

interface Vault {
  id: string;
  name: string;
  description?: string;
}
interface Role {
  id: string;
  name: string;
}
interface Device {
  id: string;
  name: string;
  type: 'DESKTOP' | 'MOBILE' | 'TABLET';
  creationTime: string;
  lastAccessTime?: string;
  lastIpAddress?: string;
}

const deleteGroupMemberDialog = ref<typeof GroupMemberRemoveDialog>();
const deletingGroupMember = ref<UserDto | null>(null);
const addMemberDialog = ref<InstanceType<typeof GroupAddMemberDialog> | null>(null);

function showDeleteDialog(u: UserDto) {
  deletingGroupMember.value = u;
  nextTick(() => deleteGroupMemberDialog.value?.show());
}

function openAccessDialog() {
  addMemberDialog.value?.show();
}

function onGroupMemberDeleted() {
  deletingGroupMember.value = null;
}

const currentPage = ref(0);
const userQuery = ref('');

const showPaginationUsers = computed(
  () => filteredUsers.value.length > props.pageSize
);

const filteredUsers = computed(() => {
  const q = userQuery.value.trim().toLowerCase();
  return [...props.group.users]
    .filter(u => {
      if (!q) return true;
      const nameMatch = u.name?.toLowerCase().includes(q);
      const usernameMatch = u.username?.toLowerCase().includes(q);
      return nameMatch || usernameMatch;
    })
    .sort((a, b) => {
      const aKey = a.name?.trim() || a.username || '';
      const bKey = b.name?.trim() || b.username || '';
      return aKey.localeCompare(bKey, 'de', { sensitivity: 'base' });
    });
});

const paginatedUsers = computed(() =>
  filteredUsers.value.slice(currentPage.value * props.pageSize, currentPage.value * props.pageSize + props.pageSize)
);

const hasNextPage = computed(
  () => (currentPage.value + 1) * props.pageSize < filteredUsers.value.length
);

const paginationBegin = computed(() =>
  filteredUsers.value.length ? currentPage.value * props.pageSize + 1 : 0,
);
const paginationEnd = computed(() =>
  Math.min((currentPage.value + 1) * props.pageSize, filteredUsers.value.length),
);

function showNextPage() {
  if (hasNextPage.value) currentPage.value += 1;
}
function showPreviousPage() {
  if (currentPage.value > 0) currentPage.value -= 1;
}

watch(() => [filteredUsers.value.length, userQuery.value], () => (currentPage.value = 0));

</script>
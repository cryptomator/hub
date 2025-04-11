<template>
  <h1 class="text-2xl font-bold text-gray-900 mb-4">User & Groups</h1>

  <!-- Tab Navigation -->
  <div class="flex gap-4 border-b border-gray-200 mb-6">
    <button :class="tab === 'users' ? activeClass : inactiveClass" @click="tab = 'users'">
      Users
    </button>
    <button :class="tab === 'groups' ? activeClass : inactiveClass" @click="tab = 'groups'">
      Groups
    </button>
    <div class="flex justify-end mb-4">
      <button type="button" class="ml-auto bg-primary text-white text-sm font-medium px-4 py-2 rounded-md shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="showCreateUserDialog()">
        {{ t('createUserDialog.button') }}
      </button>
    </div>
  </div>

  <!-- Change components based on the active tab -->
  <UserList v-if="tab === 'users'" />
  <GroupList v-else />
  <UserCreateDialog ref="createUserDialog" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import GroupList from './GroupList.vue';
import UserList from './UserList.vue';
import { useI18n } from 'vue-i18n';
import UserCreateDialog from './UserCreateDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const createUserDialog = ref<typeof UserCreateDialog>();
function showCreateUserDialog() {
  createUserDialog.value?.show();
}

const tab = ref<'users' | 'groups'>('users');

const activeClass = 'pb-2 border-b-2 border-primary font-medium text-primary';
const inactiveClass = 'pb-2 text-gray-500 hover:text-gray-700';
</script>
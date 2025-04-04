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
  </div>

  <!-- Change components based on the active tab -->
  <UserList v-if="tab === 'users'" />
  <GroupList v-else />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import GroupList from './GroupList.vue';
import UserList from './UserList.vue';

const tab = ref<'users' | 'groups'>('users');

const activeClass = 'pb-2 border-b-2 border-primary font-medium text-primary';
const inactiveClass = 'pb-2 text-gray-500 hover:text-gray-700';
</script>
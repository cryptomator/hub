<template>
  <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl mb-4">
    Users & Groups
  </h2>

  <div class="flex gap-4 mb-2">
    <button :class="tab === 'users' ? activeClass : inactiveClass" @click="tab = 'users'">
      Users
    </button>
    <button :class="tab === 'groups' ? activeClass : inactiveClass" @click="tab = 'groups'">
      Groups
    </button>
  </div>

  <UserList v-if="tab === 'users'" />
  <GroupList v-else />
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import GroupList from './GroupList.vue';
import UserList from './UserList.vue';

const { t } = useI18n({ useScope: 'global' });

const STORAGE_KEY = 'users-groups-tab';

const tab = ref<'users' | 'groups'>('users');
onMounted(() => {
  const stored = localStorage.getItem(STORAGE_KEY) as 'users' | 'groups' | null;
  if (stored) tab.value = stored;
});

watch(tab, (newVal) => {
  localStorage.setItem(STORAGE_KEY, newVal);
});

const activeClass = 'pb-2 border-b-2 border-primary font-medium text-primary';
const inactiveClass = 'pb-2 text-gray-500 hover:text-gray-700';
</script>

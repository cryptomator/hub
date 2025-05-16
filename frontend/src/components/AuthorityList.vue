<template>
  <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl mb-4">
    {{ t('authorityList.title') }}
  </h2>

  <div class="flex border-b border-gray-200 mb-6">
    <button :class="tab === 'users' ? activeClass : inactiveClass" class="px-6 py-3 text-sm font-medium transition-colors focus:outline-none" @click="tab = 'users'">
      {{ t('authorityList.users') }}
    </button>
    <button :class="tab === 'groups' ? activeClass : inactiveClass" class="px-6 py-3 text-sm font-medium transition-colors focus:outline-none" @click="tab = 'groups'">
      {{ t('authorityList.groups') }}
    </button>
  </div>

  <UserList v-if="tab === 'users'" />
  <GroupList v-else />
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
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

const activeClass = 'bg-white text-primary rounded-t-lg';
const inactiveClass = 'text-gray-500 hover:text-gray-700';
</script>

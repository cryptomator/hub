<template>
    <div v-if="loading" class="p-8 text-center text-sm text-gray-500">{{ t('common.loading') }}</div>
    <div v-else class="p-8 grid gap-6 md:grid-cols-3">
      <section class="col-span-3 md:col-span-1">
        <section class="col-span-3 md:col-span-1 rounded-2xl shadow-sm border border-gray-200 p-6 flex flex-col items-center text-center">
          <img :src="user.userPicture" alt="Profilbild" class="w-48 h-48 rounded-full object-cover border border-gray-300 mb-4"/>
          <h2 class="text-xl font-semibold mb-1">{{ user.name }}</h2>
          <p class="text-sm text-gray-500">{{ user.email }}</p>
        </section>
      </section>
      <section class="col-span-3 md:col-span-2">
        <section class="col-span-3 md:col-span-2 rounded-2xl shadow-sm border border-gray-200 p-6">
            <h3 class="text-lg font-medium mb-4">{{ t('userList.vaults.count') }}</h3>
            <ul class="space-y-2">
            <li v-for="vault in user.vaults" :key="vault.id" class="px-4 py-2 rounded-md bg-gray-50">{{ vault.id }}</li>
            <li v-if="!user.vaults?.length" class="text-sm text-gray-500">{{ t('common.none') }}</li>
            </ul>
        </section>
        <br>
        <section class="col-span-3 md:col-span-2 md:col-start-2 rounded-2xl shadow-sm border border-gray-200 p-6">
            <h3 class="text-lg font-medium mb-4">{{ t('userList.groups.count') }}</h3>
            <ul class="space-y-2">
            <li v-for="group in user.groups" :key="group" class="px-4 py-2 rounded-md bg-gray-50">{{ group }}</li>
            <li v-if="!user.groups?.length" class="text-sm text-gray-500">{{ t('common.none') }}</li>
            </ul>
        </section>
      </section>
    </div>
  </template>
  
  <script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  
  interface DetailUser {
    id: string;
    name: string;
    email: string;
    userPicture?: string;
    groups?: string[];
    vaults?: { id: string }[];
  }
  
  const props = defineProps<{ id: string }>();
  const { t } = useI18n({ useScope: 'global' });
  
  const user = ref<DetailUser>({
    id: props.id,
    name: 'Max Mustermann',
    email: 'max@example.com',
    userPicture: 'https://i.pravatar.cc/150?u=placeholder',
    groups: ['Admin', 'Support'],
    vaults: [{ id: 'Travel' }, { id: 'Tax' }],
  });
  const loading = ref<boolean>(true);
  
  onMounted(async () => {
    try {
      await new Promise((r) => setTimeout(r, 300));
    } finally {
      loading.value = false;
    }
  });
  </script>
  
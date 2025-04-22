<template>
    <div v-if="loading" class="p-8 text-center text-sm text-gray-500">{{ t('common.loading') }}</div>  
    <div v-else class="p-8 grid gap-6 md:grid-cols-3">
      <section class="col-span-3 md:col-span-1">
        <section class="col-span-3 md:col-span-1 rounded-2xl shadow-sm border border-gray-200 p-6 flex flex-col items-center text-center">
          <img :src="group.picture" alt="Gruppenbild" class="w-48 h-48 rounded-full object-cover border border-gray-300 mb-4"/>
          <h2 class="text-xl font-semibold">{{ group.name }}</h2>
        </section>
      </section>
      <section class="col-span-3 md:col-span-2">
        <section class="col-span-3 md:col-span-2 md:col-start-2 rounded-2xl shadow-sm border border-gray-200 p-6">
          <h3 class="text-lg font-medium mb-4">Members</h3>
          <ul class="space-y-2">
          <li v-for="member in group.members" :key="member.id" class="flex items-center gap-3 px-4 py-2 rounded-md bg-gray-50">
            <img :src="member.userPicture" alt="" class="w-8 h-8 rounded-full object-cover border border-gray-300" />
            <span class="truncate">{{ member.name }}</span>
          </li>
          <li v-if="!group.members?.length" class="text-sm text-gray-500"> {{ t('common.none') }}</li>
          </ul>
        </section>
      </section>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  
  interface Member {
    id: string;
    name: string;
    userPicture?: string;
  }
  
  interface DetailGroup {
    id: string;
    name: string;
    picture?: string;
    members?: Member[];
  }
  
  const props = defineProps<{ id: string }>();
  const { t } = useI18n({ useScope: 'global' });
  
  const group = ref<DetailGroup>({
    id: props.id,
    name: 'Frontend‑Team',
    picture: 'https://i.pravatar.cc/150?u=group',
    members: [
      { id: '1', name: 'Anna Marie Schmidtson', userPicture: 'https://i.pravatar.cc/50?u=anna' },
      { id: '2', name: 'Abdelmajid Achhoud',     userPicture: 'https://i.pravatar.cc/50?u=majid' },
      { id: '3', name: 'Dr. Alexander v. d. Heide', userPicture: 'https://i.pravatar.cc/50?u=alex' },
    ],
  });
  const loading = ref<boolean>(false);
  </script>
  
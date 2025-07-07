<template>
  <section class="bg-white rounded-lg shadow-sm overflow-hidden">
    <div class="px-6 py-6">
      <div class="flex flex-col items-center justify-center h-full text-center">
        <img :src="group.picture" class="w-48 h-48 rounded-full object-cover border border-gray-300 mb-4" />
        <h2 class="text-xl font-semibold text-gray-900">{{ group.name }}</h2>
      </div>

      <div class="divide-y divide-gray-100">
        <div class="flex justify-between py-3">
          <dt class="text-sm text-gray-500">{{ t('group.detail.roles') }}</dt>
          <dd class="flex flex-wrap justify-end gap-2">
            <template v-if="group.roles?.length">
              <span v-for="role in sortedRoles" :key="role.id" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20 capitalize">
                {{ role.name }}
              </span>
            </template>
            <span v-else class="text-sm text-gray-500">
              {{ t('common.none') }}
            </span>
          </dd>
        </div>
        <div class="py-3 flex justify-between">
          <dt class="text-sm text-gray-500">{{ t('group.detail.createdOn') }}</dt>
          <dd class="text-sm text-gray-900 font-medium">{{ d(group.createdAt, 'long') }}</dd>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { computed } from 'vue';

const { t, d } = useI18n({ useScope: 'global' });

const props = defineProps<{
  group: DetailGroup;
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

interface Role {
  id: string;
  name: string;
}

interface Vault {
  id: string;
  name: string;
  description: string;
}

interface User {
  id: string;
  name: string;
  userPicture?: string;
  role?: string;
  username: string;
  email?: string;
}

const sortedRoles = computed(() =>
  [...props.group.roles].sort((a, b) => a.name.localeCompare(b.name))
);

</script>
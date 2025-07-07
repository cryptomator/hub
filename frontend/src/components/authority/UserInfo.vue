<template>
  <section class="bg-white rounded-lg shadow-sm overflow-hidden">
    <div class="px-6 py-6">
      <div class="flex flex-col items-center justify-center h-full text-center">
        <img :src="user.userPicture" class="w-48 h-48 rounded-full object-cover border border-gray-300 mb-4" />
        <h2 class="text-xl font-semibold text-gray-900">
          <template v-if="user.firstName || user.lastName">
            {{ user.firstName }} {{ user.lastName }}
          </template>
          <template v-else>
            {{ user.username }}
          </template>
        </h2>
        <p v-if="user.firstName || user.lastName" class="text-sm text-gray-500 mt-1">
          {{ user.username }}
        </p>
      </div>
      <dl class="divide-y divide-gray-100">
        <div class="py-3 flex justify-between">
          <dt class="text-sm text-gray-500">{{ t('user.detail.email') }}</dt>
          <dd class="text-sm text-gray-900 font-medium">{{ user.email }}</dd>
        </div>
        <div class="py-3 flex justify-between">
          <dt class="text-sm text-gray-500">{{ t('user.detail.roles') }}</dt>
          <dd class="flex flex-wrap justify-end gap-2">
            <span v-for="role in sortedRoles" :key="role" class="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20 capitalize">
              {{ role }}
            </span>
            <span v-if="!sortedRoles.length" class="text-sm text-gray-500">
              {{ t('common.none') }}
            </span>
          </dd>
        </div>
        <div class="py-3 flex justify-between">
          <dt class="text-sm text-gray-500">{{ t('user.detail.createdOn') }}</dt>
          <dd class="text-sm text-gray-900 font-medium">{{ d(user.creationTime, 'long') }}</dd>
        </div>
      </dl>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { computed } from 'vue';

const { t, d } = useI18n({ useScope: 'global' });

const props = defineProps<{
  user: DetailUser;
}>();

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

interface Group {
  id: string;
  name: string;
  userPicture?: string;
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

const sortedRoles = computed(() => 
  [...props.user.roles ?? []].sort((a, b) => a.localeCompare(b, 'de', { sensitivity: 'base' }))
);

</script>
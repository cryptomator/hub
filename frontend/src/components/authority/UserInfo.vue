<template>
  <section class="bg-white rounded-lg shadow-sm overflow-hidden">
    <div class="px-6 py-6">
      <div class="flex flex-col items-center justify-center h-full text-center">
        <img :src="user.pictureUrl" class="w-48 h-48 rounded-full object-cover border border-gray-300 mb-4" />
        <h2 class="text-xl font-semibold text-gray-900 truncate w-full" :title="displayName">
          {{ displayName }}
        </h2>
        <p v-if="displayName !== user.name" class="text-sm text-gray-500 mt-1 truncate w-full" :title="user.name">
          {{ user.name }}
        </p>
        <span v-if="!user.enabled" class="inline-flex items-center rounded-md bg-gray-100 px-2 py-1 text-xs font-medium text-gray-600 ring-1 ring-inset ring-gray-500/10 mt-2">{{ t('user.detail.disabled') }}</span>
      </div>
      <dl class="divide-y divide-gray-100">
        <div class="py-3 flex justify-between min-w-0">
          <dt class="text-sm text-gray-500 shrink-0 mr-2">{{ t('user.detail.email') }}</dt>
          <dd class="text-sm text-gray-900 font-medium truncate min-w-0" :title="user.email">{{ user.email }}</dd>
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
      </dl>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { computed } from 'vue';
import { UserDtoWithDetails, isSelectableRealmRole } from '../../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  user: UserDtoWithDetails;
}>();

const displayName = computed(() =>
  props.user.firstName || props.user.lastName
    ? `${props.user.firstName ?? ''} ${props.user.lastName ?? ''}`.trim()
    : props.user.name
);

const sortedRoles = computed(() =>
  [...(props.user.realmRoles ?? [])].filter(isSelectableRealmRole).sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
);

</script>
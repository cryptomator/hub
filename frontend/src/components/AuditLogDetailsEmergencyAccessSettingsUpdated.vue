<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.emergencyaccess.settingsUpdated') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>admin</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedAdmin">{{ resolvedAdmin.name }}</span>
          <code class="text-xs" :class="resolvedAdmin ? 'text-gray-600' : ''">{{ event.adminId }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>enableEmergencyAccess</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ String(event.enableEmergencyAccess) }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>requiredKeyShares</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ event.requiredKeyShares }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>minMembers</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ event.minMembers }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>allowChoosingCouncil</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ String(event.allowChoosingCouncil) }}</code>
        </dd>
      </div>
      <div class="flex flex-col gap-1">
        <dt>
          <button type="button" class="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 focus:outline-hidden" :aria-expanded="councilMembersExpanded" @click="councilMembersExpanded = !councilMembersExpanded">
            <code>councilMembers</code>
            <ChevronRightIcon class="h-3.5 w-3.5 transition-transform" :class="{ 'rotate-90': councilMembersExpanded }" aria-hidden="true" />
          </button>
        </dt>
        <dd v-if="councilMembersExpanded" class="text-sm text-gray-900 pl-3 border-l border-gray-100">
          <AuditLogJsonView :value="councilMembers" :force-id="Array.isArray(councilMembers)" />
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { ChevronRightIcon } from '@heroicons/vue/20/solid';
import { onMounted, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventEmergencyAccessSettingsChangedDto } from '../common/auditlog';
import type { AuthorityDto } from '../common/backend';
import AuditLogJsonView from './AuditLogJsonView.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventEmergencyAccessSettingsChangedDto
}>();

const resolvedAdmin = ref<AuthorityDto>();
const councilMembersExpanded = ref(false);

const councilMembers = computed<unknown>(() => {
  const raw = props.event.councilMemberIds ?? '';
  if (raw === '') {
    return '';
  }
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : raw; // fall back to the raw value on unexpected shape
  } catch {
    return raw; // visible fallback on malformed input
  }
});

onMounted(async () => {
  resolvedAdmin.value = await auditlog.entityCache.getAuthority(props.event.adminId);
});
</script>

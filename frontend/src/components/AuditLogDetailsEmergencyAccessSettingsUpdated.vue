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
          <span v-if="resolvedAdmin != null">{{ resolvedAdmin.name }}</span>
          <code class="text-xs" :class="resolvedAdmin ? 'text-gray-600' : ''">{{ event.adminId }}</code>
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
          <code>allowChoosingCouncil</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ String(event.allowChoosingCouncil) }}</code>
        </dd>
      </div>
      <div v-if="councilIds.length > 0" class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>councilMembers</code>
        </dt>
        <dd class="flex items-center flex-wrap gap-2 text-sm text-gray-900">
          <code v-for="id in councilIds" :key="id" class="text-xs text-gray-700">
            {{ id }}
          </code>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventEmergencyAccessSettingsChangedDto } from '../common/auditlog';
import type { AuthorityDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventEmergencyAccessSettingsChangedDto
}>();

const resolvedAdmin = ref<AuthorityDto | undefined>();

const councilIds = computed<string[]>(() =>
  (props.event.councilMemberIds ?? '')
    .split(/\s+/)
    .map(s => s.trim())
    .filter(Boolean)
);

onMounted(async () => {
  resolvedAdmin.value = await auditlog.entityCache.getAuthority(props.event.adminId);
});
</script>

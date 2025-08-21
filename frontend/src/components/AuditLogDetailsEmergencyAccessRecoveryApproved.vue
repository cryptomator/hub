<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.emergencyaccess.recoveryApproved') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>process</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ event.processId }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>councilMember</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedCouncilMember != null">{{ resolvedCouncilMember.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedCouncilMember != null}">{{ event.councilMemberId }}</code>
        </dd>
      </div>
      <div v-if="event.ipAddress" class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>ipAddress</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ event.ipAddress }}</code>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventEmergencyAccessRecoveryApprovedDto } from '../common/auditlog';
import type { AuthorityDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventEmergencyAccessRecoveryApprovedDto
}>();

const resolvedCouncilMember = ref<AuthorityDto>();

onMounted(async () => {
  resolvedCouncilMember.value = await auditlog.entityCache.getAuthority(props.event.councilMemberId);
});
</script>

<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.emergencyaccess.recoveryStarted') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>vault</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedVault">{{ resolvedVault.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedVault}">{{ event.vaultId }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>process</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ event.processId }}</code>
        </dd>
      </div>
      <div v-if="event.councilMemberId" class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>councilMember</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedCouncilMember">{{ resolvedCouncilMember.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedCouncilMember}">{{ event.councilMemberId }}</code>
        </dd>
      </div>
      <div v-if="event.recoveryType" class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>recoveryType</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs">{{ event.recoveryType }}</code>
        </dd>
      </div>
      <div v-if="event.details" class="flex flex-col gap-1">
        <dt>
          <button type="button" class="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 focus:outline-hidden" :aria-expanded="detailsExpanded" @click="detailsExpanded = !detailsExpanded">
            <code>details</code>
            <ChevronRightIcon class="h-3.5 w-3.5 transition-transform" :class="{ 'rotate-90': detailsExpanded }" aria-hidden="true" />
          </button>
        </dt>
        <dd v-if="detailsExpanded" class="text-sm text-gray-900 pl-3 border-l border-gray-100">
          <AuditLogJsonView :value="parsedDetails" />
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { ChevronRightIcon } from '@heroicons/vue/20/solid';
import { onMounted, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventEmergencyAccessRecoveryStartedDto } from '../common/auditlog';
import type { AuthorityDto, VaultDto } from '../common/backend';
import AuditLogJsonView from './AuditLogJsonView.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventEmergencyAccessRecoveryStartedDto
}>();

const resolvedVault = ref<VaultDto>();
const resolvedCouncilMember = ref<AuthorityDto>();
const detailsExpanded = ref(false);

const parsedDetails = computed<unknown>(() => {
  const raw = props.event.details ?? '';
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
});

onMounted(async () => {
  resolvedVault.value = await auditlog.entityCache.getVault(props.event.vaultId);

  if (props.event.councilMemberId) {
    resolvedCouncilMember.value = await auditlog.entityCache.getAuthority(props.event.councilMemberId);
  }
});
</script>

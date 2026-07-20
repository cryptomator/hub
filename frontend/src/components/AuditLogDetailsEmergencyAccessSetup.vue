<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.emergencyaccess.setup') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>owner</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedOwner != null">{{ resolvedOwner.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedOwner != null}">{{ event.ownerId }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>vault</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedVault != null">{{ resolvedVault.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedVault != null}">{{ event.vaultId }}</code>
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
      <div class="flex flex-col gap-1">
        <dt>
          <button type="button" class="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 focus:outline-hidden" :aria-expanded="settingsExpanded" @click="settingsExpanded = !settingsExpanded">
            <code>settings</code>
            <ChevronRightIcon class="h-3.5 w-3.5 transition-transform" :class="{ 'rotate-90': settingsExpanded }" aria-hidden="true" />
          </button>
        </dt>
        <dd v-if="settingsExpanded" class="text-sm text-gray-900 pl-3 border-l border-gray-100">
          <AuditLogJsonView :value="parsedSettings" />
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { ChevronRightIcon } from '@heroicons/vue/20/solid';
import { onMounted, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventEmergencyAccessSetupDto } from '../common/auditlog';
import type { AuthorityDto, VaultDto } from '../common/backend';
import AuditLogJsonView from './AuditLogJsonView.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventEmergencyAccessSetupDto
}>();

const resolvedOwner = ref<AuthorityDto>();
const resolvedVault = ref<VaultDto>();
const settingsExpanded = ref(false);

const parsedSettings = computed<unknown>(() => {
  const raw = props.event.settings ?? '';
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
  resolvedOwner.value = await auditlog.entityCache.getAuthority(props.event.ownerId);
});
</script>

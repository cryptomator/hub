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
      <div class="flex items-start gap-2">
        <dt class="text-xs text-gray-500 mt-1">
          <code>settings</code>
        </dt>
        <dd class="text-xs text-gray-900">
          <pre class="max-w-[48rem] overflow-x-auto whitespace-pre-wrap break-words bg-gray-50 rounded p-2 ring-1 ring-inset ring-gray-200">{{ prettySettings }}</pre>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventEmergencyAccessSetupDto } from '../common/auditlog';
import type { AuthorityDto, VaultDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventEmergencyAccessSetupDto
}>();

const resolvedOwner = ref<AuthorityDto>();
const resolvedVault = ref<VaultDto>();

const prettySettings = computed(() => {
  const raw = props.event.settings ?? '';
  if (!raw) return '';
  try {
    const obj = JSON.parse(raw);
    return JSON.stringify(obj, null, 2);
  } catch {
    return raw;
  }
});

onMounted(async () => {
  resolvedVault.value = await auditlog.entityCache.getVault(props.event.vaultId);
  resolvedOwner.value = await auditlog.entityCache.getAuthority(props.event.ownerId);
});
</script>

<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.vaultKey.retrieve') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>retrieved by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedRetrievedBy != null">{{ resolvedRetrievedBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedRetrievedBy != null}">{{ event.retrievedBy }}</code>
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
      <div v-if="event.ipAddress != null" class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>ip address</code>
        </dt>
        <dd class="text-sm text-gray-900">
          {{ event.ipAddress }}
        </dd>
      </div>
      <div v-if="event.deviceId != null" class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>device</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedDevice != null">{{ resolvedDevice.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedDevice != null}">{{ event.deviceId }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>result</code>
        </dt>
        <dd class="text-sm text-gray-900">
          <span v-if="props.event.result === 'SUCCESS'">Success</span>
          <span v-else-if="props.event.result === 'UNAUTHORIZED'">Unauthorized</span>
          <span v-else>{{ props.event.result }}</span>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventVaultKeyRetrieveDto } from '../common/auditlog';
import { AuthorityDto, DeviceDto, VaultDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventVaultKeyRetrieveDto
}>();

const resolvedRetrievedBy = ref<AuthorityDto>();
const resolvedVault = ref<VaultDto>();
const resolvedDevice = ref<DeviceDto>();

onMounted(async () => {
  resolvedRetrievedBy.value = await auditlog.entityCache.getAuthority(props.event.retrievedBy);
  resolvedVault.value = await auditlog.entityCache.getVault(props.event.vaultId);
  if (props.event.deviceId) {
    resolvedDevice.value = await auditlog.entityCache.getDevice(props.event.deviceId);
  }
});
</script>

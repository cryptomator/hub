<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.vault.create') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>created by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedCreatedBy != null">{{ resolvedCreatedBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedCreatedBy != null}">{{ event.createdBy }}</code>
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
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>name</code>
        </dt>
        <dd class="text-sm text-gray-900">
          {{ event.vaultName }}
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>description</code>
        </dt>
        <dd class="text-sm text-gray-900">
          <span v-if="event.vaultDescription">{{ event.vaultDescription }}</span>
          <span v-else class="text-gray-400">&lt;empty&gt;</span>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventVaultCreateDto } from '../common/auditlog';
import { AuthorityDto, VaultDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventVaultCreateDto
}>();

const resolvedCreatedBy = ref<AuthorityDto>();
const resolvedVault = ref<VaultDto>();

onMounted(async () => {
  resolvedCreatedBy.value = await auditlog.entityCache.getAuthority(props.event.createdBy);
  resolvedVault.value = await auditlog.entityCache.getVault(props.event.vaultId);
});
</script>

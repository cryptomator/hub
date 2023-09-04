<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.vaultOwnership.claim') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>claimed by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedClaimedBy != null">{{ resolvedClaimedBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedClaimedBy != null}">{{ event.claimedBy }}</code>
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
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventVaultOwnershipClaimDto } from '../common/auditlog';
import { AuthorityDto, VaultDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventVaultOwnershipClaimDto
}>();

const resolvedClaimedBy = ref<AuthorityDto>();
const resolvedVault = ref<VaultDto>();

onMounted(async () => {
  resolvedClaimedBy.value = await auditlog.entityCache.getAuthority(props.event.claimedBy);
  resolvedVault.value = await auditlog.entityCache.getVault(props.event.vaultId);
});
</script>

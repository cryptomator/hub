<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.vaultAccess.grant') }}
    <span v-if="event.automatic" class="ml-1 inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">{{ t('auditLog.details.vaultAccess.automatic') }}</span>
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>granted by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedGrantedBy">{{ resolvedGrantedBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedGrantedBy}">{{ event.grantedBy }}</code>
        </dd>
      </div>
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
          <code>authority</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedAuthority">{{ resolvedAuthority.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedAuthority}">{{ event.authorityId }}</code>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventVaultAccessGrantDto } from '../common/auditlog';
import { AuthorityDto, VaultDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventVaultAccessGrantDto
}>();

const resolvedGrantedBy = ref<AuthorityDto>();
const resolvedVault = ref<VaultDto>();
const resolvedAuthority = ref<AuthorityDto>();

onMounted(async () => {
  resolvedGrantedBy.value = await auditlog.entityCache.getAuthority(props.event.grantedBy);
  resolvedVault.value = await auditlog.entityCache.getVault(props.event.vaultId);
  resolvedAuthority.value = await auditlog.entityCache.getAuthority(props.event.authorityId);
});
</script>

<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.events.createVault') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>user</code>
        </dt>
        <dd class="text-sm text-gray-900">
          <span v-if="resolvedUser != null" :title="resolvedUser.id">{{ resolvedUser.name }}</span>
          <code v-else>{{ event.userId }}></code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>vault</code>
        </dt>
        <dd class="text-sm text-gray-900">
          <span v-if="resolvedVault != null" :title="resolvedVault.id">{{ resolvedVault.name }}</span>
          <code v-else>{{ event.vaultId }}</code>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { AuditLogEntityCache, AuthorityDto, CreateVaultEventDto, VaultDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: CreateVaultEventDto
}>();

const entityCache = AuditLogEntityCache.getInstance();
const resolvedUser = ref<AuthorityDto>();
const resolvedVault = ref<VaultDto>();

onMounted(async () => {
  resolvedUser.value = await entityCache.getAuthority(props.event.userId);
  resolvedVault.value = await entityCache.getVault(props.event.vaultId);
});
</script>

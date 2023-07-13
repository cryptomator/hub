<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.events.updateVaultMembership') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>user</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedUser != null">{{ resolvedUser.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedUser != null}">{{ event.userId }}</code>
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
          <code>authority</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedAuthority != null">{{ resolvedAuthority.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedAuthority != null}">{{ event.authorityId }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>operation</code>
        </dt>
        <dd class="text-sm text-gray-900">
          <span v-if="props.event.operation === 'ADD'">Add</span>
          <span v-else-if="props.event.operation === 'REMOVE'">Remove</span>
          <span v-else>{{ props.event.operation }}</span>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { AuditLogEntityCache, AuthorityDto, UpdateVaultMembershipEventDto, VaultDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: UpdateVaultMembershipEventDto
}>();

const entityCache = AuditLogEntityCache.getInstance();
const resolvedUser = ref<AuthorityDto>();
const resolvedVault = ref<VaultDto>();
const resolvedAuthority = ref<AuthorityDto>();

onMounted(async () => {
  resolvedUser.value = await entityCache.getAuthority(props.event.userId);
  resolvedVault.value = await entityCache.getVault(props.event.vaultId);
  resolvedAuthority.value = await entityCache.getAuthority(props.event.authorityId);
});
</script>

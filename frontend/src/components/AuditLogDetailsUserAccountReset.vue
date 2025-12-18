<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.user.account.reset') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>reset by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedResetBy != null">{{ resolvedResetBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedResetBy != null}">{{ event.resetBy }}</code>
        </dd>
      </div>
    </dl>
  </td>
</template>
  
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventUserAccountResetDto } from '../common/auditlog';
import { AuthorityDto } from '../common/backend';
  
const { t } = useI18n({ useScope: 'global' });
  
const props = defineProps<{
    event: AuditEventUserAccountResetDto
  }>();
  
const resolvedResetBy = ref<AuthorityDto>();
  
onMounted(async () => {
  resolvedResetBy.value = await auditlog.entityCache.getAuthority(props.event.resetBy);
});
</script>

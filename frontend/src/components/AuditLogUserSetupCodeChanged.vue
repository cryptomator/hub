<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.user.setupCode.change') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>changed by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedChangedBy != null">{{ resolvedChangedBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedChangedBy != null}">{{ event.changedBy }}</code>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventUserSetupCodeChangeDto } from '../common/auditlog';
import { AuthorityDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
    event: AuditEventUserSetupCodeChangeDto
  }>();

const resolvedChangedBy = ref<AuthorityDto>();

onMounted(async () => {
  resolvedChangedBy.value = await auditlog.entityCache.getAuthority(props.event.changedBy);
});
</script>

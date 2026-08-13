<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.setting.autoGrant.update') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>updated by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedUpdatedBy">{{ resolvedUpdatedBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedUpdatedBy}">{{ event.updatedBy }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>enabled</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs text-gray-600">{{ event.enabled }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>trust threshold</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs text-gray-600">{{ event.trustThreshold }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>allow override</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs text-gray-600">{{ event.allowOverride }}</code>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventSettingAutoGrantUpdateDto } from '../common/auditlog';
import { AuthorityDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventSettingAutoGrantUpdateDto
}>();

const resolvedUpdatedBy = ref<AuthorityDto>();

onMounted(async () => {
  resolvedUpdatedBy.value = await auditlog.entityCache.getAuthority(props.event.updatedBy);
});
</script>

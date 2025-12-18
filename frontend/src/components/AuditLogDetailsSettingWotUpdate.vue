<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.setting.wot.update') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>updated by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedUpdatedBy != null">{{ resolvedUpdatedBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedUpdatedBy != null}">{{ event.updatedBy }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>maximum wot depth</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs text-gray-600">{{ event.wotMaxDepth }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>fingerprint verification preciseness</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <code class="text-xs text-gray-600">{{ event.wotIdVerifyLen }}</code>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventSettingWotUpdateDto } from '../common/auditlog';
import { AuthorityDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventSettingWotUpdateDto
}>();

const resolvedUpdatedBy = ref<AuthorityDto>();

onMounted(async () => {
  resolvedUpdatedBy.value = await auditlog.entityCache.getAuthority(props.event.updatedBy);
});
</script>

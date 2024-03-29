<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.device.register') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>registered by</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedRegisteredBy != null">{{ resolvedRegisteredBy.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedRegisteredBy != null}">{{ event.registeredBy }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
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
          <code>name</code>
        </dt>
        <dd class="text-sm text-gray-900">
          {{ event.deviceName }}
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>type</code>
        </dt>
        <dd class="text-sm text-gray-900">
          <span v-if="props.event.deviceType === 'BROWSER'">Browser</span>
          <span v-else-if="props.event.deviceType === 'DESKTOP'">Desktop</span>
          <span v-else-if="props.event.deviceType === 'MOBILE'">Mobile</span>
          <span v-else>{{ props.event.deviceType }}</span>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventDeviceRegisterDto } from '../common/auditlog';
import { AuthorityDto, DeviceDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventDeviceRegisterDto
}>();

const resolvedRegisteredBy = ref<AuthorityDto>();
const resolvedDevice = ref<DeviceDto>();

onMounted(async () => {
  resolvedRegisteredBy.value = await auditlog.entityCache.getAuthority(props.event.registeredBy);
  resolvedDevice.value = await auditlog.entityCache.getDevice(props.event.deviceId);
});
</script>

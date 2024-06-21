<template>
  <td class="whitespace-nowrap px-3 py-4 text-sm font-medium text-gray-900">
    {{ t('auditLog.details.wot.signedIdentity') }}
  </td>
  <td class="whitespace-nowrap py-4 pl-3 pr-4 sm:pr-6">
    <dl class="flex flex-col gap-2">
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>signer</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedSigner">{{ resolvedSigner.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedSigner != null}">{{ event.signerId }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>identity</code>
        </dt>
        <dd class="flex items-baseline gap-2 text-sm text-gray-900">
          <span v-if="resolvedUser">{{ resolvedUser.name }}</span>
          <code class="text-xs" :class="{'text-gray-600': resolvedUser != null}">{{ event.userId }}</code>
        </dd>
      </div>
      <div class="flex items-baseline gap-2">
        <dt class="text-xs text-gray-500">
          <code>signature</code>
        </dt>
        <dd class="text-sm text-gray-900">
          <a :href="jwtLink" target="_blank" class="underline text-gray-500 hover:text-gray-900">check on jwt.io</a>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventSignedWotIdDto } from '../common/auditlog';
import { UserDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventSignedWotIdDto
}>();

const resolvedUser = ref<UserDto>();
const resolvedSigner = ref<UserDto>();
const jwtLink = ref<string>(`https://jwt.io/#debugger-io?token=${props.event.signature}&publicKey=-----BEGIN%20PUBLIC%20KEY-----%0A${encodeURIComponent(props.event.signerKey)}%0A-----END%20PUBLIC%20KEY-----`);

onMounted(async () => {
  resolvedUser.value = await auditlog.entityCache.getAuthority(props.event.userId) as UserDto;
  resolvedSigner.value = await auditlog.entityCache.getAuthority(props.event.signerId) as UserDto;
});
</script>

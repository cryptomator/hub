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
          <span v-if="signatureStatus === SignatureStatus.STILL_VALID" class="text-primary">still valid</span>
          <span v-else-if="signatureStatus === SignatureStatus.SIGNER_KEY_CHANGED" class="text-amber-500">was valid; signer key changed by now</span>
          <span v-else-if="signatureStatus === SignatureStatus.SIGNED_KEY_CHANGED" class="text-amber-500">was valid; signed key changed by now</span>
          <span v-else class="text-red-600">invalid</span>
        </dd>
      </div>
    </dl>
  </td>
</template>

<script setup lang="ts">
import { base64 } from 'rfc4648';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auditlog, { AuditEventSignedWotIdDto } from '../common/auditlog';
import { UserDto } from '../common/backend';
import { UserKeys, asPublicKey } from '../common/crypto';
import { JWT, JWTHeader } from '../common/jwt';
import wot, { SignedKeys } from '../common/wot';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  event: AuditEventSignedWotIdDto
}>();

enum SignatureStatus {
  STILL_VALID,
  SIGNER_KEY_CHANGED,
  SIGNED_KEY_CHANGED,
  INVALID
}

const resolvedUser = ref<UserDto>();
const resolvedSigner = ref<UserDto>();
const signatureStatus = ref<SignatureStatus>(SignatureStatus.INVALID);
const signedFingerprint = ref<string>();
const currentFingerprint = ref<string>();

onMounted(async () => {
  const trustedUser = await auditlog.entityCache.getAuthority(props.event.userId) as UserDto;
  const signingUser = await auditlog.entityCache.getAuthority(props.event.signerId) as UserDto;

  if (trustedUser) {
    currentFingerprint.value = await wot.computeFingerprint(trustedUser);
  }

  try {
    const signerPublicKey = await asPublicKey(base64.parse(props.event.signerKey), UserKeys.ECDSA_KEY_DESIGNATION, UserKeys.ECDSA_PUB_KEY_USAGES);
    const [_, signedKeys] = await JWT.parse(props.event.signature, signerPublicKey) as [JWTHeader, SignedKeys];
    signedFingerprint.value = await wot.computeFingerprint({ecdhPublicKey: signedKeys.ecdhPublicKey, ecdsaPublicKey: signedKeys.ecdsaPublicKey});
    if (props.event.signerKey === signingUser?.ecdsaPublicKey && signedFingerprint.value === currentFingerprint.value) {
      signatureStatus.value = SignatureStatus.STILL_VALID;
    } else if (props.event.signerKey !== signingUser?.ecdsaPublicKey) {
      signatureStatus.value = SignatureStatus.SIGNER_KEY_CHANGED;
    } else if (signedFingerprint.value !== currentFingerprint.value) {
      signatureStatus.value = SignatureStatus.SIGNED_KEY_CHANGED;
    }
  } catch (e) {
    signatureStatus.value = SignatureStatus.INVALID;
  } finally {
    resolvedUser.value = trustedUser;
    resolvedSigner.value = signingUser;
  }
});
</script>

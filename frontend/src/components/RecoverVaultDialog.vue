<template>
  <HubDialog v-model:open="open" :title="t('recoverVaultDialog.title')" :busy="processing" :error="onVaultRecoverError && t('common.unexpectedError', [onVaultRecoverError.message])" :initial-focus="recoveryKeyInput" @submit="recoverVault()" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('recoverVaultDialog.description', [vault.name]) }}
    </DialogDescription>
    <div class="mt-5">
      <label for="recoveryKey" class="block text-sm font-medium text-gray-700">{{ t('recoverVaultDialog.recoveryKey') }}</label>
      <textarea
        id="recoveryKey"
        ref="recoveryKeyInput"
        v-model="recoveryKey"
        rows="6"
        name="recoveryKey"
        spellcheck="false"
        autocomplete="off"
        autocapitalize="off"
        :aria-invalid="!!recoveryKeyError"
        :aria-describedby="recoveryKeyError ? 'recoveryKeyError' : undefined"
        class="mt-1 focus:ring-primary focus:border-primary block w-full resize-none shadow-xs sm:text-sm border-gray-300 rounded-md font-mono disabled:bg-gray-200"
        :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': recoveryKeyError }"
        :disabled="processing"
      />
      <p v-if="recoveryKeyError" id="recoveryKeyError" class="mt-2 text-sm text-red-900">
        {{ recoveryKeyError }}
      </p>
    </div>

    <template #footer>
      <button type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing" :class="primaryButton">
        {{ t('recoverVaultDialog.submit') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { VaultDto, isUvfVault } from '../common/backend';
import { AccessTokenProducing } from '../common/crypto';
import { UniversalVaultFormat } from '../common/universalVaultFormat';
import userdata from '../common/userdata';
import { VaultFormat8 } from '../common/vaultFormat8';
import { primaryButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const recoveryKeyInput = ref<HTMLTextAreaElement>();

const recoveryKeyError = ref<string>();
const onVaultRecoverError = ref<Error>();

const recoveryKey = ref('');

const props = defineProps<{
  vault: VaultDto
}>();

const emit = defineEmits<{
  close: []
  recovered: []
}>();

defineExpose({
  show
});

function show() {
  recoveryKey.value = '';
  recoveryKeyError.value = undefined;
  onVaultRecoverError.value = undefined;
  open.value = true;
}

async function recoverVault() {
  if (processing.value || !open.value) {
    return;
  }
  recoveryKeyError.value = undefined;
  onVaultRecoverError.value = undefined;
  if (recoveryKey.value.trim().length === 0) {
    recoveryKeyError.value = t('recoverVaultDialog.error.formValidationFailed');
    recoveryKeyInput.value?.focus();
    return;
  }
  try {
    processing.value = true;
    const vaultKeys = await decodeRecoveryKey();
    if (!vaultKeys) {
      recoveryKeyError.value = t('recoverVaultDialog.error.invalidRecoveryKey');
      return;
    }
    const me = await userdata.me;
    const publicKey = await userdata.ecdhPublicKey;
    const jwe = await vaultKeys.encryptForUser(publicKey, true); // recovery is an owner-only action, so the owner's access token includes the recovery key
    await backend.vaults.grantAccess(props.vault.id, { userId: me.id, token: jwe });
    emit('recovered');
    open.value = false;
  } catch (error) {
    console.error('Recovering vault failed.', error);
    onVaultRecoverError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

async function decodeRecoveryKey(): Promise<AccessTokenProducing | undefined> {
  try {
    return isUvfVault(props.vault)
      ? await UniversalVaultFormat.recover(props.vault.uvfMetadataFile, recoveryKey.value)
      : await VaultFormat8.recover(recoveryKey.value);
  } catch {
    // Decode errors quote parts of the entered key, so don't log the error itself.
    console.error('Recovery key rejected.');
    return undefined;
  }
}
</script>

<template>
  <HubDialog v-model:open="open" :title="t('claimVaultOwnershipDialog.title')" :busy="processing" :error="onAuthenticationError && t('common.unexpectedError', [onAuthenticationError.message])" :initial-focus="passwordInput" @submit="authenticateVaultAdmin()" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('claimVaultOwnershipDialog.description', [vault.name]) }}
    </DialogDescription>
    <div class="mt-5">
      <label for="password" class="block text-sm font-medium text-gray-700">{{ t('claimVaultOwnershipDialog.password') }}</label>
      <input id="password" ref="passwordInput" v-model="password" type="password" name="password" :aria-invalid="!!passwordError" :aria-describedby="passwordError ? 'passwordError' : undefined" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': passwordError }" :disabled="processing" />
      <p v-if="passwordError" id="passwordError" class="mt-2 text-sm text-red-900">
        {{ passwordError }}
      </p>
    </div>

    <template #footer>
      <button type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing" :class="primaryButton">
        {{ t('claimVaultOwnershipDialog.submit') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { VaultDto } from '../common/backend';
import { UnwrapKeyError } from '../common/crypto';
import { VaultFormat8 } from '../common/vaultFormat8';
import { primaryButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const passwordInput = ref<HTMLInputElement>();

const passwordError = ref<string>();
const onAuthenticationError = ref<Error>();

const password = ref('');

const props = defineProps<{
  vault: VaultDto
}>();

const emit = defineEmits<{
  close: []
  action: [vaultKeys: VaultFormat8, ownerSigningKey: CryptoKeyPair]
}>();

defineExpose({
  show
});

function show() {
  password.value = '';
  passwordError.value = undefined;
  onAuthenticationError.value = undefined;
  open.value = true;
}

async function authenticateVaultAdmin() {
  if (processing.value || !open.value) {
    return;
  }
  passwordError.value = undefined;
  onAuthenticationError.value = undefined;
  if (password.value.length === 0) {
    passwordError.value = t('claimVaultOwnershipDialog.error.formValidationFailed');
    passwordInput.value?.focus();
    return;
  }
  try {
    processing.value = true;
    const [vaultKeys, ownerKeyPair] = await decryptWithAdminPassword();
    emit('action', vaultKeys, ownerKeyPair);
    open.value = false;
  } catch (error) {
    console.error('Authentication of vault admin failed.', error);
    if (error instanceof UnwrapKeyError) {
      passwordError.value = t('claimVaultOwnershipDialog.error.wrongPassword');
    } else {
      onAuthenticationError.value = error instanceof Error ? error : new Error('Unknown Error');
    }
  } finally {
    processing.value = false;
  }
}

async function decryptWithAdminPassword() {
  const { masterkey, authPrivateKey, authPublicKey, salt, iterations } = props.vault;
  if (!masterkey || !authPrivateKey || !authPublicKey || !salt || !iterations) {
    throw new Error('Vault is missing legacy "vault admin password" related properties.');
  }
  return VaultFormat8.decryptWithAdminPassword(password.value, masterkey, authPrivateKey, authPublicKey, salt, iterations);
}
</script>

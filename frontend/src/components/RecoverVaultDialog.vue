<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('close')">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto" @close="open = false">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" />
      </TransitionChild>

      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
            <DialogPanel class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
              <form ref="form" novalidate @submit.prevent="validateRecoveryKey()">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="sm:flex sm:items-start">
                    <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                        {{ t('recoverVaultDialog.title') }}
                      </DialogTitle>
                      <div class="mt-2">
                        <p class="text-sm text-gray-500">
                          {{ t('recoverVaultDialog.description') }}
                        </p>
                        <textarea id="recoveryKey" v-model="recoveryKey" rows="6" name="recoveryKey" class="mt-2 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary sm:text-sm" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onVaultRecoverError instanceof FormValidationFailedError }" required />
                      </div>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse items-baseline">
                  <button type="submit" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary  text-base font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm">
                    {{ t('recoverVaultDialog.submit') }}
                  </button>
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                  <div v-if="onVaultRecoverError != null">
                    <p v-if="onVaultRecoverError instanceof FormValidationFailedError" class="text-sm text-red-900">
                      {{ t('recoverVaultDialog.error.formValidationFailed') }}
                    </p>
                    <p v-else class="text-sm text-red-900">
                      {{ t('recoverVaultDialog.error.invalidRecoveryKey') }}
                    </p>
                  </div>
                </div>
              </form>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { base64 } from 'rfc4648';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto, VaultDto } from '../common/backend';

import { VaultKeys } from '../common/crypto';

class FormValidationFailedError extends Error {
  constructor() {
    super('The form is invalid.');
  }
}

const { t } = useI18n({ useScope: 'global' });

const form = ref<HTMLFormElement>();

const onVaultRecoverError = ref<Error|null>();

const open = ref(false);
const recoveryKey = ref('');
const processingVaultRecovery = ref(false);

const props = defineProps<{
  vault: VaultDto,
  me: UserDto
}>();

const emit = defineEmits<{
  close: []
  recovered: []
}>();

defineExpose({
  show
});

function show() {
  open.value = true;
}

async function validateRecoveryKey() {
  if (!form.value?.checkValidity()) {
    throw new FormValidationFailedError();
  }
  await recoverVault();
}

async function recoverVault() {
  onVaultRecoverError.value = null;
  try {
    processingVaultRecovery.value = true;
    const vaultKeys = await VaultKeys.recover(recoveryKey.value);
    if (props.me.publicKey && vaultKeys) {
      const publicKey = base64.parse(props.me.publicKey);
      const jwe = await vaultKeys.encryptForUser(publicKey);
      await backend.vaults.grantAccess(props.vault.id, props.me.id, jwe);
      emit('recovered');
      open.value = false;
    }
  } catch (error) {
    console.error('Recovering vault failed.', error);
    onVaultRecoverError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processingVaultRecovery.value = false;
  }
}
</script>

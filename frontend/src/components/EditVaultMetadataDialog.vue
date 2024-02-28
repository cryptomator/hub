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
              <form ref="form" novalidate @submit.prevent="updateVaultMetadata()">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="sm:flex sm:items-start">
                    <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-gray-100 sm:mx-0 sm:h-10 sm:w-10">
                      <PencilIcon class="h-6 w-6 text-gray-600" aria-hidden="true" />
                    </div>
                    <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                        {{ t('editVaultMetadataDialog.title') }}
                      </DialogTitle>
                      <div class="mt-2">
                        <p class="text-sm text-gray-500">
                          {{ t('editVaultMetadataDialog.description') }}
                        </p>
                      </div>
                      <div class="mt-5 sm:mt-6 text-left">      
                        <label for="vaultName" class="block text-sm font-medium text-gray-700">{{ t('editVaultMetadataDialog.vaultName') }}</label>
                        <input id="vaultName" v-model="vaultName" type="text" name="vaultName" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onUpdateVaultMetadataError instanceof FormValidationFailedError }" required />
                      </div>
                      <div class="mt-5 sm:mt-6 text-left">      
                        <label for="vaultDescription" class="block text-sm font-medium text-gray-700">
                          {{ t('editVaultMetadataDialog.vaultDescription') }}
                          <span class="text-xs text-gray-500">({{ t('common.optional') }})</span>
                        </label>
                        <input id="vaultDescription" v-model="vaultDescription" type="text" name="vaultDescription" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onUpdateVaultMetadataError instanceof FormValidationFailedError }" />
                      </div>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse items-baseline">
                  <button type="submit" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary  text-base font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm">
                    {{ t('common.update') }}
                  </button>
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                  <div v-if="onUpdateVaultMetadataError != null">
                    <p v-if="onUpdateVaultMetadataError instanceof FormValidationFailedError" class="text-sm text-red-900">
                      {{ t('editVaultMetadataDialog.error.formValidationFailed') }}
                    </p>
                    <p v-else class="text-sm text-red-900">
                      {{ t('common.unexpectedError', [onUpdateVaultMetadataError.message]) }}
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
import { PencilIcon } from '@heroicons/vue/24/outline';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { VaultDto } from '../common/backend';

class FormValidationFailedError extends Error {
  constructor() {
    super('The form is invalid.');
  }
}

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const form = ref<HTMLFormElement>();

const onUpdateVaultMetadataError = ref<Error|null>();

const vaultName = ref('');
const vaultDescription = ref<string | undefined>();

const props = defineProps<{
  vault: VaultDto
}>();

const emit = defineEmits<{
  close: []
  updated: [updatedVault: VaultDto]
}>();

defineExpose({
  show
});

function show() {
  open.value = true;
  vaultName.value = props.vault.name;
  vaultDescription.value = props.vault.description;
}

async function updateVaultMetadata() {
  onUpdateVaultMetadataError.value = null;
  try {
    if (!form.value?.checkValidity()) {
      throw new FormValidationFailedError();
    }
    const vault = props.vault;
    const updatedVault = await backend.vaults.createOrUpdateVault(vault.id, vaultName.value, vault.archived, vault.metadata, vaultDescription.value);
    emit('updated', updatedVault);
    open.value = false;
  } catch (error) {
    console.error('Updating vault metadata failed.', error);
    onUpdateVaultMetadataError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>

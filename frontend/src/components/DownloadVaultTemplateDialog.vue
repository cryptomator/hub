<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('close')">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto" @close="open = false">
      <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
          <DialogOverlay class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" />
        </TransitionChild>

        <!-- This element is to trick the browser into centering the modal contents. -->
        <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
        <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
          <div class="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
            <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
              <div class="sm:flex sm:items-start">
                <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-gray-100 sm:mx-0 sm:h-10 sm:w-10">
                  <FolderDownloadIcon class="h-6 w-6 text-gray-600" aria-hidden="true" />
                </div>
                <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left">
                  <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                    {{ t('downloadVaultTemplateDialog.title') }}
                  </DialogTitle>
                  <div class="mt-2">
                    <p class="text-sm text-gray-500">
                      {{ t('downloadVaultTemplateDialog.description') }}
                    </p>
                    <input id="password" v-model="password" type="password" name="password" class="mt-2 shadow-sm focus:ring-primary focus:border-primary block w-full sm:text-sm border-gray-300 rounded-md" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': isWrongPassword }" :placeholder="t('downloadVaultTemplateDialog.masterPassword')" />
                    <p v-if="isWrongPassword" class="mt-2 text-sm text-red-500 text-left">{{ t('common.error.wrongPassword') }}</p>
                  </div>
                </div>
              </div>
            </div>
            <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
              <button type="button" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary  text-base font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm" @click="downloadVault()">
                {{ t('common.download') }}
              </button>
              <button ref="cancelButtonRef" type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                {{ t('common.cancel') }}
              </button>
              <p v-if="onDownloadError != null && !isWrongPassword" class="text-sm text-red-900 px-4 sm:px-6 pb-3 text-right bg-red-50">
                {{ t('common.unexpectedError', [onDownloadError.message]) }}
              </p>
            </div>
          </div>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { FolderDownloadIcon } from '@heroicons/vue/outline';
import { saveAs } from 'file-saver';
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { VaultDto } from '../common/backend';
import { Masterkey, UnwrapKeyError, WrappedMasterkey } from '../common/crypto';
import { VaultConfig } from '../common/vaultconfig';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const password = ref('');

const isWrongPassword = computed(() => onDownloadError.value instanceof UnwrapKeyError);
const onDownloadError = ref<Error|null>();

const props = defineProps<{
  vault: VaultDto
}>();

defineEmits<{
  (e: 'close'): void
}>();

defineExpose({
  show
});

function show() {
  open.value = true;
}

async function downloadVault() {
  onDownloadError.value = null;
  try {
    const blob = await generateVaultZip();
    saveAs(blob, `${props.vault.name}.zip`);
    open.value = false;
  } catch (error) {
    console.error('Downloading vault template failed.', error);
    onDownloadError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function generateVaultZip(): Promise<Blob> {
  const wrappedKey = new WrappedMasterkey(props.vault.masterkey, props.vault.salt, props.vault.iterations);
  const masterkey = await Masterkey.unwrap(password.value, wrappedKey);
  const config = await VaultConfig.create(props.vault.id, masterkey);
  return await config.exportTemplate();
}
</script>

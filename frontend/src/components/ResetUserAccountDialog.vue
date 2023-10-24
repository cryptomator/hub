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
              <form novalidate @submit.prevent="resetUserAccount()">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="sm:flex sm:items-start">
                    <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-gray-100 sm:mx-0 sm:h-10 sm:w-10">
                      <ExclamationTriangleIcon class="h-6 w-6 text-gray-600" aria-hidden="true" />
                    </div>
                    <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                        {{ t('resetUserAccountDialog.title') }}
                      </DialogTitle>
                      <div class="mt-2">
                        <p class="text-sm text-gray-500">
                          {{ t('resetUserAccountDialog.description') }}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse sm:items-center">
                  <button type="submit" :disabled="processing" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-red-600 text-base font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:hover:bg-red-600 disabled:cursor-not-allowed" >
                    {{ t('resetUserAccountDialog.submit') }}
                  </button>
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.close') }}
                  </button>
                  <p v-if="onResetError != null" class="mt-3 text-center text-sm text-red-900 sm:mt-0 sm:text-right">
                    {{ t('common.unexpectedError', [onResetError.message]) }}
                  </p>
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
import { ExclamationTriangleIcon } from '@heroicons/vue/24/outline';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import { BrowserKeys } from '../common/crypto';
import router from '../router';

const { t } = useI18n({ useScope: 'global' });

const onResetError = ref<Error | null>();

const processing = ref(false);

const open = ref(false);

defineEmits<{
  (e: 'close'): void
}>();

defineExpose({
  show
});

const props = defineProps<{
  me : UserDto
}>();

async function show() {
  open.value = true;
}

async function resetUserAccount() {
  onResetError.value = null;
  try {
    processing.value = true;
    await backend.users.resetMe();
    await BrowserKeys.delete(props.me.id);
    router.go(0); // reload current page
  } catch (error) {
    console.error('Resetting user account failed.', error);
    onResetError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

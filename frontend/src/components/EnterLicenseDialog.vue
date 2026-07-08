<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('close')">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto" @close="open = false">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75 transition-opacity" />
      </TransitionChild>

      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
            <DialogPanel class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
              <form novalidate @submit.prevent="setToken()">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="sm:flex sm:items-start">
                    <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-gray-100 sm:mx-0 sm:h-10 sm:w-10">
                      <PercentBadgeIcon class="h-6 w-6 text-gray-600" aria-hidden="true" />
                    </div>
                    <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                        {{ t('enterLicenseDialog.title') }}
                      </DialogTitle>
                      <div class="mt-2">
                        <p class="text-sm text-gray-500">
                          {{ t('enterLicenseDialog.description') }}
                        </p>
                      </div>
                      <div class="mt-5 sm:mt-6 text-left">
                        <label for="licenseToken" class="block text-sm font-medium text-gray-700">{{ t('enterLicenseDialog.token') }}</label>
                        <textarea id="licenseToken" v-model="token" rows="6" name="licenseToken" :aria-invalid="showFormatError" class="mt-1 focus:ring-primary focus:border-primary block w-full resize-none shadow-xs sm:text-sm border-gray-300 rounded-md font-mono break-all disabled:bg-gray-200" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': showFormatError }" :disabled="processing" />
                        <p v-if="showFormatError" class="mt-2 text-sm text-red-900">
                          {{ t('enterLicenseDialog.error.invalidToken') }}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse items-baseline">
                  <button type="submit" :disabled="processing || !isTokenValid" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-xs px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed sm:ml-3 sm:w-auto sm:text-sm">
                    {{ t('common.save') }}
                  </button>
                  <button type="button" :disabled="processing" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-xs px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                  <div v-if="onSetTokenError">
                    <p v-if="onSetTokenError instanceof BadRequestError" class="text-sm text-red-900">
                      {{ t('enterLicenseDialog.error.rejected') }}
                    </p>
                    <p v-else class="text-sm text-red-900">
                      {{ t('common.unexpectedError', [onSetTokenError.message]) }}
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
import { PercentBadgeIcon } from '@heroicons/vue/24/outline';
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { BadRequestError } from '../common/backend';

// A JWS in compact serialization: three base64url-encoded segments separated by dots.
const JWT_PATTERN = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/;

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const token = ref('');
const onSetTokenError = ref<Error>();

// Immediate, field-level validation: the token must look like a JWT before it can be submitted.
const isTokenValid = computed(() => JWT_PATTERN.test(token.value.trim()));
// Only flag the field once the user has actually entered something.
const showFormatError = computed(() => token.value.trim().length > 0 && !isTokenValid.value);

// Editing the token invalidates any previous server verdict.
watch(token, () => onSetTokenError.value = undefined);

const emit = defineEmits<{
  close: []
  saved: []
}>();

defineExpose({
  show
});

function show() {
  token.value = '';
  onSetTokenError.value = undefined;
  open.value = true;
}

async function setToken() {
  if (!isTokenValid.value) {
    return;
  }
  onSetTokenError.value = undefined;
  try {
    processing.value = true;
    await backend.billing.setToken(token.value.trim());
    emit('saved');
    open.value = false;
  } catch (error) {
    console.error('Setting license token failed.', error);
    onSetTokenError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

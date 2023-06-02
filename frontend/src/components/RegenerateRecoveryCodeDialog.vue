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
              <form v-if="state == State.ConfirmRegenerateRecoveryCode" novalidate @submit.prevent="regenerateRecoveryCode()">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="sm:flex sm:items-start">
                    <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-gray-100 sm:mx-0 sm:h-10 sm:w-10">
                      <ArrowPathIcon class="h-6 w-6 text-gray-600" aria-hidden="true" />
                    </div>
                    <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                        {{ t('regenerateRecoveryCodeDialog.confirmRegenerateRecoveryCode.title') }}
                      </DialogTitle>
                      <div class="mt-2">
                        <p class="text-sm text-gray-500">
                          {{ t('regenerateRecoveryCodeDialog.confirmRegenerateRecoveryCode.description') }}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse sm:items-center">
                  <button type="submit" :disabled="processing" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-red-600 text-base font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:hover:bg-red-600 disabled:cursor-not-allowed" >
                    {{ t('regenerateRecoveryCodeDialog.submit') }}
                  </button>
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.close') }}
                  </button>
                  <p v-if="onRegenerateError != null" class="mt-3 text-center text-sm text-red-900 sm:mt-0 sm:text-right">
                    {{ t('common.unexpectedError', [onRegenerateError.message]) }}
                  </p>
                </div>
              </form>

              <div v-else-if="state == State.SaveRecoveryCode">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="sm:flex sm:items-start">
                    <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-gray-100 sm:mx-0 sm:h-10 sm:w-10">
                      <KeyIcon class="h-6 w-6 text-gray-600" aria-hidden="true" />
                    </div>
                    <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                        {{ t('regenerateRecoveryCodeDialog.saveRecoveryCode.title') }}
                      </DialogTitle>
                      <div class="mt-2">
                        <p class="text-sm text-gray-500">
                          {{ t('regenerateRecoveryCodeDialog.saveRecoveryCode.description') }}
                        </p>
                      </div>
                      <div class="relative mt-3">
                        <div class="overflow-hidden rounded-lg border border-gray-300 shadow-sm focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
                          <label for="recoveryCode" class="sr-only">{{ t('regenerateRecoveryCodeDialog.saveRecoveryCode.recoveryCode') }}</label>
                          <textarea id="recoveryCode" v-model="recoveryCode" rows="1" name="recoveryCode" class="block w-full resize-none border-0 py-3 font-mono text-center focus:ring-0" readonly />
  
                          <!-- Spacer element to match the height of the toolbar -->
                          <div class="py-2" aria-hidden="true">
                            <div class="h-9" />
                          </div>
                        </div>

                        <div class="absolute inset-x-0 bottom-0">
                          <div class="flex flex-nowrap justify-end space-x-2 py-2 px-2 sm:px-3">
                            <div class="flex-shrink-0">
                              <button type="button" class="relative inline-flex items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 text-sm font-medium text-gray-500 hover:bg-gray-100 sm:px-3" @click="copyRecoveryCode()">
                                <ClipboardIcon class="h-5 w-5 flex-shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
                                <span v-if="!copiedRecoveryCode" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
                                <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse sm:items-center">
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.close') }}
                  </button>
                </div>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ClipboardIcon } from '@heroicons/vue/20/solid';
import { ArrowPathIcon, KeyIcon } from '@heroicons/vue/24/outline';
import { base64 } from 'rfc4648';
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend from '../common/backend';
import { BrowserKeys, UserKeys } from '../common/crypto';
import { JWEBuilder } from '../common/jwe';
import { debounce } from '../common/util';

enum State {
  ConfirmRegenerateRecoveryCode,
  SaveRecoveryCode
}

const { t } = useI18n({ useScope: 'global' });

const onRegenerateError = ref<Error | null>();

const state = ref(State.ConfirmRegenerateRecoveryCode);
const processing = ref(false);

const open = ref(false);
const copiedRecoveryCode = ref(false);
const debouncedCopyFinish = debounce(() => copiedRecoveryCode.value = false, 2000);

const props = defineProps<{
  recoveryCode: string
}>();

const emit = defineEmits<{
  (e: 'update:recoveryCode', recoveryCode: string): void 
  (e: 'close'): void
}>();

defineExpose({
  show
});

const recoveryCode = computed({
  get: () => props.recoveryCode,
  set: recoveryCode => emit('update:recoveryCode', recoveryCode),
});

async function show() {
  state.value = State.ConfirmRegenerateRecoveryCode;
  open.value = true;
}

async function regenerateRecoveryCode() {
  onRegenerateError.value = null;
  try {
    processing.value = true;

    const me = await backend.users.me(true);
    if (me.publicKey == null || me.setupCode == null) {
      throw new Error('User not initialized.');
    }
    const browserKeys = await BrowserKeys.load(me.id);
    const browserId = await browserKeys.id();
    const myDevice = me.devices.find(d => d.id == browserId);
    if (myDevice == null) {
      throw new Error('Device not initialized.');
    }
    const newCode = crypto.randomUUID(); // TODO something else?
    const userKeys = await UserKeys.decryptOnBrowser(myDevice.userKey, browserKeys.keyPair.privateKey, base64.parse(me.publicKey));
    me.privateKey = await userKeys.encryptedPrivateKey(newCode);
    me.setupCode = await JWEBuilder.ecdhEs(userKeys.keyPair.publicKey).encrypt({ setupCode: newCode });
    await backend.users.putMe(me);
    recoveryCode.value = newCode;

    state.value = State.SaveRecoveryCode;
  } catch (error) {
    console.error('Regenerating recovery code failed.', error);
    onRegenerateError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}

async function copyRecoveryCode() {
  await navigator.clipboard.writeText(recoveryCode.value);
  copiedRecoveryCode.value = true;
  debouncedCopyFinish();
}
</script>

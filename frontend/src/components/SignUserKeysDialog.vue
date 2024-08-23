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
              <form novalidate @submit.prevent="sign" >
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="sm:flex sm:items-start">
                    <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-primary-l2 sm:mx-0 sm:h-10 sm:w-10">
                      <IdentificationIcon class="h-6 w-6 text-primary" aria-hidden="true" />
                    </div>
                    <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                        {{ t('signUserKeysDialog.title', [user.name]) }}
                      </DialogTitle>
                      <div class="mt-2 text-sm text-gray-500">
                        <p>
                          {{ t('signUserKeysDialog.description', [minVerificationLen, user.name]) }}
                        </p>
                      </div>
                      <div class="my-2">
                        <label for="fingerprint" class="block text-sm font-medium leading-6 text-gray-900">Fingerprint</label>
                        <div class="relative mt-2 rounded-md shadow-sm">
                          <input id="fingerprint" v-model="enteredFingerprint" type="text" name="fingerprint" :readonly="minVerificationLen === 0" class="block w-full rounded-md border-0 py-1.5 pr-10 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-primary text-sm sm:leading-6" @keyup="tryAutocomplete()" />
                          <div class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                            <CheckIcon v-if="fingerprintMatches" class="h-5 w-5 text-primary" aria-hidden="true"/>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" :disabled="!fingerprintMatches" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed" >
                    {{ t('signUserKeysDialog.submit') }}
                  </button>
                  <button type="button" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                </div>
                <p v-if="onSignError != null" class="text-sm text-red-900 px-4 sm:px-6 text-right bg-red-50">
                  {{ t('common.unexpectedError', [onSignError.message]) }}
                </p>
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
import { CheckIcon, IdentificationIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { TrustDto, UserDto } from '../common/backend';
import wot from '../common/wot';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);

const props = defineProps<{
  user: UserDto
  trust?: TrustDto
}>();

const emit = defineEmits<{
  close: []
  signed: [TrustDto]
}>();

defineExpose({
  show
});

const onSignError = ref<Error | null>();
const expectedFingerprint = ref<string>();
const enteredFingerprint = ref<string>('');
const minVerificationLen = ref<number>(64); // default: check all 64 hex digits of 256 bit fingerprint
const fingerprintMatches = computed(() => enteredFingerprint.value?.replaceAll(' ', '') === expectedFingerprint.value);

onMounted(fetchData);

async function fetchData() {
  expectedFingerprint.value = await wot.computeFingerprint(props.user);
  minVerificationLen.value = (await backend.settings.get()).wotIdVerifyLen;
  if (minVerificationLen.value === 0) {
    enteredFingerprint.value = expectedFingerprint.value.replace(/.{8}/g, '$&' + ' ').trim();
  }
}

function show() {
  open.value = true;
}

async function tryAutocomplete() {
  if (enteredFingerprint.value.length >= minVerificationLen.value) {
    if (expectedFingerprint.value?.startsWith(enteredFingerprint.value)) {
      enteredFingerprint.value = expectedFingerprint.value.replace(/.{8}/g, '$&' + ' ').trim();
    }
  }
}

async function sign() {
  try {
    const newTrust = await wot.sign(props.user);
    emit('signed', newTrust);
    open.value = false;
  } catch (error) {
    onSignError.value = error;
  }
}

</script>

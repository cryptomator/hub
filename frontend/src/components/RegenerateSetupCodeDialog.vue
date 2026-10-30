<template>
  <HubDialog
    v-model:open="open"
    :role="confirming ? 'alertdialog' : 'dialog'"
    :title="confirming ? t('regenerateAccountKeyDialog.confirmRegenerateAccountKey.title') : t('regenerateAccountKeyDialog.saveAccountKey.title')"
    :icon="confirming ? ExclamationTriangleIcon : undefined"
    tone="danger"
    :busy="processing"
    :error="onRegenerateError && t('common.unexpectedError', [onRegenerateError.message])"
    :initial-focus="confirming ? cancelButton : copyButton"
    @submit="regenerateSetupCode()"
    @closed="$emit('close')"
  >
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ confirming ? t('regenerateAccountKeyDialog.confirmRegenerateAccountKey.description') : t('regenerateAccountKeyDialog.saveAccountKey.description') }}
    </DialogDescription>
    <div v-if="!confirming" class="relative mt-3">
      <div class="overflow-hidden rounded-lg border border-gray-300 shadow-xs focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
        <label for="setupCode" class="sr-only">{{ t('regenerateAccountKeyDialog.saveAccountKey.accountKey') }}</label>
        <textarea id="setupCode" v-model="setupCode" rows="1" name="setupCode" class="block w-full resize-none border-0 py-3 font-mono text-center focus:ring-0" readonly />

        <!-- Spacer element to match the height of the toolbar -->
        <div class="py-2" aria-hidden="true">
          <div class="h-9" />
        </div>
      </div>

      <div class="absolute inset-x-0 bottom-0">
        <div class="flex flex-nowrap justify-end space-x-2 py-2 px-2 sm:px-3">
          <div class="shrink-0">
            <button ref="copyButton" type="button" class="relative inline-flex items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 text-sm font-medium text-gray-500 hover:bg-gray-100 sm:px-3" @click="copySetupCode()">
              <ClipboardIcon class="h-5 w-5 shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
              <span v-if="!copiedSetupCode" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
              <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <button ref="cancelButton" type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ confirming ? t('common.cancel') : t('common.close') }}
      </button>
      <button v-if="confirming" type="submit" :disabled="processing" :class="dangerButton">
        {{ t('regenerateAccountKeyDialog.submit') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { ClipboardIcon } from '@heroicons/vue/20/solid';
import { ExclamationTriangleIcon } from '@heroicons/vue/24/outline';
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend from '../common/backend';
import { JWE, Recipient } from '../common/jwe';
import userdata from '../common/userdata';
import { debounce } from '../common/util';
import { dangerButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

enum State {
  ConfirmRegenerateSetupCode,
  SaveSetupCode
}

const { t } = useI18n({ useScope: 'global' });

const onRegenerateError = ref<Error>();

const state = ref(State.ConfirmRegenerateSetupCode);
const processing = ref(false);

const open = ref(false);
const cancelButton = ref<HTMLButtonElement>();
const copyButton = ref<HTMLButtonElement>();
const copiedSetupCode = ref(false);
const debouncedCopyFinish = debounce(() => copiedSetupCode.value = false, 2000);

const confirming = computed(() => state.value === State.ConfirmRegenerateSetupCode);

const props = defineProps<{
  setupCode: string
}>();

const emit = defineEmits<{
  close: [],
  'update:setupCode': [setupCode: string],
}>();

defineExpose({
  show
});

const setupCode = computed({
  get: () => props.setupCode,
  set: setupCode => emit('update:setupCode', setupCode),
});

async function show() {
  state.value = State.ConfirmRegenerateSetupCode;
  onRegenerateError.value = undefined;
  open.value = true;
}

async function regenerateSetupCode() {
  if (processing.value || !open.value || !confirming.value) {
    return;
  }
  onRegenerateError.value = undefined;
  try {
    processing.value = true;

    const me = await userdata.me;
    const newCode = crypto.randomUUID();
    const userKeys = await userdata.decryptUserKeysWithBrowser();
    me.privateKeys = await userKeys.encryptWithSetupCode(newCode);
    me.setupCode = await JWE.build({ setupCode: newCode }).withRecipients(Recipient.ecdhEs('org.cryptomator.hub.userkey', userKeys.ecdhKeyPair.publicKey)).toCompact();
    await backend.users.putMe(me);
    setupCode.value = newCode;

    state.value = State.SaveSetupCode;
  } catch (error) {
    console.error('Regenerating setup code failed.', error);
    onRegenerateError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}

async function copySetupCode() {
  await navigator.clipboard.writeText(setupCode.value);
  copiedSetupCode.value = true;
  debouncedCopyFinish();
}
</script>

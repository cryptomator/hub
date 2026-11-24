<template>
  <HubDialog v-model:open="open" :title="t('recoveryKeyDialog.title', [vault.name])" :error="onShowRecoveryKeyError && t('common.unexpectedError', [onShowRecoveryKeyError.message])" :initial-focus="closeButton" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('recoveryKeyDialog.description', [vault.name]) }}
    </DialogDescription>
    <div class="relative mt-3">
      <div class="overflow-hidden rounded-lg border border-gray-300 shadow-xs focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
        <label for="recoveryKey" class="sr-only">{{ t('recoveryKeyDialog.recoveryKey') }}</label>
        <textarea id="recoveryKey" v-model="recoveryKey" :rows="textAreaRows" name="recoveryKey" spellcheck="false" autocomplete="off" autocapitalize="off" class="block w-full resize-none border-0 py-3 focus:ring-0 sm:text-sm" readonly />

        <!-- Spacer element to match the height of the toolbar -->
        <div class="py-2" aria-hidden="true">
          <div class="h-9" />
        </div>
      </div>

      <div class="absolute inset-x-0 bottom-0 flex justify-end py-2 px-2 sm:px-3">
        <button type="button" class="relative inline-flex shrink-0 items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 text-sm font-medium text-gray-500 hover:bg-gray-100 sm:px-3" @click="copyRecoveryKey()">
          <ClipboardIcon class="h-5 w-5 shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
          <span v-if="!copiedRecoveryKey" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
          <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
        </button>
      </div>
    </div>

    <template #footer>
      <button ref="closeButton" type="button" :class="secondaryButton" @click="open = false">
        {{ t('common.close') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { ClipboardIcon } from '@heroicons/vue/20/solid';
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { VaultDto } from '../common/backend';
import { debounce } from '../common/util';
import { secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const recoveryKey = ref<string>('');
const copiedRecoveryKey = ref(false);
const closeButton = ref<HTMLButtonElement>();
const onShowRecoveryKeyError = ref<Error>();
const debouncedCopyFinish = debounce(() => copiedRecoveryKey.value = false, 2000);
const textAreaRows = computed(() => recoveryKey.value.length > 390 ? 14 : 6);

defineProps<{
  vault: VaultDto
}>();

defineEmits<{
  close: []
}>();

defineExpose({
  show
});

async function show(recoveryKeyPromise: Promise<string>) {
  onShowRecoveryKeyError.value = undefined;
  open.value = true;
  try {
    recoveryKey.value = await recoveryKeyPromise;
  } catch (error) {
    console.error('Displaying recovery key failed.', error);
    onShowRecoveryKeyError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

async function copyRecoveryKey() {
  await navigator.clipboard.writeText(recoveryKey.value);
  copiedRecoveryKey.value = true;
  debouncedCopyFinish();
}
</script>

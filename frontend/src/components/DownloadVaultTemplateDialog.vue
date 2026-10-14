<template>
  <HubDialog v-model:open="open" :title="t('downloadVaultTemplateDialog.title')" :busy="processing" :error="onDownloadError && t('common.unexpectedError', [onDownloadError.message])" :initial-focus="downloadButton" @submit="downloadVault()" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('downloadVaultTemplateDialog.description') }}
    </DialogDescription>

    <template #footer>
      <button type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.close') }}
      </button>
      <button ref="downloadButton" type="submit" :disabled="processing" :class="primaryButton">
        {{ t('common.download') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { saveAs } from 'file-saver';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { VaultDto } from '../common/backend';
import { absBackendBaseURL } from '../common/config';
import { VaultTemplateProducing } from '../common/crypto';
import { primaryButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const downloadButton = ref<HTMLButtonElement>();
const onDownloadError = ref<Error>();

const props = defineProps<{
  vault: VaultDto
  vaultKeys: VaultTemplateProducing
}>();

defineEmits<{
  close: []
}>();

defineExpose({
  show
});

function show() {
  onDownloadError.value = undefined;
  open.value = true;
}

async function downloadVault() {
  if (processing.value || !open.value) {
    return;
  }
  onDownloadError.value = undefined;
  try {
    processing.value = true;
    const blob = await props.vaultKeys.exportTemplate(absBackendBaseURL, props.vault);
    saveAs(blob, `${props.vault.name}.zip`);
    open.value = false;
  } catch (error) {
    console.error('Downloading vault template failed.', error);
    onDownloadError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

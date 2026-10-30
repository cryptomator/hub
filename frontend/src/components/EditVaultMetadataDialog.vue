<template>
  <HubDialog v-model:open="open" :title="t('editVaultMetadataDialog.title')" :busy="processing" :error="onUpdateVaultMetadataError && t('common.unexpectedError', [onUpdateVaultMetadataError.message])" :initial-focus="vaultNameInput" @submit="updateVaultMetadata()" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('editVaultMetadataDialog.description', [originalVaultName]) }}
    </DialogDescription>
    <div class="mt-5">
      <label for="vaultName" class="block text-sm font-medium text-gray-700">{{ t('editVaultMetadataDialog.vaultName') }}</label>
      <input id="vaultName" ref="vaultNameInput" v-model="vaultName" type="text" name="vaultName" :aria-invalid="!!vaultNameError" :aria-describedby="vaultNameError ? 'vaultNameError' : undefined" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': vaultNameError }" :disabled="processing" />
      <p v-if="vaultNameError" id="vaultNameError" class="mt-2 text-sm text-red-900">
        {{ vaultNameError }}
      </p>
    </div>
    <div class="mt-5">
      <label for="vaultDescription" class="block text-sm font-medium text-gray-700">
        {{ t('editVaultMetadataDialog.vaultDescription') }}
        <span class="text-xs text-gray-500">({{ t('common.optional') }})</span>
      </label>
      <input id="vaultDescription" v-model="vaultDescription" type="text" name="vaultDescription" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :disabled="processing" />
    </div>

    <template #footer>
      <button type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing" :class="primaryButton">
        {{ t('common.save') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { VaultDto } from '../common/backend';
import { primaryButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const vaultNameInput = ref<HTMLInputElement>();

const vaultNameError = ref<string>();
const onUpdateVaultMetadataError = ref<Error>();

const originalVaultName = ref('');
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
  originalVaultName.value = props.vault.name;
  vaultName.value = props.vault.name;
  vaultDescription.value = props.vault.description;
  vaultNameError.value = undefined;
  onUpdateVaultMetadataError.value = undefined;
  open.value = true;
}

async function updateVaultMetadata() {
  if (processing.value || !open.value) {
    return;
  }
  vaultNameError.value = undefined;
  onUpdateVaultMetadataError.value = undefined;
  if (vaultName.value.trim().length === 0) {
    vaultNameError.value = t('editVaultMetadataDialog.error.formValidationFailed');
    vaultNameInput.value?.focus();
    return;
  }
  try {
    processing.value = true;
    const dto = { ...props.vault };
    dto.name = vaultName.value;
    dto.description = vaultDescription.value;
    const updatedVault = await backend.vaults.createOrUpdateVault(dto);
    emit('updated', updatedVault);
    open.value = false;
  } catch (error) {
    console.error('Updating vault metadata failed.', error);
    onUpdateVaultMetadataError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

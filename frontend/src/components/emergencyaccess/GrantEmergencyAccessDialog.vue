<template>
  <HubDialog v-model:open="open" :title="t('grantEmergencyAccessDialog.title')" :busy="processing" :error="onGrantEmergencyAccessError && t('common.unexpectedError', [onGrantEmergencyAccessError.message])" :initial-focus="cancelButton" @submit="splitRecoveryKey()" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t(settings.allowChoosingEmergencyCouncil ? 'grantEmergencyAccessDialog.description.selectCouncil' : 'grantEmergencyAccessDialog.description.default', [vault.name, settings.defaultRequiredEmergencyKeyShares]) }}
    </DialogDescription>
    <EmergencyAccessSetup ref="emergencyAccessSetup" :settings="settings" :required-key-shares="settings.defaultRequiredEmergencyKeyShares" :allow-choosing-council="settings.allowChoosingEmergencyCouncil" />

    <template #footer>
      <button ref="cancelButton" type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing || !emergencyAccessSetup || emergencyAccessSetup.hasValidationErrors" :class="primaryButton">
        {{ t('grantEmergencyAccessDialog.grant') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { SettingsDto, VaultDto } from '../../common/backend';
import { RecoveryKeyProducing } from '../../common/crypto';
import { primaryButton, secondaryButton } from '../dialogButtons';
import HubDialog from '../HubDialog.vue';
import EmergencyAccessSetup from './EmergencyAccessSetup.vue';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const cancelButton = ref<HTMLButtonElement>();
const emergencyAccessSetup = ref<InstanceType<typeof EmergencyAccessSetup>>();
const onGrantEmergencyAccessError = ref<Error>();

const props = defineProps<{
  vault: VaultDto,
  vaultKeys: RecoveryKeyProducing
  settings: SettingsDto
}>();

const emit = defineEmits<{
  close: []
  updated: [updatedVault: VaultDto]
}>();

defineExpose({
  show
});

function show() {
  onGrantEmergencyAccessError.value = undefined;
  open.value = true;
}

async function splitRecoveryKey() {
  if (processing.value || !open.value || !emergencyAccessSetup.value || emergencyAccessSetup.value.hasValidationErrors) {
    return;
  }
  onGrantEmergencyAccessError.value = undefined;
  try {
    processing.value = true;
    const { requiredKeyShares, keyShares } = await emergencyAccessSetup.value.split(props.vault.id, props.vaultKeys);
    const updatedVault = await backend.vaults.createOrUpdateVault({
      ...props.vault,
      requiredEmergencyKeyShares: requiredKeyShares,
      emergencyKeyShares: keyShares
    });
    emit('updated', updatedVault);
    open.value = false;
  } catch (error) {
    console.error('Granting emergency access failed.', error);
    onGrantEmergencyAccessError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

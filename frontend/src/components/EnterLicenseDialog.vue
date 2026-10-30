<template>
  <HubDialog v-model:open="open" :title="t('enterLicenseDialog.title')" :busy="processing" :error="setLicenseKeyErrorMessage" :initial-focus="licenseKeyInput" @submit="setLicenseKey()" @closed="$emit('close')">
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ t('enterLicenseDialog.description') }}
    </DialogDescription>
    <div class="mt-5">
      <label for="licenseKey" class="block text-sm font-medium text-gray-700">{{ t('enterLicenseDialog.licenseKey') }}</label>
      <textarea
        id="licenseKey"
        ref="licenseKeyInput"
        v-model="licenseKey"
        rows="6"
        name="licenseKey"
        spellcheck="false"
        autocomplete="off"
        autocapitalize="off"
        :aria-invalid="!!licenseKeyError"
        :aria-describedby="licenseKeyError ? 'licenseKeyError' : undefined"
        class="mt-1 focus:ring-primary focus:border-primary block w-full resize-none shadow-xs sm:text-sm border-gray-300 rounded-md font-mono break-all disabled:bg-gray-200"
        :class="{ 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500': licenseKeyError }"
        :disabled="processing"
      />
      <p v-if="licenseKeyError" id="licenseKeyError" class="mt-2 text-sm text-red-900">
        {{ licenseKeyError }}
      </p>
    </div>

    <template #footer>
      <button type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing || !isLicenseKeyValid" :class="primaryButton">
        {{ t('enterLicenseDialog.submit') }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { BadRequestError } from '../common/backend';
import { primaryButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

// A JWS in compact serialization: three base64url-encoded segments separated by dots.
const JWT_PATTERN = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/;

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const licenseKeyInput = ref<HTMLTextAreaElement>();

const onSetLicenseKeyError = ref<Error>();

const licenseKey = ref('');

// Immediate, field-level validation: the license key must look like a JWT before it can be submitted.
const isLicenseKeyValid = computed(() => JWT_PATTERN.test(licenseKey.value.trim()));
// Only flag the field once the user has actually entered something.
const licenseKeyError = computed(() => licenseKey.value.trim().length > 0 && !isLicenseKeyValid.value ? t('enterLicenseDialog.error.invalidLicenseKey') : undefined);

const setLicenseKeyErrorMessage = computed(() => {
  if (onSetLicenseKeyError.value instanceof BadRequestError) {
    return t('enterLicenseDialog.error.rejected', [onSetLicenseKeyError.value.message]);
  } else if (onSetLicenseKeyError.value) {
    return t('common.unexpectedError', [onSetLicenseKeyError.value.message]);
  } else {
    return undefined;
  }
});

// Editing the license key invalidates any previous server verdict.
watch(licenseKey, () => onSetLicenseKeyError.value = undefined);

const emit = defineEmits<{
  close: []
  saved: []
}>();

defineExpose({
  show
});

function show() {
  licenseKey.value = '';
  onSetLicenseKeyError.value = undefined;
  open.value = true;
}

async function setLicenseKey() {
  if (processing.value || !open.value || !isLicenseKeyValid.value) {
    return;
  }
  onSetLicenseKeyError.value = undefined;
  try {
    processing.value = true;
    await backend.billing.setToken(licenseKey.value.trim());
    emit('saved');
    open.value = false;
  } catch (error) {
    console.error('Setting license key failed.', error);
    onSetLicenseKeyError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

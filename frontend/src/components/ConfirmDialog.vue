<template>
  <HubDialog
    v-model:open="open"
    role="alertdialog"
    :title="title"
    :icon="icon ?? ExclamationTriangleIcon"
    :tone="tone"
    :size="size"
    :busy="processing"
    :error="error"
    :initial-focus="cancelButton"
    @submit="confirm"
    @closed="$emit('close')"
  >
    <DialogDescription as="p" class="mt-2 text-sm text-gray-500">
      {{ description }}
    </DialogDescription>
    <div v-if="$slots.default" class="mt-4">
      <slot />
    </div>

    <template #footer>
      <button ref="cancelButton" type="button" :disabled="processing" :class="secondaryButton" @click="open = false">
        {{ cancelLabel ?? t('common.cancel') }}
      </button>
      <button type="submit" :disabled="processing" :class="tone === 'danger' ? dangerButton : primaryButton">
        {{ confirmLabel }}
      </button>
    </template>
  </HubDialog>
</template>

<script setup lang="ts">
import { DialogDescription } from '@headlessui/vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/24/outline';
import { ref } from 'vue';
import type { Component } from 'vue';
import { useI18n } from 'vue-i18n';
import { dangerButton, primaryButton, secondaryButton } from './dialogButtons';
import HubDialog from './HubDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const props = withDefaults(defineProps<{
  title: string
  description: string
  confirmLabel: string
  action?: () => Promise<void>
  tone?: 'danger' | 'neutral'
  icon?: Component
  cancelLabel?: string
  size?: 'default' | 'narrow'
  failedMessage?: string
}>(), {
  action: undefined,
  tone: 'danger',
  icon: undefined,
  cancelLabel: undefined,
  size: 'default',
  failedMessage: undefined
});

const emit = defineEmits<{
  close: []
  confirmed: []
}>();

const open = ref(false);
const processing = ref(false);
const error = ref<string>();
const cancelButton = ref<HTMLButtonElement>();

defineExpose({ show });

function show() {
  error.value = undefined;
  open.value = true;
}

async function confirm() {
  if (processing.value || !open.value) {
    return;
  }
  error.value = undefined;
  try {
    processing.value = true;
    await props.action?.();
    emit('confirmed');
    open.value = false;
  } catch (e) {
    console.error('Confirmed action failed.', e);
    error.value = props.failedMessage ?? t('common.unexpectedError', [e instanceof Error ? e.message : String(e)]);
  } finally {
    processing.value = false;
  }
}
</script>

<template>
  <div class="rounded-md bg-yellow-50 p-4 text-left">
    <div class="flex">
      <div class="shrink-0">
        <ExclamationTriangleIcon class="h-5 w-5 text-yellow-400" aria-hidden="true" />
      </div>
      <div class="ml-3 text-sm text-yellow-700 space-y-2">
        <slot />
        <div v-if="requireConfirmation" class="relative flex pt-1">
          <div class="flex h-5 items-center">
            <input :id="checkboxId" v-model="confirmed" type="checkbox" class="h-4 w-4 rounded-sm border-gray-300 text-primary focus:ring-primary" />
          </div>
          <div class="ml-3 text-sm">
            <label :for="checkboxId" class="font-medium text-yellow-700">{{ t('untrustedRecipientsWarning.confirm') }}</label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { useId } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n({ useScope: 'global' });

defineProps<{
  requireConfirmation?: boolean
}>();

const confirmed = defineModel<boolean>('confirmed', { default: false });
const checkboxId = useId();
</script>

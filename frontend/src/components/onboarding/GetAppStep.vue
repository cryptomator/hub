<template>
  <VignetteFrame>
    <ol class="flex w-full max-w-68 flex-col gap-2">
      <li v-for="(step, index) in steps" :key="step.labelKey" class="flex items-center gap-2.5 rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-xs">
        <span class="w-3 shrink-0 text-center text-[11px] font-semibold text-gray-400" aria-hidden="true">{{ index + 1 }}.</span>
        <span class="flex size-7.5 shrink-0 items-center justify-center rounded-lg bg-primary-l2">
          <component :is="step.icon" class="h-4 w-4 text-primary" aria-hidden="true" />
        </span>
        <span class="text-[0.8125rem] text-gray-700">{{ t(step.labelKey, step.labelArgs ?? []) }}</span>
      </li>
    </ol>
  </VignetteFrame>
  <p>{{ t('onboarding.getApp.description') }}</p>
  <a href="https://cryptomator.org/downloads/" target="_blank" rel="noopener noreferrer" class="mt-4 block rounded-md bg-primary px-4 py-2 text-center font-medium text-white shadow-xs hover:bg-primary-d1 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary">{{ t('onboarding.getApp.download') }}</a>
</template>

<script setup lang="ts">
import { ArrowDownTrayIcon, FolderOpenIcon, KeyIcon } from '@heroicons/vue/24/outline';
import i18n from '../../i18n';
import VignetteFrame from './VignetteFrame.vue';

// mounted standalone without the i18n plugin, so read the shared composer directly
const { t } = i18n.global;

const steps = [
  { icon: ArrowDownTrayIcon, labelKey: 'onboarding.getApp.step1' },
  // the file name is passed as a parameter so translators cannot accidentally localize it
  { icon: FolderOpenIcon, labelKey: 'onboarding.getApp.step2', labelArgs: ['vault.cryptomator'] },
  { icon: KeyIcon, labelKey: 'onboarding.getApp.step3' }
];
</script>

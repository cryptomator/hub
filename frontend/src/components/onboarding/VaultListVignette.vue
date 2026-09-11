<template>
  <VignetteFrame aria-hidden="true">
    <div class="w-full max-w-68 divide-y divide-gray-100 rounded-[0.625rem] border border-gray-200 bg-white shadow-xs">
      <div v-for="vault in vaults" :key="vault.labelKey" class="flex items-center gap-2.5 px-3 py-2.5">
        <span class="flex size-7.5 shrink-0 items-center justify-center rounded-lg bg-primary-l2">
          <LockClosedIcon class="h-4 w-4 text-primary" aria-hidden="true" />
        </span>
        <span class="flex min-w-0 flex-1 flex-col gap-1.25">
          <span class="text-[11px] font-semibold text-primary">{{ t(vault.labelKey) }}</span>
          <span :class="['h-1.25 rounded-full bg-gray-200', vault.barClass]"></span>
        </span>
        <span class="flex shrink-0 -space-x-1.5">
          <VignetteAvatar v-for="index in vault.avatars" :key="index" :preset="teamAvatars[index]" />
        </span>
        <ChevronRightIcon class="h-3 w-3 shrink-0 text-gray-400" aria-hidden="true" />
      </div>
    </div>
  </VignetteFrame>
</template>

<script setup lang="ts">
import { ChevronRightIcon, LockClosedIcon } from '@heroicons/vue/24/outline';
import i18n from '../../i18n';
import { teamAvatars } from './avatars';
import VignetteAvatar from './VignetteAvatar.vue';
import VignetteFrame from './VignetteFrame.vue';

// mounted standalone without the i18n plugin, so read the shared composer directly
const { t } = i18n.global;

const vaults = [
  { labelKey: 'onboarding.vaultList.example1', barClass: 'w-20', avatars: [0, 1] },
  { labelKey: 'onboarding.vaultList.example2', barClass: 'w-14', avatars: [2] }
];
</script>

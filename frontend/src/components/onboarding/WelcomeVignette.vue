<template>
  <VignetteFrame aria-hidden="true">
    <div class="flex w-full max-w-60 items-center py-1.5">
      <div class="flex flex-col items-center gap-2">
        <span class="flex -space-x-1.5">
          <VignetteAvatar v-for="(preset, index) in teamAvatars" :key="index" :preset="preset" :size="28" />
        </span>
        <span class="text-[8px] font-semibold tracking-widest text-gray-400 uppercase">{{ t('onboarding.welcome.team') }}</span>
      </div>
      <div class="relative mx-2 mb-4.5 flex h-6 flex-1 items-center justify-center">
        <span class="absolute inset-0 top-1/2 border-t-[1.5px] border-dashed border-primary opacity-45"></span>
        <span class="animate-onboarding-key-travel relative flex bg-gray-50 px-1 will-change-[transform,opacity] motion-reduce:animate-none">
          <KeyIcon class="h-4 w-4 text-primary" aria-hidden="true" />
        </span>
      </div>
      <div class="flex flex-col items-center gap-2">
        <img src="/logo.svg" width="48" height="47" alt="" />
        <!-- product name, deliberately not translated -->
        <span class="text-[8px] font-semibold tracking-widest text-gray-400 uppercase">Hub</span>
      </div>
    </div>
  </VignetteFrame>
</template>

<script setup lang="ts">
import { KeyIcon } from '@heroicons/vue/24/outline';
import i18n from '../../i18n';
import { teamAvatars } from './avatars';
import VignetteAvatar from './VignetteAvatar.vue';
import VignetteFrame from './VignetteFrame.vue';

// mounted standalone without the i18n plugin, so read the shared composer directly
const { t } = i18n.global;
</script>

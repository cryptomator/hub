<template>
  <VignetteFrame aria-hidden="true">
    <div class="flex w-full max-w-68 flex-col gap-2">
      <div class="flex items-center gap-2.5 rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-xs">
        <KeyIcon class="h-4 w-4 shrink-0 text-primary" aria-hidden="true" />
        <span class="flex-1 font-mono text-[11px] tracking-wider text-gray-500">a1b2-••••-••••-e5f6</span>
        <DocumentDuplicateIcon class="h-4 w-4 shrink-0 text-gray-400" aria-hidden="true" />
      </div>
      <div class="flex gap-2">
        <div v-for="device in devices" :key="device.id" class="flex flex-1 items-center gap-2.5 rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-xs">
          <component :is="device.icon" class="h-4 w-4 shrink-0 text-gray-400" aria-hidden="true" />
          <span class="flex flex-1"><span :class="['h-1.25 rounded-full bg-gray-300', device.barClass]"></span></span>
          <span class="size-1.75 shrink-0 rounded-full bg-primary"></span>
        </div>
      </div>
    </div>
  </VignetteFrame>
</template>

<script setup lang="ts">
import { ComputerDesktopIcon, DevicePhoneMobileIcon, DocumentDuplicateIcon, KeyIcon } from '@heroicons/vue/24/outline';
import VignetteFrame from './VignetteFrame.vue';

const devices = [
  { id: 'desktop', icon: ComputerDesktopIcon, barClass: 'w-12' },
  { id: 'mobile', icon: DevicePhoneMobileIcon, barClass: 'w-10' }
];
</script>

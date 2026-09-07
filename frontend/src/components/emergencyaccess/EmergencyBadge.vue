<template>
  <div class="group mr-3 inline-block" @click.stop.prevent>
    <button
      type="button"
      class="inline-flex items-center gap-2 rounded-full p-2 text-xs font-medium cursor-default ring-1 outline-none focus-visible:ring-2"
      :class="isError ? 'bg-red-100 ring-red-300/70 text-red-800' : 'bg-yellow-50 ring-yellow-300/70 text-yellow-800'"
      :style="{ anchorName: anchor }"
      :aria-label="title"
    >
      <ExclamationTriangleIcon class="size-4" :class="isError ? 'text-red-600' : 'text-yellow-500'" />
    </button>

    <div
      class="tooltip-panel fixed mb-2 z-20 px-2 py-1 rounded shadow-sm border text-xs hyphens-auto invisible opacity-0 transition-[opacity,visibility] duration-150 group-hover:visible group-hover:opacity-100 group-has-focus-visible:visible group-has-focus-visible:opacity-100 pointer-coarse:group-focus-within:visible pointer-coarse:group-focus-within:opacity-100"
      :class="isError ? 'bg-red-50 border-red-300 text-red-900' : 'bg-yellow-50 border-yellow-300 text-yellow-900'"
      :style="{ positionAnchor: anchor }"
      role="tooltip"
    >
      <p class="font-bold">{{ title }}</p>
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/24/solid';

const props = defineProps<{
  type: 'warning' | 'error';
  title: string;
  message: string;
}>();

const anchor = `--ea-anchor-${useId()}`;
const isError = computed(() => props.type === 'error');
</script>

<style scoped>
.tooltip-panel {
  bottom: anchor(top);
  left: max(1rem, calc(anchor(center) - 10rem));
  right: max(1rem, calc(anchor(center) - 10rem));
  position-try-fallbacks: flip-block;
}
</style>

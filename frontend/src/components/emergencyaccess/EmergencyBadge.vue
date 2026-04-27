<template>
  <div class="badge-wrapper mr-3 inline-block" @click.stop.prevent>
    <span
      tabindex="0"
      class="inline-flex items-center gap-2 rounded-full px-2 py-2 text-xs font-medium cursor-default ring-1 outline-none focus-visible:ring-2"
      :class="isError ? 'bg-red-100 ring-red-300/70 text-red-800' : 'bg-yellow-50 ring-yellow-300/70 text-yellow-800'"
      :style="{ anchorName: anchor }"
      :aria-label="title"   
    >
      <ExclamationTriangleIcon class="h-4 w-4" :class="isError ? 'text-red-600' : 'text-yellow-500'" />
    </span>

    <div
      class="tooltip-panel px-2 py-1 rounded shadow-sm border text-xs hyphens-auto"
      :class="isError ? 'bg-red-50 border-red-300 text-red-900' : 'bg-yellow-50 border-yellow-300 text-yellow-900'"
      :style="{ positionAnchor: anchor }"
      role="tooltip"
    >
      <b>{{ title }}</b><br />
      <span>{{ message }}</span>
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
  position: fixed;
  bottom: anchor(top);
  left: calc(anchor(center) - min(calc(anchor(center) - 1rem), calc(100vw - anchor(center) - 1rem), 10rem));
  right: calc(anchor(center) - min(calc(anchor(center) - 1rem), calc(100vw - anchor(center) - 1rem), 10rem));
  margin-inline: auto;
  margin-block-end: 0.5rem;
  width: fit-content;
  min-width: 10rem;
  z-index: 20;
  position-try-fallbacks: flip-block;

  visibility: hidden;
  opacity: 0;
  transition: opacity 150ms, visibility 150ms;
}

.badge-wrapper:hover .tooltip-panel,
.badge-wrapper:focus-within .tooltip-panel {
  visibility: visible;
  opacity: 1;
}
</style>

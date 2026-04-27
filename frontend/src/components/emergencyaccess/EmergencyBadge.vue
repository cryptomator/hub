<template>
  <div v-if="type !== 'none'" class="relative mr-3 inline-block">
    <button
      type="button"
      class="inline-flex items-center gap-2 rounded-full px-2 py-2 text-xs font-medium cursor-default ring-1 outline-none focus-visible:ring-2"
      :class="badgeClasses"
      :style="{ anchorName: anchor }"
      :aria-label="title"
      @mouseenter="showIfHover"
      @mouseleave="hideIfHover"
      @focus="showIfHover"
      @blur="hideIfHover"
      @click.stop="toggleOnTouch"
    >
      <ExclamationTriangleIcon class="h-4 w-4" :class="iconColor" />
    </button>

    <div
      ref="panel"
      popover="auto"
      class="popover-panel px-2 py-1 rounded shadow-sm border text-xs hyphens-auto relative"
      :class="[tooltipClasses, positionAreaClass]"
      :style="{ positionAnchor: anchor }"
      @click.stop.prevent
    >
      <b>{{ title }}</b><br />
      <span>{{ message }}</span>
      <div
        class="absolute bottom-0 translate-y-1/2 rotate-45 w-2 h-2 border-r border-b"
        :class="[arrowClasses, arrowPositionClasses]"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, useId } from 'vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/24/solid';

const props = defineProps<{
  type: 'notCouncil' | 'broken' | 'noRedundancy' | 'insufficientCouncilMembers' | 'none';
  title: string;
  message: string;
  position?: 'center' | 'left' | 'right';
}>();

const anchor = `--ea-anchor-${useId()}`;
const panel = ref<HTMLElement | null>(null);

function isHoverDevice() {
  return window.matchMedia('(hover: hover) and (pointer: fine)').matches;
}

function showIfHover() {
  if (!isHoverDevice()) return;
  const p = panel.value;
  if (p && !p.matches(':popover-open')) p.showPopover();
}

function hideIfHover() {
  if (!isHoverDevice()) return;
  const p = panel.value;
  if (p?.matches(':popover-open')) p.hidePopover();
}

function toggleOnTouch() {
  if (isHoverDevice()) return;
  const p = panel.value;
  if (!p) return;
  if (p.matches(':popover-open')) p.hidePopover();
  else p.showPopover();
}

const positionAreaClass = computed(() => {
  switch (props.position) {
    case 'left':
      return 'pos-left';
    case 'right':
      return 'pos-right';
    case 'center':
    default:
      return 'pos-center';
  }
});

const badgeClasses = computed(() => {
  switch (props.type) {
    case 'notCouncil':
    case 'insufficientCouncilMembers':
    case 'noRedundancy':
      return 'bg-yellow-50 ring-yellow-300/70 text-yellow-800';
    case 'broken':
      return 'bg-red-100 ring-red-300/70 text-red-800';
    default:
      return '';
  }
});

const tooltipClasses = computed(() => {
  switch (props.type) {
    case 'notCouncil':
    case 'insufficientCouncilMembers':
    case 'noRedundancy':
      return 'bg-yellow-50 border-yellow-300 text-yellow-900';
    case 'broken':
      return 'bg-red-50 border-red-300 text-red-900';
    default:
      return '';
  }
});

const arrowClasses = computed(() => {
  switch (props.type) {
    case 'notCouncil':
    case 'insufficientCouncilMembers':
    case 'noRedundancy':
      return 'bg-yellow-50 border-yellow-300';
    case 'broken':
      return 'bg-red-50 border-red-300';
    default:
      return '';
  }
});

const arrowPositionClasses = computed(() => {
  switch (props.position) {
    case 'left':
      return 'left-3';
    case 'right':
      return 'right-3';
    case 'center':
    default:
      return 'left-1/2 -translate-x-1/2';
  }
});

const iconColor = computed(() => {
  return props.type === 'broken' ? 'text-red-600' : 'text-yellow-500';
});
</script>

<style scoped>
.popover-panel {
  inset: auto;
  bottom: anchor(top);
  margin: 0 0 0.5rem 0;
  width: fit-content;
  max-width: min(20rem, calc(100vw - 2rem));
  overflow: visible;
}

.popover-panel.pos-center {
  left: calc(anchor(center) - min(calc(anchor(center) - 1rem), calc(100vw - anchor(center) - 1rem), 10rem));
  right: calc(anchor(center) - min(calc(anchor(center) - 1rem), calc(100vw - anchor(center) - 1rem), 10rem));
  margin-inline: auto;
}

.popover-panel.pos-left {
  left: max(1.5rem, anchor(left));
  right: 1rem;
  margin-right: auto;
}

.popover-panel.pos-right {
  left: 1rem;
  right: max(1rem, anchor(right));
  margin-left: auto;
}
</style>

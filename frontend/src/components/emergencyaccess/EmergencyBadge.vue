<template>
  <div v-if="type !== 'none'" ref="badgeRef" class="relative mr-3 group/badge">
    <!-- Badge -->
    <span
      class="inline-flex items-center gap-2 rounded-full px-2 py-2 text-xs font-medium cursor-default ring-1"
      :class="badgeClasses"
      @click.stop="toggle"
    >
      <ExclamationTriangleIcon class="h-4 w-4" :class="iconColor" />
    </span>

    <!-- Tooltip -->
    <div
      class="transition-opacity duration-150 absolute -top-2 transform -translate-y-full w-max max-w-xs z-20"
      :class="[positionClasses, isOpen ? 'visible opacity-100' : 'invisible opacity-0 group-hover/badge:visible group-hover/badge:opacity-100']"
      :style="mobileTooltipStyle"
    >
      <div class="px-2 py-1 rounded shadow-sm text-xs hyphens-auto border relative" :class="tooltipClasses">
        <b>{{ title }}</b><br />
        <span>{{ message }}</span>

        <!-- Arrow -->
        <div
          class="absolute bottom-0 transform translate-y-1/2 rotate-45 w-2 h-2 border-r border-b"
          :class="[arrowClasses, mobileArrowStyle ? '' : arrowPositionClasses]"
          :style="mobileArrowStyle"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/24/solid';

const badgeRef = ref<HTMLElement | null>(null);
const isOpen = ref(false);
const mobileTooltipStyle = ref<Record<string, string>>({});
const mobileArrowStyle = ref<Record<string, string>>({});
const isTouchDevice = ref(false);

onMounted(() => {
  isTouchDevice.value = window.matchMedia('(hover: none)').matches;
});

function toggle() {
  if (!isTouchDevice.value) return;
  isOpen.value = !isOpen.value;
  if (isOpen.value && badgeRef.value) {
    const rect = badgeRef.value.getBoundingClientRect();
    const margin = 8;
    const maxWidth = Math.min(320, window.innerWidth - 2 * margin);
    const overflowRight = rect.left + maxWidth - (window.innerWidth - margin);
    const tooltipShift = overflowRight > 0 ? overflowRight : 0;
    mobileTooltipStyle.value = { left: `-${tooltipShift}px`, maxWidth: `${maxWidth}px` };
    // arrow: badge center relative to tooltip left edge, minus half arrow width (4px)
    const badgeCenterRelative = rect.width / 2 + tooltipShift - 4;
    mobileArrowStyle.value = { left: `${badgeCenterRelative}px`, right: 'auto' };
  } else {
    mobileTooltipStyle.value = {};
    mobileArrowStyle.value = {};
  }
}

function onDocumentClick(e: MouseEvent) {
  if (badgeRef.value && !badgeRef.value.contains(e.target as Node)) {
    isOpen.value = false;
  }
}

onMounted(() => document.addEventListener('click', onDocumentClick));
onUnmounted(() => document.removeEventListener('click', onDocumentClick));

const props = defineProps<{
  type: 'notCouncil' | 'broken' | 'noRedundancy' | 'insufficientCouncilMembers' | 'none';
  title: string;
  message: string;
  position?: 'center' | 'left' | 'right';
}>();

const positionClasses = computed(() => {
  switch (props.position) {
    case 'left':
      return 'left-0';
    case 'right':
      return 'right-0';
    case 'center':
    default:
      return 'left-1/2 -translate-x-1/2';
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
      return 'left-2';
    case 'right':
      return 'right-2';
    case 'center':
    default:
      return 'left-1/2 -translate-x-1/2';
  }
});

const iconColor = computed(() => {
  return props.type === 'broken' ? 'text-red-600' : 'text-yellow-500';
});
</script>

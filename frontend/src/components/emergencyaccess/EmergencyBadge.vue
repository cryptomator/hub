<template>
  <div v-if="type !== 'none'" class="relative mr-3 group">
    <!-- Badge -->
    <span class="inline-flex items-center gap-2 rounded-full px-2 py-2 text-xs font-medium cursor-default ring-1" :class="badgeClasses">
      <ExclamationTriangleIcon class="h-4 w-4" :class="iconColor" />
    </span>

    <!-- Tooltip -->
    <div class="invisible opacity-0 group-hover:visible group-hover:opacity-100 transition-opacity duration-150 absolute left-1/2 -translate-x-1/2 -top-2 transform -translate-y-full w-max max-w-xs z-10">
      <div class="px-2 py-1 rounded shadow-sm text-xs hyphens-auto border relative" :class="tooltipClasses">
        <b>{{ title }}</b><br />
        <span>{{ message }}</span>

        <!-- Arrow -->
        <div class="absolute bottom-0 left-1/2 transform translate-y-1/2 -translate-x-1/2 rotate-45 w-2 h-2 border-r border-b" :class="arrowClasses"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ExclamationTriangleIcon } from '@heroicons/vue/24/solid';

const props = defineProps<{
  type: 'notCouncil' | 'broken' | 'noRedundancy' | 'none';
  title: string;
  message: string;
}>();

const badgeClasses = computed(() => {
  switch (props.type) {
    case 'notCouncil':
      return 'bg-yellow-50 ring-yellow-300/70 text-yellow-800';
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
    case 'noRedundancy':
      return 'bg-yellow-50 border-yellow-300';
    case 'broken':
      return 'bg-red-50 border-red-300';
    default:
      return '';
  }
});

const iconColor = computed(() => {
  return props.type === 'broken' ? 'text-red-600' : 'text-yellow-500';
});
</script>

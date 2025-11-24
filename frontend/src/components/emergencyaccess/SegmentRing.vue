<template>
  <svg
    :width="size"
    :height="size"
    :viewBox="`0 0 ${viewBox} ${viewBox}`"
    :class="['ml-auto shrink-0', attrs.class]"
    aria-hidden="true"
  >
    <g>
      <path
        v-for="i in total"
        :key="i"
        :d="describeSegment(i - 1 + startIndex, total, radius)"
        :fill="i <= completed ? fillColor : emptyColor"
        :stroke="stroke"
        :stroke-width="strokeWidth"
      />
    </g>
  </svg>
</template>

<script setup lang="ts">
import { useAttrs, withDefaults, defineProps } from 'vue';
import { describeSegment } from '../../common/svgUtils';

const attrs = useAttrs();

type Props = {
  total: number;
  completed: number;
  startIndex?: number;
  size?: number;
  viewBox?: number;
  radius?: number;
  fillColor?: string;
  emptyColor?: string;
  stroke?: string;
  strokeWidth?: number;
};

withDefaults(defineProps<Props>(), {
  startIndex: 0,
  size: 20,
  viewBox: 36,
  radius: 16,
  fillColor: '#22c55e',
  emptyColor: '#e5e7eb',
  stroke: 'white',
  strokeWidth: 1,
});
</script>

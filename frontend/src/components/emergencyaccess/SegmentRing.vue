<template>
  <svg
    :width="size"
    :height="size"
    :viewBox="`0 0 36 36`"
    :class="['ml-auto shrink-0', attrs.class]"
    aria-hidden="true"
  >
    <g>
      <path
        v-for="i in total"
        :key="i"
        :d="describeSegment(i - 1 + startIndex, total, 16)"
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
  fillColor?: string;
  emptyColor?: string;
  stroke?: string;
  strokeWidth?: number;
};

withDefaults(defineProps<Props>(), {
  startIndex: 0,
  size: 20,
  fillColor: '#22c55e',
  emptyColor: '#e5e7eb',
  stroke: 'white',
  strokeWidth: 1,
});
</script>

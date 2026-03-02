<template>
  <div v-if="authorities.length" class="relative inline-flex -space-x-2">
    <template v-for="a in preview.list" :key="a.id">
      <div v-if="a.type === 'USER'" class="relative h-8 w-8 rounded-full ring-1 ring-gray-200 bg-white overflow-hidden flex items-center justify-center">
        <img v-if="a.pictureUrl" :src="a.pictureUrl" :alt="a.name" :title="a.name" class="h-full w-full object-cover" />
        <div v-else class="h-full w-full flex items-center justify-center text-[9px] font-semibold text-gray-700">
          {{ initials(a.name) }}
        </div>
      </div>
      <div v-else-if="a.type === 'GROUP'" :title="a.name" class="relative h-8 w-8 rounded-full ring-1 ring-gray-200 bg-gray-300 flex items-center justify-center text-[10px] font-semibold text-gray-700">
        {{ initials(a.name) }} <span v-if="a.memberSize">({{ a.memberSize }})</span>
      </div>
    </template>

    <div v-if="preview.extra > 0" class="relative z-10 h-8 w-8 ml-0.5 rounded-full ring-1 ring-gray-200 bg-gray-200 flex items-center justify-center text-[10px] font-semibold text-gray-700">
      +{{ preview.extra }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { AuthorityDto } from '../common/backend';

const props = withDefaults(defineProps<{
  authorities: AuthorityDto[];
  max?: number;
}>(), {
  max: 3,
});

const preview = computed(() => {
  const extra = Math.max(0, props.authorities.length - props.max);
  return {
    list: props.authorities.slice(0, props.max),
    extra,
  };
});

function initials(name: string): string {
  return (name ?? '')
    .split(' ')
    .map(p => p.trim()[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase();
}
</script>

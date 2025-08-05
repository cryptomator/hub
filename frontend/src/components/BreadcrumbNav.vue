<template>
  <nav class="flex items-center text-sm text-gray-500 pb-4" aria-label="Breadcrumb">
    <ol class="inline-flex items-center">
      <li v-for="(crumb, index) in crumbs" :key="index" class="flex items-center">
        <template v-if="crumb.to">
          <RouterLink :to="crumb.to" class="inline-flex items-center hover:text-gray-700">
            {{ crumb.label }}
            <ChevronRightIcon v-if="index < crumbs.length - 1" class="w-4 h-4 mx-1 text-gray-400"/>
          </RouterLink>
        </template>
        <template v-else>
          <span class="text-gray-700 font-medium">{{ crumb.label }}</span>
        </template>
        <ChevronRightIcon v-if="index < crumbs.length - 1 && !crumb.to" class="w-4 h-4 mx-1 text-gray-400"/>
      </li>
    </ol>
  </nav>
</template>

<script setup>
import { ChevronRightIcon } from '@heroicons/vue/20/solid';

defineProps({
  crumbs: {
    type: Array,
    required: false,
    default: () => [],
  },
});
</script>

<template>
  <nav class="flex items-center text-sm text-gray-500 pb-4" aria-label="Breadcrumb">
    <ol class="inline-flex items-center">
      <li v-for="(crumb, index) in crumbs" :key="index" class="flex items-center">
        <template v-if="crumb.to">
          <RouterLink :to="crumb.to" class="inline-flex items-center hover:text-gray-700">
            {{ crumb.label }}
            <svg v-if="index < crumbs.length - 1" class="w-4 h-4 mx-1 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L11.586 9 7.293 4.707a1 1 0 011.414-1.414l5 5a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            </svg>
          </RouterLink>
        </template>
        <template v-else>
          <span class="text-gray-700 font-medium">{{ crumb.label }}</span>
        </template>
        <svg v-if="index < crumbs.length - 1 && !crumb.to" class="w-4 h-4 mx-1 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L11.586 9 7.293 4.707a1 1 0 011.414-1.414l5 5a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0z" clip-rule="evenodd" />
        </svg>
      </li>
    </ol>
  </nav>
</template>

<script setup>
defineProps({
  crumbs: {
    type: Array,
    required: false,
    default: () => [],
  },
});
</script>

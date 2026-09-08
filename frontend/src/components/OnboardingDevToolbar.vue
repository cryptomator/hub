<template>
  <div class="mb-4 flex flex-wrap items-center gap-2 rounded-md border border-dashed border-gray-300 bg-gray-50 px-3 py-2 text-xs text-gray-500">
    Tour preview:
    <button v-for="variant in previewVariants" :key="variant" type="button" class="rounded-sm border border-gray-300 bg-white px-2 py-1 font-medium text-gray-700 shadow-xs hover:bg-gray-100" @click="previewOnboarding(variant)">
      {{ variant }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { previewOnboarding, previewVariants } from '../common/onboarding';
</script>

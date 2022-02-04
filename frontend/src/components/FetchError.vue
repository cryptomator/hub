<template>
  <div class="text-center">
    <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('fetchError.title') }}</h3>
    <p class="mt-1 text-sm text-gray-500">
      {{ error.message }}
    </p>
    <div v-if="retry != undefined " class="mt-6">
      <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="retry">
        <RefreshIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
        {{ t('common.retry') }}
      </button>
    </div>
  </div>
</template>


<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { RefreshIcon } from '@heroicons/vue/solid';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  error: Error,
  retry: (() => Promise<void>) | null;
}>();


</script>
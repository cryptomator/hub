<template>
  <p>
    {{ t("fetchError.description",[error.message]) }}
  </p>
  <button v-if="allowRetry" type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="() => emit('retry')">
    <RefreshIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
    {{ t('common.retry') }}
  </button>
</template>


<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { RefreshIcon } from '@heroicons/vue/solid';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  error: Error,
  allowRetry: boolean
}>();

const emit = defineEmits<{
  (e: 'retry'): void
}>();

</script>
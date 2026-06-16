<template>
  <span>{{ message }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n({ useScope: 'global' });

const ERROR_CODE = /^[A-Z][A-Z0-9_]+$/; // error codes provided by the backend, e.g. CREATE_USER_FAILED (see ErrorCodeException.java)

const props = defineProps<{
  error: Error
}>();

const message = computed(() => {
  if (ERROR_CODE.test(props.error.message)) {
    const key = `error.${props.error.message}`;
    const localized = t(key);
    if (localized !== key) {
      return localized;
    }
  }
  return t('common.unexpectedError', [props.error.message]);
});
</script>

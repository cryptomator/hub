<template>
  <ConfirmDialog ref="confirmDialog" size="narrow" :title="t('processAbortDialog.title')" :description="t('processAbortDialog.description')" :confirm-label="t('processAbortDialog.confirm')" :cancel-label="t('common.back')" :action="abortProcess" @confirmed="emit('confirmed')" @close="emit('close')" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend from '../../common/backend';
import ConfirmDialog from '../ConfirmDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const confirmDialog = ref<InstanceType<typeof ConfirmDialog>>();

const props = defineProps<{
  recoveryProcessId: string
}>();

const emit = defineEmits<{
  close: []
  confirmed: []
}>();

defineExpose({ show });

function show() {
  confirmDialog.value?.show();
}

async function abortProcess() {
  await backend.emergencyAccess.abort(props.recoveryProcessId);
}
</script>

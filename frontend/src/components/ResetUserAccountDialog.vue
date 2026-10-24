<template>
  <ConfirmDialog ref="confirmDialog" :title="t('resetUserAccountDialog.title')" :description="t('resetUserAccountDialog.description')" :confirm-label="t('resetUserAccountDialog.submit')" :action="resetUserAccount" @confirmed="router.go(0)" @close="$emit('close')" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../common/backend';
import { BrowserKeys } from '../common/crypto';
import router from '../router';
import ConfirmDialog from './ConfirmDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const confirmDialog = ref<InstanceType<typeof ConfirmDialog>>();

defineEmits<{
  close: []
}>();

defineExpose({
  show
});

const props = defineProps<{
  me : UserDto
}>();

function show() {
  confirmDialog.value?.show();
}

async function resetUserAccount() {
  await backend.users.resetMe();
  await BrowserKeys.delete(props.me.id);
}
</script>

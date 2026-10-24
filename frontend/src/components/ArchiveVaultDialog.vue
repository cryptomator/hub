<template>
  <ConfirmDialog ref="confirmDialog" tone="neutral" :icon="ArchiveBoxIcon" :title="t('archiveVaultDialog.title', [vault.name])" :description="t('archiveVaultDialog.description')" :confirm-label="t('archiveVaultDialog.confirm')" :action="archiveVault" @close="emit('close')" />
</template>

<script setup lang="ts">
import { ArchiveBoxIcon } from '@heroicons/vue/24/outline';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { VaultDto } from '../common/backend';
import ConfirmDialog from './ConfirmDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const confirmDialog = ref<InstanceType<typeof ConfirmDialog>>();

const props = defineProps<{
  vault: VaultDto
}>();

const emit = defineEmits<{
  close: []
  archived: [updatedVault: VaultDto]
}>();

defineExpose({
  show
});

function show() {
  confirmDialog.value?.show();
}

async function archiveVault() {
  emit('archived', await backend.vaults.setArchived(props.vault.id, true));
}
</script>

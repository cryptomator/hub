<template>
  <ConfirmDialog ref="confirmDialog" :title="t('deleteGroupDialog.title', [group.name])" :description="t('deleteGroupDialog.description')" :confirm-label="t('deleteGroupDialog.confirm')" :failed-message="t('deleteGroupDialog.error.failed')" :action="deleteGroup" @confirmed="emit('delete', group.id)" @close="emit('close')">
    <div class="hidden sm:flex items-center gap-2">
      <img :src="group.pictureUrl" class="w-8 h-8 rounded-full border" />
      <span class="font-medium text-sm">{{ group.name }}</span>
    </div>
  </ConfirmDialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { GroupDto } from '../../common/backend';
import ConfirmDialog from '../ConfirmDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const confirmDialog = ref<InstanceType<typeof ConfirmDialog>>();

const props = defineProps<{
  group: GroupDto;
}>();

const emit = defineEmits<{
  close: [];
  delete: [deletedGroupId: string];
}>();

defineExpose({
  show
});

function show() {
  confirmDialog.value?.show();
}

async function deleteGroup() {
  await backend.groups.removeGroup(props.group.id);
}
</script>

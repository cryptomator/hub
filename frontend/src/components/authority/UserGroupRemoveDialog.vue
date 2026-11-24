<template>
  <ConfirmDialog ref="confirmDialog" :title="t('group.member.remove.title', [user.name, group.name])" :description="t('group.member.remove.description', [user.name, group.name])" :confirm-label="t('group.member.remove.confirm')" :failed-message="t('group.member.remove.error.failed')" :action="removeUserFromGroup" @confirmed="emit('removed')" @close="emit('close')">
    <div class="hidden sm:flex items-center gap-2">
      <img :src="group.pictureUrl" class="w-8 h-8 rounded-full border" />
      <span class="font-medium text-sm">{{ group.name }}</span>
    </div>
  </ConfirmDialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { GroupDto, UserDto } from '../../common/backend';
import ConfirmDialog from '../ConfirmDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const confirmDialog = ref<InstanceType<typeof ConfirmDialog>>();

const props = defineProps<{
  group: GroupDto;
  user: UserDto;
}>();

const emit = defineEmits<{
  close: [];
  removed: [];
}>();

defineExpose({
  show
});

function show() {
  confirmDialog.value?.show();
}

async function removeUserFromGroup() {
  await backend.groups.removeMember(props.group.id, props.user.id);
}
</script>

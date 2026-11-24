<template>
  <ConfirmDialog ref="confirmDialog" :title="t('group.member.remove.title', [member.name, group.name])" :description="t(member.type === 'GROUP' ? 'group.member.remove.descriptionGroup' : 'group.member.remove.description', [member.name, group.name])" :confirm-label="t('group.member.remove.confirm')" :failed-message="t('group.member.remove.error.failed')" :action="removeMember" @confirmed="emit('delete', member.id)" @close="emit('close')">
    <div class="hidden sm:flex items-center gap-2">
      <img :src="member.pictureUrl" class="w-8 h-8 rounded-full border" />
      <div class="flex flex-col">
        <span class="font-medium text-sm">{{ member.name }}</span>
        <span v-if="fullName" class="text-xs text-gray-500">{{ fullName }}</span>
      </div>
    </div>
  </ConfirmDialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AuthorityDto, GroupDto } from '../../common/backend';
import ConfirmDialog from '../ConfirmDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const confirmDialog = ref<InstanceType<typeof ConfirmDialog>>();

const props = defineProps<{
  member: AuthorityDto;
  group: GroupDto;
}>();

const fullName = computed(() => {
  if (props.member.type === 'USER') {
    const first = props.member.firstName ?? '';
    const last = props.member.lastName ?? '';
    return `${first} ${last}`.trim();
  } else {
    return undefined;
  }
});

const emit = defineEmits<{
  close: [];
  delete: [deletedMemberId: string];
}>();

defineExpose({
  show
});

function show() {
  confirmDialog.value?.show();
}

async function removeMember() {
  await backend.groups.removeMember(props.group.id, props.member.id);
}
</script>

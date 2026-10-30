<template>
  <ConfirmDialog ref="confirmDialog" :title="t('disableUserDialog.title', [user.name])" :description="t('disableUserDialog.description', [user.name])" :confirm-label="t('disableUserDialog.confirm')" :failed-message="t('disableUserDialog.error.failed')" :action="disableUser" @confirmed="emit('disable', user)" @close="emit('close')">
    <div class="hidden sm:flex items-center gap-2">
      <img :src="user.pictureUrl" class="w-8 h-8 rounded-full border" />
      <div class="flex flex-col">
        <span class="font-medium text-sm">{{ user.name }}</span>
        <span v-if="fullName" class="text-xs text-gray-500">{{ fullName }}</span>
      </div>
    </div>
  </ConfirmDialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { UserDto } from '../../common/backend';
import ConfirmDialog from '../ConfirmDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const confirmDialog = ref<InstanceType<typeof ConfirmDialog>>();

const props = defineProps<{
  user: UserDto;
}>();

const fullName = computed(() => {
  const first = props.user.firstName ?? '';
  const last = props.user.lastName ?? '';
  return `${first} ${last}`.trim();
});

const emit = defineEmits<{
  close: [];
  disable: [disabledUser: UserDto];
}>();

defineExpose({
  show
});

function show() {
  confirmDialog.value?.show();
}

async function disableUser() {
  await backend.users.setUserEnabled(props.user.id, false);
}
</script>

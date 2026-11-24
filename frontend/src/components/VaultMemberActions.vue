<template>
  <div class="flex w-24 shrink-0 items-center justify-end gap-2">
    <div v-if="member.vaultRole == 'OWNER'" class="inline-flex items-center rounded-md bg-gray-50 px-2 py-1 text-xs font-medium text-gray-600 ring-1 ring-inset ring-gray-500/10">{{ t('vaultDetails.sharedWith.badge.owner') }}</div>
    <Menu v-if="editable" as="div" class="relative -mr-2 inline-block shrink-0 text-left">
      <MenuButton class="group relative inline-flex h-8 w-8 items-center justify-center rounded-full focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2">
        <span class="absolute -inset-1.5" />
        <span class="sr-only">Open options menu</span>
        <span class="flex h-full w-full items-center justify-center rounded-full">
          <EllipsisVerticalIcon class="h-5 w-5 text-gray-400 group-hover:text-gray-500" aria-hidden="true" />
        </span>
      </MenuButton>
      <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
        <MenuItems class="absolute right-9 top-0 z-10 w-48 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black/5 focus:outline-hidden">
          <div class="py-1">
            <MenuItem v-if="member.vaultRole == 'MEMBER'" v-slot="{ active }" @click="emit('updateRole', 'OWNER')">
              <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'cursor-pointer block px-4 py-2 text-sm']">
                {{ t('vaultDetails.sharedWith.grantOwnership') }}
              </div>
            </MenuItem>
            <MenuItem v-if="member.vaultRole == 'OWNER'" v-slot="{ active }" @click="emit('updateRole', 'MEMBER')">
              <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'cursor-pointer block px-4 py-2 text-sm']">
                {{ t('vaultDetails.sharedWith.revokeOwnership') }}
              </div>
            </MenuItem>
            <MenuItem v-slot="{ active }" @click="emit('remove')">
              <div :class="[active ? 'bg-gray-100 text-red-900' : 'text-red-700', 'cursor-pointer block px-4 py-2 text-sm']">
                {{ t('common.remove') }}<span class="sr-only"> {{ member.name }}</span>
              </div>
            </MenuItem>
          </div>
        </MenuItems>
      </transition>
    </Menu>
  </div>
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { EllipsisVerticalIcon } from '@heroicons/vue/20/solid';
import { useI18n } from 'vue-i18n';
import { MemberDto, VaultRole } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

defineProps<{
  member: MemberDto
  editable: boolean
}>();

const emit = defineEmits<{
  updateRole: [role: VaultRole]
  remove: []
}>();
</script>

<template>
  <div class="flex grow flex-col gap-y-4 p-4">
    <div class="flex h-16 shrink-0 items-center gap-x-4 px-2">
      <button v-if="showClose" type="button" class="-m-2.5 p-2.5 text-gray-300 hover:text-white" @click="emit('close')">
        <span class="sr-only">{{ t('nav.closeMenu') }}</span>
        <XMarkIcon class="h-6 w-6" aria-hidden="true" />
      </button>
      <router-link to="/app" class="flex h-12 grow items-center justify-center" @click="emit('navigate')">
        <img src="/logo.svg" class="h-12" alt="Cryptomator Hub" />
      </router-link>
    </div>

    <nav class="flex flex-1 flex-col gap-y-7 overflow-y-auto">
      <ul role="list" class="space-y-1">
        <li v-for="item in mainNav" :key="item.name">
          <router-link :to="item.to" :class="itemClasses(item.to)" @click="emit('navigate')">
            <component :is="item.icon" class="h-6 w-6 shrink-0" aria-hidden="true" />
            <span class="truncate">{{ t(item.name) }}</span>
          </router-link>
        </li>
      </ul>

      <div v-if="isAdmin">
        <hr class="border-white/10" />
        <ul role="list" class="mt-3 space-y-1">
          <li v-for="item in adminNav" :key="item.name">
            <router-link :to="item.to" :class="itemClasses(item.to)" @click="emit('navigate')">
              <component :is="item.icon" class="h-6 w-6 shrink-0" aria-hidden="true" />
              <span class="truncate">{{ t(item.name) }}</span>
            </router-link>
          </li>
        </ul>
      </div>
    </nav>

    <!-- Kept outside the scrolling nav so the dropdown isn't clipped by the nav's overflow -->
    <Menu as="div" class="relative">
      <MenuButton class="flex w-full items-center gap-x-3 rounded-md p-2 text-sm font-medium text-gray-300 hover:bg-white/5 hover:text-white focus-visible:bg-white/5 focus-visible:text-white focus:outline-hidden">
        <img class="h-8 w-8 shrink-0 rounded-full bg-white" :src="me.pictureUrl" alt="" />
        <span class="truncate">{{ me.name }}</span>
      </MenuButton>
      <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class=" opacity-100 scale-100" leave-to-class=" opacity-0 scale-95">
        <MenuItems class="absolute bottom-full left-0 z-50 mb-2 w-56 origin-bottom rounded-md bg-white shadow-lg divide-y divide-gray-100 ring-1 ring-black/5 focus:outline-hidden">
          <div class="px-3.5 py-3 truncate">
            <span class="block mb-0.5 text-xs text-gray-500">{{ t('nav.profile.signedInAs') }}</span>
            <span class="text-sm font-semibold">{{ me.name }}</span>
          </div>
          <ul v-for="(itemGroup, index) in profileDropdown" :key="`itemGroup-${index}`" class="py-1.5">
            <li v-for="item in itemGroup" :key="item.name">
              <router-link :to="item.to" @click="emit('navigate')">
                <MenuItem v-slot="{ active }">
                  <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'flex items-center px-3.5 py-1.5 text-sm']">
                    <component :is="item.icon" :class="[active ? 'text-gray-500' : 'text-gray-400', 'flex-none h-5 w-5 mr-3']" aria-hidden="true" />
                    {{ t(item.name) }}
                  </div>
                </MenuItem>
              </router-link>
            </li>
          </ul>
        </MenuItems>
      </transition>
    </Menu>
  </div>
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { XMarkIcon } from '@heroicons/vue/24/outline';
import { FunctionalComponent } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { UserDto } from '../common/backend';

export type NavigationItem = { icon: FunctionalComponent, name: string, to: string };
export type ProfileDropdownItem = { icon: FunctionalComponent, name: string, to: string };

const { t } = useI18n({ useScope: 'global' });
const route = useRoute();

defineProps<{
  me: UserDto,
  mainNav: NavigationItem[],
  adminNav: NavigationItem[],
  isAdmin: boolean,
  profileDropdown: ProfileDropdownItem[][],
  showClose?: boolean
}>();

const emit = defineEmits<{
  navigate: [],
  close: []
}>();

function itemClasses(to: string) {
  const active = route.path === to || route.path.startsWith(to + '/');
  return [
    active ? 'bg-white/10 text-white' : 'text-gray-300 hover:bg-white/5 hover:text-white',
    'group flex items-center gap-x-3 rounded-md p-2 text-sm font-medium'
  ];
}
</script>

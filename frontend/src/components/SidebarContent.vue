<template>
  <div class="flex grow flex-col gap-y-5 pb-4" :class="collapsed ? 'px-3' : 'px-4'">
    <div class="flex h-16 shrink-0 items-center" :class="collapsed ? 'justify-center' : 'px-2'">
      <router-link to="/app" class="flex h-8 items-center" @click="emit('navigate')">
        <img :src="collapsed ? '/logo.svg' : '/logo-text.svg'" class="h-8" alt="Cryptomator Hub" />
      </router-link>
    </div>

    <nav class="flex flex-1 flex-col gap-y-7 overflow-y-auto">
      <ul role="list" class="space-y-1">
        <li v-for="item in mainNav" :key="item.name">
          <router-link :to="item.to" :title="collapsed ? t(item.name) : undefined" :class="itemClasses(item.to)" @click="emit('navigate')">
            <component :is="item.icon" class="h-6 w-6 shrink-0" aria-hidden="true" />
            <span v-if="!collapsed" class="truncate">{{ t(item.name) }}</span>
          </router-link>
        </li>
      </ul>

      <div v-if="isAdmin">
        <hr class="border-white/10" />
        <ul role="list" class="mt-3 space-y-1">
          <li v-for="item in adminNav" :key="item.name">
            <router-link :to="item.to" :title="collapsed ? t(item.name) : undefined" :class="itemClasses(item.to)" @click="emit('navigate')">
              <component :is="item.icon" class="h-6 w-6 shrink-0" aria-hidden="true" />
              <span v-if="!collapsed" class="truncate">{{ t(item.name) }}</span>
            </router-link>
          </li>
        </ul>
      </div>
    </nav>

    <!-- Profile menu pinned to the bottom, kept outside the scrolling nav so the
         dropdown isn't clipped by the rail's overflow when collapsed -->
    <Menu as="div" class="relative">
      <MenuButton :title="collapsed ? me.name : undefined" :class="['flex w-full items-center gap-x-3 rounded-md p-2 text-sm font-medium text-gray-300 hover:bg-white/5 hover:text-white focus:outline-hidden', collapsed ? 'justify-center' : '']">
        <img class="h-8 w-8 shrink-0 rounded-full bg-white" :src="me.pictureUrl" alt="" />
        <span v-if="!collapsed" class="truncate">{{ me.name }}</span>
      </MenuButton>
      <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class=" opacity-100 scale-100" leave-to-class=" opacity-0 scale-95">
        <MenuItems class="absolute bottom-full left-0 z-50 mb-2 w-56 origin-bottom rounded-md bg-white shadow-lg divide-y divide-gray-100 ring-1 ring-black/5 focus:outline-hidden">
          <div class="px-3.5 py-3 truncate">
            <span class="block mb-0.5 text-xs text-gray-500">{{ t('nav.profile.signedInAs') }}</span>
            <span class="text-sm font-semibold">{{ me.name }}</span>
          </div>
          <div v-for="(itemGroup, index) in profileDropdown" :key="`itemGroup-${index}`" class="py-1.5">
            <router-link v-for="item in itemGroup" :key="item.name" :to="item.to" @click="emit('navigate')">
              <MenuItem v-slot="{ active }">
                <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'flex items-center px-3.5 py-1.5 text-sm']">
                  <component :is="item.icon" :class="[active ? 'text-gray-500' : 'text-gray-400', 'flex-none h-5 w-5 mr-3']" aria-hidden="true" />
                  {{ t(item.name) }}
                </div>
              </MenuItem>
            </router-link>
          </div>
        </MenuItems>
      </transition>
    </Menu>
  </div>
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { FunctionalComponent } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { UserDto } from '../common/backend';

export type NavigationItem = { icon: FunctionalComponent, name: string, to: string };
export type ProfileDropdownItem = { icon: FunctionalComponent, name: string, to: string };

const { t } = useI18n({ useScope: 'global' });
const route = useRoute();

const props = defineProps<{
  me: UserDto,
  mainNav: NavigationItem[],
  adminNav: NavigationItem[],
  isAdmin: boolean,
  profileDropdown: ProfileDropdownItem[][],
  collapsed: boolean
}>();

const emit = defineEmits<{
  navigate: []
}>();

function itemClasses(to: string) {
  const active = route.path.startsWith(to);
  return [
    active ? 'bg-white/10 text-white' : 'text-gray-300 hover:bg-white/5 hover:text-white',
    'group flex items-center gap-x-3 rounded-md p-2 text-sm font-medium',
    props.collapsed ? 'justify-center' : ''
  ];
}
</script>

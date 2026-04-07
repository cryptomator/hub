<template>
  <nav class="bg-tertiary2">
    <div class="mx-auto px-4 sm:px-6 lg:px-8">
      <div class="relative flex justify-between h-16">
        <div class="flex items-center gap-3">
          <!-- Mobile hamburger -->
          <button class="md:hidden p-2 rounded-md text-gray-400 hover:text-white hover:bg-gray-700 focus:outline-hidden focus:ring-2 focus:ring-inset focus:ring-white" @click="emit('openMenu')">
            <span class="sr-only">{{ t('nav.mobileMenu') }}</span>
            <Bars3Icon class="h-6 w-6" aria-hidden="true" />
          </button>

          <router-link to="/app" class="shrink-0 flex items-center">
            <img src="/logo.svg" class="h-8" alt="Logo"/>
            <span class="font-headline font-bold text-primary ml-2 pb-px">CRYPTOMATOR HUB</span>
          </router-link>
        </div>

        <div class="flex items-center">
          <!-- Profile dropdown -->
          <Menu as="div" class="ml-3 relative">
            <div>
              <MenuButton class="flex rounded-full bg-tertiary2 text-sm focus:outline-hidden focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-gray-800">
                <span class="sr-only">Open user menu</span>
                <img class="h-8 w-8 rounded-full" :src="props.me.pictureUrl" alt="" />
              </MenuButton>
            </div>
            <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class=" opacity-100 scale-100" leave-to-class=" opacity-0 scale-95">
              <MenuItems class="absolute right-0 z-50 mt-2 w-48 origin-top-right rounded-md bg-white shadow-lg divide-y divide-gray-100 ring-1 ring-black/5 focus:outline-hidden">
                <div class="px-3.5 py-3 truncate">
                  <span class="block mb-0.5 text-xs text-gray-500">{{ t('nav.profile.signedInAs') }}</span>
                  <span class="text-sm font-semibold">{{ props.me.name }}</span>
                </div>
                <div class="py-1.5">
                  <router-link to="/app/profile">
                    <MenuItem v-slot="{ active }">
                      <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'flex items-center px-3.5 py-1.5 text-sm']">
                        <UserIcon :class="[active ? 'text-gray-500' : 'text-gray-400', 'flex-none h-5 w-5 mr-3']" aria-hidden="true" />
                        {{ t('nav.profile.profile') }}
                      </div>
                    </MenuItem>
                  </router-link>
                </div>
                <div class="py-1.5">
                  <router-link to="/app/logout">
                    <MenuItem v-slot="{ active }">
                      <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'flex items-center px-3.5 py-1.5 text-sm']">
                        <ArrowRightStartOnRectangleIcon :class="[active ? 'text-gray-500' : 'text-gray-400', 'flex-none h-5 w-5 mr-3']" aria-hidden="true" />
                        {{ t('nav.profile.signOut') }}
                      </div>
                    </MenuItem>
                  </router-link>
                </div>
              </MenuItems>
            </transition>
          </Menu>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { ArrowRightStartOnRectangleIcon, Bars3Icon, UserIcon } from '@heroicons/vue/24/outline';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  me: UserDto
}>();

const emit = defineEmits<{
  openMenu: []
}>();
</script>

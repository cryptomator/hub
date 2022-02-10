<template>
  <Disclosure v-slot="{ open }" as="nav" class="bg-tertiary2">
    <div class="max-w-7xl mx-auto px-2 sm:px-6 lg:px-8">
      <div class="relative flex justify-between h-16">
        <div class="absolute inset-y-0 left-0 flex items-center sm:hidden">
          <!-- Mobile menu button-->
          <DisclosureButton class="inline-flex items-center justify-center p-2 rounded-md text-gray-400 hover:text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white">
            <span class="sr-only">{{ t('nav.mobileMenu') }}</span>
            <MenuIcon v-if="!open" class="block h-6 w-6" aria-hidden="true" />
            <XIcon v-else class="block h-6 w-6" aria-hidden="true" />
          </DisclosureButton>
        </div>
        <div class="flex-1 flex items-center justify-center sm:items-stretch sm:justify-start">
          <router-link to="/" class="shrink-0 flex items-center">
            <img src="/logo.svg" class="h-8" alt="Logo"/>
            <span class="font-headline font-bold text-primary ml-2 pb-px">CRYPTOMATOR HUB</span>
          </router-link>
          <div class="hidden sm:ml-6 sm:flex sm:space-x-8">
            <router-link v-for="item in navigation" :key="item.name" v-slot="{ isActive, href, navigate }" :to="item.to" custom>
              <a v-bind="$attrs" :href="href" :class="[isActive ? 'border-primary text-white' : 'border-transparent text-gray-300 hover:border-gray-300 hover:text-white', ' inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium']" @click="navigate">
                {{ t(item.name) }}
              </a>
            </router-link>
          </div>
        </div>

        <div class="absolute inset-y-0 right-0 flex items-center pr-2 sm:static sm:inset-auto sm:ml-6 sm:pr-0">
          <!-- Profile dropdown -->
          <Menu as="div" class="ml-3 relative">
            <div>
              <MenuButton class="bg-tertiary2 flex text-sm rounded-full focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-white">
                <span class="sr-only">Open user menu</span>
                <img class="h-8 w-8 rounded-full" :src="pictureUrl" alt="" />
              </MenuButton>
            </div>
            <transition enter-active-class="transition ease-out duration-100" enter-from-class="opacity-0 scale-95" enter-to-class="opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class=" opacity-100 scale-100" leave-to-class=" opacity-0 scale-95">
              <MenuItems class="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg py-1 bg-white divide-y divide-gray-100 ring-1 ring-black ring-opacity-5 focus:outline-none">
                <MenuItem v-for="item in profileDropdown" :key="item.name" v-slot="{ active }">
                  <router-link :to="item.to" :class="[active ? 'bg-gray-100' : '', 'block px-4 py-2 text-sm text-gray-700']">{{ t(item.name) }}</router-link>
                </MenuItem>
              </MenuItems>
            </transition>
          </Menu>
        </div>
      </div>
    </div>

    <DisclosurePanel class="sm:hidden">
      <div class="px-2 pt-2 pb-3 space-y-1">
        <router-link v-for="item in navigation" :key="item.name" :to="item.to" class="text-gray-300 hover:bg-gray-700 hover:text-white block px-3 py-2 rounded-md text-base font-medium" active-class="bg-gray-900 text-white block px-3 py-2 rounded-md text-base font-medium">
          {{ t(item.name) }}
        </router-link>
      </div>
    </DisclosurePanel>
  </Disclosure>
</template>

<script setup lang="ts">
import { Disclosure, DisclosureButton, DisclosurePanel, Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue';
import { MenuIcon, XIcon } from '@heroicons/vue/outline';
import md5 from 'blueimp-md5';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const navigation = [
  { name: 'nav.vaults', to: '/vaults' },
];
const profileDropdown = [
  { name: 'nav.profile.devices', to: '/devices' },
  { name: 'nav.profile.settings', to: '/settings' },
  { name: 'nav.profile.signOut', to: '/logout' },
];

const pictureUrl = computed(() => getPictureUrl());

const props = defineProps<{
  me : UserDto
}>();

function getPictureUrl(): string {
  let emailHash = md5(props.me.email.trim().toLowerCase());
  let gravatarUrl = `https://www.gravatar.com/avatar/${emailHash}?d=identicon`;
  return props.me.pictureUrl ?? gravatarUrl;
}

</script>

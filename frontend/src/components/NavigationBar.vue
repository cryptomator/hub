<template>
  <Disclosure v-slot="{ open }" as="nav" class="bg-tertiary2">
    <div class="max-w-7xl mx-auto px-2 sm:px-6 lg:px-8">
      <div class="relative flex justify-between h-16">
        <div class="absolute inset-y-0 left-0 flex items-center sm:hidden">
          <!-- Mobile menu button-->
          <DisclosureButton class="inline-flex items-center justify-center p-2 rounded-md text-gray-400 hover:text-white hover:bg-gray-700 focus:outline-hidden focus:ring-2 focus:ring-inset focus:ring-white">
            <span class="sr-only">{{ t('nav.mobileMenu') }}</span>
            <Bars3Icon v-if="!open" class="block h-6 w-6" aria-hidden="true" />
            <XMarkIcon v-else class="block h-6 w-6" aria-hidden="true" />
          </DisclosureButton>
        </div>
        <div class="flex-1 flex items-center justify-center sm:items-stretch sm:justify-start">
          <router-link to="/app" class="shrink-0 flex items-center">
            <img src="/logo.svg" class="h-8" alt="Logo"/>
            <span class="font-headline font-bold text-primary ml-2 pb-px">CRYPTOMATOR HUB</span>
          </router-link>
          <div class="hidden sm:ml-6 sm:flex sm:space-x-8">
            <router-link v-for="item in navigation" :key="item.name" v-slot="{ isActive, href, navigate }" :to="item.to" custom>
              <a :href="href" :class="[isActive ? 'border-primary text-white' : 'border-transparent text-gray-300 hover:border-gray-300 hover:text-white', ' inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium']" @click="navigate">
                {{ t(item.name) }}
              </a>
            </router-link>
          </div>
        </div>

        <div class="absolute inset-y-0 right-0 flex items-center pr-2 sm:static sm:inset-auto sm:ml-6 sm:pr-0">
          <!-- Profile dropdown -->
          <Menu as="div" class="ml-3 relative">
            <div>
              <MenuButton class="flex rounded-full bg-tertiary-2 text-sm focus:outline-hidden focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-gray-800">
                <span class="sr-only">Open user menu</span>
                <img class="h-8 w-8 rounded-full" :src="props.me.pictureUrl" alt="" />
              </MenuButton>
            </div>
            <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class=" opacity-100 scale-100" leave-to-class=" opacity-0 scale-95">
              <MenuItems class="absolute right-0 z-10 mt-2 w-48 origin-top-right rounded-md bg-white shadow-lg divide-y divide-gray-100 ring-1 ring-black/5 focus:outline-hidden">
                <div class="px-3.5 py-3 truncate">
                  <span class="block mb-0.5 text-xs text-gray-500">{{ t('nav.profile.signedInAs') }}</span>
                  <span class="text-sm font-semibold">{{ me.name }}</span>
                </div>
                <div v-for="(itemGroup, index) in profileDropdown" :key="`itemGroup-${index}`" class="py-1.5">
                  <router-link v-for="item in itemGroup" :key="item.name" :to="item.to">
                    <MenuItem v-slot="{ active }">
                      <div :class="[active ? 'bg-gray-100 text-gray-900' : 'text-gray-700', 'flex items-center px-3.5 py-1.5 text-sm text-gray-700']">
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
import { ArrowRightStartOnRectangleIcon, Bars3Icon, ListBulletIcon, UserIcon, WrenchIcon, XMarkIcon } from '@heroicons/vue/24/outline';
import { FunctionalComponent, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auth from '../common/auth';
import { UserDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const navigation = [
  { name: 'nav.vaults', to: '/app/vaults' },
];

type ProfileDropdownItem = { icon: FunctionalComponent, name: string, to: string };

const profileDropdownSections = {
  infoSection :
    [
      { icon: UserIcon, name: 'nav.profile.profile', to: '/app/profile' }
    ],

  adminSection :
    [
      { icon: ListBulletIcon, name: 'nav.profile.auditlog', to: '/app/admin/auditlog' },
      { icon: WrenchIcon, name: 'nav.profile.admin', to: '/app/admin/settings' }
    ],

  hubSection :
    [
      { icon: ArrowRightStartOnRectangleIcon, name: 'nav.profile.signOut', to: '/app/logout' }
    ],

};

const profileDropdown = ref<ProfileDropdownItem[][]>([]);
const props = defineProps<{
  me : UserDto
}>();

onMounted(async () => {
  if ((await auth).hasRole('admin')) {
    profileDropdown.value = [profileDropdownSections.infoSection, profileDropdownSections.adminSection, profileDropdownSections.hubSection];
  } else {
    profileDropdown.value = [profileDropdownSections.infoSection, profileDropdownSections.hubSection];
  }
});
</script>

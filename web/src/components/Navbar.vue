<template>
  <Disclosure v-slot="{ open }" as="nav" class="bg-gray-800">
    <div class="max-w-7xl mx-auto px-2 sm:px-6 lg:px-8">
      <div class="relative flex items-center justify-between h-16">
        <div class="absolute inset-y-0 left-0 flex items-center sm:hidden">
          <!-- Mobile menu button-->
          <DisclosureButton class="inline-flex items-center justify-center p-2 rounded-md text-gray-400 hover:text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white">
            <span class="sr-only">
              Open main menu</span>
            <MenuIcon v-if="!open" class="block h-6 w-6" aria-hidden="true" />
            <XIcon v-else class="block h-6 w-6" aria-hidden="true" />
          </DisclosureButton>
        </div>
        <div class="flex-1 flex items-center justify-center sm:items-stretch sm:justify-start">
          <div class="flex-shrink-0 flex items-center">
            <img
              src="../assets/logo.svg"
              class="h-8"
              alt="Logo"
            >
            <span class="font-headline font-bold text-primary ml-2 pb-px">CRYPTOMATOR HUB</span>
            <!--<img class="block lg:hidden h-8 w-auto" src="https://tailwindui.com/img/logos/workflow-mark-indigo-500.svg" alt="Workflow" />
            <img class="hidden lg:block h-8 w-auto" src="https://tailwindui.com/img/logos/workflow-logo-indigo-500-mark-white-text.svg" alt="Workflow" />-->
          </div>
          <div class="hidden sm:block sm:ml-6">
            <div class="flex space-x-4">
              <!--<router-link v-for="item in navigation" :key="item.name" :to="item.href" :class="[item.current ? 'bg-gray-900 text-white' : 'text-gray-300 hover:bg-gray-700 hover:text-white', 'px-3 py-2 rounded-md text-sm font-medium']" :aria-current="item.current ? 'page' : undefined">{{ item.name }}</router-link>-->
              <!--<a v-for="item in navigation" :key="item.name" :href="item.href" :class="[item.current ? 'bg-gray-900 text-white' : 'text-gray-300 hover:bg-gray-700 hover:text-white', 'px-3 py-2 rounded-md text-sm font-medium']" :aria-current="item.current ? 'page' : undefined">{{ item.name }}</a>-->
              <router-link to="/"
                           class="text-gray-300 hover:bg-gray-700 hover:text-white', 'px-3 py-2 rounded-md text-sm font-medium'"
                           active-class="bg-gray-900 text-white px-3 py-2 rounded-md text-sm font-medium"
                           exact-path
              >
                Home
              </router-link>
              <router-link to="/vaults/create"
                           class="text-gray-300 hover:bg-gray-700 hover:text-white', 'px-3 py-2 rounded-md text-sm font-medium'"
                           active-class="bg-gray-900 text-white px-3 py-2 rounded-md text-sm font-medium"
                           exact-path
              >
                {{ t('create_vault_title') }}
              </router-link>
              <router-link to="/user"
                           class="text-gray-300 hover:bg-gray-700 hover:text-white', 'px-3 py-2 rounded-md text-sm font-medium'"
                           active-class="bg-gray-900 text-white px-3 py-2 rounded-md text-sm font-medium"
                           exact-path
              >
                User Details
              </router-link>
              <!-- TODO decide to use logout() directly and remove Logout.vue-->
              <router-link v-if="isLoggedIn()"
                           to="/logout"
                           class="text-gray-300 hover:bg-gray-700 hover:text-white', 'px-3 py-2 rounded-md text-sm font-medium'"
                           active-class="bg-gray-900 text-white px-3 py-2 rounded-md text-sm font-medium"
                           exact-path
              >
                Logout
              </router-link>
              <router-link to="/setup"
                           class="text-gray-300 hover:bg-gray-700 hover:text-white', 'px-3 py-2 rounded-md text-sm font-medium'"
                           active-class="bg-gray-900 text-white px-3 py-2 rounded-md text-sm font-medium"
                           exact-path
              >
                Setup
              </router-link>
            </div>
          </div>
        </div>
        <div>
          <select v-model="$i18n.locale" class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
            <option v-for="(lang, i) in languages" :key="`Lang${i}`" :value="lang">
              {{ lang }}
            </option>
          </select>
        </div>
        <!-- Profile dropdown -->
        <Menu as="div" class="ml-3 relative">
          <div>
            <MenuButton class="bg-gray-800 flex text-sm rounded-full focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-white">
              <span class="sr-only">Open user menu</span>
              <img class="h-8 w-8 rounded-full" src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80" alt="" />
            </MenuButton>
          </div>
          <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
            <MenuItems class="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg py-1 bg-white ring-1 ring-black ring-opacity-5 focus:outline-none">
              <MenuItem v-slot="{ active }">
                <a href="#" :class="[active ? 'bg-gray-100' : '', 'block px-4 py-2 text-sm text-gray-700']">
                  ?Your connected Devices?
                </a>
              </MenuItem>
              <MenuItem v-if="isLoggedIn()" v-slot="{ active }" @click="logout()">
                <a :class="[active ? 'bg-gray-100' : '', 'block px-4 py-2 text-sm text-gray-700']">
                  Log out
                </a>
              </MenuItem>
              <MenuItem v-if="!isLoggedIn()" v-slot="{ active }" @click="logout()">
                <a :class="[active ? 'bg-gray-100' : '', 'block px-4 py-2 text-sm text-gray-700']">
                  Logged out
                </a>
              </MenuItem>
            </MenuItems>
          </transition>
        </Menu>
      </div>
    </div>
    <DisclosurePanel class="sm:hidden">
      <div class="px-2 pt-2 pb-3 space-y-1">
        <a v-for="item in navigation" :key="item.name" :href="item.href" :class="[item.current ? 'bg-gray-900 text-white' : 'text-gray-300 hover:bg-gray-700 hover:text-white', 'block px-3 py-2 rounded-md text-base font-medium']" :aria-current="item.current ? 'page' : undefined">{{ item.name }}</a>
      </div>
    </DisclosurePanel>
  </Disclosure>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { Disclosure, DisclosureButton, DisclosurePanel, Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue'
import { MenuIcon, XIcon } from '@heroicons/vue/outline'
import { Locales } from "../locales/locales";
import { useI18n } from 'vue-i18n'
import auth from '../common/auth';

const navigation = [
  { name: 'Home', href: '/'},
  { name: "{{ t(\'create_vault_title\') }}", href: '/vaults/create'},
  { name: 'User Details', href: '/user'},
]

const languages = Locales

export default defineComponent({
  name: "Navbar.vue",
  components: {
    Disclosure,
    DisclosureButton,
    DisclosurePanel,
    Menu,
    MenuButton,
    MenuItem,
    MenuItems,
    MenuIcon,
	XIcon,
  },
  setup(){
    const { t } = useI18n({
  	  useScope: 'global'
  	})
    return {
      t,
      navigation,
      languages
    }
  },
  methods: {
    isLoggedIn(){
      return auth.isAuthenticated();
    },
    logout() {
      auth.logout();
    }
  }

})
</script>

<style scoped>

</style>
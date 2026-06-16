<template>
  <!-- Mobile off-canvas sidebar -->
  <TransitionRoot as="template" :show="mobileOpen">
    <Dialog class="relative z-50 md:hidden" @close="emit('close')">
      <TransitionChild as="template" enter="transition-opacity ease-linear duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="transition-opacity ease-linear duration-300" leave-from="opacity-100" leave-to="opacity-0">
        <div class="fixed inset-0 bg-gray-900/80" />
      </TransitionChild>

      <div class="fixed inset-0 flex">
        <TransitionChild as="template" enter="transition ease-in-out duration-300 transform" enter-from="-translate-x-full" enter-to="translate-x-0" leave="transition ease-in-out duration-300 transform" leave-from="translate-x-0" leave-to="-translate-x-full">
          <DialogPanel class="relative mr-16 flex w-full max-w-xs flex-1 bg-tertiary2">
            <DialogTitle class="sr-only">{{ t('nav.sidebar.title') }}</DialogTitle>
            <TransitionChild as="template" enter="ease-in-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in-out duration-300" leave-from="opacity-100" leave-to="opacity-0">
              <div class="absolute top-0 left-full flex w-16 justify-center pt-5">
                <button type="button" class="-m-2.5 p-2.5" @click="emit('close')">
                  <span class="sr-only">{{ t('nav.closeMenu') }}</span>
                  <XMarkIcon class="h-6 w-6 text-white" aria-hidden="true" />
                </button>
              </div>
            </TransitionChild>
            <SidebarContent :me="me" :main-nav="mainNav" :admin-nav="adminNav" :is-admin="isAdmin" :profile-dropdown="profileDropdown" :collapsed="false" @navigate="emit('close')" />
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>

  <!-- Static sidebar for desktop -->
  <div class="group relative hidden bg-tertiary2 transition-[width] duration-200 md:flex md:flex-col" :class="collapsed ? 'md:w-16' : 'md:w-64'">
    <SidebarContent :me="me" :main-nav="mainNav" :admin-nav="adminNav" :is-admin="isAdmin" :profile-dropdown="profileDropdown" :collapsed="collapsed" />
    <button type="button" class="absolute top-1/2 -right-3 z-10 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-full border border-white/10 bg-tertiary2 text-gray-300 opacity-0 shadow-sm transition-opacity hover:text-white focus:opacity-100 focus:outline-hidden group-hover:opacity-100 group-focus-within:opacity-100" :title="collapsed ? t('nav.sidebar.expand') : t('nav.sidebar.collapse')" @click="toggleCollapsed">
      <ChevronDoubleRightIcon v-if="collapsed" class="h-3.5 w-3.5" aria-hidden="true" />
      <ChevronDoubleLeftIcon v-else class="h-3.5 w-3.5" aria-hidden="true" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ArrowRightStartOnRectangleIcon, ChevronDoubleLeftIcon, ChevronDoubleRightIcon, LifebuoyIcon, ListBulletIcon, LockClosedIcon, UserGroupIcon, UserIcon, UsersIcon, WrenchIcon, XMarkIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auth from '../common/auth';
import backend, { LicenseUserInfoDto, UserDto } from '../common/backend';
import SidebarContent, { NavigationItem, ProfileDropdownItem } from './SidebarContent.vue';

const { t } = useI18n({ useScope: 'global' });

defineProps<{
  me: UserDto,
  mobileOpen: boolean
}>();

const emit = defineEmits<{
  close: []
}>();

const COLLAPSE_KEY = 'hub.sidebar.collapsed';

function loadCollapsed(): boolean {
  try {
    return localStorage.getItem(COLLAPSE_KEY) === 'true';
  } catch {
    return false;
  }
}

const collapsed = ref(loadCollapsed());

function toggleCollapsed() {
  collapsed.value = !collapsed.value;
  try {
    localStorage.setItem(COLLAPSE_KEY, String(collapsed.value));
  } catch {
    // storage may be unavailable (private mode / blocked)
  }
}

const mainNav = ref<NavigationItem[]>([
  { icon: LockClosedIcon, name: 'nav.vaults', to: '/app/vaults' }
]);

const adminNav: NavigationItem[] = [
  { icon: UsersIcon, name: 'nav.users', to: '/app/users' },
  { icon: UserGroupIcon, name: 'nav.groups', to: '/app/groups' },
  { icon: ListBulletIcon, name: 'nav.profile.auditlog', to: '/app/admin/auditlog' },
  { icon: WrenchIcon, name: 'nav.profile.admin', to: '/app/admin/settings' }
];

const profileDropdown: ProfileDropdownItem[][] = [
  [{ icon: UserIcon, name: 'nav.profile.profile', to: '/app/profile' }],
  [{ icon: ArrowRightStartOnRectangleIcon, name: 'nav.profile.signOut', to: '/app/logout' }]
];

const isAdmin = ref(false);
const licenseStatus = ref<LicenseUserInfoDto>();

const isCommunityLicense = computed(() => !licenseStatus.value?.expiresAt);

onMounted(async () => {
  isAdmin.value = (await auth).hasRole('admin');

  licenseStatus.value = await backend.license.getUserInfo();
  const emergencyAccessEnabled = (await backend.settings.get()).enableEmergencyAccess;
  if (!isCommunityLicense.value && emergencyAccessEnabled) {
    try {
      const recoverable = await backend.vaults.listRecoverable();
      const unique = Array.from(new Map(recoverable.map(v => [v.id, v])).values());
      if (unique.length > 0 && !mainNav.value.some(i => i.to === '/app/emergency-access')) {
        mainNav.value.push({ icon: LifebuoyIcon, name: 'nav.emergencyAccess', to: '/app/emergency-access' });
      }
    } catch (e) {
      console.error('Failed to load emergency-access vaults:', e);
    }
  }
});
</script>

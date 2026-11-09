<template>
  <!-- Mobile off-canvas sidebar -->
  <TransitionRoot as="template" :show="mobileOpen">
    <Dialog class="relative z-50 md:hidden" @close="emit('close')">
      <TransitionChild as="template" enter="transition-opacity ease-linear duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="transition-opacity ease-linear duration-300" leave-from="opacity-100" leave-to="opacity-0">
        <div class="fixed inset-0 bg-gray-900/80" />
      </TransitionChild>

      <div class="fixed inset-0 flex">
        <TransitionChild as="template" enter="transition ease-in-out duration-300 transform" enter-from="-translate-x-full" enter-to="translate-x-0" leave="transition ease-in-out duration-300 transform" leave-from="translate-x-0" leave-to="-translate-x-full">
          <DialogPanel class="relative flex w-full max-w-xs flex-1 bg-tertiary2">
            <DialogTitle class="sr-only">{{ t('nav.sidebar.title') }}</DialogTitle>
            <SidebarContent :me="me" :main-nav="mainNav" :admin-nav="adminNav" :is-admin="isAdmin" :profile-dropdown="profileDropdown" :show-close="true" @navigate="emit('close')" @close="emit('close')" />
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>

  <!-- Static sidebar for desktop -->
  <div class="hidden bg-tertiary2 md:flex md:w-64 md:flex-col">
    <SidebarContent :me="me" :main-nav="mainNav" :admin-nav="adminNav" :is-admin="isAdmin" :profile-dropdown="profileDropdown" />
  </div>
</template>

<script setup lang="ts">
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ArrowRightStartOnRectangleIcon, LifebuoyIcon, ListBulletIcon, LockClosedIcon, UserGroupIcon, UserIcon, UsersIcon, WrenchIcon } from '@heroicons/vue/24/outline';
import { computed, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import auth from '../common/auth';
import backend, { EntitlementsDto, SettingsDto, UserDto } from '../common/backend';
import licenseEntitlements from '../common/entitlements';
import SidebarContent, { NavigationItem, ProfileDropdownItem } from './SidebarContent.vue';

const { t } = useI18n({ useScope: 'global' });
const route = useRoute();

defineProps<{
  me: UserDto,
  mobileOpen: boolean
}>();

const emit = defineEmits<{
  close: []
}>();

const entitlements = ref<EntitlementsDto>();
const settings = ref<SettingsDto>();

const mainNav = computed(() => {
  const nav: NavigationItem[] = [{ icon: LockClosedIcon, name: 'nav.vaults', to: '/app/vaults' }];
  if (entitlements.value?.emergencyAccessEnabled && settings.value?.enableEmergencyAccess) {
    nav.push({ icon: LifebuoyIcon, name: 'nav.emergencyAccess', to: '/app/emergency-access' });
  }
  return nav;
});

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

onMounted(async () => {
  isAdmin.value = (await auth).hasRole('admin');
  await fetchEmergencyAccessAvailability();
});

// the sidebar stays mounted while navigating, so retry on navigation if fetching failed
watch(() => route.path, async () => {
  if (settings.value === undefined) {
    await fetchEmergencyAccessAvailability();
  }
});

async function fetchEmergencyAccessAvailability() {
  try {
    [settings.value, entitlements.value] = await Promise.all([backend.settings.get(), licenseEntitlements.get()]);
  } catch (error) {
    console.error('Determining emergency access availability failed.', error);
  }
}
</script>

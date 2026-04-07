<template>
  <!-- Mobile backdrop -->
  <Transition enter-active-class="transition-opacity duration-200" enter-from-class="opacity-0" enter-to-class="opacity-100" leave-active-class="transition-opacity duration-200" leave-from-class="opacity-100" leave-to-class="opacity-0">
    <div v-if="mobileOpen" class="fixed inset-0 bg-black/50 z-40 md:hidden" @click="emit('close')" />
  </Transition>

  <aside :class="[
    'flex flex-col bg-tertiary2 flex-shrink-0 transition-all duration-200',
    // Mobile: fixed overlay, slide in/out
    'fixed inset-y-0 left-0 z-50 w-56',
    mobileOpen ? 'translate-x-0' : '-translate-x-full',
    // Desktop: relative, always visible, collapsible
    collapsed ? 'md:w-14' : 'md:w-56',
    'md:relative md:inset-auto md:z-auto md:translate-x-0',
  ]">
    <nav class="flex-1 overflow-y-auto px-2 py-4 space-y-1">
      <router-link v-for="item in mainNav" :key="item.name" :to="item.to" custom v-slot="{ href, navigate }">
        <a :href="href" @click="(e) => { navigate(e); emit('close') }" :title="collapsed ? t(item.name) : undefined" :class="[route.path.startsWith(item.to) ? 'bg-tertiary text-white' : 'text-gray-300 hover:bg-tertiary hover:text-white', 'flex items-center gap-3 px-2 py-2 text-sm font-medium rounded-md']">
          <component :is="item.icon" class="h-5 w-5 flex-shrink-0" aria-hidden="true" />
          <span v-if="!collapsed" class="whitespace-nowrap">{{ t(item.name) }}</span>
        </a>
      </router-link>

      <template v-if="isAdmin">
        <hr class="border-tertiary my-2" />
        <router-link v-for="item in adminNav" :key="item.name" :to="item.to" custom v-slot="{ href, navigate }">
          <a :href="href" @click="(e) => { navigate(e); emit('close') }" :title="collapsed ? t(item.name) : undefined" :class="[route.path.startsWith(item.to) ? 'bg-tertiary text-white' : 'text-gray-300 hover:bg-tertiary hover:text-white', 'flex items-center gap-3 px-2 py-2 text-sm font-medium rounded-md']">
            <component :is="item.icon" class="h-5 w-5 flex-shrink-0" aria-hidden="true" />
            <span v-if="!collapsed" class="whitespace-nowrap">{{ t(item.name) }}</span>
          </a>
        </router-link>
      </template>
    </nav>

    <!-- Collapse toggle (desktop only) -->
    <div class="hidden md:flex px-2 py-3 border-t border-tertiary">
      <button @click="collapsed = !collapsed" :class="[collapsed ? 'justify-center' : 'justify-end', 'flex w-full text-gray-400 hover:text-white']" :title="collapsed ? t('nav.sidebar.expand') : t('nav.sidebar.collapse')">
        <ChevronLeftIcon v-if="!collapsed" class="h-5 w-5" aria-hidden="true" />
        <ChevronRightIcon v-else class="h-5 w-5" aria-hidden="true" />
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ChevronLeftIcon, ChevronRightIcon, ExclamationTriangleIcon, ListBulletIcon, LockClosedIcon, UserGroupIcon, UsersIcon, WrenchIcon } from '@heroicons/vue/24/outline';
import { FunctionalComponent, computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import auth from '../common/auth';
import backend, { LicenseUserInfoDto, VaultDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });
const route = useRoute();

const props = defineProps<{
  mobileOpen: boolean
}>();

const emit = defineEmits<{
  close: []
}>();

type NavItem = { icon: FunctionalComponent, name: string, to: string };

const collapsed = ref(false);

const mainNav = ref<NavItem[]>([
  { icon: LockClosedIcon, name: 'nav.vaults', to: '/app/vaults' },
]);

const adminNav: NavItem[] = [
  { icon: UsersIcon, name: 'nav.users', to: '/app/users' },
  { icon: UserGroupIcon, name: 'nav.groups', to: '/app/groups' },
  { icon: ListBulletIcon, name: 'nav.profile.auditlog', to: '/app/admin/auditlog' },
  { icon: WrenchIcon, name: 'nav.profile.admin', to: '/app/admin/settings' },
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
      const recoverable = await backend.vaults.listRecoverable().catch(() => [] as VaultDto[]);
      const unique = Array.from(new Map(recoverable.map(v => [v.id, v])).values());
      if (unique.length > 0) {
        mainNav.value.push({ icon: ExclamationTriangleIcon, name: 'nav.emergencyAccess', to: '/app/emergency-access' });
      }
    } catch (e) {
      console.error('Failed to load emergency-access vaults:', e);
    }
  }
});
</script>

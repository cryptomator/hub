<template>
  <div class="flex h-screen overflow-hidden">
    <Sidebar :me="me" :mobile-open="mobileOpen" @close="mobileOpen = false" />

    <div class="flex min-w-0 flex-1 flex-col overflow-hidden">
      <!-- Mobile top bar -->
      <div class="flex h-16 shrink-0 items-center gap-x-4 bg-tertiary2 px-4 shadow-sm sm:px-6 md:hidden">
        <button type="button" class="-m-2.5 p-2.5 text-gray-300 hover:text-white" @click="mobileOpen = true">
          <span class="sr-only">{{ t('nav.mobileMenu') }}</span>
          <Bars3Icon class="h-6 w-6" aria-hidden="true" />
        </button>
        <router-link to="/app" class="flex items-center">
          <img src="/logo-text.svg" class="h-8" alt="Cryptomator Hub" />
        </router-link>
      </div>

      <main class="flex-1 overflow-y-auto">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Bars3Icon } from '@heroicons/vue/24/outline';
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { UserDto } from '../common/backend';
import Sidebar from './Sidebar.vue';

const { t } = useI18n({ useScope: 'global' });

defineProps<{
  me: UserDto
}>();

const mobileOpen = ref(false);

// Close the mobile off-canvas dialog when switching to the desktop layout, otherwise
// its body scroll-lock would persist (the lock is tied to the dialog's open state, not CSS).
const desktopQuery = window.matchMedia('(min-width: 768px)');
function closeOnDesktop(e: MediaQueryListEvent) {
  if (e.matches) {
    mobileOpen.value = false;
  }
}
onMounted(() => desktopQuery.addEventListener('change', closeOnDesktop));
onBeforeUnmount(() => desktopQuery.removeEventListener('change', closeOnDesktop));
</script>

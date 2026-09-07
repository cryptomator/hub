<template>
  <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8 flex justify-center">
    <div v-if="status === 'loading'">
      {{ t('common.loading') }}
    </div>
    <div v-else-if="status === 'error'">
      <FetchError :error="error!" :retry="retry" />
    </div>

    <div v-else class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
      <div class="flex justify-center mb-3 sm:mb-5">
        <img src="/logo.svg" class="h-12" alt="Logo" aria-hidden="true" />
      </div>

      <div v-if="status === 'accountSetup'" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlockSuccess.accountSetup.title') }}
        </h1>
        <p class="my-3">
          {{ t('unlockSuccess.accountSetup.description') }}
        </p>
        <router-link to="/app/setup" class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-xs text-white bg-primary focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">{{ t('unlockSuccess.accountSetup.goToSetup') }}</router-link>
      </div>

      <div v-else-if="status === 'deviceSetup'" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlockSuccess.deviceSetup.title') }}
        </h1>
        <p class="my-3">
          {{ t('unlockSuccess.deviceSetup.description') }}
        </p>
        <router-link v-if="hasBrowserKeys" to="/app/profile" class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-xs text-white bg-primary focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary">
          {{ t('unlockSuccess.deviceSetup.goToProfile') }}
        </router-link>
      </div>

      <div v-else-if="status === 'archived'" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlock.noAccessVaultArchived.title') }}
        </h1>
        <p class="mt-2">
          {{ t('unlock.noAccessVaultArchived.description') }}
        </p>
      </div>

      <div v-else-if="status === 'denied'" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlockSuccess.noVaultAccess.title') }}
        </h1>
        <p class="mt-2">
          {{ t('unlockSuccess.noVaultAccess.description') }}
        </p>
      </div>

      <div v-else-if="status === 'allowed'" class="text-sm text-gray-500">
        <h1 class="text-2xl leading-6 font-medium text-gray-900">
          {{ t('unlockSuccess.title') }}
        </h1>
        <p class="mt-2">
          {{ t('unlockSuccess.description') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import FetchError from './FetchError.vue';

export type UnlockStatus = 'loading' | 'error' | 'accountSetup' | 'deviceSetup' | 'archived' | 'denied' | 'allowed';

const { t } = useI18n({ useScope: 'global' });

defineProps<{
  status: UnlockStatus,
  error?: Error,
  retry: () => Promise<void>,
  hasBrowserKeys: boolean
}>();
</script>

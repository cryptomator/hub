<template>
  <div v-if="license?.expiresAt && now > license.expiresAt" class="rounded-md bg-red-50 p-4 mb-3">
    <div class="flex">
      <div class="flex-shrink-0">
        <XCircleIcon class="h-5 w-5 text-red-400" aria-hidden="true" />
      </div>
      <div class="ml-3">
        <h3 class="text-sm font-medium text-red-800">{{ t('licenseAlert.licenseExpired.title') }}</h3>
        <i18n-t keypath="licenseAlert.licenseExpired.description" scope="global" tag="p" class="mt-2 text-sm text-red-700">
          <router-link to="/app/admin/settings" class="text-sm text-red-700 underline hover:text-red-600">
            {{ t('licenseAlert.button') }}
          </router-link>
        </i18n-t>
      </div>
    </div>
  </div>

  <div v-else-if="license && (license.licensedSeats - license.usedSeats < 0)" class="rounded-md bg-yellow-50 p-4 mb-3">
    <div class="flex">
      <div class="flex-shrink-0">
        <ExclamationTriangleIcon class="h-5 w-5 text-yellow-400" aria-hidden="true" />
      </div>
      <div class="ml-3">
        <h3 class="text-sm font-medium text-yellow-800">{{ t('licenseAlert.noRemainingSeats.title') }}</h3>
        <i18n-t keypath="licenseAlert.noRemainingSeats.description" scope="global" tag="p" class="mt-2 text-sm text-yellow-700">
          <router-link to="/app/admin/settings" class="text-sm text-yellow-700 underline hover:text-yellow-600">
            {{ t('licenseAlert.button') }}
          </router-link>
        </i18n-t>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ExclamationTriangleIcon, XCircleIcon } from '@heroicons/vue/20/solid';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { LicenseUserInfoDto } from '../common/backend';

const { t } = useI18n({ useScope: 'global' });

const now = ref<Date>(new Date());
const license = ref<LicenseUserInfoDto>();

onMounted(fetchData);

async function fetchData() {
  try {
    license.value = await backend.license.getUserInfo();
  } catch (error) {
    console.error('Retrieving license information failed', error);
  }
}
</script>

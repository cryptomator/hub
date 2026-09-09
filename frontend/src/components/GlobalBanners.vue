<template>
  <LicenseAlert v-if="licenseStatus" :is-admin="isAdmin" :license-status="licenseStatus" />

  <ContentBanner v-if="anyUserHasLegacyDevices" type="warning" :title="t('legacyDeviceBanner.title')" class="mb-4">
    {{ t('legacyDeviceBanner.admin.description') }}
  </ContentBanner>

  <ContentBanner v-else-if="hasLegacyDevices" type="warning" :title="t('legacyDeviceBanner.title')" class="mb-4">
    {{ t('legacyDeviceBanner.user.description') }}
  </ContentBanner>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import globalBanners from '../common/globalBanners';
import ContentBanner from './ContentBanner.vue';
import LicenseAlert from './LicenseAlert.vue';

const { t } = useI18n({ useScope: 'global' });
const route = useRoute();

const { isAdmin, licenseStatus, hasLegacyDevices, anyUserHasLegacyDevices } = globalBanners;

onMounted(refresh);
watch(() => route.path, refresh);

async function refresh() {
  try {
    await globalBanners.refresh();
  } catch (error) {
    console.error('Retrieving instance-wide announcements failed.', error);
  }
}
</script>

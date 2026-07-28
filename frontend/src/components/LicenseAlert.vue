<template>
  <ContentBanner v-if="props.licenseStatus.isExpired()" type="error" :title="t('licenseAlert.licenseExpired.title')" class="mb-4">
    <i18n-t v-if="props.isAdmin" keypath="licenseAlert.licenseExpired.admin.description" scope="global" tag="p">
      <router-link to="/app/admin/settings" class="underline hover:no-underline">
        {{ t('licenseAlert.button') }}
      </router-link>
    </i18n-t>
    <p v-else>{{ t('licenseAlert.licenseExpired.user.description') }}</p>
  </ContentBanner>

  <ContentBanner v-else-if="props.licenseStatus.isExceeded()" type="warning" :title="t('licenseAlert.noRemainingSeats.title')" class="mb-4">
    <i18n-t v-if="props.isAdmin" keypath="licenseAlert.noRemainingSeats.admin.description" scope="global" tag="p">
      <router-link to="/app/admin/settings" class="underline hover:no-underline">
        {{ t('licenseAlert.button') }}
      </router-link>
    </i18n-t>
    <p v-else>{{ t('licenseAlert.noRemainingSeats.user.description') }}</p>
  </ContentBanner>

  <ContentBanner v-else-if="props.licenseStatus.isExpiredWithinLeeway()" type="warning" :title="t('licenseAlert.licenseExpiredWithinLeeway.title')" class="mb-4">
    <i18n-t v-if="props.isAdmin" keypath="licenseAlert.licenseExpiredWithinLeeway.admin.description" scope="global" tag="p">
      <router-link to="/app/admin/settings" class="underline hover:no-underline">
        {{ t('licenseAlert.button') }}
      </router-link>
    </i18n-t>
    <p v-else>{{ t('licenseAlert.licenseExpiredWithinLeeway.user.description') }}</p>
  </ContentBanner>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { LicenseUserInfoDto } from '../common/backend';
import ContentBanner from './ContentBanner.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  isAdmin: boolean,
  licenseStatus: LicenseUserInfoDto
}>();

</script>

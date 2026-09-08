<template>
  <ContentBanner type="info" :title="t('missingEntitlements.title')" :link-text="t('admin.licenseInfo.manageSubscription')" :link-url="manageSubscriptionUrl">
    {{ t('missingEntitlements.description') }} <!-- TODO: link to feature comparison? -->
  </ContentBanner>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { BillingDto } from '../common/backend';
import config, { absFrontendBaseURL, ConfigDto } from '../common/config';
import ContentBanner from './ContentBanner.vue';

const { t } = useI18n({ useScope: 'global' });

const cfg = ref<ConfigDto>(config.get());
const billing = ref<BillingDto>();

const manageSubscriptionUrl = computed(() => {
  if (!billing.value) {
    return undefined;
  }
  const returnUrl = `${absFrontendBaseURL}admin/settings`;
  return `${cfg.value.billingUrl}?hub_id=${encodeURIComponent(billing.value.hubId)}&return_url=${encodeURIComponent(returnUrl)}&token_transfer=session`;
});

onMounted(async () => {
  try {
    billing.value = await backend.billing.get();
  } catch (error) {
    console.error('Retrieving billing information failed.', error);
  }
});
</script>

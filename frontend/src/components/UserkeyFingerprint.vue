<template>
  <div>
    <h2 class="text-base font-semibold leading-6 text-gray-900">
      {{ t('userkeyFingerprint.title') }}
    </h2>
    <p class="mt-1 text-sm text-gray-500">
      {{ t('userkeyFingerprint.description') }}
    </p>

    <div class="mt-4 bg-white rounded-md shadow-sm flex w-full">
      <div class="rounded-none rounded-l-md px-3 py-2 ring-1 ring-inset ring-gray-300 focus-within:ring-2 focus-within:ring-primary focus-within:z-10 w-full">
        <label for="keyFingerprint" class="sr-only">{{ t('userkeyFingerprint.title') }}</label>
        <input id="keyFingerprint" v-model="keyFingerprint" name="keyFingerprint" class="block w-full border-0 p-0 text-gray-900 font-mono text-lg placeholder:text-gray-400 focus:ring-0" readonly />
      </div>
    </div>
  </div>

</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { getFingerprint } from '../common/crypto';

const { t } = useI18n({ useScope: 'global' });

const keyFingerprint = ref<string | undefined>();

const props = defineProps<{
  userPublicKey?: string
}>();

onMounted(fetchData);

async function fetchData() {
  keyFingerprint.value = await getFingerprint(props.userPublicKey).then((key) => key?.replace(/.{8}/g, "$&" + " "))
}

</script>

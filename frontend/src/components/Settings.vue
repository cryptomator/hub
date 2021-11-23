<template>
  <div>
    <select v-model="$i18n.locale" class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-primary focus:border-primary sm:text-sm rounded-md">
      <option v-for="(lang, i) in languages" :key="`Lang${i}`" :value="lang">
        {{ lang }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { Locales } from '../locales/locales';
// import { useI18n } from 'vue-i18n';

const languages = Locales;

// const { t } = useI18n({ useScope: 'global' });
</script>

<template>
  <div class="pb-5 border-b border-gray-200">
    <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
      Settings
    </h2>
  </div>

  <div class="mt-5">
    <select v-model="$i18n.locale" class="block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-primary focus:border-primary sm:text-sm rounded-md">
      <option v-for="locale in Locales" :key="locale" :value="locale">
        {{ locale }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { Locales } from '../locales/locales';
import { useI18n } from 'vue-i18n';

const { t } = useI18n({ useScope: 'global' });
</script>

<template>
  <div>
    <select v-model="$i18n.locale" class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
      <option v-for="(lang, i) in languages" :key="`Lang${i}`" :value="lang">
        {{ lang }}
      </option>
    </select>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import { Locales } from '../locales/locales';
import { useI18n } from 'vue-i18n';

const languages = Locales;

export default defineComponent({
  name: "Settings",
  setup() {
    const { t } = useI18n({
      useScope: 'global'
    })
    return {
      t,
      languages
    }
  },
})
</script>

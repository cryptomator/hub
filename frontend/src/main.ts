import { createApp } from 'vue';
import { createI18n } from 'vue-i18n';
import App from './App.vue';
import './css/fonts.css';
import { datetimeFormats, defaultLocale } from './i18n/index';
import './index.css';
import router from './router';

// migrate to // import messages from '@intlify/vite-plugin-vue-i18n/messages'; as soon as it works
import de from './i18n/de-DE.json';
import en from './i18n/en-US.json';

const i18n = createI18n({
  locale: defaultLocale,
  fallbackLocale: defaultLocale,
  messages: {
    de,
    en
  },
  datetimeFormats,
  globalInjection: true
});

createApp(App)
  .use(router)
  .use(i18n)
  .mount('#app');

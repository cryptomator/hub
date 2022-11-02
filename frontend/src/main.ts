import { createApp } from 'vue';
import { createI18n } from 'vue-i18n';
import App from './App.vue';
import './css/fonts.css';
import { datetimeFormats, defaultLocale, messages } from './i18n/index';
import './index.css';
import router from './router';
// migrate to // import messages from '@intlify/vite-plugin-vue-i18n/messages'; as soon as it works

const i18n = createI18n({
  locale: defaultLocale,
  fallbackLocale: defaultLocale,
  messages,
  datetimeFormats,
  globalInjection: true
});

createApp(App)
  .use(router)
  .use(i18n)
  .mount('#app');

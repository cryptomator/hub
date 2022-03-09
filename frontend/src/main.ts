import { createApp } from 'vue';
import { createI18n } from 'vue-i18n';
import App from './App.vue';
import './css/fonts.css';
import { datetimeFormats, defaultLocale, messages } from './i18n/index';
import './index.css';
import router from './router';

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

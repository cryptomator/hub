import VueNotificationList from '@dafcoe/vue-notification';
import { createApp } from 'vue';
import { createI18n } from 'vue-i18n';
import App from './App.vue';
import './css/notification.css';
import './index.css';
import { defaultLocale, messages } from './locales/index';
import router from './router';

const i18n = createI18n({
  locale: defaultLocale,
  fallbackLocale: defaultLocale,
  messages,
  globalInjection: true
});

createApp(App)
  .use(router)
  .use(i18n)
  .use(VueNotificationList)
  .mount('#app');

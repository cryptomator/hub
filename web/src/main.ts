import { createApp } from 'vue';
import { createI18n } from 'vue-i18n';
import App from './App.vue';
import deDE from './locales/de-DE.json';
import enUS from './locales/en-US.json';
import router from './router';

const i18n = createI18n({
  locale: 'en-US',
  fallbackLocale: 'en-US',
  legacy: false,
  messages: {
    'en-US': enUS,
    'de-DE': deDE
  }
})

createApp(App)
  .use(router)
  .use(i18n)
  .mount('#app')

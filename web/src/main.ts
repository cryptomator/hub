import { createApp } from 'vue';
import { createI18n } from 'vue-i18n';
import App from './App.vue';
import { defaultLocale, messages } from "./locales/index";
import router from './router';
import './index.css'

const i18n = createI18n({
  locale: defaultLocale,
  fallbackLocale: defaultLocale,
  messages,
  globalInjection: true
})

createApp(App)
  .use(router)
  .use(i18n)
  .mount('#app')

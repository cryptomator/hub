import { createApp } from 'vue';
import App from './App.vue';
import './css/fonts.css';
import i18n from './i18n';
import './index.css';
import router from './router';

// migrate to // import messages from '@intlify/vite-plugin-vue-i18n/messages'; as soon as it works

createApp(App)
  .use(router)
  .use(i18n)
  .mount('#app');

import vueI18nPlugin from '@intlify/unplugin-vue-i18n/vite';
import tailwindcss from '@tailwindcss/vite';
import vue from '@vitejs/plugin-vue';
import path from 'node:path';
import { defineConfig } from 'vite';

// https://vitejs.dev/config/
export default defineConfig({
  base: './', // we use a <base href="/"/> tag, which all other urls need to be relative to
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag === 'altcha-widget'
        }
      }
    }),
    vueI18nPlugin({
      include: path.resolve(__dirname, './src/i18n/*.json')
    }),
    tailwindcss(),
  ],
  build: {
    target: 'esnext',
    assetsInlineLimit: 0,
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [{
            includeDependenciesRecursively: false, // only the matched module itself, like rollup's manualChunks
            name: (id: string) => {
              if (id.includes('/node_modules/@heroicons/')) {
                return 'heroicons';
              } else if (id.includes('/node_modules/vue')) {
                return 'vue';
              } else if (id.includes('/node_modules/')) {
                return 'libs';
              } else if (id.includes('/src/i18n/') || id.startsWith('virtual:intlify-i18n')) { // unplugin-vue-i18n emits locale messages as virtual modules
                return 'locales';
              } else if (id.includes('/src/components/emergencyaccess/')) {
                return 'emergency-access';
              }
              return null;
            }
          }]
        }
      }
    }
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    strictPort: true,
    proxy: {
      '/api/': 'http://127.0.0.1:8080'
    }
  }
});

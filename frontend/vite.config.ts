import vueI18nPlugin from '@intlify/unplugin-vue-i18n/vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import { defineConfig } from 'vite';

// https://vitejs.dev/config/
export default defineConfig({
  base: './', // we use a <base href="/"/> tag, which all other urls need to be relative to
  plugins: [
    vue(),
    vueI18nPlugin({
      include: path.resolve(__dirname, './src/i18n/*.json')
    }),
  ],
  build: {
    minify: 'esbuild',
    target: 'esnext',
    assetsInlineLimit: 0,
    rollupOptions: {
      output: {
        manualChunks: (id: string) => {
          if (id.includes('node_modules')) {
            return 'vendor';
          } else {
            return 'main';
          }
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

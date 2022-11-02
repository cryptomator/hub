// migrate to @intlify/unplugin-vue-i18n as soon as `mvn run dist` works
import { vueI18n } from '@intlify/vite-plugin-vue-i18n';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import { defineConfig } from 'vite';

// https://vitejs.dev/config/
export default defineConfig({
  base: './', // we use a <base href="/"/> tag, which all other urls need to be relative to
  plugins: [
    vue(),
    vueI18n({
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
    proxy: {
      '/api/': 'http://127.0.0.1:8080'
    }
  }
});

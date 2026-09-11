import { defineConfig, mergeConfig } from 'vitest/config';
import viteConfig from './vite.config';

export default mergeConfig(viteConfig, defineConfig({
  test: {
    environment: 'happy-dom',
    setupFiles: ['test/setup.ts'],
    coverage: {
      reporter: ['html', 'text-summary', 'lcov']
    }
  }
}));

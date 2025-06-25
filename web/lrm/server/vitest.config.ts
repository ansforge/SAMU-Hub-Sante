import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'node',
    coverage: {
      enabled: true,
      provider: 'v8',
      reportsDirectory: 'coverage',
      reporter: ['json', 'text', 'text-summary', 'lcov', 'clover'],
      exclude: ['node_modules/', 'coverage/*'],
      include: ['src/**/*.ts'],
    },
  },
});
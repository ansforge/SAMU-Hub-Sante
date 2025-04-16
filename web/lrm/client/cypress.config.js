import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    requestTimeout: 30000,
    experimentalStudio: true,
    setupNodeEvents(_on, _config) {
      // implement node event listeners here
    },
  },
});

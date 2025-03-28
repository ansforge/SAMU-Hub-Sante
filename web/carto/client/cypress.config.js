import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    requestTimeout: 30000,
    experimentalStudio: true,
    setupNodeEvents() {
      // implement node event listeners here
    },
  },
});

import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    requestTimeout: 30000,
    experimentalStudio: true,
    setupNodeEvents(_on, _config) {
      // implement node event listeners here
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      require('@cypress/code-coverage/task')(_on, _config);
      return _config;
    },
  },
});

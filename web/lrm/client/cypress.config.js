import { defineConfig } from 'cypress';
import codeCoverageTask from '@cypress/code-coverage/task';

export default defineConfig({
  e2e: {
    requestTimeout: 30000,
    experimentalStudio: true,
    setupNodeEvents(_on, _config) {
      // implement node event listeners here
      codeCoverageTask(_on, _config);
      return _config;
    },
  },
});

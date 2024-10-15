import { defineConfig } from 'cypress'

export default defineConfig({
  e2e: {
    requestTimeout: 15000,
    experimentalStudio: true,
    setupNodeEvents (on, config) {
      // implement node event listeners here
    }
  }
})

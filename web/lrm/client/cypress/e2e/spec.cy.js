import 'cypress-cdp'
describe('Demo schemas download spec', () => {
  it('Accesses the demo page and successfully downloads all the schemas from the branch indicated in the config', () => {
    cy.visit('http://localhost:3000/')
    // Wait for the event listeners to get hooked up
    cy.hasEventListeners('#demo-login-button', { type: 'click' })
    // Go to demo page
    cy.get('#demo-login-button').click()
    // Wait for all schema-related fetches to complete with 200 status
    cy.waitForMessagesList()
    cy.waitForSchemas()
  })
})

import 'cypress-cdp'
describe('Demo page spec', () => {
  it('Accesses the demo page, successfully download all the schemas and example messages from the branch indicated in the config, verify presence of all required visual elements', () => {
    cy.visit('http://localhost:3000/')
    // Wait for the event listeners to get hooked up
    cy.hasEventListeners('#demo-login-button', { type: 'click' })
    // Go to demo page
    cy.get('#demo-login-button').click()
    cy.waitForResponse('**messagesList.json')
    // Wait for all schema-related fetches to complete with 200 status
    cy.waitForResponse('**/src/main/resources/json-schema/**')
    cy.waitForResponse('**/resources/sample/examples/**')

    // Verify visual presence of required elements
    cy.get('#vhost-selector').should('be.visible')
    cy.get('#message-type-tabs').should('be.visible')
    cy.get('#examples-list').should('be.visible')

    cy.iterateOverSchemasAndMessages()
  })
})

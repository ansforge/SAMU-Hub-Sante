import 'cypress-cdp'
describe('Json creator page spec', () => {
  it('Accesses the json creator, successfully download all the schemas and example messages from the branches available in the source elector, verify presence of all required visual elements', () => {
    cy.on('uncaught:exception', (err, runnable) => {
      // returning false here prevents Cypress from failing the test
      return false
    })
    cy.visit('http://localhost:3000/lrm')
    // Wait for the event listeners to get hooked up
    cy.hasEventListeners('[data-cy="json-creator-button"]', { type: 'click' })
    // Go to demo page
    cy.get('[data-cy="json-creator-button"]').as('jsonBtn')
    cy.get('@jsonBtn').click()
    // Wait for all schema-related fetches to complete with 200 status
    cy.waitForResponse('**messagesList.json')
    cy.waitForResponse('**/src/main/resources/json-schema/**')
    cy.waitForResponse('**/resources/sample/examples/**')

    // Verify visual presence of required elements
    cy.get('[data-cy="source-selector"]').parent().should('be.visible')
    cy.get('[data-cy="message-type-tabs"]').should('be.visible')
    cy.get('[data-cy="examples-list"]').should('be.visible')

    // Iterate over every message in every message type tab in every source and verify that the sent request succeeds
    // Click on combobox to open the dropdown menu
    cy.get('[data-cy="source-selector"]').parent().trigger('mousedown')
    cy.get('.v-overlay-container>div>div>div.v-list.v-list--one-line').children('.v-list-item:not(:last-child)').each(($source) => {
      // Click on each source
      // TODO: Find a way to properly test source selection by running .iterableOverSchemasAndMessages() for each source
      cy.get($source).trigger('mousedown')
    })

    cy.iterateOverSchemasAndMessages()
  })
})

import 'cypress-cdp'
describe('Demo page spec', () => {
  it('Accesses the demo page, successfully download all the schemas and example messages from the branch indicated in the config, verify presence of all required visual elements', () => {
    cy.visit('http://localhost:3000/')
    // Wait for the event listeners to get hooked up
    cy.hasEventListeners('#demo-login-button', { type: 'click' })
    // Go to demo page
    cy.get('#demo-login-button').click()
    // Wait for all schema-related fetches to complete with 200 status
    cy.waitForMessagesList()
    cy.waitForSchemas()
    cy.intercept('GET', '**/resources/sample/examples/**').as('getExample').then(() => cy.wait('@getExample'))

    // Verify visual presence of required elements
    cy.get('#vhost-selector').should('be.visible')
    cy.get('#message-type-tabs').should('be.visible')
    cy.get('#examples-list').should('be.visible')

    // Iterate over every message in every message type tab and verify that the sent request succeeds
    // First we click on each message type tab (first click doesn't do anything)
    cy.get('#message-type-tabs>div>div').children().each(($tab) => {
      // We only click on buttons
      if ($tab[0].tagName === 'BUTTON') {
        cy.get($tab).click()
      }
      // Now we click on each message (first click deselects the message selected by default, but it's not a problem since the related request is sent immediately on tab access)
      cy.get('.v-window-item--active>div>div>div>#examples-chips>div>div').children().each(($chip) => {
        cy.get($chip).click()
      })
    })
  })
})

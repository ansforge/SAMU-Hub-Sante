Cypress.Commands.add('waitForResponse', (alias) => {
  cy.intercept('GET', `${alias}`).as(`${alias}`).then(() => {
    cy.wait(`@${alias}`)
  })
})

/**
 * Iterate over every message in every message type tab and verify that the sent request succeeds
 */
Cypress.Commands.add('iterateOverSchemasAndMessages', () => {
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

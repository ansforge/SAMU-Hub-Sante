Cypress.Commands.add('waitForSchemas', () => {
  cy.intercept('GET', '**/src/main/resources/json-schema/**').as('schemas').then(() => {
    cy.wait('@schemas')
  })
})

Cypress.Commands.add('waitForMessagesList', () => {
  cy.intercept('GET', '**messagesList.json').as('messagesList').then(() => {
    cy.wait('@messagesList')
  })
})

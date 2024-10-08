Cypress.Commands.add('waitForResponse', (alias) => {
  cy.intercept('GET', `${alias}`).as(`${alias}`).then(() => {
    cy.wait(`@${alias}`)
  })
})

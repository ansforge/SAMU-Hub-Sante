/* eslint-disable no-undef */

describe('Demo page spec', () => {
  it('Accesses the demo page, successfully download all the schemas and example messages from the branch indicated in the config, verify presence of all required visual elements', () => {
    cy.on('uncaught:exception', (_err, _runnable) => {
      // returning false here prevents Cypress from failing the test
      return false;
    });

    // Intercept network requests for messages list, schemas, and examples
    cy.intercept('GET', '**/messagesList.json').as('messagesList');
    cy.intercept('GET', '**/src/main/resources/json-schema/**').as('schemas');
    cy.intercept('GET', '**/resources/sample/examples/**').as('examples');

    // Visit the LRM demo page
    cy.visit('http://localhost:3000/lrm');

    // Wait for the login button to be present and clickable
    cy.get('[data-cy="demo-login-button"]')
      .as('loginBtn')
      .should('be.visible')
      .and('not.be.disabled');
    cy.get('@loginBtn').click();

    // Wait for network requests to finish
    cy.wait('@messagesList');
    cy.wait('@schemas');
    cy.wait('@examples');

    // Verify visual presence of required elements
    cy.get('[data-cy="vhost-selector"]').should('be.visible');
    cy.get('[data-cy="message-type-tabs"]').should('be.visible');
    cy.get('[data-cy="examples-list"]').should('be.visible');

    cy.iterateOverSchemasAndMessages();
  });
});

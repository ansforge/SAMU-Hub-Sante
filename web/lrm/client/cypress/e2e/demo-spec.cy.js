/* eslint-disable no-undef */

describe('Demo page spec', () => {
  it('Accesses the demo page, successfully download all the schemas and example messages from the branch indicated in the config, verify presence of all required visual elements', () => {
    cy.on('uncaught:exception', (_err, _runnable) => {
      // returning false here prevents Cypress from failing the test
      return false;
    });

    // Intercept schema and example requests before visiting the page
    cy.intercept('GET', '**/messagesList.json').as('messagesList');
    cy.intercept('GET', '**/src/main/resources/json-schema/**').as(
      'jsonSchema'
    );
    cy.intercept('GET', '**/resources/sample/examples/**').as('examples');

    cy.visit('http://localhost:3000/lrm');
    cy.wait(1000); // Wait for initial page load

    // Go to demo page
    cy.get('[data-cy="demo-login-button"]').as('loginBtn');
    cy.get('@loginBtn').click();

    // Wait for all schema-related fetches to complete with 200 status
    cy.wait('@messagesList').its('response.statusCode').should('eq', 200);
    cy.wait('@jsonSchema').its('response.statusCode').should('eq', 200);
    cy.wait('@examples').its('response.statusCode').should('eq', 200);

    // Verify visual presence of required elements
    cy.get('[data-cy="vhost-selector"]').should('be.visible');
    cy.get('[data-cy="message-type-tabs"]').should('be.visible');
    cy.get('[data-cy="examples-list"]').should('be.visible');

    // If cy.iterateOverSchemasAndMessages() is a custom command using only Cypress, keep it.
    // Otherwise, replace with native Cypress logic as needed.
    cy.iterateOverSchemasAndMessages();
  });
});

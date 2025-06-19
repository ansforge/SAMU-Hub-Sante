/* eslint-disable no-undef */

describe('Json creator page spec', () => {
  it('Accesses the json creator, successfully download all the schemas and example messages from the branches available in the source elector, verify presence of all required visual elements', () => {
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
    cy.get('[data-cy="json-creator-button"]').as('jsonBtn');
    cy.get('@jsonBtn').click();

    // Wait for all schema-related fetches to complete with 200 status
    cy.wait('@messagesList').its('response.statusCode').should('eq', 200);
    cy.wait('@jsonSchema').its('response.statusCode').should('eq', 200);
    cy.wait('@examples').its('response.statusCode').should('eq', 200);

    // Verify visual presence of required elements
    cy.get('[data-cy="source-selector"]').parent().should('be.visible');
    cy.get('[data-cy="message-type-tabs"]').should('be.visible');
    cy.get('[data-cy="examples-list"]').should('be.visible');

    // Iterate over every message in every message type tab in every source and verify that the sent request succeeds
    // Click on combobox to open the dropdown menu
    cy.get('[data-cy="source-selector"]').parent().trigger('mousedown');
    cy.get('.v-overlay-container>div>div>div.v-list.v-list--one-line')
      .children('.v-list-item:not(:last-child)')
      .each(($source) => {
        // Click on each source
        cy.wrap($source).trigger('mousedown');
      });

    cy.iterateOverSchemasAndMessages();
  });
});

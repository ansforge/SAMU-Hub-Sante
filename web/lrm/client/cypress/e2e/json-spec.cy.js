/* eslint-disable no-undef */

describe('Json creator page spec', () => {
  it('Accesses the json creator, successfully download all the schemas and example messages from the branches available in the source elector, verify presence of all required visual elements', () => {
    cy.on('uncaught:exception', (_err, _runnable) => {
      // returning false here prevents Cypress from failing the test
      return false;
    });

    // Visit the LRM demo page
    cy.visit('http://localhost:3000/lrm');
    cy.wait(10000);

    // Wait for the json creator button to be present and clickable
    cy.get('[data-cy="json-creator-button"]')
      .as('jsonBtn')
      .should('be.visible')
      .and('not.be.disabled');
    cy.get('@jsonBtn').click();

    // Intercept network requests for messages list, schemas, and examples
    cy.intercept('GET', '**/messagesList.json').as('messagesList');
    cy.intercept('GET', '**/src/main/resources/json-schema/**').as('schemas');
    cy.intercept('GET', '**/resources/sample/examples/**').as('examples');

    // Wait for network requests to finish
    cy.wait('@messagesList');
    cy.wait('@schemas');
    cy.wait('@examples');

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
        // TODO: Find a way to properly test source selection by running .iterableOverSchemasAndMessages() for each source
        cy.wrap($source).trigger('mousedown');
      });

    cy.iterateOverSchemasAndMessages();
  });
});

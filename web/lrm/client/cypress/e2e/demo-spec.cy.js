import 'cypress-cdp';
import { describe, it, cy } from 'cypress';

describe('Demo page spec', () => {
  it('Accesses the demo page, successfully download all the schemas and example messages from the branch indicated in the config, verify presence of all required visual elements', () => {
    cy.on('uncaught:exception', (_err, _runnable) => {
      // returning false here prevents Cypress from failing the test
      return false;
    });
    cy.visit('http://localhost:3000/lrm');
    // Arbitrary wait to avoid chrome's reloading behavior breaking the tests
    cy.wait(5000);
    // Wait for the event listeners to get hooked up
    cy.hasEventListeners('[data-cy="demo-login-button"]', { type: 'click' });
    // Go to demo page
    cy.get('[data-cy="demo-login-button"]').as('loginBtn');
    cy.get('@loginBtn').click();
    cy.waitForResponse('**messagesList.json');
    // Wait for all schema-related fetches to complete with 200 status
    cy.waitForResponse('**/src/main/resources/json-schema/**');
    cy.waitForResponse('**/resources/sample/examples/**');
    cy.wait(5000);

    // Verify visual presence of required elements
    cy.get('[data-cy="vhost-selector"]').should('be.visible');
    cy.get('[data-cy="message-type-tabs"]').should('be.visible');
    cy.get('[data-cy="examples-list"]').should('be.visible');

    cy.iterateOverSchemasAndMessages();
  });
});

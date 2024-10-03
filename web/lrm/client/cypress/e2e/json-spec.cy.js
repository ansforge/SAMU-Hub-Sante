import 'cypress-cdp'
describe('Json creator page spec', () => {
  it('Accesses the json creator, successfully download all the schemas and example messages from the branches available in the source elector, verify presence of all required visual elements', () => {
    cy.visit('http://localhost:3000/')
    // Wait for the event listeners to get hooked up
    cy.hasEventListeners('#json-creator-button', { type: 'click' })
    // Go to demo page
    cy.get('#json-creator-button').click()
    // Wait for all schema-related fetches to complete with 200 status
    cy.waitForMessagesList()
    cy.waitForSchemas()
    cy.intercept('GET', '**/resources/sample/examples/**').as('getExample').then(() => cy.wait('@getExample'))

    // Verify visual presence of required elements
    cy.get('#source-selector').parent().should('be.visible')
    cy.get('#message-type-tabs').should('be.visible')
    cy.get('#examples-list').should('be.visible')

    // Iterate over every message in every message type tab in every source and verify that the sent request succeeds
    // Click on combobox to open the dropdown menu
    cy.get('#source-selector').parent().trigger('mousedown')
    cy.get('.v-overlay-container>div>div>div.v-list.v-list--one-line').children('.v-list-item:not(:last-child)').each(($source) => {
      // Click on each source
      cy.get($source).trigger('mousedown')
    })
    // Get the list of branches from the generated overlay element
    // <div class="v-list v-theme--light v-list--density-default v-list--one-line" tabindex="-1" role="listbox" aria-live="polite"><!----><!----><div class="v-virtual-scroll__spacer" style="padding-top: 0px;"></div><div class="v-list-item v-list-item--active v-list-item--link v-theme--light v-list-item--density-default v-list-item--one-line rounded-0 v-list-item--variant-text" tabindex="-2" role="option"><span class="v-list-item__overlay"></span><span class="v-list-item__underlay"></span><div class="v-list-item__prepend"><!----><!----><!----><div class="v-list-item__spacer"></div></div><div class="v-list-item__content" data-no-activator=""><div class="v-list-item-title">feature/lrm/test-interface-rework</div><!----><!----></div><!----></div><div class="v-list-item v-list-item--link v-theme--light v-list-item--density-default v-list-item--one-line rounded-0 v-list-item--variant-text" tabindex="-2" role="option"><span class="v-list-item__overlay"></span><span class="v-list-item__underlay"></span><div class="v-list-item__prepend"><!----><!----><!----><div class="v-list-item__spacer"></div></div><div class="v-list-item__content" data-no-activator=""><div class="v-list-item-title">main</div><!----><!----></div><!----></div><div class="v-list-item v-list-item--link v-theme--light v-list-item--density-default v-list-item--one-line rounded-0 v-list-item--variant-text" tabindex="-2" role="option"><span class="v-list-item__overlay"></span><span class="v-list-item__underlay"></span><div class="v-list-item__prepend"><!----><!----><!----><div class="v-list-item__spacer"></div></div><div class="v-list-item__content" data-no-activator=""><div class="v-list-item-title">develop</div><!----><!----></div><!----></div><div class="v-list-item v-list-item--link v-theme--light v-list-item--density-default v-list-item--one-line rounded-0 v-list-item--variant-text" tabindex="-2" role="option"><span class="v-list-item__overlay"></span><span class="v-list-item__underlay"></span><div class="v-list-item__prepend"><!----><!----><!----><div class="v-list-item__spacer"></div></div><div class="v-list-item__content" data-no-activator=""><div class="v-list-item-title">auto/model_tracker</div><!----><!----></div><!----></div><div class="v-list-item v-list-item--link v-theme--light v-list-item--density-default v-list-item--one-line rounded-0 v-list-item--variant-text" tabindex="-2" role="option"><span class="v-list-item__overlay"></span><span class="v-list-item__underlay"></span><div class="v-list-item__prepend"><!----><!----><!----><div class="v-list-item__spacer"></div></div><div class="v-list-item__content" data-no-activator=""><div class="v-list-item-title">{branchName}</div><!----><!----></div><!----></div><div class="v-virtual-scroll__spacer" style="padding-bottom: 0px;"></div><!----></div>
    cy.get('.v-list.v-list--one-line').children().each(($branch) => {
      cy.log($branch[0].innerText)
    })
    // We click on each message type tab (first click doesn't do anything)
    cy.get('#message-type-tabs>div>div').children().each(($tab) => {
      // We only click on buttons
      if ($tab[0].tagName === 'BUTTON') {
        cy.get($tab).click()
      }
      // We click on each message (first click deselects the message selected by default, but it's not a problem since the related request is sent immediately on tab access)
      cy.get('.v-window-item--active>div>div>div>#examples-chips>div>div').children().each(($chip) => {
        cy.get($chip).click()
      })
    })
  })
})

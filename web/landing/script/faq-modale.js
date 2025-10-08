// Custom script to open the question when the anchor is matching its ID
// Function to check if the anchor in the URL matches the h3 id
function clickButtonOnAnchorMatch() {
    // Get the current anchor from the URL (e.g., #technologie)
    const anchor = window.location.hash.substring(1); // Remove the '#' symbol

    // Find the h3 element by ID
    const h3Element = document.querySelector(`h3#${anchor}`);

    // If the h3 element exists and has a button inside it
    if (h3Element) {
        const button = h3Element.querySelector('button');
        if (button) {
            // Simulate a click on the button
            button.click();
        }
    }
}

// Run the function when the page loads
window.addEventListener('DOMContentLoaded', clickButtonOnAnchorMatch);
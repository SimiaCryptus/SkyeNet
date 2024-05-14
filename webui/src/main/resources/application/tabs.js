function updateTabs() {
    document.querySelectorAll('.tab-button').forEach(button => {
        button.addEventListener('click', (event) => { // Ensure the event is passed as a parameter
            event.stopPropagation();
            const forTab = button.getAttribute('data-for-tab');
            let tabsParent = button.closest('.tabs-container');
            tabsParent.querySelectorAll('.tab-button').forEach(tabButton => {
                if (tabButton.closest('.tabs-container') === tabsParent) tabButton.classList.remove('active')
            });
            button.classList.add('active');
            let selectedContent = null;
            tabsParent.querySelectorAll('.tab-content').forEach(content => {
                if (content.closest('.tabs-container') === tabsParent) {
                    if (content.getAttribute('data-tab') === forTab) {
                        content.classList.add('active');
                        content.style.display = 'block'; // Ensure the content is displayed
                        selectedContent = content;
                    } else {
                        content.classList.remove('active')
                        content.style.display = 'none'; // Ensure the content is hidden
                    }
                }
            });
            if (selectedContent !== null) updateNestedTabs(selectedContent);
        })
    });
}

function updateNestedTabs(element) {
    element.querySelectorAll('.tabs-container').forEach(tabsContainer => {
        try {
            let hasActiveButton = false;
            tabsContainer.querySelectorAll('.tab-button').forEach(nestedButton => {
                if (nestedButton.classList.contains('active')) {
                    hasActiveButton = true;
                }
            });
            if (!hasActiveButton) {
                /* Determine if a tab-content element in this tabs-container has the active class. If so, use its data-tab value to find the matching button and ensure it is marked active */
                const activeContent = tabsContainer.querySelector('.tab-content.active');
                if (activeContent) {
                    const activeTab = activeContent.getAttribute('data-tab');
                    const activeButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${activeTab}"]`);
                    if (activeButton !== null) {
                        activeButton.classList.add('active');
                    }
                } else {
                    /* Add 'active' to the class list of the first button */
                    const firstButton = tabsContainer.querySelector('.tab-button');
                    if (firstButton !== null) {
                        firstButton.classList.add('active');
                    }
                }
            }
        } catch (e) {
            console.log("Error updating tabs: " + e);
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    updateTabs();
    updateNestedTabs(document);
});

window.updateTabs = updateTabs; // Expose updateTabs to the global scope
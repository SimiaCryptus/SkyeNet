function updateTabs() {
    document.querySelectorAll('.tab-button').forEach(button => {
        button.addEventListener('click', (event) => { // Ensure the event is passed as a parameter
            event.stopPropagation();
            const forTab = button.getAttribute('data-for-tab');
            let tabsParent = button.closest('.tabs-container');
            tabsParent.querySelectorAll('.tab-button').forEach(tabButton => tabButton.classList.remove('active'));
            button.classList.add('active');
            tabsParent.querySelectorAll('.tab-content').forEach(content => {
                const contentParent = content.closest('.tabs-container');
                if (contentParent === tabsParent) {
                    if (content.getAttribute('data-tab') === forTab) {
                        content.classList.add('active');
                       content.style.display = 'block'; // Ensure the content is displayed
                    } else {
                        content.classList.remove('active')
                       content.style.display = 'none'; // Ensure the content is hidden
                    }
                }
            });
           // Ensure nested tabs are updated
           updateNestedTabs(tabsParent);
        })
    });
}

function updateNestedTabs(tabsParent) {
    tabsParent.querySelectorAll('.tab-content .tabs-container').forEach(nestedTabsContainer => {
        const firstNestedButton = nestedTabsContainer.querySelector('.tab-button');
        if (firstNestedButton) {
            firstNestedButton.click();
        }
    });
}
document.addEventListener('DOMContentLoaded', () => {
    try {
        updateTabs();
       // Set the initial active tab
       document.querySelectorAll('.tabs-container').forEach(tabsContainer => {
           const firstButton = tabsContainer.querySelector('.tab-button');
           if (firstButton) {
               firstButton.click();
           }
       });
    } catch (e) {
        console.log("Error updating tabs: " + e);
    }
});

window.updateTabs = updateTabs; // Expose updateTabs to the global scope
const observer = new MutationObserver(updateTabs);
const observerOptions = {childList: true, subtree: true};
const tabCache = new Map();

function updateTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    tabButtons.forEach(button => {
        if (button.hasListener) return;
        button.hasListener = true;
        // console.log(`Adding click event listener to tab button: ${button.getAttribute('data-for-tab')}`);
        button.addEventListener('click', (event) => {
            // console.log(`Tab button clicked: ${button.getAttribute('data-for-tab')}`);
            event.stopPropagation();
            const forTab = button.getAttribute('data-for-tab');
            const tabsContainerId = button.closest('.tabs-container').id;
            // console.log(`Tabs container ID: ${tabsContainerId}`);
            // console.log(`Saving selected tab to localStorage: selectedTab_${tabsContainerId} = ${forTab}`);
            try {
                localStorage.setItem(`selectedTab_${tabsContainerId}`, forTab);
            } catch (e) {
                console.warn('Failed to save tab state to localStorage:', e);
            }
            let tabsParent = button.closest('.tabs-container');
            const tabButtons = tabsParent.querySelectorAll('.tab-button');
            for (let i = 0; i < tabButtons.length; i++) {
                if (tabButtons[i].closest('.tabs-container') === tabsParent) {
                    tabButtons[i].classList.remove('active');
                }
            }
            button.classList.add('active');
            // console.log(`Active tab set to: ${forTab}`);
            let selectedContent = null;
            const tabContents = tabsParent.querySelectorAll('.tab-content');
            for (let i = 0; i < tabContents.length; i++) {
                const content = tabContents[i];
                if (content.closest('.tabs-container') !== tabsParent) continue;
                if (content.getAttribute('data-tab') === forTab) {
                    content.classList.add('active');
                    content.style.display = 'block'; // Ensure the content is displayed
                    // console.log(`Content displayed for tab: ${forTab}`);
                    selectedContent = content;
                } else {
                    content.classList.remove('active');
                    content.style.display = 'none'; // Ensure the content is hidden
                    // console.log(`Content hidden for tab: ${content.getAttribute('data-tab')}`);
                }
            }
            if (selectedContent !== null) {
                requestAnimationFrame(() => updateNestedTabs(selectedContent));
            }
        });
        // Check if the current button should be activated based on localStorage
        const savedTab = getSavedTab(button.closest('.tabs-container').id);
        if (button.getAttribute('data-for-tab') === savedTab) {
            button.dispatchEvent(new Event('click'));
        }
    });
}

function getSavedTab(containerId) {
    if (tabCache.has(containerId)) {
        return tabCache.get(containerId);
    }
    try {
        const savedTab = localStorage.getItem(`selectedTab_${containerId}`);
        tabCache.set(containerId, savedTab);
        return savedTab;
    } catch (e) {
        console.warn('Failed to retrieve saved tab from localStorage:', e);
        return null;
    }
}

function updateNestedTabs(element) {
    const tabsContainers = element.querySelectorAll('.tabs-container');
    for (let i = 0; i < tabsContainers.length; i++) {
        const tabsContainer = tabsContainers[i];
        try {
            // console.log(`Updating nested tabs for container: ${tabsContainer.id}`);
            let hasActiveButton = false;
            const nestedButtons = tabsContainer.querySelectorAll('.tab-button');
            for (let j = 0; j < nestedButtons.length; j++) {
                const nestedButton = nestedButtons[j];
            }
            if (nestedButton.classList.contains('active')) {
                hasActiveButton = true;
                // console.log(`Found active nested button: ${nestedButton.getAttribute('data-for-tab')}`);
            }
            if (!hasActiveButton) {
                /* Determine if a tab-content element in this tabs-container has the active class. If so, use its data-tab value to find the matching button and ensure it is marked active */
                const activeContent = tabsContainer.querySelector('.tab-content.active');
                if (activeContent) {
                    const activeTab = activeContent.getAttribute('data-tab');
                    const activeButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${activeTab}"]`);
                    if (activeButton !== null) {
                        activeButton.classList.add('active');
                        // console.log(`Set active nested button: ${activeTab}`);
                    }
                } else {
                    /* Add 'active' to the class list of the first button */
                    const firstButton = tabsContainer.querySelector('.tab-button');
                    if (firstButton !== null) {
                        firstButton.classList.add('active');
                        // console.log(`Set first nested button as active: ${firstButton.getAttribute('data-for-tab')}`);
                    }
                }
            }
            const savedTab = getSavedTab(tabsContainer.id);
            // console.log(`Retrieved saved tab from localStorage: selectedTab_${tabsContainer.id} = ${savedTab}`);
            if (savedTab) {
                const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
                if (savedButton) {
                    savedButton.classList.add('active');
                    const forTab = savedButton.getAttribute('data-for-tab');
                    const selectedContent = tabsContainer.querySelector(`.tab-content[data-tab="${forTab}"]`);
                    if (selectedContent) {
                        selectedContent.classList.add('active');
                        selectedContent.style.display = 'block';
                    }
                    // console.log(`Restored saved tab: ${savedTab}`);
                }
            }
        } catch (e) {
            // console.log("Error updating tabs: " + e);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // console.log('Document loaded. Initializing tabs...');
    updateTabs();
    updateNestedTabs(document);
    observer.observe(document.body, observerOptions);

});
window.addEventListener('beforeunload', () => {
    observer.disconnect();
});

window.updateTabs = updateTabs; // Expose updateTabs to the global scope
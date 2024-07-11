const observer = new MutationObserver(updateTabs);
const observerOptions = {childList: true, subtree: true};
const tabCache = new Map();

export function updateTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabsContainers = new Set();
    tabButtons.forEach(button => {
        const tabsContainer = button.closest('.tabs-container');
        tabsContainers.add(tabsContainer);
        if (button.hasListener) return;
        button.hasListener = true;
        // console.log(`Adding click event listener to tab button: ${button.getAttribute('data-for-tab')}, button element:`, button);
        button.addEventListener('click', (event) => {
            // console.log(`Tab button clicked: ${button.getAttribute('data-for-tab')}, event:`, event);
            event.stopPropagation();
            const forTab = button.getAttribute('data-for-tab');
            const tabsContainerId = button.closest('.tabs-container').id;
            // console.log(`Tabs container ID: ${tabsContainerId}, button:`, button);
            // console.log(`Saving selected tab to localStorage: selectedTab_${tabsContainerId} = ${forTab}, button:`, button);
            try {
                localStorage.setItem(`selectedTab_${tabsContainerId}`, forTab);
                tabCache.set(tabsContainerId, forTab); // Update the cache
            } catch (e) {
                console.warn('Failed to save tab state to localStorage:', e);
            }
            let tabsParent = button.closest('.tabs-container');
            const tabButtons = tabsParent.querySelectorAll('.tab-button');
            tabButtons.forEach(btn => {
                if (btn.closest('.tabs-container') === tabsParent) {
                    btn.classList.remove('active');
                }
            });
            button.classList.add('active');
            // console.log(`Active tab set to: ${forTab}, button:`, button);
            let selectedContent = null;
            const tabContents = tabsParent.querySelectorAll('.tab-content');
            tabContents.forEach(content => {
                if (content.closest('.tabs-container') !== tabsParent) return;
                if (content.getAttribute('data-tab') === forTab) {
                    content.classList.add('active');
                    content.style.display = 'block'; // Ensure the content is displayed
                    // console.log(`Content displayed for tab: ${forTab}, content element:`, content);
                    selectedContent = content;
                } else {
                    content.classList.remove('active');
                    content.style.display = 'none'; // Ensure the content is hidden
                    // console.log(`Content hidden for tab: ${content.getAttribute('data-tab')}, content element:`, content);
                }
            });
            if (selectedContent !== null) {
                requestAnimationFrame(() => updateNestedTabs(selectedContent));
            }
        });
    });

    // Restore the selected tabs from localStorage
    tabsContainers.forEach(tabsContainer => {
        const savedTab = getSavedTab(tabsContainer.id);
        if (savedTab) {
            const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
            if (savedButton) {
                savedButton.click(); // Simulate a click to activate the tab
                // console.log(`Restored saved tab: ${savedTab}`);
            }
        } else {
            tabsContainer.querySelector('.tab-button')?.click(); // Activate the first tab
        }
    });
}

export function restoreTabs() {
    // Restore the selected tabs from localStorage before adding event listeners
    document.querySelectorAll('.tabs-container').forEach(tabsContainer => {
        const savedTab = localStorage.getItem(`selectedTab_${tabsContainer.id}`);
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
                console.log(`Restored saved tab: ${savedTab}`);
            }
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
     tabsContainers.forEach(tabsContainer => {
         try {
             let hasActiveButton = false;
             const nestedButtons = tabsContainer.querySelectorAll('.tab-button');
             nestedButtons.forEach(nestedButton => {
                 // console.log(`Checking nested button: ${nestedButton.getAttribute('data-for-tab')}, nestedButton element:`, nestedButton);
                 if (nestedButton.classList.contains('active')) {
                     hasActiveButton = true;
                 }
             });
             if (!hasActiveButton) {
                 const activeContent = tabsContainer.querySelector('.tab-content.active');
                 if (activeContent) {
                     const activeTab = activeContent.getAttribute('data-tab');
                     const activeButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${activeTab}"]`);
                     if (activeButton) {
                         activeButton.click(); // Simulate a click to activate the tab
                     }
                 } else {
                     tabsContainer.querySelector('.tab-button')?.click(); // Activate the first tab
                 }
             }
             const savedTab = getSavedTab(tabsContainer.id);
             if (savedTab) {
                 const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
                 if (savedButton) {
                    if (!savedButton.classList.contains('active')) {
                        savedButton.click(); // Simulate a click to activate the tab only if it's not already active
                    }
                 }
             }
         } catch (e) {
             console.warn('Failed to update nested tabs:', e);
         }
     });
 }

document.addEventListener('DOMContentLoaded', () => {
    updateTabs();
    observer.observe(document.body, observerOptions);

});
window.addEventListener('beforeunload', () => {
    observer.disconnect();
});

window.updateTabs = updateTabs; // Expose updateTabs to the global scope
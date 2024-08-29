const observer = new MutationObserver(updateTabs);
const observerOptions = {childList: true, subtree: true};
const tabCache = new Map();
let isRestoringTabs = false;

export function updateTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabsContainers = new Set();
    tabButtons.forEach(button => {
        const tabsContainer = button.closest('.tabs-container');
        tabsContainers.add(tabsContainer);
        if (button.hasListener) return;
        button.hasListener = true;
        button.addEventListener('click', (event) => {
            event.stopPropagation();
            const forTab = button.getAttribute('data-for-tab');
            let tabsContainerId = button.closest('.tabs-container').id;
            if (button.classList.contains('active')) return; // Skip if already active
            try {
                localStorage.setItem(`selectedTab_${tabsContainerId}`, forTab);
                tabCache.set(tabsContainerId, forTab); // Update the cache
            } catch (e) {
                console.warn('Failed to save tab state to localStorage:', e);
            }
            let tabsParent = button.closest('.tabs-container');
            const allTabButtons = tabsParent.querySelectorAll('.tab-button');
            allTabButtons.forEach(btn => {
                btn.classList.remove('active');
            });
            button.classList.add('active');
            // console.log(`Active tab set to: ${forTab}, button:`, button);
            let selectedContent = null;
            const tabContents = tabsParent.querySelectorAll('.tab-content');
            tabContents.forEach(content => {
                if (content.getAttribute('data-tab') === forTab) {
                    content.classList.add('active');
                    content.style.display = 'block'; // Ensure the content is displayed
                    selectedContent = content;
                    // Recursively update nested tabs
                    updateNestedTabs(selectedContent);
                } else {
                    content.classList.remove('active');
                    content.style.display = 'none'; // Hide the content instead of removing it
                }
            });
        });
    });

    // Restore the selected tabs from localStorage
    isRestoringTabs = true;
    try {
        tabsContainers.forEach(tabsContainer => {
            const savedTab = getSavedTab(tabsContainer.id);
            if (savedTab) {
                const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
                if (savedButton) {
                    savedButton.click(); // Activate the tab
                }
            } else {
                tabsContainer.querySelector('.tab-button')?.click(); // Activate the first tab
            }
        });
    } finally {
        isRestoringTabs = false;
    }
}

export function restoreTabs() {
    isRestoringTabs = true;
    // Restore the selected tabs from localStorage before adding event listeners
    try {
        document.querySelectorAll('.tabs-container').forEach(tabsContainer => {
            const savedTab = localStorage.getItem(`selectedTab_${tabsContainer.id}`);
            if (savedTab) {
                const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
                tabsContainer.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));
                if (savedButton) {
                    savedButton.classList.add('active');
                    const forTab = savedButton.getAttribute('data-for-tab');
                    const selectedContent = tabsContainer.querySelector(`.tab-content[data-tab="${forTab}"]`);
                    if (selectedContent) {
                        selectedContent.classList.add('active');
                        selectedContent.style.display = 'block'; // Ensure the content is displayed
                        updateNestedTabs(selectedContent);
                    }
                    // Hide other tab contents
                    tabsContainer.querySelectorAll(`.tab-content:not([data-tab="${forTab}"])`).forEach(content => {
                        content.style.display = 'none';
                    });
                } else {
                    tabsContainer.querySelector('.tab-button')?.click(); // Activate the first tab if no saved tab
                }
            }
        });
    } finally {
        isRestoringTabs = false;
    }
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
            tabsContainer.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));
            let hasActiveButton = false;
            const nestedButtons = tabsContainer.querySelectorAll('.tab-button');
            nestedButtons.forEach(nestedButton => {
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
                        setTimeout(() => activeButton.click(), 0); // Activate the tab asynchronously
                    }
                } else {
                    setTimeout(() => tabsContainer.querySelector('.tab-button')?.click(), 0); // Activate the first tab asynchronously
                }
            }
             const savedTab = getSavedTab(tabsContainer.id);
            // console.log(`Saved tab for container ${tabsContainer.id}: ${savedTab}`);
             if (savedTab) {
                 const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
                 if (savedButton) {
                     if (!savedButton.classList.contains('active')) {
                         setTimeout(() => savedButton.click(), 0); // Activate the tab only if it's not already active, asynchronously
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
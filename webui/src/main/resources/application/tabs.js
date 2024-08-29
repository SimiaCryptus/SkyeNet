const observer = new MutationObserver(updateTabs);
const observerOptions = {childList: true, subtree: true};
const tabCache = new Map();
let isRestoringTabs = false;

export function updateTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabsContainers = new Set();
    const clickHandler = (event) => {
        event.stopPropagation();
        const button = event.currentTarget;
        const forTab = button.getAttribute('data-for-tab');
        const tabsContainer = button.closest('.tabs-container');
        const tabsContainerId = tabsContainer.id;
        if (button.classList.contains('active')) return;
        try {
            localStorage.setItem(`selectedTab_${tabsContainerId}`, forTab);
            tabCache.set(tabsContainerId, forTab);
        } catch (e) {
            console.warn('Failed to save tab state to localStorage:', e);
        }
        const buttons = tabsContainer.querySelectorAll('.tab-button');
        for (let i = 0; i < buttons.length; i++) {
            buttons[i].classList.remove('active');
        }
        button.classList.add('active');
        const contents = tabsContainer.querySelectorAll('.tab-content');
        for (let i = 0; i < contents.length; i++) {
            const content = contents[i];
            if (content.getAttribute('data-tab') === forTab) {
                content.classList.add('active');
                content.style.display = 'block';
                updateNestedTabs(content);
            } else {
                content.classList.remove('active');
                content.style.display = 'none';
            }
        }
    };

    tabButtons.forEach(button => {
        const tabsContainer = button.closest('.tabs-container');
        tabsContainers.add(tabsContainer);
        if (button.hasListener) return;
        button.hasListener = true;
        button.addEventListener('click', clickHandler);
    });

    // Restore the selected tabs from localStorage
    if (!isRestoringTabs) {
        isRestoringTabs = true;
        tabsContainers.forEach(tabsContainer => {
            requestAnimationFrame(() => {
                const savedTab = getSavedTab(tabsContainer.id);
                const buttonToActivate = savedTab
                    ? tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`)
                    : tabsContainer.querySelector('.tab-button');
                buttonToActivate?.click();
            });
        });
        isRestoringTabs = false;
    }
}

// Remove the restoreTabs function as it's now redundant with the improved updateTabs function

function getSavedTab(containerId) {
    if (tabCache.has(containerId)) return tabCache.get(containerId);
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
    for (const tabsContainer of tabsContainers) {
        try {
            const buttons = tabsContainer.querySelectorAll('.tab-button');
            let hasActiveButton = false;
            for (let i = 0; i < buttons.length; i++) {
                const btn = buttons[i];
                if (btn.classList.contains('active')) {
                    hasActiveButton = true;
                } else {
                    btn.classList.remove('active');
                }
            }
            if (!hasActiveButton) {
                const activeContent = tabsContainer.querySelector('.tab-content.active');
                const activeTab = activeContent?.getAttribute('data-tab');
                const buttonToActivate = activeTab
                    ? tabsContainer.querySelector(`.tab-button[data-for-tab="${activeTab}"]`)
                    : tabsContainer.querySelector('.tab-button');
                if (buttonToActivate) requestAnimationFrame(() => buttonToActivate.click());
            }
            const savedTab = getSavedTab(tabsContainer.id);
            const savedButton = savedTab && tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
            if (savedButton && !savedButton.classList.contains('active')) requestAnimationFrame(() => savedButton.click());
        } catch (e) {
            console.warn('Failed to update nested tabs:', e);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    updateTabs();
    observer.observe(document.body, observerOptions);
});

window.addEventListener('beforeunload', () => {
    observer.disconnect();
});
const observer = new MutationObserver(updateTabs);
const observerOptions = {childList: true, subtree: true};
const tabCache = new Map();
let isRestoringTabs = false;
const MAX_RECURSION_DEPTH = 10;
const OPERATION_TIMEOUT = 5000; // 5 seconds
function setActiveTab(button, tabsContainer, depth = 0) {
    const forTab = button.getAttribute('data-for-tab');
    const tabsContainerId = tabsContainer.id;
    if (button.classList.contains('active')) return;
    try {
        localStorage.setItem(`selectedTab_${tabsContainerId}`, forTab);
        tabCache.set(tabsContainerId, forTab);
    } catch (e) {
        console.warn('Failed to save tab state to localStorage:', e);
    }
    tabsContainer.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));
    button.classList.add('active');
    tabsContainer.querySelectorAll('.tab-content').forEach(content => {
        if (content.getAttribute('data-tab') === forTab) {
            content.classList.add('active');
            content.style.display = 'block';
            updateNestedTabs(content, depth + 1);
        } else {
            content.classList.remove('active');
            content.style.display = 'none';
        }
    });
}

export function updateTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabsContainers = new Set();

    tabButtons.forEach(button => {
        const tabsContainer = button.closest('.tabs-container');
        tabsContainers.add(tabsContainer);
    });

    tabsContainers.forEach(tabsContainer => {
        if (tabsContainer.hasListener) return;
        tabsContainer.hasListener = true;
        tabsContainer.addEventListener('click', (event) => {
            const button = event.target.closest('.tab-button');
            if (button && tabsContainer.contains(button)) {
                setActiveTab(button, tabsContainer, 0);
                event.stopPropagation();
            }
        });
    });

    // Restore the selected tabs from localStorage
    if (!isRestoringTabs) {
        isRestoringTabs = true;
        tabsContainers.forEach(tabsContainer => {
            requestAnimationFrame(() => {
                const savedTab = getSavedTab(tabsContainer.id);
                const buttonToActivate = savedTab
                    ? tabsContainer.querySelector(`.tab-button[data-for-tab="${CSS.escape(savedTab)}"]`)
                    : tabsContainer.querySelector('.tab-button');
                if (buttonToActivate) {
                    buttonToActivate.click();
                }
            });
        });
        isRestoringTabs = false;
    }
}

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

function updateNestedTabs(element, depth = 0) {
    if (depth >= MAX_RECURSION_DEPTH) {
        console.warn('Max recursion depth reached in updateNestedTabs');
        return;
    }

    const tabsContainers = element.querySelectorAll('.tabs-container');
    const timeoutId = setTimeout(() => console.warn('updateNestedTabs operation timed out'), OPERATION_TIMEOUT);
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
                const savedTab = getSavedTab(tabsContainer.id);
                const savedButton = savedTab
                    ? tabsContainer.querySelector(`.tab-button[data-for-tab="${CSS.escape(savedTab)}"]`)
                    : null;
                if (savedButton && !savedButton.classList.contains('active')) {
                    requestAnimationFrame(() => savedButton.click());
                }
            }
        } catch (e) {
            console.warn('Failed to update nested tabs:', e);
        }
        clearTimeout(timeoutId);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    updateTabs();
    observer.observe(document.body, observerOptions);
});

window.addEventListener('beforeunload', () => {
    observer.disconnect();
});
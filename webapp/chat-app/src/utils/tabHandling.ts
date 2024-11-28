const LOG_PREFIX = '[TabHandler]';

interface TabState {
    containerId: string;
    activeTab: string;
}

// Define TabContainer interface
interface TabContainer extends HTMLElement {
    id: string;
    hasListener?: boolean;
    tabClickHandler?: (event: Event) => void;
}

// Add debounce utility to prevent multiple rapid updates
function debounce<T extends (...args: any[]) => void>(func: T, wait: number) {
    let timeout: NodeJS.Timeout;
    return function executedFunction(this: any, ...args: Parameters<T>) {
        const later = () => {
            clearTimeout(timeout);
            func.apply(this, args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}


const tabStates = new Map<string, TabState>();
const processedTabs = new Set<string>();
// Track active mutations to prevent infinite loops
let isMutating = false;


// Move helper functions to module scope
function saveTabState(containerId: string, activeTab: string) {
    try {
        tabStates.set(containerId, {containerId, activeTab});
        localStorage.setItem(`tab_state_${containerId}`, activeTab);
        console.log(`${LOG_PREFIX} Saved tab state:`, {containerId, activeTab});
    } catch (error) {
        console.warn(`${LOG_PREFIX} Failed to save tab state:`, error);
    }
}

function updateNestedTabs(element: HTMLElement) {
    const nestedContainers = element.querySelectorAll('.tabs-container');
    nestedContainers.forEach(container => {
        if (container instanceof HTMLElement) {
            setupTabContainer(container as TabContainer);
            restoreTabState(container as TabContainer);
        }
    });
}

// Move setActiveTab to module scope
function setActiveTab(button: HTMLElement, container: HTMLElement) {
    const forTab = button.getAttribute('data-for-tab');
    if (!forTab) return;
    console.log(`${LOG_PREFIX} Setting active tab:`, {
        containerId: container.id,
        tab: forTab
    });
    // Save tab state
    saveTabState(container.id, forTab);
    // Update UI
    container.querySelectorAll('.tab-button').forEach(btn =>
        btn.classList.remove('active')
    );
    button.classList.add('active');
    container.querySelectorAll('.tab-content').forEach(content => {
        if (content.getAttribute('data-tab') === forTab) {
            content.classList.add('active');
            (content as HTMLElement).style.display = 'block';
            // Update nested tabs
            updateNestedTabs(content as HTMLElement);
        } else {
            content.classList.remove('active');
            (content as HTMLElement).style.display = 'none';
        }
    });
}

// Declare restoreTabState at module level
function restoreTabState(container: TabContainer) {
    try {
        const containerId = container.id;
        const savedTab = localStorage.getItem(`tab_state_${containerId}`) ||
            tabStates.get(containerId)?.activeTab;
        if (savedTab) {
            const button = container.querySelector(
                `.tab-button[data-for-tab="${savedTab}"]`
            ) as HTMLElement;
            if (button) {
                console.log(`${LOG_PREFIX} Restoring tab state:`, {
                    containerId,
                    savedTab
                });
                setActiveTab(button, container);
            }
        } else {
            // Set first tab as active by default
            const firstButton = container.querySelector('.tab-button') as HTMLElement;
            if (firstButton) {
                setActiveTab(firstButton, container);
            }
        }
    } catch (error) {
        console.warn(`${LOG_PREFIX} Failed to restore tab state:`, error);
    }
}


export const updateTabs = debounce(() => {
    if (isMutating) {
        console.debug(`${LOG_PREFIX} Skipping update during mutation`);
        return;
    }
    isMutating = true;
    console.log(`${LOG_PREFIX} Updating tabs...`);

    const tabButtons = document.querySelectorAll('.tab-button');
    const tabsContainers = new Set<TabContainer>();

    tabButtons.forEach(button => {
        const tabsContainer = button.closest('.tabs-container') as TabContainer;
        if (tabsContainer && !processedTabs.has(tabsContainer.id)) {
            tabsContainers.add(tabsContainer);
            processedTabs.add(tabsContainer.id);
            setupTabContainer(tabsContainer);
        }
    });

    // Restore saved tab states
    tabsContainers.forEach(container => {
        restoreTabState(container);
    });
    isMutating = false;
}, 100);

// Add cleanup function to reset state
export function resetTabState() {
    processedTabs.clear();
    tabStates.clear();
    isMutating = false;
}

function setupTabContainer(container: TabContainer) {
    if (!container.id) {
        container.id = `tab-container-${Math.random().toString(36).substr(2, 9)}`;
    }

    console.log(`${LOG_PREFIX} Setting up tab container:`, container.id);
    // Remove existing listener if present to prevent duplicates
    if (container.hasListener) {
        if (container.tabClickHandler) {
            container.removeEventListener('click', container.tabClickHandler);
        }
    }

    // Create and store click handler with proper type
    container.tabClickHandler = (event: Event) => {
        const button = (event.target as HTMLElement).closest('.tab-button');
        if (button && container.contains(button)) {
            setActiveTab(button as HTMLElement, container);
            event.stopPropagation();
        }
    };
    // Add the event listener
    container.addEventListener('click', container.tabClickHandler);
    // Set initial active tab if none is active
    const activeButton = container.querySelector('.tab-button.active');
    if (!activeButton) {
        const firstButton = container.querySelector('.tab-button');
        if (firstButton) {
            setActiveTab(firstButton as HTMLElement, container);
        }
    }

}
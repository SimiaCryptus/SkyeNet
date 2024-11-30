import {expandMessageReferences} from "../components/MessageList";
import {store} from '../store';

const LOG_PREFIX = '[TabHandler]';

export interface TabState {
    containerId: string;
    activeTab: string;
}

// Define and export TabContainer interface
export interface TabContainer extends HTMLElement {
    id: string;
    hasListener?: boolean;
    tabClickHandler?: (event: Event) => void;
    activeTabState?: string;
    lastKnownState?: TabState;
    contentObservers?: Map<string, MutationObserver>;
}

// Add diagnostic counters
const diagnostics = {
    saveCount: 0,
    restoreCount: 0,
    restoreSuccess: 0,
    restoreFail: 0
};
// Track scroll positions for tabs
const tabScrollPositions = new Map<string, number>();
// Add tab state version tracking
const tabStateVersions = new Map<string, number>();
let currentStateVersion = 0;
// Add active tab tracking
const activeTabStates = new Map<string, string>();

// Add function to get current active tab
function getActiveTab(containerId: string): string | undefined {
    return activeTabStates.get(containerId);
}

// Add function to set active tab state
export const setActiveTabState = (containerId: string, tabId: string): void => {
    activeTabStates.set(containerId, tabId);
};

// Add debounce utility to prevent multiple rapid updates
export function debounce<T extends (...args: any[]) => void>(func: T, wait: number) {
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
// Track active mutations to prevent infinite loops
let isMutating = false;
// Track tab state history
const tabStateHistory = new Map<string, string[]>();

// Add function to synchronize tab button states
function synchronizeTabButtonStates() {
    document.querySelectorAll('.tabs-container').forEach((container: Element) => {
        if (container instanceof HTMLElement) {
            const activeTab = getActiveTab(container.id);
            if (activeTab) {
                // Update button states
                container.querySelectorAll('.tab-button').forEach(button => {
                    if (button.getAttribute('data-for-tab') === activeTab) {
                        button.classList.add('active');
                    } else {
                        button.classList.remove('active');
                    }
                });
            }
        }
    });
}

// Add function to track tab state history
function trackTabStateHistory(containerId: string, activeTab: string) {
    if (!tabStateHistory.has(containerId)) {
        tabStateHistory.set(containerId, []);
    }
    const history = tabStateHistory.get(containerId)!;
    if (history[history.length - 1] !== activeTab) {
        history.push(activeTab);
        // Keep history limited to last 10 states
        if (history.length > 10) {
            history.shift();
        }
    }
}

// Helper function to initialize tab content
function initializeTabContent(content: Element) {
    // Re-initialize syntax highlighting if needed
    if ((window as any).Prism) {
        (window as any).Prism.highlightAllUnder(content);
    }
    // Re-initialize other interactive elements
    content.querySelectorAll('.referenced-message').forEach(ref => {
        ref.addEventListener('click', (e) => {
            if (e.target === ref) {
                ref.classList.toggle('expanded');
            }
        });
    });
}

// Define the content processor function
function processTabContent(content: Element) {
    const messages = store.getState().messages.messages;
    const rawContent = content.innerHTML;
    const processedContent = expandMessageReferences(rawContent, messages);
    if (rawContent !== processedContent) {
        // Preserve scroll position
        const scrollTop = (content as HTMLElement).scrollTop;
        content.innerHTML = processedContent;
        (content as HTMLElement).scrollTop = scrollTop;
        // Re-initialize any interactive elements
        initializeTabContent(content);
    }
}


// Move helper functions to module scope
export function saveTabState(containerId: string, activeTab: string) {
    try {
        diagnostics.saveCount++;
        // Increment version for this container
        currentStateVersion++;
        tabStateVersions.set(containerId, currentStateVersion);

        console.debug(`${LOG_PREFIX} Saving tab state #${diagnostics.saveCount}:`, {
            containerId,
            activeTab,
            existingStates: tabStates.size,
            currentStates: Array.from(tabStates.entries()),
            version: currentStateVersion
        });
        // Only store in memory
        const state = {containerId, activeTab};
        tabStates.set(containerId, state);
        // Store state on container element for quick reference
        const container = document.getElementById(containerId) as TabContainer;
        if (container) {
            container.lastKnownState = state;
            container.dataset.stateVersion = currentStateVersion.toString();
        }
        trackTabStateHistory(containerId, activeTab);
    } catch (error) {
        console.warn(`${LOG_PREFIX} Failed to save tab state:`, error);
    }
}

// Export function to get all current tab states
export const getAllTabStates = (): Map<string, TabState> => {
    return new Map(tabStates);
}
// Export function to restore multiple tab states
export const restoreTabStates = (states: Map<string, TabState>): void => {
    states.forEach((state) => {
        tabStates.set(state.containerId, state);
        const container = document.getElementById(state.containerId) as TabContainer;
        if (container) {
            restoreTabState(container);
        }
    });
}

function updateNestedTabs(element: HTMLElement) {
    const MAX_RECURSION_DEPTH = 10;
    const OPERATION_TIMEOUT = 5000;
    const depth = 0;

    function processNestedTabs(element: HTMLElement, currentDepth: number) {
        if (currentDepth >= MAX_RECURSION_DEPTH) {
            console.warn('Max recursion depth reached in updateNestedTabs');
            return;
        }

        const nestedContainers = element.querySelectorAll('.tabs-container');
        nestedContainers.forEach(container => {
            if (container instanceof HTMLElement) {
                try {
                    setupTabContainer(container as TabContainer);
                    restoreTabState(container as TabContainer);
                    // Process next level of nested tabs
                    processNestedTabs(container, currentDepth + 1);
                } catch (e) {
                    console.warn('Failed to process nested tab container:', e);
                }
            }
        });
    }

    const timeoutId = setTimeout(() => console.warn('updateNestedTabs operation timed out'), OPERATION_TIMEOUT);

    processNestedTabs(element, depth);
    clearTimeout(timeoutId);
}

// Move setActiveTab to module scope
export function setActiveTab(button: HTMLElement, container: HTMLElement) {
    const forTab = button.getAttribute('data-for-tab');
    if (!forTab) return;
    const tabContainer = container as TabContainer;
    // Early return if tab is already active to prevent unnecessary updates
    if (tabContainer.activeTabState === forTab) {
        console.debug(`${LOG_PREFIX} Tab ${forTab} already active, skipping update`);
        return;
    }
    // Store the active tab state
    setActiveTabState(container.id, forTab);

    // Batch DOM operations
    requestAnimationFrame(() => {
        // Update UI for this specific container
        container.querySelectorAll('.tab-button').forEach(btn => {
            btn.classList.toggle('active', btn.getAttribute('data-for-tab') === forTab);
        });
        container.querySelectorAll('.tab-content').forEach(content => {
            const isActive = content.getAttribute('data-tab') === forTab;
            content.classList.toggle('active', isActive);
            (content as HTMLElement).style.display = isActive ? 'block' : 'none';
        });
    });
    // Store scroll position and state before switching
    // Store scroll position of current active tab before switching
    const currentActiveContent = container.querySelector('.tab-content.active');
    if (currentActiveContent instanceof HTMLElement) {
        tabScrollPositions.set(currentActiveContent.getAttribute('data-tab') || '', currentActiveContent.scrollTop);
    }

    // Get previous state for logging
    const previousTab = tabContainer.lastKnownState?.activeTab || tabStates.get(container.id)?.activeTab;

    // Save new state immediately
    saveTabState(container.id, forTab);
    // Store state in container element for persistence
    tabContainer.lastKnownState = {containerId: container.id, activeTab: forTab};


    // Update UI for this specific container and ensure proper class handling
    const allTabButtons = container.querySelectorAll('.tab-button');
    allTabButtons.forEach(btn => {
        if (btn.getAttribute('data-for-tab') === forTab) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    console.log(`${LOG_PREFIX} Setting active tab:`, {
        containerId: container.id,
        tab: forTab,
        previousTab: previousTab
    });
    // Clean up existing observers for the container
    if (tabContainer.contentObservers) {
        tabContainer.contentObservers.forEach(observer => observer.disconnect());
    }
    tabContainer.contentObservers = new Map();

    container.querySelectorAll('.tab-content').forEach(content => {
        if (content.getAttribute('data-tab') === forTab) {
            content.classList.add('active');
            (content as HTMLElement).style.display = 'block';
            // Restore scroll position
            const savedScrollTop = tabScrollPositions.get(forTab);
            if (savedScrollTop !== undefined) {
                (content as HTMLElement).scrollTop = savedScrollTop;
            }
            // Process content and update nested tabs
            requestAnimationFrame(() => {
                processTabContent(content);
                updateNestedTabs(content as HTMLElement);
            });
            // Set up enhanced mutation observer with debounced handler
            const debouncedProcess = debounce((element: Element) => {
                processTabContent(element);
            }, 100);
            const observer = new MutationObserver((mutations) => {
                mutations.forEach(mutation => {
                    if (mutation.target instanceof Element) {
                        debouncedProcess(mutation.target);
                    }
                });
            });

            observer.observe(content, {
                childList: true,
                subtree: true,
                characterData: true,
                attributes: true,
                attributeFilter: ['data-ref-id']
            });
            // Store observer reference for cleanup
            (content as any)._contentObserver = observer;
            // Initial content processing with RAF
            requestAnimationFrame(() => processTabContent(content));

        } else {
            content.classList.remove('active');
            (content as HTMLElement).style.display = 'none';
            // Cleanup observer when tab is hidden
            if ((content as any)._contentObserver) {
                (content as any)._contentObserver.disconnect();
                delete (content as any)._contentObserver;
            }
        }
    });
}

// Declare restoreTabState at module level
function restoreTabState(container: TabContainer) {
    try {
        diagnostics.restoreCount++;
        const containerId = container.id;
        const containerVersion = parseInt(container.dataset.stateVersion || '0');
        const storedVersion = tabStateVersions.get(containerId) || 0;
        // Skip restore if container has newer version
        if (containerVersion > storedVersion) {
            console.debug(`${LOG_PREFIX} Skipping restore - container has newer state:`, {
                containerId,
                containerVersion,
                storedVersion
            });
            return;
        }
        console.debug(`${LOG_PREFIX} Attempting to restore tab state #${diagnostics.restoreCount}:`, {
            containerId,
            lastKnownState: container.lastKnownState,
            storedState: tabStates.get(containerId),
            allStates: Array.from(tabStates.entries()),
            version: storedVersion
        });
        // First try to get state from container element
        // Try to get active tab from memory first
        const savedTab = getActiveTab(containerId) ||
            container.lastKnownState?.activeTab ||
            tabStates.get(containerId)?.activeTab;
        if (savedTab) {
            const button = container.querySelector(
                `.tab-button[data-for-tab="${savedTab}"]`
            ) as HTMLElement;
            if (button) {
                setActiveTab(button, container);
                // Update container's last known state
                container.lastKnownState = {containerId, activeTab: savedTab};
                diagnostics.restoreSuccess++;
                console.debug(`${LOG_PREFIX} Successfully restored tab state:`, {
                    containerId,
                    activeTab: savedTab,
                    successCount: diagnostics.restoreSuccess
                });
            }
        } else {
            diagnostics.restoreFail++;
            console.warn(`${LOG_PREFIX} No saved state found for container:`, {
                containerId,
                failCount: diagnostics.restoreFail
            });
            const firstButton = container.querySelector('.tab-button') as HTMLElement;
            if (firstButton) {
                setActiveTab(firstButton, container);
                const forTab = firstButton.getAttribute('data-for-tab');
                if (forTab) {
                    container.lastKnownState = {containerId, activeTab: forTab};
                }
            }
        }
    } catch (error) {
        console.warn(`${LOG_PREFIX} Failed to restore tab state:`, error);
        diagnostics.restoreFail++;
    }
}

// Add cleanup function to reset state
export function resetTabState() {
    tabStates.clear();
    tabStateHistory.clear();
    tabStateVersions.clear();
    activeTabStates.clear();
    currentStateVersion = 0;
    isMutating = false;
}


export const updateTabs = debounce(() => {
    if (isMutating) {
        console.debug(`${LOG_PREFIX} Skipping update during mutation`);
        return;
    }
    isMutating = true;
    const processed = new Set<string>();

    // Get current tab states
    const currentStates = getAllTabStates();

    console.log(`${LOG_PREFIX} Updating tabs...`);

    const tabButtons = document.querySelectorAll('.tab-button');
    const tabsContainers = new Set<TabContainer>();

    tabButtons.forEach(button => {
        const container = button.closest('.tabs-container') as TabContainer;
        if (container && !processed.has(container.id)) {
            processed.add(container.id);
            tabsContainers.add(container);
        }
    });
    tabsContainers.forEach(container => {
        setupTabContainer(container);
        // Restore state for this container if it exists
        const state = currentStates.get(container.id);
        if (state) {
                if (state) {
                    restoreTabState(container);
                }
        }
    });
    // Synchronize tab button states after update
    synchronizeTabButtonStates();

    isMutating = false;
    processed.clear();
}, 100);


function setupTabContainer(container: TabContainer) {
    if (container.hasListener) return;
    if (!container.id) container.id = `tab-container-${Math.random().toString(36).substr(2, 9)}`;
    console.log(`${LOG_PREFIX} Setting up tab container:`, container.id);
    container.tabClickHandler = (event: Event) => {
        const button = (event.target as HTMLElement).closest('.tab-button');
        if (button && container.contains(button)) {
            setActiveTab(button as HTMLElement, container);
            event.stopPropagation();
        }
    };
    container.addEventListener('click', container.tabClickHandler);
    container.hasListener = true;
    const activeContent = container.querySelectorAll('.tab-content').values()
        .filter(content => { content.classList.contains('active')}).toArray();
    if (activeContent.length === 0) {
        const firstButton = container.querySelector('.tab-button') as HTMLElement;
        if (firstButton) {
            setActiveTab(firstButton, container);
        }
    }
    container.querySelectorAll('.tab-content').forEach(content => {
        const isActive = content.classList.contains('active');
        (content as HTMLElement).style.display = isActive ? 'block' : 'none';
        if (isActive) {
            const tabId = content.getAttribute('data-tab');
            if (tabId) {
                container.activeTabState = tabId;
                setActiveTabState(container.id, tabId);
            }
        }
    });


}
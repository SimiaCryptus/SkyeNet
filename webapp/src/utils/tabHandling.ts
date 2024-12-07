/**
 * Controls verbosity of debug logging. When enabled, provides detailed operation tracking.
 * Disable in production to reduce console noise.
 */
const VERBOSE_LOGGING = false && process.env.NODE_ENV === 'development';

/**
 * Error tracking by category for monitoring and debugging
 */
const errors = {
    setupErrors: 0,
    restoreErrors: 0,
    saveErrors: 0,
    updateErrors: 0
};

/**
 * Core interfaces for tab state management
 */
export interface TabState {
    containerId: string;
    activeTab: string;
}

/**
 * Operational metrics for monitoring tab system health
 */
const diagnostics = {
    saveCount: 0,
    restoreCount: 0,
    restoreSuccess: 0,
    restoreFail: 0
};
/**
 * Global state management for tab system
 */
const tabStateVersions = new Map<string, number>();
let currentStateVersion = 0;

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
let isMutating = false;
const tabStateHistory = new Map<string, string[]>();

/**
 * Retrieves the currently active tab for a container
 */
function getActiveTab(containerId: string): string | undefined {
    return tabStates.get(containerId)?.activeTab;
}

// Add function to set active tab state
export const setActiveTabState = (containerId: string, tabId: string): void => {
    tabStates.set(containerId, {containerId, activeTab: tabId});
};

function trackTabStateHistory(containerId: string, activeTab: string) {
    if (!tabStateHistory.has(containerId)) {
        tabStateHistory.set(containerId, []);
    }
    const history = tabStateHistory.get(containerId)!;
    if (history[history.length - 1] !== activeTab) {
        history.push(activeTab);
        if (history.length > 10) {
            history.shift();
        }
    }
}

export function saveTabState(containerId: string, activeTab: string) {
    try {
        diagnostics.saveCount++;
        currentStateVersion++;
        tabStateVersions.set(containerId, currentStateVersion);
        // console.trace(`Saving tab state #${diagnostics.saveCount}:`, {
        //     containerId,
        //     activeTab,
        //     existingStates: tabStates.size,
        //     version: currentStateVersion
        // });
        const state = {containerId, activeTab};
        tabStates.set(containerId, state);
        trackTabStateHistory(containerId, activeTab);
    } catch (error) {
        errors.saveErrors++;
        console.error(`Failed to save tab state:`, {
            error,
            containerId,
            activeTab,
            totalErrors: errors.saveErrors
        });
    }
}

export const getAllTabStates = (): Map<string, TabState> => {
    return new Map(tabStates);
}

export const restoreTabStates = (states: Map<string, TabState>): void => {
    states.forEach((state) => {
        tabStates.set(state.containerId, state);
        const container = document.getElementById(state.containerId);
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
                    setupTabContainer(container);
                    restoreTabState(container);
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

export function setActiveTab(button: Element, container: Element) {
    const previousTab = getActiveTab(container.id);
    const forTab = button.getAttribute('data-for-tab');
    if (!forTab) return;
    if (VERBOSE_LOGGING) console.trace(`Changing tab ${previousTab} -> ${forTab}`, {
        tab: forTab,
        previousTab: previousTab,
        button: button,
        containerId: container.id
    })
    setActiveTabState(container.id, forTab);
    saveTabState(container.id, forTab);
    // Update to handle nested tab buttons correctly
    const tabsContainer = container.querySelector('.tabs');
    if (tabsContainer) {
        tabsContainer.querySelectorAll('.tab-button').forEach(btn => {
            if (btn.getAttribute('data-for-tab') === forTab) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    }
    container.querySelectorAll(':scope > .tab-content').forEach(content => {
        if (content.getAttribute('data-tab') === forTab) {
            content.classList.add('active');
            (content as HTMLElement).style.display = 'block';
            updateNestedTabs(content as HTMLElement);
        } else {
            content.classList.remove('active');
            (content as HTMLElement).style.display = 'none';
            if ((content as any)._contentObserver) {
                (content as any)._contentObserver.disconnect();
                delete (content as any)._contentObserver;
            }
        }
    });
    if (VERBOSE_LOGGING) console.trace(`${'Updated active tab'}`, {
        containerId: container.id,
        activeTab: forTab
    })
}

function restoreTabState(container: Element) {
    try {
        diagnostics.restoreCount++;
        const containerId = container.id;
        // const storedVersion = tabStateVersions.get(containerId) || 0;
        // console.debug(`Attempting to restore tab state #${diagnostics.restoreCount}:`, {
        //     containerId,
        //     storedState: tabStates.get(containerId),
        //     allStates: Array.from(tabStates.entries()),
        //     version: storedVersion
        // });
        const savedTab = getActiveTab(containerId) ||
            tabStates.get(containerId)?.activeTab;
        if (savedTab) {
            // Update button selector to handle nested structure
            const tabsContainer = container.querySelector('.tabs');
            const button = tabsContainer?.querySelector(`.tab-button[data-for-tab="${savedTab}"]`) as HTMLElement;
            if (button) {
                setActiveTab(button, container);
                diagnostics.restoreSuccess++;
                // console.debug(`Successfully restored tab state:`, {
                //     containerId,
                //     activeTab: savedTab,
                //     successCount: diagnostics.restoreSuccess
                // });
            } else {
                diagnostics.restoreFail++;
                console.warn(`No matching tab button found for tab:`, {
                    containerId,
                    savedTab,
                    failCount: diagnostics.restoreFail
                });
            }
        } else {
            diagnostics.restoreFail++;
            const firstButton = container.querySelector('.tab-button') as HTMLElement;
            if (firstButton) {
                // console.warn(`No saved state found for container:`, {
                //     containerId,
                //     failCount: diagnostics.restoreFail,
                //     firstButton: firstButton
                // });
                setActiveTab(firstButton, container);
            }
        }
    } catch (error) {
        console.warn(`Failed to restore tab state:`, error);
        diagnostics.restoreFail++;
    }
}

export function resetTabState() {
    tabStates.clear();
    tabStateHistory.clear();
    tabStateVersions.clear();
    currentStateVersion = 0;
    isMutating = false;
}

export const updateTabs = debounce(() => {
    if (isMutating) {
        console.debug(`Skipping update during mutation`);
        return;
    }
    try {
        const currentStates = getAllTabStates();
        const processed = new Set<string>();
        const tabsContainers = document.querySelectorAll('.tabs-container').values().toArray();
        isMutating = true;
        console.debug(`Starting tab update`, {
            containersCount: document.querySelectorAll('.tabs-container').length,
            existingStates: currentStates.size,
            tabsContainers: tabsContainers.map(c => c.id)
        });
        tabsContainers.forEach(container => {
            setupTabContainer(container);
            const activeTab = getActiveTab(container.id)
                || currentStates.get(container.id)?.activeTab
                || container.querySelector(':scope > .tab-button.active')?.getAttribute('data-for-tab')
            ;
            if (activeTab) {
                const state: TabState = {
                    containerId: container.id,
                    activeTab: activeTab
                };
                tabStates.set(container.id, state);
                restoreTabState(container);
            } else {
                // console.warn(`No active tab found for container`, {
                //     containerId: container.id
                // });
            }
        });
        document.querySelectorAll('.tabs-container').forEach((container: Element) => {
            if (container instanceof HTMLElement) {
                let activeTab: string | undefined = getActiveTab(container.id);
                if (!activeTab) {
                    // console.warn(`No active tab found`, {
                    //     containerId: container.id,
                    //     action: 'checking active button'
                    // });
                    // Update active button selector
                    const tabsContainer = container.querySelector('.tabs');
                    const activeButton = tabsContainer?.querySelector('.tab-button.active');
                    if (activeButton) {
                        activeTab = activeButton.getAttribute('data-for-tab') || '';
                    }
                }
                if (!activeTab) {
                    // console.warn(`No active button found`, {
                    //     containerId: container.id,
                    //     action: 'defaulting to first tab'
                    // });
                    // Update first button selector
                    const tabsContainer = container.querySelector('.tabs');
                    const firstButton = tabsContainer?.querySelector('.tab-button') as HTMLElement;
                    if (firstButton) {
                        activeTab = firstButton.getAttribute('data-for-tab') || '';
                    } else {
                        console.warn(`No tab buttons found`, {
                            containerId: container.id,
                            action: 'skipping update'
                        });
                    }
                }

                let activeCount = 0;
                let inactiveCount = 0;
                // Update button iteration
                const tabsContainer = container.querySelector('.tabs');
                if (tabsContainer) {
                    tabsContainer.querySelectorAll('.tab-button').forEach(button => {
                        if (button.getAttribute('data-for-tab') === activeTab) {
                            button.classList.add('active');
                            activeCount++;
                        } else {
                            button.classList.remove('active');
                            inactiveCount++;
                        }
                    });
                }
                if (VERBOSE_LOGGING) console.debug(`${`Synchronized ${activeCount + inactiveCount} buttons`}`, {
                    activeTab,
                    activeCount,
                    inactiveCount,
                    containerId: container.id,
                    container,
                })
            }
        });
        isMutating = false;
        processed.clear();
    } catch (error) {
        errors.updateErrors++;
        console.error(`Error during tab update:`, {
            error,
            totalErrors: errors.updateErrors
        });
    } finally {
        isMutating = false;
    }
}, 100);


function setupTabContainer(container: Element) {
    try {
        if (!container.id) {
            container.id = `tab-container-${Math.random().toString(36).substr(2, 9)}`;
            console.warn(`Generated missing container ID`, {
                containerId: container.id
            });
        }
        if (VERBOSE_LOGGING) console.debug(`Initializing container`, {
            existingActiveTab: getActiveTab(container.id),
            containerId: container.id,
        })
        container.addEventListener('click', (event: Event) => {
            const button = (event.target as HTMLElement).closest('.tab-button');
            if (button && container.contains(button)) {
                setActiveTab(button, container);
                event.stopPropagation();
            }
        });
    } catch (error) {
        errors.setupErrors++;
        console.error(`Failed to setup tab container`, {
            error,
            containerId: container.id,
            totalErrors: errors.setupErrors
        });
        throw error;
    }
}
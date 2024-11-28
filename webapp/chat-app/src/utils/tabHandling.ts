import {expandMessageReferences} from "../components/MessageList";
import {store} from '../store';

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
    // Process nested tabs after content update
    requestAnimationFrame(() => {
        updateNestedTabs(content as HTMLElement);
    });
}


// Move helper functions to module scope
function saveTabState(containerId: string, activeTab: string) {
    try {
        // Only store in memory
        tabStates.set(containerId, {containerId, activeTab});
        console.log(`${LOG_PREFIX} Saved tab state:`, {containerId, activeTab});
    } catch (error) {
        console.warn(`${LOG_PREFIX} Failed to save tab state:`, error);
    }
}

function updateNestedTabs(element: HTMLElement) {
    const MAX_RECURSION_DEPTH = 10;
    const OPERATION_TIMEOUT = 5000;
    const depth = 0;

    if (depth >= MAX_RECURSION_DEPTH) {
        console.warn('Max recursion depth reached in updateNestedTabs');
        return;
    }

    const nestedContainers = element.querySelectorAll('.tabs-container');
    const timeoutId = setTimeout(() => console.warn('updateNestedTabs operation timed out'), OPERATION_TIMEOUT);

    nestedContainers.forEach(container => {
        if (container instanceof HTMLElement) {
            try {
                setupTabContainer(container as TabContainer);
                restoreTabState(container as TabContainer);
            } catch (e) {
                console.warn('Failed to process nested tab container:', e);
            }
        }
    });
    clearTimeout(timeoutId);
}

// Move setActiveTab to module scope
function setActiveTab(button: HTMLElement, container: HTMLElement) {
    const forTab = button.getAttribute('data-for-tab');
    if (!forTab) return;
    // Check if button is already active
    if (button.classList.contains('active')) return;

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
        const containerId = container.id;
        const savedTab = tabStates.get(containerId)?.activeTab;
        if (savedTab) {
            const button = container.querySelector(
                `.tab-button[data-for-tab="${savedTab}"]`
            ) as HTMLElement;
            if (button) {
                setActiveTab(button, container);
            }
        } else {
            const firstButton = container.querySelector('.tab-button') as HTMLElement;
            if (firstButton) {
                setActiveTab(firstButton, container);
            }
        }
    } catch (error) {
        console.warn(`${LOG_PREFIX} Failed to restore tab state:`, error);
    }
}

// Add cleanup function to reset state
export function resetTabState() {
    tabStates.clear();
    isMutating = false;
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
        const container = button.closest('.tabs-container') as TabContainer;
        if (container) {
            tabsContainers.add(container);
        }
    });
    tabsContainers.forEach(container => {
        setupTabContainer(container);
        restoreTabState(container);
    });
    isMutating = false;
}, 100);


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
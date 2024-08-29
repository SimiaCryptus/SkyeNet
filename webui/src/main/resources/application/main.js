import {connect} from './chat.js';
import {applyToAllSvg, getSessionId, refreshReplyForms, refreshVerbose, substituteMessages} from './functions.js';
import {restoreTabs, updateTabs} from './tabs.js';
import {setupUIHandlers} from './uiHandlers.js';
import {onWebSocketText} from './messageHandling.js';
import {setupFormSubmit, setupMessageInput, setupUserInfo} from './uiSetup.js';
import {fetchAppConfig, showMenubar, singleInput, stickyInput} from './appConfig.js';

console.log('Main script started');

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

export const debouncedUpdateDocumentComponents = debounce(updateDocumentComponents, 250);

export function updateDocumentComponents() {
    try {
        updateTabs();
    } catch (e) {
        console.error("Error updating tabs:", e);
    }
    try {
        if (typeof Prism !== 'undefined') Prism.highlightAll();
    } catch (e) {
        console.error("Error highlighting code:", e);
    }
    try {
        refreshVerbose();
    } catch (e) {
        console.error("Error refreshing verbose:", e);
    }
    try {
        refreshReplyForms()
    } catch (e) {
        console.error("Error refreshing reply forms:", e);
    }
    try {
        if (typeof mermaid !== 'undefined') {
            const mermaidDiagrams = document.querySelectorAll('.mermaid:not(.mermaid-processed)');
            if (mermaidDiagrams.length > 0) {
                mermaid.run();
                mermaidDiagrams.forEach(diagram => diagram.classList.add('mermaid-processed'));
            }
        }
    } catch (e) {
        console.error("Error running mermaid:", e);
    }
    try {
        applyToAllSvg();
    } catch (e) {
        console.error("Error applying SVG pan zoom:", e);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM content loaded');
    if (typeof mermaid !== 'undefined') mermaid.run();

    restoreTabs();
    setupUIHandlers();

    const loginLink = document.getElementById('login');
    const usernameLink = document.getElementById('username');
    const userSettingsLink = document.getElementById('user-settings');
    const userUsageLink = document.getElementById('user-usage');
    const logoutLink = document.getElementById('logout');
    const form = document.getElementById('main-input');
    const messageInput = document.getElementById('chat-input');
    const sessionId = getSessionId();

    if (sessionId) {
        console.log(`Connecting with session ID: ${sessionId}`);
        connect(sessionId, (event) => onWebSocketText(event, document.getElementById('messages'), singleInput, stickyInput, showMenubar, substituteMessages, debouncedUpdateDocumentComponents));
    } else {
        console.log('Connecting without session ID');
        connect(undefined, (event) => onWebSocketText(event, document.getElementById('messages'), singleInput, stickyInput, showMenubar, substituteMessages, debouncedUpdateDocumentComponents));
    }

    setupMessageInput(form, messageInput);
    setupFormSubmit(form, messageInput);
    setupUserInfo(loginLink, usernameLink, userSettingsLink, userUsageLink, logoutLink);

    fetchAppConfig(sessionId)
        .then(({singleInput, stickyInput, loadImages, showMenubar}) => {
            // Use the config values as needed
            console.log('App config loaded:', {singleInput, stickyInput, loadImages, showMenubar});
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });

    updateTabs();

    document.querySelectorAll('.tabs-container').forEach(tabsContainer => {
        console.log('Restoring tabs for container:', tabsContainer.id);
        const savedTab = localStorage.getItem(`selectedTab_${tabsContainer.id}`);
        if (savedTab) {
            const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
            console.log('Main script finished loading');
            if (savedButton) {
                savedButton.click();
                console.log(`Restored saved tab: ${savedTab}`);
            }
        }
    });
});
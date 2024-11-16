import {connect} from './chat.js';
import {getCachedElement, getSessionId, refreshReplyForms, refreshVerbose} from './functions.js';
import {updateTabs} from './tabs.js';
import {setupUIHandlers} from './uiHandlers.js';
import {onWebSocketText} from './messageHandling.js';
import {setupFormSubmit, setupMessageInput, setupUserInfo} from './uiSetup.js';
import {fetchAppConfig} from './appConfig.js';

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

const updateDocumentComponents = debounce(function () {
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
            const mermaidDiagrams = Array.from(document.getElementsByClassName('mermaid')).filter(el => !el.classList.contains('mermaid-processed'));
            if (mermaidDiagrams.length > 0) {
                mermaid.run();
                mermaidDiagrams.forEach(diagram => diagram.classList.add('mermaid-processed'));
            }
        }
    } catch (e) {
        console.error("Error running mermaid:", e);
    }
    // try {
    //     applyToAllSvg();
    // } catch (e) {
    //     console.error("Error applying SVG pan zoom:", e);
    // }
}, 250);

document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM content loaded');
    if (typeof mermaid !== 'undefined') mermaid.run();

    updateTabs();
    setupUIHandlers();

    const loginLink = getCachedElement('login');
    const usernameLink = getCachedElement('username');
    const userSettingsLink = getCachedElement('user-settings');
    const userUsageLink = getCachedElement('user-usage');
    const logoutLink = getCachedElement('logout');
    const form = getCachedElement('main-input');
    const messageInput = getCachedElement('chat-input');
    const sessionId = getSessionId();
    const messages = getCachedElement('messages');

    if (sessionId) {
        console.log(`Connecting with session ID: ${sessionId}`);
        connect(sessionId, (event) => onWebSocketText(event, messages, updateDocumentComponents));
    } else {
        console.log('Connecting without session ID');
        connect(undefined, (event) => onWebSocketText(event, messages, updateDocumentComponents));
    }

    setupMessageInput(form, messageInput);
    setupFormSubmit(form, messageInput);
    setupUserInfo(loginLink, usernameLink, userSettingsLink, userUsageLink, logoutLink);

    fetchAppConfig(sessionId)
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });


});
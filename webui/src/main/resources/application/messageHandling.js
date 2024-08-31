import {showMenubar, singleInput, stickyInput} from './appConfig.js';

const messageVersions = new Map();
const messageMap = new Map(); // Use Map instead of object for better performance
const MAX_SUBSTITUTION_DEPTH = 10;
const OPERATION_TIMEOUT = 5000; // 5 seconds

export function onWebSocketText(event, messagesDiv, updateDocumentComponents) {
    if (!messagesDiv) return;
    const [messageId, messageVersion, ...contentParts] = event.data.split(',');
    const messageContent = contentParts.join(',');
    messageVersions.set(messageId, messageVersion);
    messageMap.set(messageId, messageContent);

    const messageDivs = messagesDiv.querySelectorAll(`[id="${messageId}"]`);
    messageDivs.forEach((messageDiv) => {
        messageDiv.innerHTML = messageContent;
        substituteMessages(messageDiv, 0, [messageId]);
    });
    if (messageDivs.length === 0 && !messageId.startsWith("z")) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message message-container ${messageId.startsWith('u') ? 'user-message' : 'response-message'}`;
        messageDiv.id = messageId;
        messageDiv.innerHTML = messageContent;
        messagesDiv.appendChild(messageDiv);
        substituteMessages(messageDiv, 0, [messageId]);
    }
    const mainInput = document.getElementById('main-input');
    if (mainInput) {
        if (singleInput) mainInput.style.display = 'none';
        if (stickyInput) {
            mainInput.style.position = 'sticky';
            mainInput.style.zIndex = '1';
            mainInput.style.top = showMenubar ? '30px' : '0px';
        }
    } else {
        console.log("Error: Could not find #main-input");
    }

    requestAnimationFrame(() => {
        updateDocumentComponents();
    });
}


function substituteMessages(messageDiv, depth, outerMessageIds) {
    if (depth > MAX_SUBSTITUTION_DEPTH) {
        console.warn('Max substitution depth reached');
        return;
    }
    const timeoutId = setTimeout(() => console.warn('substituteMessages operation timed out'), OPERATION_TIMEOUT);
    for (const [innerMessageId, content] of messageMap) {
        if (!innerMessageId.startsWith("z") || outerMessageIds.includes(innerMessageId)) continue;
        const elements = messageDiv.querySelectorAll(`[id="${innerMessageId}"]`);
        for (let i = 0; i < elements.length; i++) {
            const element = elements[i];
            if (element.innerHTML !== content) {
                try {
                    element.innerHTML = content;
                    substituteMessages(element, depth + 1, [...outerMessageIds, innerMessageId]);
                } catch (e) {
                    console.warn('Error during message substitution:', e);
                }
            }
        }
    }
    clearTimeout(timeoutId);
}
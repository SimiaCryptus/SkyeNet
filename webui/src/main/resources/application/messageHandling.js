import {showMenubar, singleInput, stickyInput} from './appConfig.js';

const messageVersions = new Map();
const messageMap = new Map(); // Use Map instead of object for better performance


export function onWebSocketText(event, messagesDiv, updateDocumentComponents) {
    if (!messagesDiv) return;
    const [messageId, messageVersion, ...contentParts] = event.data.split(',');
    const messageContent = contentParts.join(',');
    messageVersions.set(messageId, messageVersion);
    messageMap.set(messageId, messageContent);

    const messageDivs = messagesDiv.querySelectorAll(`[id="${messageId}"]`);
    messageDivs.forEach((messageDiv) => {
        messageDiv.innerHTML = messageContent;
        substituteMessages(messageId, messageDiv);
    });
    if (messageDivs.length === 0 && !messageId.startsWith("z")) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message message-container ${messageId.startsWith('u') ? 'user-message' : 'response-message'}`;
        messageDiv.id = messageId;
        messageDiv.innerHTML = messageContent;
        messagesDiv.appendChild(messageDiv);
        substituteMessages(messageId, messageDiv);
    }
    requestAnimationFrame(() => {
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    });
    const mainInput = document.getElementById('main-input');
    if (mainInput) {
        if (singleInput) mainInput.style.display = 'none';
        if (stickyInput) {
            mainInput.style.cssText = `position: sticky; z-index: 1; top: ${showMenubar ? '30px' : '0px'}`;
        }
    } else {
        console.log("Error: Could not find #main-input");
    }

    requestAnimationFrame(updateDocumentComponents);
}


export function substituteMessages(outerMessageId, messageDiv) {
    for (const [innerMessageId, content] of messageMap) {
        if (!innerMessageId.startsWith("z") || outerMessageId === innerMessageId) continue;
        const elements = messageDiv.querySelectorAll(`[id="${innerMessageId}"]`);
        for (let i = 0; i < elements.length; i++) {
            const element = elements[i];
            if (element.innerHTML !== content) {
                element.innerHTML = content;
                substituteMessages(innerMessageId, element);
            }
        }
    }
}
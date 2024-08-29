let messageVersions = {};
window.messageMap = {}; // Make messageMap global

export function onWebSocketText(event, messagesDiv, singleInput, stickyInput, showMenubar, substituteMessages, debouncedUpdateDocumentComponents) {
    if (!messagesDiv) return;
    const firstCommaIndex = event.data.indexOf(',');
    const secondCommaIndex = event.data.indexOf(',', firstCommaIndex + 1);
    const messageId = event.data.substring(0, firstCommaIndex);
    const messageVersion = event.data.substring(firstCommaIndex + 1, secondCommaIndex);
    const messageContent = event.data.substring(secondCommaIndex + 1);
    messageVersions[messageId] = messageVersion;
    window.messageMap[messageId] = messageContent;

    const messageDivs = document.querySelectorAll('[id="' + messageId + '"]');
    messageDivs.forEach((messageDiv) => {
        if (messageDiv) {
            messageDiv.innerHTML = messageContent;
            substituteMessages(messageId, messageDiv);
        }
    });
    if (messageDivs.length === 0 && !messageId.startsWith("z")) {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message message-container ' + (messageId.startsWith('u') ? 'user-message' : 'response-message');
        messageDiv.id = messageId;
        messageDiv.innerHTML = messageContent;
        if (messagesDiv) messagesDiv.appendChild(messageDiv);
        substituteMessages(messageId, messageDiv);
    }
    if (messagesDiv) messagesDiv.scrollTop = messagesDiv.scrollHeight;
    if (singleInput) {
        const mainInput = document.getElementById('main-input');
        if (mainInput) {
            mainInput.style.display = 'none';
        } else {
            console.log("Error: Could not find .main-input");
        }
    }
    if (stickyInput) {
        const mainInput = document.getElementById('main-input');
        if (mainInput) {
            mainInput.style.position = 'sticky';
            mainInput.style.zIndex = '1';
            mainInput.style.top = showMenubar ? '30px' : '0px';
        } else {
            console.log("Error: Could not find .main-input");
        }
    }
    debouncedUpdateDocumentComponents();
}
import {showModal} from './functions.js';
import {queueMessage} from './chat.js';

export function setupMessageInput(form, messageInput) {
    if (messageInput) {
        messageInput.addEventListener('keydown', (event) => {
            console.log('Key pressed in message input:', event.key);
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                form.dispatchEvent(new Event('submit'));
            }
        });
        let originalScrollHeight = messageInput.scrollHeight;
        messageInput.style.height = (messageInput.scrollHeight) + 'px';
        let postEditScrollHeight = messageInput.scrollHeight;
        let heightAdjustment = postEditScrollHeight - originalScrollHeight;
        messageInput.style.height = '';
        messageInput.addEventListener('input', function () {
            console.log('Input event on message input');
            this.style.height = 'auto';
            this.style.height = (this.scrollHeight - heightAdjustment) + 'px';

            const computedStyle = window.getComputedStyle(this);
            let lineHeight = computedStyle.lineHeight;
            if (lineHeight === 'normal') {
                lineHeight = parseInt(computedStyle.fontSize) * 1.2;
            } else {
                lineHeight = parseInt(lineHeight);
            }

            const maxLines = 20;
            if (this.scrollHeight > lineHeight * maxLines) {
                this.style.height = (lineHeight * maxLines) + 'px';
                this.style.overflowY = 'scroll'; // Enable vertical scrolling
            } else {
                this.style.overflowY = 'hidden'; // Hide the scrollbar when not needed
            }
        });
        messageInput.focus();
    }
}

export function setupFormSubmit(form, messageInput) {
    if (form) form.addEventListener('submit', (event) => {
        event.preventDefault();
        console.log('Form submitted');
        queueMessage(messageInput.value);
        messageInput.value = '';

        const sendButton = form.querySelector('.ws-control');
        sendButton.disabled = true;
        sendButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';

        setTimeout(() => {
            sendButton.disabled = false;
            sendButton.innerHTML = '<i class="fas fa-paper-plane"></i> Send';
        }, 2000);
    });
}

export function setupUserInfo(loginLink, usernameLink, userSettingsLink, userUsageLink, logoutLink) {
    fetch('/userInfo')
        .then(response => {
            console.log('UserInfo fetch response:', response);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log('UserInfo data:', data);
            if (data.name) {
                usernameLink.textContent = data.name;
                usernameLink.style = 'visibility: visible';
                userSettingsLink.addEventListener('click', () => showModal('/userSettings'));
                userSettingsLink.style = 'visibility: visible';
                userUsageLink.addEventListener('click', () => showModal('/usage', false));
                userUsageLink.style = 'visibility: visible';
                logoutLink.href = '/logout';
                logoutLink.style = 'visibility: visible';
                loginLink.style = 'visibility: hidden';
            }
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });
}
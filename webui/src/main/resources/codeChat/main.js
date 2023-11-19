function showModal(endpoint) {
    fetchData(endpoint);
    document.getElementById('modal').style.display = 'block';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

async function fetchData(endpoint) {
    try {
        // Add session id to the endpoint as a path parameter
        const sessionId = getSessionId();
        if (sessionId) {
            endpoint = endpoint + "?sessionId=" + sessionId;
        }
        const response = await fetch(endpoint);
        const text = await response.text();
        document.getElementById('modal-content').innerHTML = "<div>" + text + "</div>";
        Prism.highlightAll();
    } catch (error) {
        console.error('Error fetching data:', error);
    }
}

let messageVersions = {};

function onWebSocketText(event) {
    console.log('WebSocket message:', event);
    const messagesDiv = document.getElementById('messages');

    // Parse message e.g. "id,version,content"
    const firstCommaIndex = event.data.indexOf(',');
    const secondCommaIndex = event.data.indexOf(',', firstCommaIndex + 1);
    const messageId = event.data.substring(0, firstCommaIndex);
    const messageVersion = event.data.substring(firstCommaIndex + 1, secondCommaIndex);
    const messageContent = event.data.substring(secondCommaIndex + 1);
    // If messageVersion isn't more than the version for the messageId using the version map, then ignore the message
    if (messageVersion <= (messageVersions[messageId] || 0)) {
        console.log("Ignoring message with id " + messageId + " and version " + messageVersion);
        return;
    } else {
        messageVersions[messageId] = messageVersion;
    }

    let messageDiv = document.getElementById(messageId);

    if (messageDiv) {
        messageDiv.innerHTML = messageContent;
    } else {
        messageDiv = document.createElement('div');
        messageDiv.className = 'message message-container'; // Add the message-container class
        messageDiv.id = messageId;
        messageDiv.innerHTML = messageContent;
        messagesDiv.appendChild(messageDiv);
    }

    messagesDiv.scrollTop = messagesDiv.scrollHeight;
    Prism.highlightAll();
}

document.addEventListener('DOMContentLoaded', () => {

    document.querySelector('.close').addEventListener('click', closeModal);

    window.addEventListener('click', (event) => {
        if (event.target === document.getElementById('modal')) {
            closeModal();
        }
    });

    const form = document.getElementById('main-input');
    const messageInput = document.getElementById('chat-input');

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        send(messageInput.value);
        messageInput.value = '';
    });

    messageInput.addEventListener('keydown', (event) => {
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

    messageInput.addEventListener('input', function() {
        // Reset the height to a single row to get the scroll height for the current content
        this.style.height = 'auto';
        // Set the height to the scroll height, which represents the height of the content
        this.style.height = (this.scrollHeight - heightAdjustment) + 'px';

        // Get the computed style for the element
        const computedStyle = window.getComputedStyle(this);
        // Get the line height, check if it's 'normal', and calculate it based on the font size if needed
        let lineHeight = computedStyle.lineHeight;
        if (lineHeight === 'normal') {
            // Use a typical browser default multiplier for 'normal' line-height
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

    connect(undefined, onWebSocketText);

    document.body.addEventListener('click', (event) => {
        const target = event.target;
        if (target.classList.contains('play-button')) {
            const messageId = target.getAttribute('data-id');
            send('!' + messageId + ',run');
        } else if (target.classList.contains('regen-button')) {
            const messageId = target.getAttribute('data-id');
            send('!' + messageId + ',regen');
        } else if (target.classList.contains('cancel-button')) {
            const messageId = target.getAttribute('data-id');
            send('!' + messageId + ',stop');
        } else if (target.classList.contains('href-link')) {
            const messageId = target.getAttribute('data-id');
            send('!' + messageId + ',link');
        } else if (target.classList.contains('text-submit-button')) {
            const messageId = target.getAttribute('data-id');
            const text = document.querySelector('.reply-input[data-id="' + messageId + '"]').value;
            send('!' + messageId + ',userTxt,' + text);
        }
    });

    fetch('appInfo')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (data.applicationName) {
                document.title = data.applicationName;
            }
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });

});


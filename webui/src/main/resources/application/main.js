function showModal(endpoint, useSession = true) {
    fetchData(endpoint, useSession);
    document.getElementById('modal').style.display = 'block';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

async function fetchData(endpoint, useSession = true) {
    try {
        // Add session id to the endpoint as a path parameter
        if(useSession) {
            const sessionId = getSessionId();
            if (sessionId) {
                endpoint = endpoint + "?sessionId=" + sessionId;
            }
        }
        document.getElementById('modal-content').innerHTML = "<div>Loading...</div>";
        const response = await fetch(endpoint);
        const text = await response.text();
        document.getElementById('modal-content').innerHTML = "<div>" + text + "</div>";
        Prism.highlightAll();
    } catch (error) {
        console.error('Error fetching data:', error);
    }
}

let messageVersions = {};
let singleInput = false;
let stickyInput = false;

function onWebSocketText(event) {
    console.log('WebSocket message:', event);
    const messagesDiv = document.getElementById('messages');
    const firstCommaIndex = event.data.indexOf(',');
    const secondCommaIndex = event.data.indexOf(',', firstCommaIndex + 1);
    const messageId = event.data.substring(0, firstCommaIndex);
    const messageVersion = event.data.substring(firstCommaIndex + 1, secondCommaIndex);
    const messageContent = event.data.substring(secondCommaIndex + 1);
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
        if(singleInput) {
            const mainInput = document.getElementById('main-input');
            if (mainInput) {
                mainInput.style.display = 'none';
            } else {
                console.log("Error: Could not find .main-input");
            }
        }
        if(stickyInput) {
            const mainInput = document.getElementById('main-input');
            if (mainInput) {
                // Keep at top of screen
                mainInput.style.position = 'sticky';
                mainInput.style.zIndex = '1';
                mainInput.style.top = '30px';
            } else {
                console.log("Error: Could not find .main-input");
            }
        }
    }

    messagesDiv.scrollTop = messagesDiv.scrollHeight;
    Prism.highlightAll();
    refreshVerbose();
    refreshReplyForms()
}

function toggleVerbose() {
    let verboseToggle = document.getElementById('verbose');
    if(verboseToggle.innerText === 'Hide Verbose') {
        const elements = document.getElementsByClassName('verbose');
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.add('verbose-hidden'); // Add the 'verbose-hidden' class to hide
        }
        verboseToggle.innerText = 'Show Verbose';
    } else if(verboseToggle.innerText === 'Show Verbose') {
        const elements = document.getElementsByClassName('verbose');
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.remove('verbose-hidden'); // Remove the 'verbose-hidden' class to show
        }
        verboseToggle.innerText = 'Hide Verbose';
    } else {
        console.log("Error: Unknown state for verbose button");
    }
}

function refreshReplyForms() {
    document.querySelectorAll('.reply-input').forEach(messageInput => {
        messageInput.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                let form = messageInput.closest('form');
                if (form) {
                    let textSubmitButton = form.querySelector('.text-submit-button');
                    if (textSubmitButton) {
                        textSubmitButton.click();
                    } else {
                        form.dispatchEvent(new Event('submit', { cancelable: true }));
                    }
                }
            }
        });
    });
}


function refreshVerbose() {
    let verboseToggle = document.getElementById('verbose');
    if(verboseToggle.innerText === 'Hide Verbose') {
        const elements = document.getElementsByClassName('verbose');
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.remove('verbose-hidden'); // Remove the 'verbose-hidden' class to show
        }
    } else if(verboseToggle.innerText === 'Show Verbose') {
        const elements = document.getElementsByClassName('verbose');
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.add('verbose-hidden'); // Add the 'verbose-hidden' class to hide
        }
    } else {
        console.log("Error: Unknown state for verbose button");
    }
}

document.addEventListener('DOMContentLoaded', () => {


    function setTheme(theme) {
        document.getElementById('theme_style').href = theme + '.css';
        localStorage.setItem('theme', theme);
    }
    const theme_normal = document.getElementById('theme_normal');
    if (theme_normal) {
        theme_normal.addEventListener('click', () => setTheme('main'));
    }
    const theme_night = document.getElementById('theme_night');
    if (theme_night) {
        theme_night.addEventListener('click', () => setTheme('night'));
    }

    const theme_forest = document.getElementById('theme_forest');
    if (theme_forest) {
        theme_forest.addEventListener('click', () => setTheme('forest'));
    }
    const theme_pony = document.getElementById('theme_pony');
    if (theme_pony) {
        theme_pony.addEventListener('click', () => setTheme('pony'));
    }
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme != null) {
        document.getElementById('theme_style').href = savedTheme + '.css';
    }


    document.getElementById('history').addEventListener('click', () => showModal('sessions'));
    document.getElementById('settings').addEventListener('click', () => showModal('settings'));
    document.getElementById('usage').addEventListener('click', () => showModal('usage'));
    document.getElementById('verbose').addEventListener('click', () => toggleVerbose());
    document.getElementById('delete').addEventListener('click', () => showModal('delete'));
    document.getElementById('cancel').addEventListener('click', () => showModal('cancel'));
    document.getElementById('threads').addEventListener('click', () => showModal('threads'));
    document.getElementById('share').addEventListener('click', () => showModal('share?url=' + encodeURIComponent(window.location.href), false));
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

    const sessionId = getSessionId();
    if (sessionId) {
        connect(sessionId, onWebSocketText);
    } else {
        connect(undefined, onWebSocketText);
    }

    document.body.addEventListener('click', (event) => {
        const target = event.target;
        if (target.classList.contains('href-link')) {
            const messageId = target.getAttribute('data-id');
            send('!' + messageId + ',link');
        } else if (target.classList.contains('play-button')) {
            const messageId = target.getAttribute('data-id');
            send('!' + messageId + ',run');
        } else if (target.classList.contains('regen-button')) {
            const messageId = target.getAttribute('data-id');
            send('!' + messageId + ',regen');
        } else if (target.classList.contains('cancel-button')) {
            const messageId = target.getAttribute('data-id');
            send('!' + messageId + ',stop');
        } else if (target.classList.contains('text-submit-button')) {
            const messageId = target.getAttribute('data-id');
            const text = document.querySelector('.reply-input[data-id="' + messageId + '"]').value;
            // url escape the text
            const escapedText = encodeURIComponent(text);
            send('!' + messageId + ',userTxt,' + escapedText);
        }
    });

    document.getElementById("files").addEventListener("click", function (event) {
        event.preventDefault();
        const sessionId = getSessionId();
        const url = "fileIndex/" + sessionId + "/";
        window.open(url, "_blank");
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
            if (data.singleInput) {
                singleInput = data.singleInput;
            }
            if (data.stickyInput) {
                stickyInput = data.stickyInput;
            }
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });


    // Get the login and username links
    const loginLink = document.getElementById('login');
    const usernameLink = document.getElementById('username');
    const userSettingsLink = document.getElementById('user-settings');
    const userUsageLink = document.getElementById('user-usage');
    const logoutLink = document.getElementById('logout');

    // Fetch user information
    fetch('userInfo')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (data.name) {
                // Update the username link with the user's name and make it visible
                usernameLink.textContent = data.name;
                usernameLink.style = 'visibility: visible';

                // Update the href for user settings and make it visible
                userSettingsLink.addEventListener('click', () => showModal('/userSettings'));
                userSettingsLink.style = 'visibility: visible';

                // Update the href for user usage and make it visible
                userUsageLink.addEventListener('click', () => showModal('/usage', false));
                userUsageLink.style = 'visibility: visible';

                // Update the logout link and make it visible
                logoutLink.href = '/logout';
                logoutLink.style = 'visibility: visible';

                // Hide the login link since the user is logged in
                loginLink.style = 'visibility: hidden';
            }
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });
/*

            <a id="privacy">Privacy Policy</a>
            <a id="tos">Terms of Service</a>
 */
    // Get the privacy and terms links
    const privacyLink = document.getElementById('privacy');
    const tosLink = document.getElementById('tos');
    if (privacyLink) {
        // Update the privacy link with the user's name and make it visible
        privacyLink.addEventListener('click', () => showModal('/privacy.html', false));
    }
    if (tosLink) {
        // Update the terms link with the user's name and make it visible
        tosLink.addEventListener('click', () => showModal('/tos.html', false));
    }
});


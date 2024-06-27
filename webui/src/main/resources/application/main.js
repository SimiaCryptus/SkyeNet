let messageVersions = {};
let messageMap = {};
let singleInput = false;
let stickyInput = false;
let loadImages = "true";
let showMenubar = true;
let messageDiv;

function onWebSocketText(event) {
    console.debug('WebSocket message:', event);
    const messagesDiv = document.getElementById('messages');
    if (!messagesDiv) return;
    const firstCommaIndex = event.data.indexOf(',');
    const secondCommaIndex = event.data.indexOf(',', firstCommaIndex + 1);
    const messageId = event.data.substring(0, firstCommaIndex);
    const messageVersion = event.data.substring(firstCommaIndex + 1, secondCommaIndex);
    const messageContent = event.data.substring(secondCommaIndex + 1);
    messageVersions[messageId] = messageVersion;
    messageMap[messageId] = messageContent;

    const messageDivs = document.querySelectorAll('[id="' + messageId + '"]');
    messageDivs.forEach((messageDiv) => {
        if (messageDiv) {
            messageDiv.innerHTML = messageContent;
            substituteMessages(messageId, messageDiv);
        }
    });
    if (messageDivs.length == 0 && !messageId.startsWith("z")) {
        messageDiv = document.createElement('div');
        messageDiv.className = 'message message-container ' + (messageId.startsWith('u') ? 'user-message' : 'response-message');
        messageDiv.id = messageId;
        messageDiv.innerHTML = messageContent;
        if (messagesDiv) messagesDiv.appendChild(messageDiv);
        substituteMessages(messageId, messageDiv);

        // Scroll to the new message with smooth behavior
        messageDiv.scrollIntoView({behavior: 'smooth', block: 'end'});
    }
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
            // Keep at top of screen
            mainInput.style.position = 'sticky';
            mainInput.style.zIndex = '1';
            mainInput.style.top = showMenubar ? '30px' : '0px';
        } else {
            console.log("Error: Could not find .main-input");
        }
    }
    if (messagesDiv) messagesDiv.scrollTop = messagesDiv.scrollHeight;
    try {
        if (typeof Prism !== 'undefined') Prism.highlightAll();
    } catch (e) {
        console.log("Error highlighting code: " + e);
    }
    try {
        refreshVerbose();
    } catch (e) {
        console.log("Error refreshing verbose: " + e);
    }
    try {
        refreshReplyForms()
    } catch (e) {
        console.log("Error refreshing reply forms: " + e);
    }
    try {
        if (typeof mermaid !== 'undefined') mermaid.run();
    } catch (e) {
        console.log("Error running mermaid: " + e);
    }
    try {
        applyToAllSvg();
    } catch (e) {
        console.log("Error applying SVG pan zoom: " + e);
    }
    try {
        updateTabs();
    } catch (e) {
        console.log("Error updating tabs: " + e);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (typeof mermaid !== 'undefined') mermaid.run();
    applyToAllSvg();

    // Set a timer to periodically apply svgPanZoom to all SVG elements
    setInterval(() => {
        applyToAllSvg();
    }, 5000); // Adjust the interval as needed

    // Restore the selected tabs from localStorage before adding event listeners
    document.querySelectorAll('.tabs-container').forEach(tabsContainer => {
        const savedTab = localStorage.getItem(`selectedTab_${tabsContainer.id}`);
        if (savedTab) {
            const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
            if (savedButton) {
                savedButton.classList.add('active');
                const forTab = savedButton.getAttribute('data-for-tab');
                const selectedContent = tabsContainer.querySelector(`.tab-content[data-tab="${forTab}"]`);
                if (selectedContent) {
                    selectedContent.classList.add('active');
                    selectedContent.style.display = 'block';
                }
                console.log(`Restored saved tab: ${savedTab}`);
            }
        }
    });
    document.querySelectorAll('.tabs-container').forEach(tabsContainer => {
        const savedTab = localStorage.getItem(`selectedTab_${tabsContainer.id}`);
        if (savedTab) {
            const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
            if (savedButton) {
                savedButton.click();
                console.log(`Restored saved tab: ${savedTab}`);
            }
        }
    });

    const historyElement = document.getElementById('history');
    if (historyElement) historyElement.addEventListener('click', () => showModal('sessions'));
    const settingsElement = document.getElementById('settings');
    if (settingsElement) settingsElement.addEventListener('click', () => showModal('settings'));
    const usageElement = document.getElementById('usage');
    if (usageElement) usageElement.addEventListener('click', () => showModal('usage'));
    const verboseElement = document.getElementById('verbose');
    if (verboseElement) verboseElement.addEventListener('click', () => toggleVerbose());
    const deleteElement = document.getElementById('delete');
    if (deleteElement) deleteElement.addEventListener('click', () => showModal('delete'));
    const cancelElement = document.getElementById('cancel');
    if (cancelElement) cancelElement.addEventListener('click', () => showModal('cancel'));
    const threadsElement = document.getElementById('threads');
    if (threadsElement) threadsElement.addEventListener('click', () => showModal('threads'));
    const shareElement = document.getElementById('share');
    if (shareElement) {
        shareElement.addEventListener('click', () => showModal('share?url=' + encodeURIComponent(window.location.href) + "&loadImages=" + loadImages, false));
    }
    const closeElement = document.querySelector('.close');
    if (closeElement) closeElement.addEventListener('click', closeModal);

    const loginLink = document.getElementById('login');
    const usernameLink = document.getElementById('username');
    const userSettingsLink = document.getElementById('user-settings');
    const userUsageLink = document.getElementById('user-usage');
    const logoutLink = document.getElementById('logout');

    const form = document.getElementById('main-input');
    const messageInput = document.getElementById('chat-input');

    const sessionId = getSessionId();
    if (sessionId) {
        connect(sessionId, onWebSocketText);
    } else {
        connect(undefined, onWebSocketText);
    }

    document.addEventListener('click', (event) => {
        const modal = document.getElementById('modal');
        if (event.target === modal) {
            closeModal();
        }
    });

    if (form) form.addEventListener('submit', (event) => {
        event.preventDefault();
        send(messageInput.value);
        messageInput.value = '';

        // Disable the send button and add a loading spinner
        const sendButton = form.querySelector('.ws-control');
        sendButton.disabled = true;
        sendButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';

        // Re-enable the button after a short delay (you may want to do this when you receive a response instead)
        setTimeout(() => {
            sendButton.disabled = false;
            sendButton.innerHTML = '<i class="fas fa-paper-plane"></i> Send';
        }, 2000);
    });

    if (messageInput) {
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
        messageInput.addEventListener('input', function () {
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
    }

    document.body.addEventListener('click', (event) => {
        const target = event.target;
        const hrefLink = findAncestor(target, '.href-link');
        if (hrefLink) {
            const messageId = hrefLink.getAttribute('data-id');
            if (messageId && messageId !== '' && messageId !== null) send('!' + messageId + ',link');
        } else {
            const playButton = findAncestor(target, '.play-button');
            if (playButton) {
                const messageId = playButton.getAttribute('data-id');
                if (messageId && messageId !== '' && messageId !== null) send('!' + messageId + ',run');
            } else {
                const regenButton = findAncestor(target, '.regen-button');
                if (regenButton) {
                    const messageId = regenButton.getAttribute('data-id');
                    if (messageId && messageId !== '' && messageId !== null) send('!' + messageId + ',regen');
                } else {
                    const cancelButton = findAncestor(target, '.cancel-button');
                    if (cancelButton) {
                        const messageId = cancelButton.getAttribute('data-id');
                        if (messageId && messageId !== '' && messageId !== null) send('!' + messageId + ',stop');
                    } else {
                        const textSubmitButton = findAncestor(target, '.text-submit-button');
                        if (textSubmitButton) {
                            const messageId = textSubmitButton.getAttribute('data-id');
                            const text = document.querySelector('.reply-input[data-id="' + messageId + '"]').value;
                            // url escape the text
                            const escapedText = encodeURIComponent(text);
                            if (messageId && messageId !== '' && messageId !== null) send('!' + messageId + ',userTxt,' + escapedText);
                        }
                    }
                }
            }
        }
    });

    let filesElement = document.getElementById("files");
    if (filesElement) filesElement.addEventListener("click", function (event) {
        event.preventDefault();
        const sessionId = getSessionId();
        const url = "fileIndex/" + sessionId + "/";
        window.open(url, "_blank");
    });

    fetch('appInfo?session=' + sessionId)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (data) {
                if (data.applicationName) {
                    document.title = data.applicationName;
                }
                if (data.singleInput) {
                    singleInput = data.singleInput;
                }
                if (data.stickyInput) {
                    stickyInput = data.stickyInput;
                }
                if (data.loadImages) {
                    loadImages = data.loadImages;
                }
                if (data.showMenubar != null) {
                    showMenubar = data.showMenubar;
                    if (data.showMenubar === false) {
                        const menubar = document.getElementById('toolbar');
                        if (menubar) menubar.style.display = 'none';
                        const namebar = document.getElementById('namebar');
                        if (namebar) namebar.style.display = 'none';
                        const mainInput = document.getElementById('main-input');
                        if (mainInput) {
                            mainInput.style.top = '0px';
                        }
                        const session = document.getElementById('session');
                        if (session) {
                            session.style.top = '0px';
                            session.style.width = '100%';
                            session.style.position = 'absolute';
                        }
                    }
                }
            }
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });

    fetch('/userInfo')
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

    updateTabs();

    // Restore the selected tabs from localStorage
    document.querySelectorAll('.tabs-container').forEach(tabsContainer => {
        const savedTab = localStorage.getItem(`selectedTab_${tabsContainer.id}`);
        if (savedTab) {
            const savedButton = tabsContainer.querySelector(`.tab-button[data-for-tab="${savedTab}"]`);
            if (savedButton) {
                savedButton.click();
                console.log(`Restored saved tab: ${savedTab}`);
            }
        }
    });
});
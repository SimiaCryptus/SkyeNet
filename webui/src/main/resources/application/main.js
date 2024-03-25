function showModal(endpoint, useSession = true) {
    fetchData(endpoint, useSession);
    const modal = document.getElementById('modal');
    if (modal) modal.style.display = 'block';
}

function closeModal() {
    const modal = document.getElementById('modal');
    if (modal) modal.style.display = 'none';
}

async function fetchData(endpoint, useSession = true) {
    try {
        // Add session id to the endpoint as a path parameter
        if (useSession) {
            const sessionId = getSessionId();
            if (sessionId) {
                endpoint = endpoint + "?sessionId=" + sessionId;
            }
        }
        const modalContent = document.getElementById('modal-content');
        if (modalContent) modalContent.innerHTML = "<div>Loading...</div>";
        const response = await fetch(endpoint);
        const text = await response.text();
        if (modalContent) modalContent.innerHTML = "<div>" + text + "</div>";
        if (typeof Prism !== 'undefined') {
            Prism.highlightAll();
        }
    } catch (error) {
        console.error('Error fetching data:', error);
    }
}

let messageVersions = {};
let messageMap = {};
let singleInput = false;
let stickyInput = false;
let loadImages = "true";

(function () {
    class SvgPanZoom {

        // Make sure to update the init function to avoid attaching multiple listeners to the same SVG
        init(svgElement) {
            console.log("Initializing SvgPanZoom for an SVG element");
            if (svgElement.dataset.svgPanZoomInitialized) return; // Skip if already initialized
            svgElement.dataset.svgPanZoomInitialized = true; // Mark as initialized
            this.svgElement = svgElement;
            this.currentTransform = {x: 0, y: 0, scale: 1};
            this.onMove = this.onMove.bind(this);
            this.onClick = this.onClick.bind(this);
            this.handleZoom = this.handleZoom.bind(this);
            this.ensureTransformGroup();
            this.attachEventListeners();
        }

        // Ensure the SVG has a <g> element for transformations
        ensureTransformGroup() {
            console.log("Ensuring transform group exists in the SVG");
            if (!this.svgElement.querySelector('g.transform-group')) {
                const group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                group.classList.add('transform-group');
                while (this.svgElement.firstChild) {
                    group.appendChild(this.svgElement.firstChild);
                }
                this.svgElement.appendChild(group);
            }
            this.transformGroup = this.svgElement.querySelector('g.transform-group');
        }

        // Attach event listeners for panning and zooming
        attachEventListeners() {
            console.log("Attaching event listeners for panning and zooming");
            this.svgElement.addEventListener('click', this.onClick.bind(this));
            this.svgElement.addEventListener('mousemove', this.onMove.bind(this));
            this.svgElement.addEventListener('wheel', this.handleZoom.bind(this));
        }

        // Start panning
        onClick(event) {
            if (this.isPanning) {
                this.isPanning = false;
                console.log("Ending pan");
            } else {
                this.isPanning = true;
                console.log("Starting pan");
                this.startX = event.clientX;
                this.startY = event.clientY;
                this.priorPan = {x: this.currentTransform.x, y: this.currentTransform.y};
            }
        }

        // Perform panning
        onMove(event) {
            const moveScale = this.svgElement.viewBox.baseVal.width / this.svgElement.width.baseVal.value;
            if (this.isPanning === false) return;
            const dx = event.clientX - this.startX;
            const dy = event.clientY - this.startY;
            if (this.currentTransform.x) {
                this.currentTransform.x = dx * moveScale + this.priorPan.x;
            } else {
                this.currentTransform.x = dx * moveScale + this.priorPan.x;
            }
            if (this.currentTransform.y) {
                this.currentTransform.y = dy * moveScale + this.priorPan.y;
            } else {
                this.currentTransform.y = dy * moveScale + this.priorPan.y;
            }
            console.log("Panning %s, %s", this.currentTransform.x, this.currentTransform.y);
            this.updateTransform();
        }

        // Handle zooming
        handleZoom(event) {
            event.preventDefault();
            const direction = event.deltaY > 0 ? -1 : 1;
            const zoomFactor = 0.1;
            this.currentTransform.scale += direction * zoomFactor;
            this.currentTransform.scale = Math.max(0.1, this.currentTransform.scale); // Prevent inverting
            console.log("Handling zoom %s (%s)", direction, this.currentTransform.scale);
            this.updateTransform();
        }

        // Update SVG transform
        updateTransform() {
            console.log("Updating SVG transform");
            const transformAttr = `translate(${this.currentTransform.x} ${this.currentTransform.y}) scale(${this.currentTransform.scale})`;
            this.transformGroup.setAttribute('transform', transformAttr);
        }
    }

    // Expose the library to the global scope
    window.SvgPanZoom = SvgPanZoom;
})();

function applyToAllSvg() {
    console.log("Applying SvgPanZoom to all SVG elements");
    document.querySelectorAll('svg').forEach(svg => {
        if (!svg.dataset.svgPanZoomInitialized) {
            new SvgPanZoom().init(svg);
        }
    });
}

function substituteMessages(outerMessageId, messageDiv) {
    Object.entries(messageMap).forEach(([innerMessageId, content]) => {
        if(outerMessageId !== innerMessageId && messageDiv) messageDiv.querySelectorAll('[id="' + innerMessageId + '"]').forEach((element) => {
            if (element.innerHTML !== content) {
                //console.log("Substituting message with id " + innerMessageId + " and content " + content);
                element.innerHTML = content;
                substituteMessages(innerMessageId, element);
            }
        });
    });
}

function onWebSocketText(event) {
    console.log('WebSocket message:', event);
    const messagesDiv = document.getElementById('messages');
    if (!messagesDiv) return;
    const firstCommaIndex = event.data.indexOf(',');
    const secondCommaIndex = event.data.indexOf(',', firstCommaIndex + 1);
    const messageId = event.data.substring(0, firstCommaIndex);
    const messageVersion = event.data.substring(firstCommaIndex + 1, secondCommaIndex);
    const messageContent = event.data.substring(secondCommaIndex + 1);
    // if (messageVersion <= (messageVersions[messageId] || 0)) {
    //     console.log("Ignoring message with id " + messageId + " and version " + messageVersion);
    //     return;
    // } else {
        messageVersions[messageId] = messageVersion;
        messageMap[messageId] = messageContent;
    // }
    // Cleanup: remove temporary event listeners

    const messageDivs = document.querySelectorAll('[id="' + messageId + '"]');
    messageDivs.forEach((messageDiv) => {
        if (messageDiv) {
            messageDiv.innerHTML = messageContent;
            substituteMessages(messageId, messageDiv);
        }
    });
    if(messageDivs.length == 0) {
        messageDiv = document.createElement('div');
        messageDiv.className = 'message message-container'; // Add the message-container class
        messageDiv.id = messageId;
        messageDiv.innerHTML = messageContent;
        if (messagesDiv) messagesDiv.appendChild(messageDiv);
        substituteMessages(messageId, messageDiv);
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
                mainInput.style.top = '30px';
            } else {
                console.log("Error: Could not find .main-input");
            }
        }
    }
    if (messagesDiv) messagesDiv.scrollTop = messagesDiv.scrollHeight;
    if (typeof Prism !== 'undefined') Prism.highlightAll();
    refreshVerbose();
    refreshReplyForms()
    if (typeof mermaid !== 'undefined') mermaid.run();
    updateTabs();
    applyToAllSvg();
}

function updateTabs() {
    document.querySelectorAll('.tab-button').forEach(button => {
        button.addEventListener('click', (event) => { // Ensure the event is passed as a parameter
            event.stopPropagation();
            const forTab = button.getAttribute('data-for-tab');
            let tabsParent = button.closest('.tabs-container');
            tabsParent.querySelectorAll('.tab-content').forEach(content => {
                const contentParent = content.closest('.tabs-container');
                if (contentParent === tabsParent) {
                    if (content.getAttribute('data-tab') === forTab) {
                        content.classList.add('active');
                    } else if (content.classList.contains('active')) {
                        content.classList.remove('active')
                    }
                }
            });
        })
    });
}

function toggleVerbose() {
    let verboseToggle = document.getElementById('verbose');
    if (verboseToggle.innerText === 'Hide Verbose') {
        const elements = document.getElementsByClassName('verbose');
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.add('verbose-hidden'); // Add the 'verbose-hidden' class to hide
        }
        verboseToggle.innerText = 'Show Verbose';
    } else if (verboseToggle.innerText === 'Show Verbose') {
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
                        form.dispatchEvent(new Event('submit', {cancelable: true}));
                    }
                }
            }
        });
    });
}


function refreshVerbose() {
    let verboseToggle = document.getElementById('verbose');
    if (verboseToggle.innerText === 'Hide Verbose') {
        const elements = document.getElementsByClassName('verbose');
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.remove('verbose-hidden'); // Remove the 'verbose-hidden' class to show
        }
    } else if (verboseToggle.innerText === 'Show Verbose') {
        const elements = document.getElementsByClassName('verbose');
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.add('verbose-hidden'); // Add the 'verbose-hidden' class to hide
        }
    } else {
        console.log("Error: Unknown state for verbose button");
    }
}

document.addEventListener('DOMContentLoaded', () => {
    updateTabs();
    if (typeof mermaid !== 'undefined') mermaid.run();
    applyToAllSvg();

    // Set a timer to periodically apply svgPanZoom to all SVG elements
    setInterval(() => {
        applyToAllSvg();
    }, 5000); // Adjust the interval as needed
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
    const theme_alien = document.getElementById('theme_alien');
    if (theme_alien) {
        theme_alien.addEventListener('click', () => setTheme('alien'));
    }
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme != null) {
        document.getElementById('theme_style').href = savedTheme + '.css';
    }


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

    window.addEventListener('click', (event) => {
        if (event.target === document.getElementById('modal')) {
            closeModal();
        }
    });

    const form = document.getElementById('main-input');
    const messageInput = document.getElementById('chat-input');

    if (form) form.addEventListener('submit', (event) => {
        event.preventDefault();
        send(messageInput.value);
        messageInput.value = '';
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

    const sessionId = getSessionId();
    if (sessionId) {
        connect(sessionId, onWebSocketText);
    } else {
        connect(undefined, onWebSocketText);
    }

    function findAncestor(element, selector) {
        while (element && !element.matches(selector)) {
            element = element.parentElement;
        }
        return element;
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

    document.getElementById("files").addEventListener("click", function (event) {
        event.preventDefault();
        const sessionId = getSessionId();
        const url = "fileIndex/" + sessionId + "/";
        window.open(url, "_blank");
    });

    fetch('appInfo')
        .then(response => {
            if (!response.ok) {
                console.error('There was a problem with the fetch operation:', error);
            }
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
})
;


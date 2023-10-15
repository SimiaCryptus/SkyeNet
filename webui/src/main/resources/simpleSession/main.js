function showModal(endpoint) {
    fetchData(endpoint);
    document.getElementById('modal').style.display = 'block';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

async function fetchData(endpoint) {
    try {
        const response = await fetch(endpoint);
        const text = await response.text();
        document.getElementById('modal-content').innerHTML = "<pre><code class=\"language-text\">" + text + "</code></pre>";
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
    updateMessageDivs();
}

function updateMessageDivs()
{
    /*
    console.log("Updating message divs");
    Get all message containers
    var messageContainers = document.querySelectorAll('.message-container');
    // Loop through each message container
    messageContainers.forEach(function (container) {
        console.log("Updating message container: " + container);
        // Get the first child which is a div (header) and the remaining children (content)
        var header = container.children[0];
        while (header.tagName !== 'DIV') {
            header = header.nextElementSibling;
        }
        var content = Array.from(container.children);
        // Remove the header from the content array (it isn't nessessary the first element)
        content.splice(content.indexOf(header), 1);


        // Add click event listener to the header
        header.addEventListener('click', function () {
            console.log("Header clicked: " + header);
            // Toggle the collapsible-content class on each content element
            content.forEach(function (elem) {
                console.log("Toggling collapsible-content class on element: " + elem);
                elem.classList.toggle('collapsible-content');
            });
        });

        // Initialize content as collapsible
        content.forEach(function (elem) {
            console.log("Adding collapsible-content class to element: " + elem);
            elem.classList.add('collapsible-content');
        });

        // Expand the content initially
        header.click();
    });
     */
}


document.addEventListener('DOMContentLoaded', () => {

    document.getElementById('api').addEventListener('click', () => showModal('yamlDescriptor'));
    document.getElementById('history').addEventListener('click', () => showModal('sessions'));
    document.querySelector('.close').addEventListener('click', closeModal);

    window.addEventListener('click', (event) => {
        if (event.target === document.getElementById('modal')) {
            closeModal();
        }
    });

    const form = document.getElementById('form');
    const messageInput = document.getElementById('message');

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

    const sessionId = getSessionId();
    if (sessionId) {
        connect(sessionId, onWebSocketText);
    } else {
        connect(undefined, onWebSocketText);
    }

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

    document.getElementById("files").addEventListener("click", function (event) {
        event.preventDefault(); // Prevent the default behavior of the anchor tag
        const sessionId = getSessionId();
        const url = "fileIndex/" + sessionId + "/";
        window.open(url, "_blank"); // Open the URL in a new tab
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

    updateMessageDivs();
});


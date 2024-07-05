let socket;
let reconnectAttempts = 0;
const MAX_RECONNECT_DELAY = 30000; // Maximum delay of 30 seconds

export function send(message) {
    console.log('Sending message:', message);
    if (socket.readyState !== 1) {
        console.error('WebSocket is not open. Message not sent:', message);
        return false;
    }
    socket.send(message);
    return true;
}

export function connect(sessionId, customReceiveFunction) {

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    const port = window.location.port;
    const path = getWebSocketPath();

    socket = new WebSocket(`${protocol}//${host}:${port}${path}ws?sessionId=${sessionId}`);

    socket.addEventListener('open', (event) => {
        console.log('WebSocket connected:', event);
        showDisconnectedOverlay(false);
        reconnectAttempts = 0;
    });
    socket.addEventListener('message', (event) => {
        if (customReceiveFunction) {
            customReceiveFunction(event);
        } else {
            onWebSocketText(event);
        }
    });


    socket.addEventListener('close', (event) => {
        console.log('WebSocket closed:', event);
        showDisconnectedOverlay(true);
        reconnect(sessionId, customReceiveFunction);
    });

    socket.addEventListener('error', (event) => {
        console.error('WebSocket error:', event);
    });
}

function getWebSocketPath() {
    const path = window.location.pathname;
    const strings = path.split('/');
    return (strings.length >= 2 && strings[1] !== '' && strings[1] !== 'index.html')
        ? '/' + strings[1] + '/'
        : '/';
}

function reconnect(sessionId, customReceiveFunction) {
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), MAX_RECONNECT_DELAY);
    console.log(`Attempting to reconnect in ${delay}ms...`);
    setTimeout(() => {
        connect(sessionId, customReceiveFunction);
        reconnectAttempts++;
    }, delay);
}

function showDisconnectedOverlay(show) {
    document.querySelectorAll('.ws-control').forEach(element => {
        element.disabled = show;
    });
}

// Implement a message queue to handle potential disconnections
let messageQueue = [];

export function queueMessage(message) {

    messageQueue.push(message);
    processMessageQueue();
}

function processMessageQueue() {
    if (socket.readyState === WebSocket.OPEN) {
        while (messageQueue.length > 0) {
            const message = messageQueue.shift();
            send(message);
        }
    }
}
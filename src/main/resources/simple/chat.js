function getSessionId() {
    if (!window.location.hash) {
        fetch('/newSession')
            .then(response => {
                if (response.ok) {
                    return response.text();
                } else {
                    throw new Error('Failed to get new session ID');
                }
            })
            .then(sessionId => {
                window.location.hash = sessionId;
                connect(sessionId);
            });
    } else {
        return window.location.hash.substring(1);
    }
}

let socket;

function send(message) {
    socket.send(message);
}

function connect(sessionId, customReceiveFunction) {
    socket = new WebSocket(`ws://localhost:8080/ws?sessionId=${sessionId}`);

    socket.addEventListener('open', (event) => {
        console.log('WebSocket connected:', event);
        showDisconnectedOverlay(false);
    });

    socket.addEventListener('message', customReceiveFunction || receive);

    socket.addEventListener('close', (event) => {
        console.log('WebSocket closed:', event);
        showDisconnectedOverlay(true);
        setTimeout(() => {
            connect(getSessionId(), customReceiveFunction);
        }, 5000);
    });

    socket.addEventListener('error', (event) => {
        console.error('WebSocket error:', event);
    });
}

function showDisconnectedOverlay(show) {
    const overlay = document.getElementById('disconnected-overlay');
    overlay.style.display = show ? 'block' : 'none';
}

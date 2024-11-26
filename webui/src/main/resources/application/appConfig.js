export let singleInput = false;
export let stickyInput = false;
export let loadImages = "true";
export let showMenubar = true;
export let websocket = {
    url: window.location.hostname,
    port: window.location.port
};

export function fetchAppConfig(sessionId) {
    return fetch('appInfo?session=' + sessionId)
        .then(response => {
            console.log('AppInfo fetch response:', response);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log('AppInfo data:', data);
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
                if (data.websocket) {
                    websocket = data.websocket;
                }
                if (data.showMenubar != null) {
                    showMenubar = data.showMenubar;
                    applyMenubarConfig(showMenubar);
                }
                // Make config available globally
                window.appConfig = {
                    singleInput,
                    stickyInput,
                    loadImages,
                    showMenubar,
                    websocket
                };
            }
            return {singleInput, stickyInput, loadImages, showMenubar, websocket};
        });
}

function applyMenubarConfig(showMenubar) {
    if (showMenubar === false) {
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
export async function fetchData(endpoint, useSession = true) {
    try {
        // Add session id to the endpoint as a path parameter
        if (useSession) {
            const sessionId = getSessionId();
            if (sessionId) {
                endpoint = endpoint + "?sessionId=" + sessionId;
            }
        }
        const modalContent = getCachedElement('modal-content');
        if (modalContent) modalContent.innerHTML = "<div>Loading...</div>";
        const response = await fetch(endpoint);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const text = await response.text();
        if (modalContent) modalContent.innerHTML = "<div>" + text + "</div>";
        if (typeof Prism !== 'undefined') {
            Prism.highlightAll();
        }
    } catch (error) {
        console.error('Error fetching data:', error);
        const modalContent = document.getElementById('modal-content');
        if (modalContent) modalContent.innerHTML = "<div>Error loading content. Please try again later.</div>";
        throw error; // Re-throw the error for the caller to handle if needed
    }
}

export function getSessionId() {
    if (!window.location.hash) {
        return fetch('newSession')
            .then(response => {
                if (response.ok) {
                    return response.text();
                } else {
                    throw new Error(`Failed to get new session ID. Status: ${response.status}`);
                }
            })
            .then(sessionId => {
                window.location.hash = sessionId;
                return sessionId;
            })
            .catch(error => {
                console.error('Error getting session ID:', error.message);
                throw error; // Re-throw the error for the caller to handle
            });
    } else {
        return window.location.hash.substring(1);
    }
}

const elementCache = new Map();

export function getCachedElement(id) {
    if (!elementCache.has(id)) {
        const element = document.getElementById(id);
        if (element) {
            elementCache.set(id, element);
        }
    }
    return elementCache.get(id);
}

export function showModal(endpoint, useSession = true) {
    fetchData(endpoint, useSession).then(r => {
        const modal = getCachedElement('modal');
        if (modal) modal.style.display = 'block';
    });
}

export function closeModal() {
    const modal = getCachedElement('modal');
    if (modal) modal.style.display = 'none';
}

(function () {
    class SvgPanZoom {

        // Make sure to update the init function to avoid attaching multiple listeners to the same SVG
        init(svgElement) {
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
            if (this.priorPan) {
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
            }
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


export function toggleVerbose() {
    let verboseToggle = getCachedElement('verbose');
    if (verboseToggle.innerText === 'Hide Verbose') {
        const elements = Array.from(document.getElementsByClassName('verbose'));
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.add('verbose-hidden'); // Add the 'verbose-hidden' class to hide
        }
        verboseToggle.innerText = 'Show Verbose';
    } else if (verboseToggle.innerText === 'Show Verbose') {
        const elements = Array.from(document.getElementsByClassName('verbose'));
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.remove('verbose-hidden'); // Remove the 'verbose-hidden' class to show
        }
        verboseToggle.innerText = 'Hide Verbose';
    } else {
        console.log("Error: Unknown state for verbose button");
    }
}

export function refreshReplyForms() {
    Array.from(document.getElementsByClassName('reply-input')).forEach(messageInput => {
        messageInput.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                let form = messageInput.closest('form');
                if (form) {
                    let textSubmitButton = form.getElementsByClassName('text-submit-button')[0];
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


export function refreshVerbose() {
    let verboseToggle = getCachedElement('verbose');
    if (verboseToggle.innerText === 'Hide Verbose') {
        const elements = Array.from(document.getElementsByClassName('verbose'));
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.remove('verbose-hidden'); // Remove the 'verbose-hidden' class to show
        }
    } else if (verboseToggle.innerText === 'Show Verbose') {
        const elements = Array.from(document.getElementsByClassName('verbose'));
        for (let i = 0; i < elements.length; i++) {
            elements[i].classList.add('verbose-hidden'); // Add the 'verbose-hidden' class to hide
        }
    } else {
        console.log("Error: Unknown state for verbose button");
    }
}

export function findAncestor(element, selector) {
    while (element && !element.matches(selector)) {
        element = element.parentElement;
    }
    return element;
}


export function applyToAllSvg() {
    Array.from(document.getElementsByTagName('svg')).forEach(svg => {
        if (!svg.dataset.svgPanZoomInitialized) {
            new SvgPanZoom().init(svg);
        }
    });
}
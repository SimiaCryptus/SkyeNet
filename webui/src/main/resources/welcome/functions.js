async function fetchData(endpoint) {
    try {
        // Add session id to the endpoint as a path parameter
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


function showModal(endpoint) {
    fetchData(endpoint).then(r => {
        const modal = document.getElementById('modal');
        if (modal) modal.style.display = 'block';
    });
}

function closeModal() {
    const modal = document.getElementById('modal');
    if (modal) modal.style.display = 'none';
}

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

function findAncestor(element, selector) {
    while (element && !element.matches(selector)) {
        element = element.parentElement;
    }
    return element;
}


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
        if (outerMessageId !== innerMessageId && messageDiv) messageDiv.querySelectorAll('[id="' + innerMessageId + '"]').forEach((element) => {
            if (element.innerHTML !== content) {
                //console.log("Substituting message with id " + innerMessageId + " and content " + content);
                element.innerHTML = content;
                substituteMessages(innerMessageId, element);
            }
        });
    });
}

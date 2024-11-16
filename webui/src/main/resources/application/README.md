# Web UI JavaScript Architecture

## Overview

This is a modular JavaScript application that provides a web-based chat interface with real-time WebSocket communication, theme customization, and dynamic UI
components. The architecture emphasizes performance, maintainability, and user experience.

### Key Features

* Real-time bidirectional communication via WebSockets
* Multiple theme support with persistent preferences
* Dynamic content loading and caching
* Responsive UI with accessibility features
* Robust error handling and recovery

## Quick Start

```javascript
// Initialize the application
import {connect} from './chat.js';
import {setupUIHandlers} from './uiHandlers.js';
import {getSessionId} from './functions.js';

// Set up WebSocket connection
const sessionId = getSessionId();
connect(sessionId, handleWebSocketMessage);

// Configure UI
setupUIHandlers();
```

## Core Modules

### main.js

The application entry point that coordinates initialization and setup:

* Manages DOM content loading
* Initializes WebSocket connection
* Sets up UI event handlers
* Configures theme and appearance

Example usage:

```javascript
document.addEventListener('DOMContentLoaded', () => {
    setupUIHandlers();
    setupMessageInput(form, messageInput);
    connect(sessionId, handleWebSocketMessage);
    fetchAppConfig(sessionId).then(config => {
        applyConfiguration(config);
    });
});
```

### chat.js

Manages WebSocket communication with features:

* Automatic reconnection with exponential backoff
* Message queueing during disconnections
* Custom message handlers
* Connection state management

Example usage:

```javascript
// Send a message
queueMessage('Hello world');

// Connect with custom handler
connect(sessionId, (event) => {
    console.log('Received:', event.data);
});
// Handle disconnection
socket.addEventListener('close', () => {
    showDisconnectedOverlay(true);
    reconnect(sessionId, customReceiveFunction);
});
```

### functions.js

Core utility functions for common operations:

* DOM element caching
* Modal management
* Session handling
* UI state management

Example usage:

```javascript
// Show a modal
showModal('settings');

// Get cached DOM element
const element = getCachedElement('modal-content');
// Toggle verbose mode
toggleVerbose();
```

### messageHandling.js

Message processing functionality with support for:

* Versioned messages
* Message substitution
* Depth-limited recursion
* Timeout protection

Message format:

```javascript
// Format: messageId,version,content
// Example: m1,1,Hello world
// Special prefixes:
// u* * User messages (e.g., u1, u2)
// z* * System messages (e.g., z1, z2)
// m* * Application messages (e.g., m1, m2)
```

### uiHandlers.js

UI event handling system with support for:

* Event delegation
* Action dispatching
* Keyboard shortcuts
* Modal management

Supported actions:

* userTxt: Submit user text input

## Supporting Modules

### appConfig.js

Application configuration management with:

* Runtime configuration updates
* Persistent settings
* Dynamic UI adaptation

Configuration options:

```javascript
{
    singleInput : boolean,    // Single/multiple input mode
    stickyInput : boolean,    // Input stays visible
    loadImages : boolean,     // Auto-load images
    showMenubar : boolean,    // Show/hide menubar
    applicationName : string  // Custom application title
}
```

### tabs.js

Tab system implementation featuring:

* Persistent tab state
* Nested tabs support
* Dynamic tab updates
* Memory efficient caching

HTML structure:

```html

<div class="tabs-container" id="main-tabs">
    <button class="tab-button" data-for-tab="tab1">Tab 1</button>
    <div class="tab-content" data-tab="tab1">Content 1</div>
    <!-* Nested tabs example -->
    <div class="tabs-container" id="nested-tabs">
        <button class="tab-button" data-for-tab="nested1">Nested 1</button>
        <div class="tab-content" data-tab="nested1">Nested Content 1</div>
    </div>
</div>
```

### theme.js

Theme management:
Available themes:

* main: Default theme
* night: Dark mode
* forest: Nature-inspired
* pony: Playful theme
* alien: Sci-fi theme

### uiSetup.js

UI initialization:
Features:

* Auto-expanding textareas
* Enter to submit (Shift+Enter for newline)
* Loading state indicators
* User authentication state management

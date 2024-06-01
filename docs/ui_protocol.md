# Developer Guide for the Skyenet UI System

## Overview

The Skyenet UI system is designed to facilitate real-time, interactive web applications using WebSockets. It provides a
framework for managing WebSocket connections, handling user interactions, and displaying dynamic content. The core
components of this system
include `SocketManagerBase`, `ApplicationInterface`, `SessionTask`, `TabbedDisplay`, `Retryable`, `AgentPatterns`,
and `Discussable`.

## Components

### 1. SocketManagerBase

`SocketManagerBase` is an abstract class that manages WebSocket connections, message queuing, and message broadcasting.

#### Key Methods:

- **addSocket(socket: ChatSocket, session: org.eclipse.jetty.websocket.api.Session)**: Adds a new WebSocket connection.
- **removeSocket(socket: ChatSocket)**: Removes an existing WebSocket connection.
- **send(out: String)**: Queues a message for sending.
- **getReplay(): List<String>**: Retrieves a list of messages for replay.
- **onWebSocketText(socket: ChatSocket, message: String)**: Handles incoming WebSocket messages.
- **hrefLink(linkText: String, classname: String = "href-link", id: String? = null, handler: Consumer<Unit>)**: Creates
  an HTML link that triggers a handler when clicked.
- **textInput(handler: Consumer<String>)**: Creates an HTML text input form that triggers a handler when submitted.

#### Example Usage:

```kotlin
class MySocketManager(
    session: Session,
    dataStorage: StorageInterface?,
    owner: User?,
    applicationClass: Class<*>
) : SocketManagerBase(session, dataStorage, owner, applicationClass) {

    override fun onRun(userMessage: String, socket: ChatSocket) {
        // Handle user message
    }
}
```

### 2. ApplicationInterface

`ApplicationInterface` provides methods to create interactive HTML elements and manage tasks.

#### Key Methods:

- **hrefLink(linkText: String, classname: String = "href-link", id: String? = null, handler: Consumer<Unit>)**: Creates
  an HTML link.
- **textInput(handler: Consumer<String>)**: Creates an HTML text input form.
- **newTask(root: Boolean = true): SessionTask**: Creates a new task for displaying progress.

#### Example Usage:

```kotlin
val appInterface = ApplicationInterface(socketManager)
val linkHtml = appInterface.hrefLink("Click me", handler = Consumer { println("Link clicked") })
val inputHtml = appInterface.textInput(handler = Consumer { input -> println("Input received: $input") })
```

### 3. SessionTask

`SessionTask` represents a task that can display progress and messages.

#### Key Methods:

- **add(message: String, showSpinner: Boolean = true, tag: String = "div", className: String = "response-message")**:
  Adds a message to the task output.
- **hideable(ui: ApplicationInterface?, message: String, showSpinner: Boolean = true, tag: String = "div", className:
  String = "response-message")**: Adds a hideable message.
- **echo(message: String, showSpinner: Boolean = true, tag: String = "div")**: Echos a user message.
- **header(message: String, showSpinner: Boolean = true, tag: String = "div", classname: String = "response-header")**:
  Adds a header message.
- **verbose(message: String, showSpinner: Boolean = true, tag: String = "pre")**: Adds a verbose message.
- **error(ui: ApplicationInterface?, e: Throwable, showSpinner: Boolean = false, tag: String = "div")**: Displays an
  error message.
- **complete(message: String = "", tag: String = "div", className: String = "response-message")**: Completes the task
  and hides the spinner.
- **image(image: BufferedImage)**: Displays an image.

#### Example Usage:

```kotlin
val task = appInterface.newTask()
task.add("Processing started")
task.complete("Processing complete")
```

### 4. TabbedDisplay

`TabbedDisplay` manages a tabbed interface for displaying content.

#### Key Methods:

- **render()**: Renders the tabbed interface as HTML.
- **clear()**: Clears all tabs.
- **update()**: Updates the content of the tabbed interface.
- **operator fun set(name: String, content: String)**: Sets the content of a tab by name.

#### Example Usage:

```kotlin
val tabbedDisplay = TabbedDisplay(task)
tabbedDisplay["Tab 1"] = "Content for Tab 1"
tabbedDisplay.update()
```

### 5. Retryable

`Retryable` extends `TabbedDisplay` to add a retry mechanism for tasks.

#### Key Methods:

- **renderTabButtons()**: Renders the tab buttons with a retry link.

#### Example Usage:

```kotlin
val retryable = Retryable(appInterface, task) { content ->
    // Process content
    "Processed content"
}
retryable.update()
```

### 6. AgentPatterns

`AgentPatterns` provides utility functions for displaying content in tabs.

#### Key Methods:

- **displayMapInTabs(map: Map<String, String>, ui: ApplicationInterface? = null, split: Boolean = map.entries.map {
  it.value.length + it.key.length }.sum() > 10000)**: Displays a map of content in tabs.

#### Example Usage:

```kotlin
val contentMap = mapOf("Tab 1" to "Content 1", "Tab 2" to "Content 2")
val tabsHtml = AgentPatterns.displayMapInTabs(contentMap, appInterface)
```

### 7. Discussable

`Discussable` facilitates interactive discussions with users, allowing for feedback and revisions.

#### Key Methods:

- **call(): T**: Starts the discussion and returns the final result.

#### Example Usage:

```kotlin
val discussable = Discussable(
    task = task,
    userMessage = { "User message" },
    initialResponse = { message -> "Initial response" },
    outputFn = { response -> "Output: $response" },
    ui = appInterface,
    reviseResponse = { history -> "Revised response" },
    heading = "Discussion"
)
val result = discussable.call()
```

## Conclusion

The Skyenet UI system provides a robust framework for building interactive web applications with WebSocket support. By
leveraging the provided components, developers can easily manage WebSocket connections, handle user interactions, and
display dynamic content in a structured and efficient manner.
# Tabbed UI Documentation

## Overview

The tabbed UI components in the codebase are specifically designed to facilitate the creation, management, and seamless
integration of tabbed interfaces within web applications. Ideal for applications requiring dynamic content switching
without page reloads, these components support various content types and are driven by user interactions or backend
processes. They are particularly useful in dashboards, multi-step forms, and settings panels.

## Components

### TabbedDisplay

`TabbedDisplay` is a base class for creating tabbed displays. It manages a list of tabs, each associated with a label
and content. It provides methods to render tabs, manage tab content, and handle user interactions to switch between
tabs.

#### Key Methods and Properties

### Retryable

`Retryable` extends `TabbedDisplay` to add functionality for retrying operations. It is particularly useful for
operations that might fail and need a retry mechanism, encapsulated within a tab.

#### Key Methods

### AgentPatterns

`AgentPatterns` contains utility methods for displaying content in a tabbed format without directly managing the tab
state.

#### Key Functions

a tabbed interface, optionally splitting content into separate tasks for performance.

### MarkdownUtil

`MarkdownUtil` provides utilities for rendering Markdown content into HTML, including special handling for Mermaid
diagrams and tabbed displays of Markdown versus rendered HTML.

#### Key Methods

Markdown into HTML, with support for Mermaid diagrams and an optional tabbed interface.

## Usage Examples

### Example 1: Basic Tabbed Display

```kotlin
val tabbedDisplay = TabbedDisplay(sessionTask)
tabbedDisplay.set("Tab 1", "Content for Tab 1")
tabbedDisplay.set("Tab 2", "Content for Tab 2")
tabbedDisplay.update()
```

This example demonstrates how to create a simple tabbed interface with two tabs.

Detailed examples of using these components can be added here, including code snippets and explanations of how to
integrate the tabbed UI into a web application.

## Conclusion

The tabbed UI components are versatile tools for building interactive and dynamic web interfaces. By understanding and
utilizing these components, developers can enhance the user experience of their applications.

### Tabs

The `TabbedDisplay` class in `TabbedDisplay.kt` implements a tabbed UI component that allows displaying multiple pieces
of content in a tabbed interface.

#### High Level Design

- The `TabbedDisplay` class maintains a list of tab name and content `StringBuilder` pairs.
- It provides methods to get/set tab content by name, find tabs by name, and update the rendered HTML.
- The `render()` method generates the HTML for the tabbed interface, including tab buttons and content divs.
- An instance keeps a reference to the `container` `StringBuilder` it is rendered into, allowing it to update itself.

#### Usage

To use the `TabbedDisplay` class:

1. Create an instance, passing the `SessionTask` it is associated with
2. Add tabs using the `set(name: String, content: String)` method. This will create the tab if it doesn't exist or
   update its content if it does.
3. Retrieve tab content using the `get(i: String)` method
4. Call `update()` after modifying tabs to re-render the component HTML

The tabbed content will automatically be displayed in the associated `SessionTask`.

### Retry

The `Retryable` class in `Retryable.kt` extends `TabbedDisplay` to add a "retry" button that re-runs a block of code and
adds the output as a new tab.

#### High Level Design

`Retryable` overrides the `renderTabButtons()` method to add a recycle ♻ button after the tab buttons. This button, when
clicked, triggers the retry mechanism.

- When clicked, the retry callback:
    1. Adds a new tab with a "Retrying..." placeholder and calls `update()`
    2. Runs the `process` lambda passed to the constructor, passing a `StringBuilder` for output
    3. Replaces the placeholder content with the final output
- This allows easily re-running a block of code and capturing the new output in a new tab

#### Usage

To use `Retryable`:

1. Create an instance, passing the `ApplicationInterface`, `SessionTask` and retry process lambda
2. The retry button will automatically be shown and will run the `process` lambda when clicked
3. The `process` lambda should return the `String` content to display in the new tab

### Example 2: Using Retryable

 ```kotlin
 val applicationInterface = getApplicationInterface() // Assume this returns an ApplicationInterface instance
val sessionTask = getSessionTask() // Assume this returns a SessionTask instance
val retryable = Retryable(applicationInterface, sessionTask) { stringBuilder ->
    try {
        // Code that might fail and needs retrying
        "Operation successful"
    } catch (e: Exception) {
        stringBuilder.append("Error encountered: ${e.message}")
        "Retry failed"
    }
}
retryable.update()
 ```

This example demonstrates how to use `Retryable` to add retry functionality. The lambda function provided to `Retryable`
is executed when the retry button is clicked. If the operation is successful, it returns a success message; otherwise,
it logs the error and returns a failure message.
By using `Retryable`, you can add retry functionality to a tabbed display with just a few lines of code.

### Acceptable

The `Acceptable` class is designed to handle user interactions that require acceptance or feedback before proceeding. It
extends the functionality of `TabbedDisplay` by integrating user input and decision-making processes directly into the
tabbed interface.

#### High Level Design

`Acceptable` manages a sequence of user interactions within a tabbed display, where each tab can represent a stage in a
decision-making process. It uses a combination of user prompts, text inputs, and acceptance links to gather and process
user feedback.

- The class initializes with a user message and a function to process the initial response.
- It dynamically adds tabs based on user interactions and updates the display accordingly.
- A feedback mechanism allows users to revise their responses, which the system processes to potentially alter the
  subsequent flow or decisions.

#### Key Methods

- `main()`: Orchestrates the initial display and subsequent updates based on user interactions.
- `feedbackForm()`: Generates the HTML form for user feedback within the tab.
- `acceptLink()`: Provides a link for the user to confirm their decision, moving the process to the next stage.

#### Usage

To use `Acceptable`:

1. Instantiate `Acceptable` with necessary parameters like the session task, user message, initial response processing
   function, and UI interface.
2. Use the `call()` method to start the interaction process and wait for the user's final acceptance.
3. The class handles user inputs and updates the tabbed display dynamically, reflecting the stages of user interaction
   and decision-making.

### Example 3: Using Acceptable

```kotlin
val sessionTask = getSessionTask() // Assume this returns a SessionTask instance
val userMessage = "Please review the information and accept to proceed."
val acceptable = Acceptable(
    task = sessionTask,
    userMessage = userMessage,
    initialResponse = { msg -> processInitialResponse(msg) },
    outputFn = { response -> response.toString() },
    ui = getApplicationInterface(), // Assume this returns an ApplicationInterface instance
    reviseResponse = { history -> reviseUserResponse(history) },
    heading = "User Acceptance Required"
)
acceptable.call()
```

This example demonstrates how to use `Acceptable` to manage a user acceptance process within a tabbed interface. The
user is prompted to review information and provide feedback or accept to proceed, with each stage managed as a separate
tab.

Let me know if you have any other questions!
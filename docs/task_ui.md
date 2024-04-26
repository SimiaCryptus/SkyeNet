# Task UI

The `SessionTask` class provides a way to display long-running tasks with progress updates in the UI. It allows you to
append messages, headers, errors, and images to the task output.

## Methods

### add(message: String, showSpinner: Boolean = true, tag: String = "div", className: String = "response-message")

Adds a message to the task output. The message will be wrapped in the specified HTML tag with the given CSS class.
If `showSpinner` is true, a loading spinner will be displayed after the message to indicate ongoing processing.
The `tag` parameter allows customization of the HTML tag used, and `className` specifies the CSS class for styling.

### hideable(ui: ApplicationInterface?, message: String, showSpinner: Boolean = true, tag: String = "div", className: String = "response-message"): StringBuilder?

Adds a hideable message to the task output. The message will include a close button, allowing the user to manually hide
the message. This method returns a `StringBuilder` instance containing the message, which can be manipulated further if
needed. The `ui` parameter is used to handle UI interactions for the close button.

### echo(message: String, showSpinner: Boolean = true, tag: String = "div")

Echos a user message to the task output.
This method is typically used for echoing user inputs or commands back to the UI for confirmation or logging purposes.

### header(message: String, showSpinner: Boolean = true, tag: String = "div", classname: String = "response-header")

Adds a header to the task output with the specified CSS class.
Headers are useful for separating sections of output or introducing new stages of task progress.

### verbose(message: String, showSpinner: Boolean = true, tag: String = "pre")

Adds a verbose message to the task output. Verbose messages are hidden by default and wrapped in a `<pre>` tag.
This method is ideal for displaying detailed diagnostic or debug information that can be expanded or collapsed by the
user.

### error(ui: ApplicationInterface?, e: Throwable, showSpinner: Boolean = false, tag: String = "div")

Displays an error in the task output. This method is specialized to handle different types of exceptions:

- `ValidationError`: Displays a message indicating a validation error along with a detailed stack trace.
- `FailedToImplementException`: Shows a message indicating a failure in implementation, including relevant code snippets
  and language details.
- Other exceptions: Displays the exception name and a complete stack trace to aid in debugging. The `showSpinner`
  parameter can be set to `false` to not show a spinner, as errors typically denote the end of processing.

### complete(message: String = "", tag: String = "div", className: String = "response-message")

Displays a final message in the task output and hides the spinner, indicating that the task has been completed. If no
message is provided, only the spinner will be hidden without any additional text.

### Placeholder Mechanism
 
Each `SessionTask` instance generates a unique placeholder in the UI, represented by an HTML `div` element with an `id`
attribute set to the task's `operationID`. This placeholder serves as a dynamic container where task-related updates and
outputs are rendered in real-time. This mechanism is crucial for maintaining a responsive and interactive user interface
during the execution of tasks.

### Non-Root Tasks

When creating a new task with the `newTask(root: Boolean)` method, setting `root` to `false` allows the creation of a
subordinate task. Non-root tasks are typically used for operations that are part of a larger task or workflow. They
inherit the context and permissions of the parent task, enabling structured and hierarchical task management within the
application.

### image(image: BufferedImage)

Displays an image in the task output. The image is saved as a PNG file, and the URL of the saved image is embedded
within an `<img>` tag to be displayed in the UI.

## Saving Files

The `saveFile(relativePath: String, data: ByteArray): String` method allows saving file data and returns the URL of the
saved file. This is useful for displaying images or providing file downloads in the task output.
This method is crucial for managing file outputs in tasks that involve file generation or manipulation, ensuring that
users can access or download the generated files directly from the task UI.

## Overview of the Task UI API

The Task UI API in the provided Kotlin codebase is designed to facilitate the creation and management of user interface
tasks within a web application. This API is part of a larger system that likely involves real-time interaction with
users through a web interface. The main components involved in the Task UI API
include `SessionTask`, `ApplicationInterface`, and utility functions and classes that support task management and
display.
The API's design focuses on providing a seamless and dynamic user experience, where tasks can be monitored and
controlled interactively, enhancing the overall user engagement and efficiency of the web application.

### Key Components

#### 1. `SessionTask`

`SessionTask` is an abstract class that represents a task session in the UI. It is designed to handle the dynamic output
of content to the user interface during the execution of a task. Key functionalities include:

- **Progress Tracking**: Allows real-time tracking of task progress through various states, providing immediate feedback
  to the user.
- **Interactive Elements**: Supports adding interactive elements like buttons and links within the task output, enabling
  user actions directly from the task interface.

- **Dynamic Content Management**: It manages a buffer that aggregates output content which can be dynamically updated
  and displayed in the UI.
- **Abstract Methods**:
    - `send(html: String)`: Sends the compiled HTML content to the UI.
    - `saveFile(relativePath: String, data: ByteArray)`: Saves a file and returns a URL to access it, used for handling
      file outputs like images.
- **Utility Methods**:
    - `add(message: String, showSpinner: Boolean, tag: String, className: String)`: Adds a message to the UI with
      configurable HTML wrapping and CSS styling.
    - `hideable(...)`, `echo(...)`, `header(...)`, `verbose(...)`, `error(...)`, `complete(...)`, `image(...)`: These
      methods provide specialized ways to add different types of content to the UI, such as hideable messages, errors,
      headers, and images.

#### 2. `ApplicationInterface`

`ApplicationInterface` serves as a bridge between the task management logic and the socket communication
layer (`SocketManagerBase`). It provides methods to interact with the user through hyperlinks and form inputs, and to
manage tasks:

- **Dynamic Task Creation**: Dynamically creates tasks based on user interactions or automated triggers, ensuring that
  each task is tailored to the specific needs of the operation.
- **Enhanced User Interaction**: Facilitates richer user interaction models by providing utility methods for creating
  interactive UI components like hyperlinks and text inputs.

- **Task Creation**: `newTask(root: Boolean)`: Creates a new `SessionTask` instance.
- **UI Element Creation**:
    - `hrefLink(...)`: Generates HTML for a clickable link that triggers a specified handler.
    - `textInput(...)`: Generates HTML for a text input form that triggers a specified handler upon submission.

#### 3. Utility Functions and Classes

- **`AgentPatterns`**: Contains utility functions like `displayMapInTabs(...)`, which helps in displaying data in a
  tabbed interface.
- **Error Handling**: Includes mechanisms to gracefully handle and display errors within the UI, ensuring that users are
  well-informed about any issues during task execution.
- **Session Management**: Provides robust session management capabilities to maintain the state and continuity of user
  tasks.
- **Image Handling**: The `toPng()` extension function for `BufferedImage` converts an image to PNG format, useful in
  tasks that involve image processing.

### Usage Example

To use the Task UI API, a developer would typically instantiate a `SessionTask` through `ApplicationInterface` and use
the task's methods to dynamically add content to the UI based on the application's logic and user interactions. For
example:

```kotlin
val appInterface = ApplicationInterface(socketManager)
val task = appInterface.newTask()
task.header("Processing Data")
// Perform some data processing
task.add("Data processed successfully", showSpinner = false)
task.complete("Task completed.")
```

### Conclusion

The Task UI API provides a robust set of tools for managing interactive tasks in a web application. By abstracting the
complexities of real-time UI updates and task management, it allows developers to focus on the core logic of their
applications while providing a responsive and interactive user experience.

The `SessionTask` class in the Kotlin codebase provides a mechanism to manage and display linked tasks in a web UI,
particularly through the use of placeholders. This functionality is crucial for tasks that are interdependent or need to
be executed in a sequence, allowing the UI to dynamically update as tasks progress or complete.

## Placeholders

The placeholder is a unique identifier used to represent a task in the UI dynamically. It allows the system to update
the task's output in real-time without reloading the entire page. Here's how it is implemented and used:

#### Placeholder Generation

Each `SessionTask` instance has an `operationID`, which is a unique identifier for that task. The `placeholder` is an
HTML `div` element with its `id` attribute set to the `operationID`. This `div` acts as a placeholder in the HTML
document where the task's output will be dynamically inserted or updated.

```kt
val placeholder: String get() = "<div id=\"$operationID\"></div>"
```

#### Using the Placeholder

When a new task is created, its placeholder is initially empty. As the task progresses, messages, errors, images, or
other outputs are dynamically inserted into this placeholder using JavaScript and WebSocket communication. This approach
allows the UI to remain responsive and update in real-time as the task outputs change.

### Example of Placeholder Usage

Here’s a simplified example to illustrate how placeholders are typically used in the system:

1. **Task Creation**: A new `SessionTask` is instantiated, and its placeholder is added to the web page.
   ```kt
   val task = appInterface.newTask()
   val placeholderHtml = task.placeholder
   ```

2. **Task Execution**: The task performs its operations, during which it may use methods like `add`, `error`,
   or `complete` to update its output.
   ```kt
   task.add("Processing data...")
   // Some processing happens here
   task.complete("Processing complete.")
   ```

3. **Dynamic UI Update**: As the task updates its output, these changes are sent to the client's browser using WebSocket
   messages. The JavaScript on the client side listens for these messages and updates the inner HTML of the `div` with
   the corresponding `operationID`.

4. **Final Output**: Once the task completes, the final message is displayed in the placeholder, and no further updates
   occur unless a new task is linked or started.

### Conclusion

The placeholder mechanism in `SessionTask` is a powerful feature that supports dynamic and real-time updates to the web
UI without requiring page refreshes. It is especially useful in applications that involve complex or long-running tasks,
providing users with immediate feedback and enhancing the interactivity of the application.
# main\kotlin\com\simiacryptus\skyenet\apps\coding\CodingApp.kt

## CodingApp Class Documentation

The `CodingApp` class is a part of the `com.simiacryptus.skyenet.apps.coding` package and extends the functionality of
the `ApplicationServer` from the `com.simiacryptus.skyenet.webui.application` package. This class is designed to
integrate a coding environment into a web application, leveraging an interpreter for code execution. It facilitates user
interaction with the coding environment through messages and executes code using a specified interpreter.

### Constructor

```kotlin
CodingApp < T: Interpreter > (
    applicationName: String,
private val interpreter: KClass<T>,
open val symbols: Map<String, Any> = mapOf(),
val temperature: Double = 0.1
)
```

#### Parameters

- `applicationName: String` - The name of the application. This is used as an identifier and title for the application
  server.
- `interpreter: KClass<T>` - The Kotlin class (KClass) of the interpreter to be used for executing code. The interpreter
  class must extend the `Interpreter` interface.
- `symbols: Map<String, Any>` - An optional map of symbols (variables and functions) that should be preloaded or made
  available in the interpreter's execution context. Defaults to an empty map.
- `temperature: Double` - An optional parameter influencing the randomness or creativity of the interpreter's output (if
  applicable). Defaults to 0.1.

### Methods

#### userMessage

```kotlin
override fun userMessage(
  session: Session,
  user: User?,
  userMessage: String,
  ui: ApplicationInterface,
  api: API
)
```

This method is invoked when a user sends a message to the application. It is responsible for setting up and starting
a `CodingAgent` instance to handle the user's request.

##### Parameters

- `session: Session` - The current session object, encapsulating session-related data such as cookies and session
  variables.
- `user: User?` - An optional user object representing the currently authenticated user. May be null if the user is not
  authenticated.
- `userMessage: String` - The message sent by the user. In the context of a coding application, this could be code to
  execute or commands for the interpreter.
- `ui: ApplicationInterface` - The user interface abstraction layer, allowing interaction with the web UI.
- `api: API` - An API object providing access to external services or functionalities. In this context, it is cast
  to `ClientManager.MonitoredClient` to set a budget constraint for the operation.

### Usage Example

To create an instance of `CodingApp` with a specific interpreter:

```kotlin
val myCodingApp = CodingApp(
  applicationName = "My Coding App",
  interpreter = MyCustomInterpreter::class,
  symbols = mapOf("println" to fun(s: String) { println(s) }),
  temperature = 0.2
)
```

In this example, `MyCustomInterpreter::class` refers to a custom interpreter class that extends the `Interpreter`
interface. The `symbols` map includes a custom `println` function as an example of preloading symbols into the
interpreter's execution context.

### Conclusion

The `CodingApp` class provides a foundational framework for integrating coding and execution environments into web
applications. By customizing the interpreter and preloaded symbols, developers can tailor the coding experience to fit
the specific needs of their application.

# main\kotlin\com\simiacryptus\skyenet\apps\general\WebDevAgent.kt

## WebDevApp & WebDevAgent Documentation

The `WebDevApp` and `WebDevAgent` classes are part of a web development assistant application designed to facilitate the
creation of web applications by interacting with users and generating code based on their requirements. This
documentation provides an overview of these classes, their functionalities, and how they interact with other components.

### Overview

- **WebDevApp**: This class extends `ApplicationServer` and serves as the entry point for the web development assistant
  application. It handles user messages and initializes the `WebDevAgent` with appropriate settings.
- **WebDevAgent**: Acts as the orchestrator for generating web development code. It utilizes an `ActorSystem` to manage
  different actors responsible for generating HTML, CSS, JavaScript code, and discussing architecture based on user
  inputs.

### WebDevApp

#### Key Features

- **User Message Handling**: Processes messages from users and initializes a `WebDevAgent` with settings such as budget,
  tools, and model preferences.
- **Settings Management**: Stores and retrieves user-specific settings like budget, tools, and the model to be used for
  code generation.

#### Important Methods

- `userMessage(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API)`: Handles
  messages from users. It prepares settings and starts a `WebDevAgent` instance to process the user's request.
- `initSettings(session: Session)`: Initializes default settings for new users.

### WebDevAgent

#### Key Features

- **Code Generation**: Generates HTML, CSS, and JavaScript code based on user requirements.
- **Architecture Discussion**: Discusses and outlines the high-level architecture for a web development project.
- **Resource Management**: Parses and generates additional resources required for the web application.

#### Actors

- **HtmlCodingActor**: Generates a skeleton HTML file.
- **JavascriptCodingActor**: Generates JavaScript code.
- **CssCodingActor**: Generates CSS code.
- **ResourceListParser**: Parses the list of page resources.
- **ArchitectureDiscussionActor**: Outlines the project's high-level architecture.

#### Important Methods

- `start(userMessage: String)`: Initiates the code generation process by discussing architecture and generating initial
  HTML code.
- `draftHtmlCode(task: SessionTask, request: Array<ApiModel.ChatMessage>)`: Generates HTML code based on user input and
  discussion results.
- `draftResourceCode(task: SessionTask, request: Array<ApiModel.ChatMessage>, actor: SimpleActor, path: String, languages: Array<String>)`:
  Generates resource code (CSS/JS) based on user input.
- `append(codeRequest: Array<ApiModel.ChatMessage>, response: String)`: Appends the assistant's response to the chat
  messages array.
- `generateResourcesButton(...)`: Generates a button in the UI to trigger the resource code generation process.

#### Interfaces and Data Classes

- **PageResourceListParser**: Interface for parsing HTML to identify resources.
- **PageResourceList**: Data class representing a list of resources.
- **PageResource**: Represents an individual resource with a path and description.

#### Usage

1. **Initialization**: An instance of `WebDevApp` is created and configured to listen for user messages.
2. **User Interaction**: When a user sends a message, `WebDevApp` processes it and creates an instance of `WebDevAgent`.
3. **Code Generation**: `WebDevAgent` utilizes various actors to generate code and resources based on the user's
   requirements and inputs.

#### Logging

The `WebDevAgent` class uses SLF4J for logging purposes, facilitating the tracking of its operations and any errors that
occur during the execution.

### Conclusion

The `WebDevApp` and `WebDevAgent` classes provide a powerful framework for automating the generation of web development
code, making it easier for developers to kickstart their projects. Through an interactive process, users can specify
their requirements, and the system generates the necessary code and resources, streamlining the development workflow.

# main\kotlin\com\simiacryptus\skyenet\apps\coding\CodingAgent.kt

#### CodingAgent Class Documentation

The `CodingAgent` class is a part of the `com.simiacryptus.skyenet.apps.coding` package, designed to facilitate the
interaction between users and code generation services, leveraging models from the OpenAI API. It extends the
functionality of `ActorSystem` and integrates with various components of the application, including storage interfaces,
session management, and user interfaces.

##### Key Components

- **API Integration**: Utilizes the OpenAI API for generating code responses based on user inputs.
- **Interpreter**: Supports different interpreters for executing the generated code.
- **User Interface**: Communicates with users through a web interface, displaying code, feedback, and options for
  further actions.
- **Session Management**: Manages user sessions to keep track of interactions and code generation history.
- **Authorization**: Checks if a user is authorized to execute code based on predefined operation types.

##### Main Features

- **Code Generation and Display**: Generates code based on user prompts and displays it through the user interface.
- **Feedback Loop**: Allows users to provide feedback on generated code, which can be used for improvements or
  adjustments.
- **Execution of Code**: Provides functionality to execute generated code and display the output or result back to the
  user.
- **Error Handling**: Catches and displays errors during code generation, execution, or other processes.

##### Methods

- **start(userMessage: String)**: Initializes the code generation process based on a user's message.
- **displayCode(task: SessionTask, codeRequest: CodingActor.CodeRequest)**: Displays the generated code to the user.
- **displayFeedback(task: SessionTask, request: CodingActor.CodeRequest, response: CodeResult)**: Displays options for
  the user to provide feedback, regenerate code, or execute the code.
- **execute(task: SessionTask, response: CodeResult, request: CodingActor.CodeRequest)**: Executes the generated code
  and displays the result or output.

##### Usage Example

```kotlin
// Initialize API client with your OpenAI API key
val apiClient = API("your_api_key")

// Create an instance of CodingAgent with necessary parameters
val codingAgent = CodingAgent(
  api = apiClient,
  dataStorage = yourDataStorageInstance,
  session = currentSession,
  user = currentUser,
  ui = yourApplicationInterface,
  interpreter = YourInterpreter::class,
  symbols = mapOf("exampleSymbol" to Any()),
  temperature = 0.1,
  details = "Optional details",
  model = ChatModels.GPT35Turbo
)

// Start the code generation process with a user message
codingAgent.start("Generate a hello world program in Python")
```

##### Dependencies

- **OpenAI API Client**: For interacting with OpenAI's services.
- **Application-Specific Classes**: Such as `StorageInterface`, `Session`, `User`, `ApplicationInterface`, and
  interpreters.
- **Kotlin Reflect**: For handling interpreter classes dynamically.

##### Error Handling

The class includes comprehensive error handling to manage exceptions that may occur during API calls, code generation,
or execution. Errors are logged and communicated back to the user through the web interface.

##### Conclusion

The `CodingAgent` class provides a robust framework for integrating code generation and execution capabilities into
applications. By leveraging OpenAI's powerful models and providing user interaction and feedback mechanisms, it offers a
dynamic tool for enhancing coding-related features in software projects.

# main\kotlin\com\simiacryptus\skyenet\apps\coding\ToolAgent.kt

## ToolAgent Class Documentation

The `ToolAgent` class is an abstract class designed to facilitate the interaction between users and code generation
tools within a web application context. It extends the `CodingAgent` class, integrating functionalities such as code
generation, feedback display, and OpenAPI specification generation. This class is part of a larger system that aims to
automate various aspects of coding and software development through AI-driven tools.

### Package

```kotlin
package com.simiacryptus.skyenet.apps.coding
```

### Imports

The class uses a variety of imports from different packages to support its functionalities, including web servlet
handling, file operations, logging, and JSON utilities.

### Constructor

The `ToolAgent` class constructor initializes the agent with a set of parameters required for its operation:

```kotlin
ToolAgent < T : Interpreter > (
    api: API,
dataStorage: StorageInterface,
session: Session,
user: User?,
ui: ApplicationInterface,
interpreter: KClass<T>,
symbols: Map<String, Any>,
temperature: Double = 0.1,
details: String?,
model: ChatModels = ChatModels.GPT35Turbo,
actorMap: Map<ActorTypes, CodingActor>
)
```

#### Parameters

- `api`: The API client instance for interacting with the AI model.
- `dataStorage`: Interface for data storage operations.
- `session`: Current session information.
- `user`: The user object, nullable.
- `ui`: Interface for application UI interactions.
- `interpreter`: The class of the interpreter to use.
- `symbols`: A map of symbols for code interpretation.
- `temperature`: The AI model's temperature for code generation.
- `details`: Additional details for the AI model.
- `model`: The AI model to use for code generation.
- `actorMap`: A map of actor types to their corresponding `CodingActor` instances.

### Methods

#### displayFeedback

Displays feedback for a code generation task.

```kotlin
override fun displayFeedback(task: SessionTask, request: CodingActor.CodeRequest, response: CodeResult)
```

#### createToolButton

Generates a button for exporting generated code to a tool.

```kotlin
private fun createToolButton(
  task: SessionTask,
  request: CodingActor.CodeRequest,
  response: CodeResult,
  formText: StringBuilder,
  formHandle: () -> StringBuilder
): String
```

#### openAPIParsedActor

Creates an actor for parsing OpenAPI specifications.

```kotlin
private fun openAPIParsedActor(): ParsedActor<OpenAPI>
```

#### servletActor

Creates an actor for generating servlet code.

```kotlin
private fun servletActor(): CodingActor
```

#### schemaActor

Creates an actor for generating data schema code.

```kotlin
private fun schemaActor(): CodingActor
```

#### displayCodeFeedback

Displays feedback for code generation with an option for user interaction.

```kotlin
private fun displayCodeFeedback(
  task: SessionTask,
  actor: CodingActor,
  request: CodingActor.CodeRequest,
  response: CodeResult = Companion.execWrap { actor.answer(request, api = api) },
  onComplete: (String) -> Unit
)
```

#### buildTestPage

Generates a test page for the servlet.

```kotlin
private fun buildTestPage(openAPI: OpenAPI, servletImpl: String, task: SessionTask)
```

#### getInterpreterString

Abstract method to get the interpreter string.

```kotlin
abstract fun getInterpreterString(): String
```

### Companion Object

Contains a logger and an `execWrap` function to execute code within a specific class loader context.

### Usage

This class is designed to be extended by specific tool agents that implement the abstract `getInterpreterString` method
and possibly override other methods to customize behavior. It facilitates the integration of AI-driven code generation
into web applications, enhancing the development process with automated code suggestions, feedback, and tool generation
capabilities.

# main\kotlin\com\simiacryptus\skyenet\interpreter\ProcessInterpreter.kt

## ProcessInterpreter Class Documentation

The `ProcessInterpreter` class is a part of the `com.simiacryptus.skyenet.interpreter` package and extends
functionalities for interpreting and executing code in a specified programming language within a separate process. This
class is designed to be flexible, allowing the execution of commands in various languages by configuring its behavior
through a definitions map (`defs`).

### Constructor

#### ProcessInterpreter(defs: Map<String, Any> = mapOf())

Initializes a new instance of the `ProcessInterpreter` class with optional definitions to customize its behavior.

- **Parameters:**
    - `defs`: A map of definitions to configure the interpreter. Supported keys include:
        - `command`: Specifies the command to execute. Can be a single string or a list of strings representing the
          command and its arguments.
        - `language`: Indicates the programming language. Defaults to "bash" if not specified.
        - `workingDir`: Sets the working directory for the process.
        - `env`: A map of environment variables to set for the process.

### Properties

#### command: List<String>

A read-only property that returns the command to be executed as a list of strings. The command is derived from
the `defs` map and can be specified as either a single string or a list of strings.

#### getLanguage(): String

Returns the programming language specified in the `defs` map. Defaults to "bash" if not specified.

#### getSymbols(): Map<String, Any>

Returns the definitions map (`defs`) used to configure the interpreter.

### Methods

#### validate(code: String): Throwable?

Validates the provided code. This implementation always considers the code valid and returns `null`.

- **Parameters:**
    - `code`: The code to validate.
- **Returns:** Always returns `null`, indicating no validation errors.

#### run(code: String): Any?

Executes the provided code within a separate process according to the configured command and environment.

- **Parameters:**
    - `code`: The code to execute.
- **Returns:** The output of the executed code if successful. If an error occurs during execution, it returns a
  formatted string containing both the error and output. If the process times out, a `RuntimeException` is thrown.

- **Throws:**
    - `IllegalArgumentException`: If the command specified in `defs` is invalid.
    - `RuntimeException`: If the process times out.

### Companion Object

Contains a logger for the `ProcessInterpreter` class.

### Usage Example

To use the `ProcessInterpreter`, you may initialize it with a map of definitions specifying the desired command,
language, working directory, and environment variables. Here's a simple example:

```kotlin
val defs = mapOf(
  "command" to "python3",
  "language" to "python",
  "workingDir" to "/path/to/working/directory",
  "env" to mapOf("PYTHONPATH" to "/path/to/lib")
)

val interpreter = ProcessInterpreter(defs)
val output = interpreter.run("print('Hello, world')")
println(output)
```

This example sets up a `ProcessInterpreter` to execute Python code, prints "Hello, world", and then outputs the result.

### Notes

- The `ProcessInterpreter` class is designed to be open, allowing for extension and customization.
- The execution environment (e.g., working directory, environment variables) can be tailored to fit specific
  requirements through the `defs` map.
- The `run` method captures both standard output and error output from the executed process, providing comprehensive
  feedback on execution results.

# main\kotlin\com\simiacryptus\skyenet\webui\application\ApplicationDirectory.kt

## ApplicationDirectory Class Documentation

The `ApplicationDirectory` class is a Kotlin abstract class designed to facilitate the creation and management of web
applications using the Jetty server. It provides a structured way to set up web applications with authentication,
various servlets, and WebSocket support. This class is part of a larger framework aimed at building web UIs and
applications, with a focus on chat servers as a specific use case.

### Overview

The `ApplicationDirectory` class serves as a base for creating web application directories, handling server
initialization, servlet mapping, resource serving, and optional OAuth-based authentication. It abstracts away the
boilerplate needed to start a Jetty server and deploy web applications, allowing developers to focus on the specifics of
their application logic.

### Key Components

#### Properties

- `localName`: The local hostname, defaulting to "localhost".
- `publicName`: The public hostname, default to "localhost".
- `port`: The port on which the server will listen, defaulting to 8081.
- `domainName`: The resolved domain name, set during initialization.
- `childWebApps`: A list of child web applications, each represented by a `ChildWebApp` data class containing a path and
  a chat server instance.

#### Abstract Members

- `childWebApps`: Abstract property that should be overridden to define child web applications.
- `toolServlet`: An optional abstract property for a servlet related to specific tools, which can be null.

#### Methods

##### Initialization and Server Startup

- `_main(args: Array<String>)`: The entry point for initializing and starting the server. It sets up the platform,
  deciphers keys, creates web contexts, and starts the Jetty server.
- `init(isServer: Boolean)`: Initializes the application directory, setting up interceptors and resolving the domain
  name.
- `start(port: Int, vararg webAppContexts: WebAppContext)`: Starts the Jetty server on the specified port with the
  provided web application contexts.

##### Servlet and WebAppContext Creation

- `newWebAppContext(path: String, server: ChatServer)`: Creates a `WebAppContext` for a given chat server.
- `newWebAppContext(path: String, baseResource: Resource, resourceBase: String, indexServlet: Servlet? = null)`:
  Overloaded method to create a `WebAppContext` with more customization options.
- `newWebAppContext(path: String, servlet: Servlet)`: Creates a `WebAppContext` for a specific servlet.

##### Utility Methods

- `setupPlatform()`: Configures the platform-specific settings, like Selenium factory setup.
- `httpConnectionFactory()`: Configures and returns an `HttpConnectionFactory` for the server.

#### Companion Object

Contains utility methods and properties, such as `allResources(resourceName: String)` for loading resources and a logger
instance.

### Usage

To use the `ApplicationDirectory` class, one must extend it and implement the abstract properties and methods, providing
the specifics of the child web applications and any additional servlets or tools required. The `_main` method serves as
the starting point, which should be called with the necessary arguments to initialize and start the server.

### Example

```kotlin
class MyApplicationDirectory : ApplicationDirectory() {
  override val childWebApps = listOf(
    ChildWebApp("/chat", ChatServer())
  )

  override val toolServlet: ToolServlet? = MyToolServlet()

  // Implement other abstract members and methods as needed
}

fun main(args: Array<String>) {
  MyApplicationDirectory()._main(args)
}
```

This example defines a specific application directory with a chat server and a tool servlet, then starts the
application.

# main\kotlin\com\simiacryptus\skyenet\webui\application\ApplicationServer.kt

## Developer Documentation for ApplicationServer

The `ApplicationServer` class is an abstract class that extends the `ChatServer` class, providing a framework for
building web applications with specific functionalities such as user authentication, session management, file storage,
and real-time communication. It is part of the `com.simiacryptus.skyenet.webui.application` package.

### Overview

The `ApplicationServer` class is designed to serve as the backbone for web applications, encapsulating common
server-side logic such as handling requests for application and user information, managing user sessions, and providing
access to stored files. It leverages servlets for handling different types of requests and employs a filter to enforce
access control based on user authorization.

#### Key Properties

- `applicationName`: A `String` representing the name of the application.
- `path`: The base path for the application on the server.
- `resourceBase`: The base directory for the application's static resources.
- `root`: A `File` object representing the root directory for the application's data storage.
- `description`, `singleInput`, `stickyInput`: Configurable properties about the application.
- `appInfo`: A lazy-initialized property that holds application-specific information.
- `dataStorage`: An instance of `StorageInterface` for data storage operations.

#### Servlets

The class initializes several `ServletHolder` objects for handling specific types of requests:

- `appInfoServlet`: Serves application information.
- `userInfo`: Handles requests for user information.
- `usageServlet`: Provides usage statistics.
- `fileZip`, `fileIndex`: Serve requests for accessing and downloading files.
- `sessionSettingsServlet`, `sessionShareServlet`, `sessionThreadsServlet`, `deleteSessionServlet`, `cancelSessionServlet`:
  Handle various session-related operations.

#### Methods

- `newSession(user: User?, session: Session)`: Creates a new `SocketManager` instance for managing real-time
  communication for a session.
- `userMessage(...)`: A method to be overridden for handling user messages.
- `getSettings(...)`: Retrieves user-specific settings.
- `configure(webAppContext: WebAppContext)`: Configures the web application context, adding filters and servlets.

#### Filters

The class adds a filter to the web application context to enforce access control based on user authorization.

### Usage

To use the `ApplicationServer` class, one must extend it and implement the abstract methods. Here's a simplified
example:

```kotlin
class MyApplicationServer : ApplicationServer("MyApp", "/myapp") {
  override fun userMessage(
    session: Session,
    user: User?,
    userMessage: String,
    ui: ApplicationInterface,
    api: API
  ) {
    // Handle user message
  }
}

fun main() {
  val server = MyApplicationServer()
  server.start()
}
```

This example demonstrates creating a simple application server with custom logic for handling user messages.
The `start()` method (inherited from `ChatServer`) is called to start the server.

### Conclusion

The `ApplicationServer` class provides a robust foundation for building web applications with real-time communication
capabilities, user authentication, and session management. By extending this class and implementing the required
methods, developers can focus on application-specific logic while leveraging the provided infrastructure for common
server-side functionalities.

# main\kotlin\com\simiacryptus\skyenet\webui\application\ApplicationSocketManager.kt

## ApplicationSocketManager Class Documentation

`ApplicationSocketManager` serves as an abstract base class designed to manage WebSocket connections for specific
application functionalities within a web-based application. It extends the `SocketManagerBase` class, integrating with a
chat interface and handling user messages through WebSocket connections. This class is a part of
the `com.simiacryptus.skyenet.webui.application` package.

### Constructor

```kotlin
ApplicationSocketManager(
  session: Session,
  owner: User?,
dataStorage: StorageInterface?,
applicationClass: Class<*>,
)
```

#### Parameters

- `session`: An instance of `Session` representing the current session.
- `owner`: An optional `User` instance representing the owner of the session. Can be null.
- `dataStorage`: An optional `StorageInterface` instance for data storage. Can be null.
- `applicationClass`: A `Class<*>` instance representing the application's class.

### Methods

#### onRun

```kotlin
override fun onRun(userMessage: String, socket: ChatSocket)
```

This method is called when a user message is received through the WebSocket. It processes the user message and interacts
with the application-specific functionalities.

##### Parameters

- `userMessage`: The message sent by the user.
- `socket`: An instance of `ChatSocket` representing the WebSocket connection through which the message was received.

#### userMessage (Abstract)

```kotlin
abstract fun userMessage(
  session: Session,
  user: User?,
  userMessage: String,
  socketManager: ApplicationSocketManager,
  api: API
)
```

An abstract method that must be implemented by subclasses to define how user messages are handled within the application
context.

##### Parameters

- `session`: The current `Session` instance.
- `user`: An optional `User` instance representing the message sender. Can be null.
- `userMessage`: The message sent by the user.
- `socketManager`: The `ApplicationSocketManager` instance managing the WebSocket connection.
- `api`: An instance of `API` for interacting with external services or APIs.

### Properties

#### applicationInterface

```kotlin
open val applicationInterface: ApplicationInterface
```

A lazy-initialized property that provides an instance of `ApplicationInterface`, which serves as an interface for
application-specific functionalities.

### Companion Object

#### spinner

```kotlin
val spinner: String
```

A companion object property that provides an HTML string for a loading spinner, which can be used in the web UI to
indicate a loading or processing state.

### Usage

Due to its abstract nature, `ApplicationSocketManager` must be extended by a concrete class that implements
the `userMessage` method to handle application-specific user messages. This subclass can then be utilized within the
application to manage WebSocket connections, process user messages, and interact with the application's functionalities
through the provided `API` instance.

```kotlin
class MyApplicationSocketManager(
  session: Session,
  owner: User?,
  dataStorage: StorageInterface?,
  applicationClass: Class<*>
) : ApplicationSocketManager(session, owner, dataStorage, applicationClass) {
  override fun userMessage(
    session: Session,
    user: User?,
    userMessage: String,
    socketManager: ApplicationSocketManager,
    api: API
  ) {
    // Implementation specific to MyApplication
  }
}
```

This setup allows for a flexible and modular approach to handling WebSocket connections and user interactions within
different parts of the web application.

# main\kotlin\com\simiacryptus\skyenet\webui\application\ApplicationInterface.kt

## Developer Documentation for ApplicationInterface

The `ApplicationInterface` class provides an abstraction layer over the `ApplicationSocketManager`, facilitating the
creation of web UI components such as hyperlinks, text input forms, and tasks for tracking long-running operations.
Below are the key functionalities provided by this class.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.application`
- **Dependencies**:
    - `com.simiacryptus.jopenai.describe.Description`: Used for annotating methods and parameters with descriptions.
    - `com.simiacryptus.skyenet.webui.session.SessionTask`: Represents a task within a session.
    - `java.util.concurrent.atomic.AtomicBoolean`: Used for ensuring that a task is executed one at a time.
    - `java.util.function.Consumer`: Represents an operation that accepts a single input argument and returns no result.

### Methods

#### `hrefLink`

Generates HTML for a clickable link that triggers a specified handler when clicked.

##### Parameters

- `linkText` (`String`): The text to display for the link.
- `classname` (`String`): The CSS class to apply to the link. Defaults to `"href-link"`.
- `handler` (`Consumer<Unit>`): The action to perform when the link is clicked.

##### Returns

- `String`: HTML for the clickable link.

##### Example Usage

```kotlin
val linkHtml = applicationInterface.hrefLink("Click Me", "custom-class") {
  // Handler code goes here
}
```

#### `textInput`

Creates HTML for a text input form that triggers a specified handler upon submission.

##### Parameters

- `handler` (`Consumer<String>`): The action to perform when the form is submitted, receiving the input text as a
  parameter.

##### Returns

- `String`: HTML for the text input form.

##### Example Usage

```kotlin
val formHtml = applicationInterface.textInput { inputText ->
  // Handler code for processing inputText
}
```

#### `newTask`

Initiates a new session task that can be used to display the progress of a long-running operation. Currently, the method
does not support cancelable tasks and defaults to non-cancelable tasks.

##### Returns

- `SessionTask`: An object representing the new task.

##### Example Usage

```kotlin
val task = applicationInterface.newTask()
// Use the task to track progress of a long operation
```

### Utility Methods

#### `oneAtATime`

A companion object method that wraps a given handler to ensure it is executed one at a time, preventing concurrent
execution.

##### Type Parameters

- `T`: The type of the input to the handler.

##### Parameters

- `handler` (`Consumer<T>`): The original handler to be wrapped.

##### Returns

- `Consumer<T>`: A wrapped handler that ensures one-at-a-time execution.

##### Example Usage

This method is used internally by `hrefLink` and `textInput` methods to wrap the provided handlers.

---

This documentation provides an overview of the `ApplicationInterface` class functionalities and how to utilize them to
create web UI components efficiently.

# main\kotlin\com\simiacryptus\skyenet\webui\chat\ChatSocket.kt

## ChatSocket Class Documentation

The `ChatSocket` class is part of the `com.simiacryptus.skyenet.webui.chat` package and extends the `WebSocketAdapter`
provided by the Jetty WebSocket API. This class is designed to handle WebSocket connections for a chat application,
facilitating real-time communication between the server and clients.

### Overview

The `ChatSocket` class integrates with the `SocketManager` to manage WebSocket sessions and handle incoming and outgoing
messages. It provides mechanisms to connect, receive messages, and close WebSocket sessions. It also interacts with
the `SocketManagerBase` to retrieve user information associated with each session.

### Constructor

```kotlin
ChatSocket(private val sessionState : SocketManager)
```

- **Parameters:**
    - `sessionState`: An instance of `SocketManager` that manages WebSocket sessions and messages.

### Properties

- **user**: Retrieves the user associated with the current WebSocket session.

### Methods

#### onWebSocketConnect(session: Session)

Called when a WebSocket connection is established. It registers the socket with the `SocketManager`, and replays any
messages that need to be sent to the client immediately upon connection.

- **Parameters:**
    - `session`: The `Session` instance representing the established WebSocket connection.

#### onWebSocketText(message: String)

Invoked when a text message is received from the client. It forwards the message to the `SocketManager` for processing.

- **Parameters:**
    - `message`: The text message received from the client.

#### onWebSocketClose(statusCode: Int, reason: String?)

Called when the WebSocket connection is closed. It unregisters the socket from the `SocketManager`.

- **Parameters:**
    - `statusCode`: The status code indicating the reason for closure.
    - `reason`: An optional string providing more detail about the closure reason.

### Companion Object

- **log**: A logger instance for logging debug or error information related to WebSocket connections.

### Usage Example

Below is a hypothetical scenario demonstrating how an instance of `ChatSocket` might be utilized within a chat
application:

```kotlin
// Assuming a server setup that initializes WebSocket endpoints
val socketManager = SocketManager()
val chatSocket = ChatSocket(socketManager)

// The server would typically handle the lifecycle of WebSocket connections automatically,
// invoking onWebSocketConnect, onWebSocketText, and onWebSocketClose as appropriate.
```

### Notes

This class relies heavily on the Jetty WebSocket API and the custom `SocketManager` class for managing WebSocket
sessions and messages. It is designed for use within a chat application, but the principles demonstrated could be
adapted for other types of real-time communication applications.

Remember to handle exceptions and errors gracefully, especially in the `onWebSocketConnect` method when sending replay
messages, to ensure a robust and stable application.

# main\kotlin\com\simiacryptus\skyenet\webui\chat\ChatServer.kt

## ChatServer Class Documentation

The `ChatServer` class is an abstract class designed to facilitate the creation and management of a chat server using
Jetty as the underlying web server technology. It provides a structured way to handle WebSocket connections for
real-time messaging, manage sessions, and integrate with authentication and data storage systems.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.chat`
- **Dependencies**: Requires Jetty server libraries for web and WebSocket support.

### Key Components

#### Properties

- `applicationName`: An abstract property that should be overridden to specify the name of the application.
- `dataStorage`: An open property that can be overridden to provide integration with a data storage interface. It is
  nullable.
- `sessions`: A mutable map that tracks active sessions and their corresponding `SocketManager` instances.

#### Inner Classes

- `WebSocketHandler`: A class that extends `JettyWebSocketServlet` to configure WebSocket behavior and handle incoming
  WebSocket connections. It is responsible for creating `ChatSocket` instances based on session information.

#### Abstract Methods

- `newSession(user: User?, session: Session): SocketManager`: An abstract method that must be implemented to create a
  new `SocketManager` instance for a given user and session. This method is called when a new WebSocket connection is
  established without an existing session.

#### Open Methods

- `configure(webAppContext: WebAppContext)`: An open method that can be overridden to add additional servlets or
  configure the `WebAppContext`. By default, it sets up servlets for handling default requests, WebSocket connections,
  and new session creation.

### Usage

To use the `ChatServer` class, one must create a subclass that implements the abstract properties and methods. Here's a
simplified example:

```kotlin
class MyChatServer(resourceBase: String) : ChatServer(resourceBase) {
  override val applicationName = "MyChatApp"

  override fun newSession(user: User?, session: Session): SocketManager {
    // Implement logic to create and return a SocketManager instance for the new session
  }
}
```

In the subclass, you must provide the application name and implement the `newSession` method to handle session creation.
You may also override the `dataStorage` property if your application requires integration with a storage system.

### Additional Information

- The `WebSocketHandler` inner class is a critical component that manages WebSocket connections. It uses the Jetty
  WebSocket API to configure connection parameters such as timeouts, buffer sizes, and message sizes.
- The `configure` method sets up the necessary servlets for the web application. It is crucial for initializing the chat
  server and ensuring that the WebSocket and session management functionalities are properly registered.

### Conclusion

The `ChatServer` class provides a foundation for building chat applications with WebSocket support in Kotlin using the
Jetty server. By subclassing and implementing the required abstract methods, developers can create customized chat
servers tailored to their specific needs.

# main\kotlin\com\simiacryptus\skyenet\webui\chat\ChatSocketManager.kt

## ChatSocketManager Class Documentation

The `ChatSocketManager` class, part of the `com.simiacryptus.skyenet.webui.chat` package, facilitates the integration of
OpenAI's GPT models into a chat interface within a web application. This class manages the chat session, including
sending and receiving messages, processing user inputs, and generating responses using the specified GPT model.

### Class Overview

- **Package**: `com.simiacryptus.skyenet.webui.chat`
- **Superclass**: `SocketManagerBase`
- **Purpose**: To manage chat interactions between a user and an AI model within a web application.

### Constructor Parameters

- `session`: The current user session.
- `model`: The OpenAI text model to use for generating responses. Default is `ChatModels.GPT35Turbo`.
- `userInterfacePrompt`: A prompt displayed to the user at the start of the chat session.
- `initialAssistantPrompt`: The initial message from the assistant. Default is an empty string.
- `systemPrompt`: A system-level prompt that influences the assistant's responses.
- `api`: The `OpenAIClient` instance used to communicate with OpenAI's API.
- `temperature`: Controls the randomness of the model's responses. Default is `0.3`.
- `applicationClass`: The class of the application server.
- `storage`: Interface for storage operations. Optional.

### Key Methods

#### `onRun(userMessage: String, socket: ChatSocket)`

This method is invoked to process a user's message. It sends the user's message to the OpenAI model and then sends the
model's response back to the user.

- **Parameters**:
    - `userMessage`: The message from the user.
    - `socket`: The chat socket through which the message was received.

#### `renderResponse(response: String): String`

Converts the given response string into HTML format using Markdown rendering.

- **Parameters**:
    - `response`: The response string to render.
- **Returns**: The rendered HTML as a `String`.

#### `onResponse(response: String, responseContents: String)`

A hook method that can be overridden to perform actions after a response has been generated and sent. By default, it
does nothing.

- **Parameters**:
    - `response`: The response generated by the AI model.
    - `responseContents`: The full HTML content of the response to be sent.

### Initialization Block

Upon instantiation, the class checks if `userInterfacePrompt` is not blank and sends it to the user, rendered as
Markdown inside a div element with the class `initial-prompt`.

### Lazy Properties

- `messages`: A lazy-initialized mutable list of `ApiModel.ChatMessage` objects. It includes system and assistant
  prompts as initial messages.

### Exception Handling

In the `onRun` method, if an error occurs while fetching the response from the OpenAI API, an error message is sent back
to the user.

### Logging

The class includes a companion object with a logger (`log`) for logging information and errors.

### Usage

This class is designed to be used within a web application that requires real-time chat functionality with AI-generated
responses. It requires an active session, a configured OpenAI client (`OpenAIClient`), and optionally, a storage
interface for persisting chat histories or other data.

---

This documentation provides an overview of the `ChatSocketManager` class, its purpose, key functionalities, and how it
integrates within a web application for chatting with an AI model.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\ApiKeyServlet.kt

## Developer Documentation: ApiKeyServlet

The `ApiKeyServlet` class, part of the `com.simiacryptus.skyenet.webui.servlet` package, is a servlet that manages API
key records. It enables CRUD (Create, Read, Update, Delete) operations on API key records and provides functionality for
inviting users to use an API key.

### Overview

This servlet extends `jakarta.servlet.http.HttpServlet` and overrides the `doGet` and `doPost` methods to handle HTTP
GET and POST requests, respectively. It interacts with `ApplicationServices` to authenticate users and manage user
settings, API key records, and usage summaries.

The `ApiKeyRecord` data class represents the structure of an API key record, including the owner's email, the API key
itself, a mapped key, a budget, and an optional comment.

### Key Features

- **API Key Management:** Users can create, edit, and delete their API key records.
- **Invitation System:** Users can invite others to use their API key.
- **Usage Summary:** Displays a summary of usage statistics for each API key.

### Endpoints and Operations

#### HTTP GET

- **Edit Record:** `?action=edit&apiKey=<API_KEY>`
    - Displays an edit page for an existing API key record.
- **Delete Record:** `?action=delete&apiKey=<API_KEY>`
    - Deletes an API key record.
- **Create Record:** `?action=create`
    - Displays an edit page with an empty record for creating a new API key record.
- **Invite:** `?action=invite&apiKey=<API_KEY>`
    - Displays a confirmation page for inviting another user to use an API key.
- **Default:** Displays the index page listing all API key records owned by the user.

#### HTTP POST

- **Accept Invite:** `action=acceptInvite&apiKey=<API_KEY>`
    - Accepts an invitation to use an API key.
- **Edit/Create Record:** `action=<ACTION>&apiKey=<API_KEY>&mappedKey=<MAPPED_KEY>&budget=<BUDGET>&comment=<COMMENT>`
    - Updates an existing API key record or creates a new one based on the provided details.

### Usage

#### Authentication

The servlet uses `ApplicationServices.authenticationManager.getUser(req.getCookie())` to authenticate users based on
cookies. Unauthenticated users are not allowed to access or modify API key records.

#### Data Persistence

API key records are stored in a JSON file (`apiKeys.json`) within a directory specified
by `ApplicationServices.dataStorageRoot`. The `saveRecords` method is responsible for serializing the `apiKeyRecords`
list to JSON and saving it to the file.

#### Error Handling

The servlet handles various error conditions, such as unauthorized access, missing or invalid parameters, and attempts
to manipulate records not owned by the user. It responds with appropriate HTTP status codes and error messages.

### Example Usage

To edit an API key record, a user would navigate to `http://<SERVER_ADDRESS>/apiKeys/?action=edit&apiKey=<API_KEY>`,
where `<SERVER_ADDRESS>` is the address of the server hosting the servlet and `<API_KEY>` is the API key to be edited.

### Conclusion

The `ApiKeyServlet` class provides a comprehensive solution for managing API key records within a web application. It
leverages Kotlin's features and integrates with `ApplicationServices` for authentication and data management, offering a
secure and user-friendly interface for API key management.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\CorsFilter.kt

### CorsFilter Class

The `CorsFilter` class is a servlet filter designed to handle Cross-Origin Resource Sharing (CORS) for web applications.
This class is part of the `com.simiacryptus.skyenet.webui.servlet` package and ensures that web applications can
securely manage cross-origin requests. It is annotated with `@WebFilter`, making it discoverable by the servlet
container to apply the filter to specified URL patterns.

#### Key Features:

- **Cross-Origin Request Handling:** Adds CORS headers to HTTP responses to allow or restrict resources to be requested
  from another domain.
- **Flexible URL Pattern Matching:** Configured to apply to all URL patterns (`"/*"`) but excludes paths ending
  with `/ws`.
- **Error Logging:** Captures and logs any exceptions thrown during the filtering process.

#### Usage:

1. **Deployment:** The filter is automatically registered and mapped to URL patterns due to the `@WebFilter` annotation.
   It applies to all paths except those ending with `/ws`.
2. **Configuration:** No initial configuration is required in the `init` method, making it straightforward to use.
3. **Exception Handling:** The filter logs exceptions without interrupting the filter chain unless a critical error
   occurs.

#### Methods:

- `init(filterConfig: FilterConfig?)`: Initializes the filter. This implementation does not require any initialization
  logic.
- `doFilter(request: ServletRequest?, response: ServletResponse, chain: FilterChain)`: Applies CORS headers to the HTTP
  response if the request URI does not end with `/ws`. It then proceeds with the filter chain, handling any exceptions
  by logging them and rethrowing.
- `destroy()`: Cleans up any resources if necessary. This implementation does not perform any cleanup actions.

#### CORS Headers Added:

- `Access-Control-Allow-Origin: *`: Allows all domains to access the resource.
- `Access-Control-Allow-Methods: POST, GET, OPTIONS, DELETE, PUT`: Specifies the allowed methods for cross-origin
  requests.
- `Access-Control-Max-Age: 3600`: Indicates how long the results of a preflight request can be cached.
- `Access-Control-Allow-Headers: Content-Type, x-requested-with, authorization`: Specifies the headers allowed in the
  actual request.

#### Error Handling:

The filter uses a companion object to obtain a logger instance from SLF4J. It logs any exceptions that occur during the
filtering process under the warning level, ensuring that issues are noted without necessarily halting the application.

#### Example:

```java

@WebFilter(asyncSupported = true, urlPatterns =["/*"])
class CorsFilter :

Filter {
    ...
    override fun doFilter(request:ServletRequest ?, response:ServletResponse, chain:FilterChain){
        ...
        try {
            chain.doFilter(request, response)
        } catch (e:Exception){
            log.warn("Error in filter", e)
            throw e
        }
    }
    ...
}
```

This code snippet demonstrates how the `CorsFilter` class adds CORS headers to responses for HTTP requests, excluding
those ending with `/ws`, and logs any exceptions encountered during the process.

#### Conclusion:

The `CorsFilter` class provides a simple yet effective way to manage CORS in web applications, ensuring that
cross-origin requests are handled securely and efficiently. Its ease of deployment and configuration makes it a valuable
tool for developers working on web applications that require cross-origin resource sharing.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\CancelThreadsServlet.kt

## CancelThreadsServlet Documentation

The `CancelThreadsServlet` class is part of the web UI module for managing session-based operations within an
application. It extends `HttpServlet` and is designed to handle HTTP GET and POST requests to cancel threads associated
with a specific session.

### Overview

This servlet allows users to cancel running threads for a given session through a web interface. It provides two main
functionalities:

- **GET Request Handling**: Displays a confirmation form to the user for canceling the session's threads.
- **POST Request Handling**: Processes the cancellation request after user confirmation.

### Dependencies

- **Jakarta Servlet API**: For handling HTTP requests and responses.
- **ApplicationServices**: Provides access to core platform services like authentication and client management.
- **AuthorizationInterface**: For checking user authorization for certain operations.

### Usage

#### GET Request

When a GET request is received, the servlet checks if the request contains a `sessionId` parameter. If not, it responds
with a `400 Bad Request` status and a message indicating that a session ID is required. If a `sessionId` is provided, it
renders an HTML form asking the user to confirm the cancellation by typing 'confirm'.

##### Request Parameters

- **sessionId**: The unique identifier of the session whose threads are to be canceled.

##### Response

- **Content-Type**: `text/html`
- **Status**: `200 OK` (if `sessionId` is provided), `400 Bad Request` (if `sessionId` is missing)

#### POST Request

The POST request handler expects two parameters: `sessionId` and `confirm`. It first checks if the `confirm` parameter
equals 'confirm' (case-insensitive), throwing an exception if not. Then, it validates the presence of `sessionId` and
performs authorization checks to ensure the user is allowed to cancel sessions or global sessions.

If all validations pass, it retrieves the thread pool associated with the session and user, shuts it down immediately,
and interrupts all active threads. Finally, it redirects the user to the root path.

##### Request Parameters

- **sessionId**: The unique identifier of the session whose threads are to be canceled.
- **confirm**: A confirmation input from the user, expected to be 'confirm'.

##### Response

- **Content-Type**: `text/html`
- **Status**: `200 OK` (if the operation is successful), `400 Bad Request` (if parameters are missing or invalid), or
  throws an exception for unauthorized access.

### Security

This servlet implements basic security checks:

- It requires a confirmation input from the user to proceed with the cancellation.
- It performs authorization checks to ensure the user has the necessary permissions to cancel sessions or global
  sessions.

### Example Usage

#### Canceling a Session's Threads

1. A user navigates to the servlet's URL with a `sessionId` parameter in the query string.
2. The servlet responds with an HTML form asking for confirmation.
3. The user types 'confirm' in the form and submits it.
4. The servlet processes the request, cancels the session's threads, and redirects the user to the homepage.

#### Error Handling

- If the user attempts to cancel without the correct `sessionId` or without confirming, the servlet responds with
  appropriate error messages.
- If the user is unauthorized to perform the cancellation, an exception is thrown.

### Conclusion

The `CancelThreadsServlet` provides a web interface for safely and securely canceling threads associated with a specific
session, ensuring that only authorized users can perform such operations and requiring explicit user confirmation to
prevent accidental cancellations.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\DeleteSessionServlet.kt

## DeleteSessionServlet

The `DeleteSessionServlet` class extends `HttpServlet` and is responsible for handling HTTP GET and POST requests to
delete a user session from the application. It is part of the `com.simiacryptus.skyenet.webui.servlet` package and
utilizes various services and components from the application to perform authorization checks and session deletion.

### Constructor

#### `DeleteSessionServlet(ApplicationServer server)`

Initializes a new instance of the `DeleteSessionServlet` class with the specified `ApplicationServer`.

- **Parameters:**
    - `server`: An instance of `ApplicationServer` that provides access to application-level services such as data
      storage and authentication management.

### Methods

#### `doGet(HttpServletRequest req, HttpServletResponse resp)`

Handles the HTTP GET request by displaying an HTML form to the user, allowing them to confirm the deletion of a session.

- **Parameters:**
    - `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet returns to the client.
- **Behavior:**
    - Sets the content type of the response to "text/html".
    - Checks if the request contains a "sessionId" parameter. If not, it returns an HTTP 400 (Bad Request) status.
    - If a "sessionId" is provided, it displays an HTML form where the user can confirm the deletion of the session by
      typing "confirm".

#### `doPost(HttpServletRequest req, HttpServletResponse resp)`

Handles the HTTP POST request to delete a specified session after the user confirms the action.

- **Parameters:**
    - `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet sends back to the client.
- **Behavior:**
    - Validates that the "confirm" parameter is present and equals "confirm", case-insensitively. If not, throws an
      exception.
    - Sets the content type of the response to "text/html".
    - Checks if the request contains a "sessionId" parameter. If not, it returns an HTTP 400 (Bad Request) status.
    - Retrieves the session and the user from the request.
    - Checks if the user is authorized to delete the session. If the session is global, it also checks if the user has
      public operation authorization.
    - If authorized, it deletes the session using the `dataStorage` service of the `ApplicationServer`.
    - Redirects the user to the home page ("/").

### Usage

This servlet is typically mapped to a URL pattern (e.g., `/delete`) in the web application's deployment descriptor or
through annotations. Users navigate to this URL (or are redirected there) when they wish to delete a session. The
servlet first presents a confirmation form (via `doGet`) and, upon submission, processes the deletion (via `doPost`),
ensuring that only authorized users can delete sessions and that confirmation is received to prevent accidental
deletions.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\AppInfoServlet.kt

## AppInfoServlet Class Documentation

`AppInfoServlet` is a servlet class designed to serve information about an application in JSON format. It extends
the `HttpServlet` class from the Jakarta Servlet API, enabling it to respond to HTTP GET requests. The class is generic,
allowing it to serve information of any type as long as it can be serialized into JSON.

### Package

```java
package com.simiacryptus.skyenet.webui.servlet;
```

### Imports

The class uses several imports for its functionality:

- `com.simiacryptus.jopenai.util.JsonUtil`: Utility class for JSON operations, specifically for object serialization.
- `jakarta.servlet.http.HttpServlet`: Base class for HTTP servlets.
- `jakarta.servlet.http.HttpServletRequest`: Interface to provide request information for HTTP servlets.
- `jakarta.servlet.http.HttpServletResponse`: Interface to assist in sending a response to an HTTP client.

### Class Declaration

```java
class AppInfoServlet<T>(val info:T) :

HttpServlet()
```

The class is declared with a single generic type parameter `<T>`, allowing it to handle any type of object for
serialization. It has one constructor parameter, `info`, which is the object to be serialized and sent in the HTTP
response.

### Methods

#### `doGet(req: HttpServletRequest, resp: HttpServletResponse)`

This method overrides the `doGet` method from the `HttpServlet` class. It is called by the server (via the service
method) to allow a servlet to handle a GET request.

##### Parameters

- `req: HttpServletRequest`: The HttpServletRequest object that contains the request the client has made of the servlet.
- `resp: HttpServletResponse`: The HttpServletResponse object that contains the response the servlet sends to the
  client.

##### Functionality

1. Sets the content type of the response to `text/json` to indicate that the response will be in JSON format.
2. Sets the HTTP status code of the response to `SC_OK (200)` to indicate that the request has succeeded.
3. Serializes the `info` object into a JSON string using the `JsonUtil.objectMapper().writeValueAsString(info)` method.
4. Writes the serialized JSON string to the response's output stream.

### Example Usage

To use the `AppInfoServlet`, you would typically declare it within your web application's servlet configuration, passing
in the object you wish to serialize and serve. For example:

```kotlin
val appInfo = AppInfo("MyApp", "1.0")
val servlet = AppInfoServlet(appInfo)
```

Here, `AppInfo` would be a data class (or any class) that contains information about your application. The servlet could
then be mapped to a URL pattern via your web application's configuration, making the application info available as a
JSON response at the specified URL.

### Conclusion

The `AppInfoServlet` class provides a simple and flexible way to serve application information or any other data in JSON
format over HTTP. Its generic design allows it to be used with a wide variety of data types, making it a useful utility
in web applications that require dynamic data serialization and transmission.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\NewSessionServlet.kt

## NewSessionServlet Documentation

The `NewSessionServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends
the `HttpServlet` class provided by the Jakarta Servlet API. This servlet is designed to handle HTTP GET requests by
generating a new session ID and returning it to the client. Below is a detailed overview of its implementation and
usage.

### Overview

The `NewSessionServlet` is a simple yet essential component for session management within a web application. It
leverages the `StorageInterface` to generate a globally unique identifier (GUID) that serves as a session ID. This
session ID is then returned to the client as a plain text response. The servlet is designed to be lightweight and
efficient, focusing solely on session creation.

### Implementation Details

- **Package:** `com.simiacryptus.skyenet.webui.servlet`
- **Class:** `NewSessionServlet`
- **Superclass:** `HttpServlet`

#### Key Methods

##### `doGet(HttpServletRequest req, HttpServletResponse resp)`

- **Parameters:**
    - `HttpServletRequest req`: The HTTP request object.
    - `HttpServletResponse resp`: The HTTP response object.
- **Operation:** Handles the HTTP GET request by generating a new session ID and returning it to the client.
- **Response Content-Type:** `text/plain`
- **Response Status:** `HttpServletResponse.SC_OK` (HTTP 200)
- **Output:** Writes the generated session ID to the response writer.

### Usage

To utilize the `NewSessionServlet`, it must be properly mapped in the web application's deployment
descriptor (`web.xml`) or through annotations, depending on the servlet's version and configuration. Once mapped, it
listens for HTTP GET requests on its designated path. When a request is received, it generates a new session ID
using `StorageInterface.newGlobalID()`, sets the appropriate response headers, and writes the session ID as a plain text
response to the client.

#### Example Deployment Descriptor Configuration

```xml

<servlet>
    <servlet-name>NewSessionServlet</servlet-name>
    <servlet-class>com.simiacryptus.skyenet.webui.servlet.NewSessionServlet</servlet-class>
</servlet>
<servlet-mapping>
<servlet-name>NewSessionServlet</servlet-name>
<url-pattern>/newSession</url-pattern>
</servlet-mapping>
```

#### Security Considerations

While the `NewSessionServlet` serves a fundamental role, it's crucial to consider security implications, such as:

- Ensuring that the session ID generation mechanism (`StorageInterface.newGlobalID()`) produces sufficiently random and
  unpredictable IDs to prevent session guessing attacks.
- Implementing rate limiting or other protective measures to mitigate the risk of abuse, such as session flooding.

### Conclusion

The `NewSessionServlet` provides a straightforward solution for generating new session IDs in a web application. Its
simplicity and reliance on the `StorageInterface` for ID generation ensure that it can be easily integrated and used
within various web projects, contributing to effective session management practices.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\FileServlet.kt

## FileServlet Class Documentation

The `FileServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends `HttpServlet`. It is
designed to serve files and directories within a web application, providing both file download and directory listing
capabilities. This servlet interacts with a storage interface to retrieve file system paths and uses asynchronous I/O
for efficient file serving.

### Constructor

#### FileServlet(StorageInterface dataStorage)

Initializes a new instance of the `FileServlet` class using the specified `StorageInterface` for data storage
operations.

- **Parameters:**
    - `dataStorage`: An implementation of `StorageInterface` that provides access to the underlying storage mechanism (
      e.g., file system, cloud storage).

### Public Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

Handles GET requests by serving files or directories based on the requested path.

- **Parameters:**
    - `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet returns to the client.

### Private Methods

#### directoryHTML(HttpServletRequest req, Session session, String filePath, String folders, String files)

Generates HTML content for directory listings, including links to contained files and subdirectories.

- **Parameters:**
    - `req`: The `HttpServletRequest` object associated with the request.
    - `session`: The current `Session` object.
    - `filePath`: The path to the directory being listed.
    - `folders`: HTML formatted string containing links to subdirectories.
    - `files`: HTML formatted string containing links to files within the directory.
- **Returns:** A `String` containing the HTML content for the directory listing.

### Utility Methods

#### parsePath(String path)

Parses and validates the requested path, ensuring it is safe and conforms to expected formats.

- **Parameters:**
    - `path`: The path string to parse and validate.
- **Returns:** A `List<String>` representing the sanitized segments of the path.

### Cache Management

The servlet uses a Guava `Cache` for managing open `FileChannel` instances to improve performance by reusing channels
for subsequent requests. The cache is configured with a maximum size and a removal listener to ensure that channels are
properly closed when evicted from the cache.

### Exception Handling

The servlet includes checks and exception handling to prevent directory traversal attacks and ensure that only valid
paths are processed. It throws `IllegalArgumentException` if an invalid path is encountered.

### Logging

The class uses SLF4J for logging, providing valuable debug and error information.

### Example Usage

The `FileServlet` is typically mapped to a URL pattern in a web application's deployment descriptor (web.xml) or through
annotations, allowing it to serve requests for files and directories under a specific path.

```xml

<servlet>
    <servlet-name>fileServlet</servlet-name>
    <servlet-class>com.simiacryptus.skyenet.webui.servlet.FileServlet</servlet-class>
</servlet>
<servlet-mapping>
<servlet-name>fileServlet</servlet-name>
<url-pattern>/files/*</url-pattern>
</servlet-mapping>
```

In this configuration, requests to `/files/*` are handled by `FileServlet`, which serves the corresponding files or
directories from the configured storage interface.

### Conclusion

The `FileServlet` class provides a robust solution for serving files and directories within a web application,
leveraging asynchronous I/O and caching for efficient operation. It includes important security measures to ensure safe
operation and is configurable to work with different storage backends.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\LogoutServlet.kt

## LogoutServlet Documentation

The `LogoutServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and is responsible for
handling logout requests within the application. This class extends `HttpServlet` and overrides the `doGet` method to
provide functionality for logging out users.

### Overview

When a logout request is received, the servlet performs the following actions:

1. Retrieves the user's cookie.
2. Identifies the user associated with the cookie.
3. If a user is found, logs the user out and redirects to the home page.
4. If no user is associated with the cookie, responds with a bad request status.

### Usage

This servlet is intended to be mapped to a specific URL pattern within the web application's deployment
descriptor (`web.xml`) or through annotations, to handle GET requests for user logout.

### Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

- **Parameters:**
    - `HttpServletRequest req`: The request object containing the client's request.
    - `HttpServletResponse resp`: The response object for sending data to the client.
- **Functionality:** Handles the GET request by logging out the user. It first attempts to retrieve the user's session
  cookie and then identifies the user associated with this cookie. If a user is found, it proceeds to log out the user
  by calling the `logout` method on the `authenticationManager` and then redirects the user to the home page. If no user
  is associated with the provided cookie, it sets the response status to `HttpServletResponse.SC_BAD_REQUEST`.

### Dependencies

- **ApplicationServices:** Utilizes the `ApplicationServices` class to access the `authenticationManager` for performing
  logout operations.
- **Jakarta Servlet API:** This class extends `HttpServlet` and uses `HttpServletRequest` and `HttpServletResponse` from
  the Jakarta Servlet API.

### Example

To utilize this servlet, ensure it's properly mapped to handle logout requests. For example, if mapped to `/logout`, a
user can initiate a logout by navigating to `http://yourdomain.com/logout`.

### Logging

The servlet includes a private static logger (`log`) for logging important events or errors. The logger is initialized
using SLF4J.

### Security Considerations

- Ensure that the cookie retrieval and user identification process is secure to prevent unauthorized access.
- Consider implementing CSRF protection for logout requests to prevent malicious sites from forcibly logging out users.

### Conclusion

The `LogoutServlet` provides a straightforward mechanism for handling user logout requests within a web application,
leveraging the application's authentication manager to securely manage user sessions.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\OAuthBase.kt

## OAuthBase Class Documentation

The `OAuthBase` class serves as an abstract foundation for implementing OAuth-based authentication within a web
application. It is part of the `com.simiacryptus.skyenet.webui.servlet` package and utilizes the Eclipse Jetty
server's `WebAppContext` to configure authentication settings.

### Overview

OAuth is an open standard for access delegation, commonly used as a way for users to grant websites or applications
access to their information on other websites but without giving them the passwords. This class abstracts the common
logic required to set up OAuth authentication, requiring implementers to define the specifics of their OAuth provider.

### Constructor

#### `OAuthBase(String redirectUri)`

- **Parameters:**
    - `redirectUri`: A `String` specifying the URI to which users will be redirected after authentication. This URI must
      be registered with the OAuth provider.
- **Description**: Constructs an instance of the `OAuthBase` class, initializing it with the provided redirect URI.

### Abstract Method

#### `configure(WebAppContext context, boolean addFilter)`

- **Parameters:**
    - `context`: An instance of `WebAppContext` from the Eclipse Jetty server, representing the web application's
      context.
    - `addFilter`: A `Boolean` indicating whether an authentication filter should be added to the web application. This
      is true by default.
- **Returns**: The modified `WebAppContext` instance, after applying the necessary configurations for OAuth
  authentication.
- **Description**: This method is abstract and must be implemented by subclasses. It is responsible for configuring the
  web application context for OAuth authentication, including setting up any necessary filters or parameters based on
  the specific OAuth provider being used.

### Usage

To use the `OAuthBase` class, one must extend it and implement the `configure` method, providing the logic specific to
the OAuth provider in use. Here is a simplified example:

```java
public class GoogleOAuth extends OAuthBase {

    public GoogleOAuth(String redirectUri) {
        super(redirectUri);
    }

    @Override
    public WebAppContext configure(WebAppContext context, boolean addFilter) {
        // Implementation specific to Google OAuth
        if (addFilter) {
            // Add necessary authentication filters to the context
        }
        // Perform additional configuration as needed
        return context;
    }
}
```

In this example, `GoogleOAuth` extends `OAuthBase`, providing the specifics of configuring a web application context for
Google's OAuth service.

### Conclusion

The `OAuthBase` class abstracts the common aspects of setting up OAuth authentication within a web application, allowing
for easy integration of various OAuth providers by extending and implementing its methods.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\SessionIdFilter.kt

## SessionIdFilter Documentation

The `SessionIdFilter` class is a custom implementation of the `Filter` interface, designed to provide session-based
authentication for web applications. It is part of the `com.simiacryptus.skyenet.webui.servlet` package. This filter
checks if an incoming HTTP request is secure and, if so, verifies the session ID present in the request's cookies. If
the session ID is missing or invalid, the filter redirects the user to a specified login page.

### Constructor

The `SessionIdFilter` class has a constructor that takes two parameters:

- `isSecure`: A lambda function that determines whether a given `HttpServletRequest` should be considered secure and
  thus subject to session validation. It returns `true` if the request is secure, `false` otherwise.
- `loginRedirect`: A `String` specifying the URL to which the user should be redirected if their session ID is invalid
  or missing.

### Methods

#### `init(filterConfig: FilterConfig?)`

This method is an override from the `Filter` interface and is called by the web container to indicate to a filter that
it is being placed into service. The `SessionIdFilter` implementation of this method is empty, meaning no initialization
action is taken.

#### `doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain)`

This method implements the core functionality of the `SessionIdFilter`. It checks if the incoming request is
an `HttpServletRequest` and the response is an `HttpServletResponse`. If so, and if the request is determined to be
secure by the `isSecure` lambda, it then checks for a session ID cookie. If a session ID cookie is present, it validates
the session ID by checking if there is a user associated with it through
the `ApplicationServices.authenticationManager.getUser(sessionIdCookie)` method. If the session ID is invalid or
missing, the user is redirected to the `loginRedirect` URL. Otherwise, the filter chain is allowed to proceed by
calling `chain.doFilter(request, response)`.

#### `destroy()`

This method is another override from the `Filter` interface. It is called by the web container to indicate to a filter
that it is being taken out of service. The `SessionIdFilter` implementation of this method is empty, meaning no teardown
action is taken.

### Usage

To use the `SessionIdFilter`, it must be registered and mapped to URL patterns within your web application's `web.xml`
file or programmatically via the Servlet API. When registering the filter, you will need to provide an implementation of
the `isSecure` lambda function and a login redirect URL that fits your application's authentication flow.

This filter is particularly useful in applications where certain resources or paths require the user to be
authenticated, adding an additional layer of security and user management.

### Example

Here is a simple example of how to instantiate and register the `SessionIdFilter`:

```java
FilterRegistration.Dynamic sessionFilter = servletContext.addFilter("SessionIdFilter", new SessionIdFilter(
        request -> request.getRequestURI().startsWith("/secure"), // Example isSecure implementation
        "/login.jsp" // Example login redirect URL
));

sessionFilter.

addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true,"/secure/*");
```

In this example, any request to a path starting with `/secure` is considered to need authentication. If the session ID
is not valid, the user is redirected to `login.jsp`.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\OAuthGoogle.kt

## OAuthGoogle Class Documentation

The `OAuthGoogle` class is designed to facilitate Google OAuth 2.0 authentication for web applications. It integrates
with Google's authentication and authorization services to securely authenticate users and grant access to their Google
services like Gmail, Drive, and Calendar. This class extends the `OAuthBase` class and is tailored for Google's OAuth
2.0 implementation.

### Features

- Supports multiple Google API scopes for accessing a wide range of Google services.
- Provides servlets for handling login and OAuth callback.
- Seamlessly integrates with web applications using the Jetty server.
- Manages user sessions and securely stores authentication credentials.

### Dependencies

- Google API Client Libraries
- Jetty Server
- Jakarta Servlet API
- SLF4J Logging

### Usage

1. **Initialization**: Instantiate the `OAuthGoogle` class by providing the required parameters such as the redirect
   URI, application name, and a function to retrieve the Google client secrets.

2. **Configuration**: Use the `configure` method to add the necessary servlets and filters to your `WebAppContext`. This
   setup is required for handling the authentication flow.

3. **Authentication Flow**: Direct users to the login servlet (`/login` or `/googleLogin`) to initiate the
   authentication process. After authentication, users will be redirected to the specified redirect URI with an
   authorization code, which is then exchanged for an access token.

4. **User Session**: Upon successful authentication, a session cookie is created and sent to the user's browser, which
   is then used for subsequent authenticated requests.

### Key Components

#### Servlets

- **LoginServlet**: Initiates the OAuth flow by redirecting the user to Google's authorization server.
- **CallbackServlet**: Handles the callback from Google's authorization server, exchanges the authorization code for an
  access token, and establishes a user session.

#### Methods

- **configure**: Configures the web application context to include necessary servlets and filters for the authentication
  process.
- **urlDecode**: Utility method for URL decoding.

#### Scopes

A predefined list of scopes is included, covering various Google services like Gmail, Drive, and Calendar. These scopes
determine the level of access the application has to the user's Google services.

### Example

To integrate Google OAuth into a web application:

```kotlin
val googleOAuth = OAuthGoogle(
  redirectUri = "http://yourapplication.com/oauth2callback",
  applicationName = "Your Application Name",
  key = { Thread.currentThread().contextClassLoader.getResourceAsStream("client_secrets.json") }
)

val webAppContext = WebAppContext()
googleOAuth.configure(webAppContext, addFilter = true)
```

### Security Considerations

- Ensure the client secrets are securely stored and not exposed.
- Use HTTPS to protect sensitive information during transmission.
- Validate and sanitize all inputs to prevent security vulnerabilities.

### Troubleshooting

- **Invalid Redirect URI**: Ensure the redirect URI provided during initialization matches the one configured in the
  Google Cloud Console.
- **Missing Scopes**: If accessing a Google service fails, verify that the required scope is included in the `scopes`
  list.

### Conclusion

The `OAuthGoogle` class provides a robust solution for integrating Google OAuth 2.0 authentication into web
applications. By following the documentation and adhering to security best practices, developers can securely
authenticate users and access Google services on their behalf.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\SessionListServlet.kt

## SessionListServlet Documentation

The `SessionListServlet` class is part of the web UI module of the application, designed to handle HTTP GET requests to
display a list of user sessions. It extends `jakarta.servlet.http.HttpServlet` and interacts with session data stored
via a `StorageInterface`. This servlet is responsible for generating an HTML page that lists all sessions associated
with the authenticated user, providing links to individual sessions.

### Dependencies

- **Jakarta Servlet API**: Used for handling HTTP requests and responses.
- **ApplicationServer**: Provides application-specific services and configurations.
- **StorageInterface**: An interface for session data storage and retrieval.
- **AuthenticationManager**: A component of the application services for user authentication.

### Constructor Parameters

- `dataStorage: StorageInterface`: The storage interface used for accessing session data.
- `prefix: String`: A URL prefix used for creating links to individual sessions.
- `applicationServer: ApplicationServer`: The application server instance providing additional functionalities like
  application description.

### Core Methods

#### `doGet(HttpServletRequest req, HttpServletResponse resp)`

Handles HTTP GET requests by generating an HTML page that lists all sessions for the authenticated user.

- **Parameters**:
    - `req: HttpServletRequest`: The HTTP request object.
    - `resp: HttpServletResponse`: The HTTP response object.

- **Functionality**:
    1. **Content Type**: Sets the response content type to `text/html`.
    2. **User Authentication**: Retrieves the authenticated user based on the cookie provided in the request.
    3. **Session Retrieval**: Fetches a list of sessions associated with the authenticated user from the `dataStorage`.
    4. **HTML Generation**: Dynamically generates HTML content that lists all the user's sessions, including session
       names and creation times. Each session is presented as a clickable row that redirects to a detailed session view,
       prefixed by the provided URL prefix.
    5. **Response Writing**: Writes the generated HTML content to the response writer.

- **HTML Structure**:
    - The generated HTML document includes a `<table>` element where each row represents a session. The table has two
      columns: "Session Name" and "Created", displaying the session's name and creation time, respectively.
    - The document includes minimal styling for readability and interactivity, such as hover effects on table rows.

### Usage Example

This servlet is typically mapped to a specific URL pattern within the web application's servlet context. Once mapped, it
listens for GET requests on that URL, authenticates the user, retrieves their sessions, and displays them in an HTML
table.

```kotlin
val sessionListServlet = SessionListServlet(dataStorage, "/sessions", applicationServer)
servletContext.addServlet("SessionListServlet", sessionListServlet).addMapping("/sessions/list")
```

In this example, the `SessionListServlet` is instantiated with a `dataStorage` instance for session management, a URL
prefix `/sessions` for creating session detail links, and an `applicationServer` instance for additional
functionalities. It is then registered with the servlet context to handle requests to `/sessions/list`.

### Conclusion

The `SessionListServlet` provides a simple yet effective way to display user session information in a web UI, leveraging
Kotlin's string templates and HTML generation capabilities for dynamic content rendering. It showcases the integration
of servlets, session management, and user authentication in a Kotlin-based web application.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\SessionShareServlet.kt

## SessionShareServlet Developer Documentation

The `SessionShareServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends
the `HttpServlet` class to provide functionality for sharing sessions within the Skyenet web application. This servlet
allows users to share their application sessions by generating and providing a URL that can be accessed by others.

### Overview

When a `GET` request is received, the servlet performs several key operations:

1. **Authentication and Authorization**: It authenticates the user making the request and checks if they are authorized
   to share sessions.
2. **URL Validation**: Validates the provided URL to ensure it points to a valid session that can be shared.
3. **Session Sharing**: If the session has not been shared before, it generates a unique share ID, saves session
   information, and provides a shareable URL. If the session has already been shared, it reuses the existing share ID
   and URL.
4. **Response Generation**: Generates an HTML response that either provides the shareable URL or indicates an error or
   forbidden access if the user is not authorized.

### Key Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

This is the main method that handles incoming `GET` requests. It performs the steps outlined in the overview section.

- **Parameters**:
    - `HttpServletRequest req`: The request object containing client request information.
    - `HttpServletResponse resp`: The response object used to send data back to the client.

#### url(String appName, String shareId)

Generates the URL for the shared session based on the application name and share ID.

- **Parameters**:
    - `String appName`: The name of the application whose session is being shared.
    - `String shareId`: The unique identifier for the shared session.
- **Returns**: A `String` representing the URL for the shared session.

#### acceptHost(User user, String host)

Determines if the host from the provided URL is acceptable for sharing based on the user's authorization.

- **Parameters**:
    - `User user`: The user attempting to share the session.
    - `String host`: The host part of the URL being shared.
- **Returns**: `true` if the host is acceptable for sharing; otherwise, `false`.

#### validateUrl(String previousShare)

Validates if the provided URL (associated with a previous share) is still accessible.

- **Parameters**:
    - `String previousShare`: The URL of the previously shared session.
- **Returns**: `true` if the URL is valid and accessible; otherwise, `false`.

### Usage and Examples

To use this servlet, it must be properly mapped in the web application's deployment descriptor (`web.xml`) or through
annotations. Clients can share a session by sending a GET request with the session URL as a parameter. The servlet
authenticates the user, validates the request, and provides a shareable URL if authorized.

### Error Handling

The servlet handles various error scenarios, including:

- Missing or invalid URL parameter.
- Unauthorized access attempts.
- Errors during session sharing or URL generation.

In these cases, the servlet responds with appropriate HTTP status codes (e.g., 400 Bad Request, 403 Forbidden) and
descriptive error messages.

### Security Considerations

- Authentication and authorization checks are critical to prevent unauthorized sharing.
- The acceptHost method ensures that only URLs from trusted hosts can be shared.
- ValidateUrl prevents sharing of sessions that are no longer accessible or valid.

### Conclusion

The `SessionShareServlet` provides a crucial functionality within the Skyenet web application, enabling users to share
their sessions securely and efficiently. Proper understanding and usage of this servlet are important for developers
working on or extending the Skyenet platform.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\ProxyHttpServlet.kt

## ProxyHttpServlet Documentation

The `ProxyHttpServlet` class is designed to act as a reverse proxy, primarily supporting interactions with the OpenAI
API. It extends `HttpServlet` and provides mechanisms for forwarding requests to a specified target URL while handling
authentication, logging, and budget checks.

### Features

- **Flexible Target URL**: The target URL for the proxy is customizable, allowing redirection of requests to any
  specified endpoint.
- **Asynchronous HTTP Client**: Utilizes an asynchronous HTTP client for efficient request handling and forwarding.
- **Request and Response Modification**: Offers hooks for modifying requests before they are forwarded and responses
  before they are returned to the client.
- **Budget Management**: Integrates budget checks to prevent exceeding predefined limits based on API key usage.
- **Logging**: Detailed logging of requests and responses for monitoring and debugging purposes.

### Usage

#### Initialization

The servlet requires a target URL, which is the base URL to which requests will be proxied. This can be specified during
instantiation:

```kotlin
val proxyServlet = ProxyHttpServlet("https://api.openai.com/v1/")
```

#### Asynchronous Client Configuration

The servlet uses a lazily initialized `CloseableHttpAsyncClient` for asynchronous request handling. The client is
configured with:

- A retry strategy with a maximum of 0 retries and a 1-second timeout.
- A request configuration with a 5-minute timeout for connection requests and responses.
- A connection manager supporting up to 1000 simultaneous connections per route and in total.

#### Request Handling

Requests to the servlet are processed by the `service` method, which:

1. Initiates an asynchronous context.
2. Extracts and verifies the API key from the request headers.
3. Checks if the request exceeds the budget associated with the API key.
4. Forwards the request to the target URL if the budget check passes.
5. Modifies the response based on the implementation of `onResponse` before returning it to the client.

#### Request and Response Modification

The `onRequest` and `onResponse` methods can be overridden to modify requests and responses. By default, `onRequest`
returns the request body bytes as-is, and `onResponse` logs the request and response details without modification.

#### Running a Test Server

The companion object includes a `main` method and a `test` function for running a test server with
the `ProxyHttpServlet` and a simple "Hello, World!" servlet. This can be used for local testing and development.

### Example

To run the test server:

```kotlin
fun main(args: Array<String>) {
  ProxyHttpServlet.test()
}
```

This starts a Jetty server on port 8080 with the proxy servlet mapped to `/proxy/*` and a test servlet at `/test`.
Accessing `http://localhost:8080/proxy/test` will proxy the request to the test servlet, demonstrating the proxy
functionality.

### Conclusion

The `ProxyHttpServlet` class provides a versatile and efficient means to proxy HTTP requests, with features supporting
request modification, budget management, and detailed logging. Its asynchronous client ensures efficient processing,
making it suitable for scenarios requiring the forwarding of HTTP requests with additional processing and checks.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\SessionSettingsServlet.kt

## SessionSettingsServlet Documentation

The `SessionSettingsServlet` class is part of the web UI package and serves as a servlet for handling session settings
within an application. It extends `HttpServlet` and interacts with the application server, session information, and
authentication manager to retrieve and update user-specific settings.

### Overview

This servlet is designed to provide a web interface for users to view and modify their session settings. It supports two
HTTP methods: `GET` for retrieving and displaying settings, and `POST` for updating those settings.

### Constructor

- `SessionSettingsServlet(ApplicationServer server)`: Initializes the servlet with a reference to the application
  server.

### Methods

#### `doGet(HttpServletRequest req, HttpServletResponse resp)`

Handles the HTTP GET request by displaying a user's current session settings in a web form.

- **Parameters**:
    - `HttpServletRequest req`: The request object containing client request information.
    - `HttpServletResponse resp`: The response object for sending data back to the client.

- **Behavior**:
    - Sets the content type of the response to "text/html".
    - Checks if the request contains a "sessionId" parameter. If not, it responds with a 400 Bad Request status and a
      message indicating that a session ID is required.
    - Retrieves the session using the provided session ID and obtains the current user from the authentication manager
      using the request's cookies.
    - Fetches the user's settings associated with the session and converts them to JSON format.
    - Generates and sends an HTML form pre-populated with the user's settings in a textarea, allowing the user to modify
      and save their settings.

#### `doPost(HttpServletRequest req, HttpServletResponse resp)`

Handles the HTTP POST request to update a user's session settings based on the form submission.

- **Parameters**:
    - `HttpServletRequest req`: The request object containing client request information.
    - `HttpServletResponse resp`: The response object for sending data back to the client.

- **Behavior**:
    - Sets the content type of the response to "text/html".
    - Checks if the request contains a "sessionId" parameter. If not, it responds with a 400 Bad Request status and a
      message indicating that a session ID is required.
    - Retrieves the session using the provided session ID.
    - Parses the updated settings from the "settings" request parameter and converts them from JSON format to a Java
      object.
    - Updates the user's settings in the data storage using the session information and the parsed settings object.
    - Redirects the user back to the settings page with the session ID included in the URL fragment.

### Usage

This servlet is intended to be mapped to a specific URL pattern within a web application. Users can navigate to the
corresponding URL to view or modify their session settings. The servlet relies on the application server for session
management, settings retrieval, and storage, as well as the authentication manager for user identification.

### Dependencies

- `ApplicationServer`: Used to interact with session management and settings storage.
- `Session`: Represents the user's current session.
- `authenticationManager`: Provides user authentication and identification services.
- `JsonUtil`: Utility for converting between JSON strings and Java objects.

### Example URL Patterns

- To view session settings: `GET /sessionSettings?sessionId=abc123`
- To update session settings: `POST /sessionSettings` (with form data including `sessionId` and updated `settings`).

This servlet provides a convenient way for users to manage their session-specific settings through a web interface,
enhancing the user experience and allowing for customized application behavior.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\UsageServlet.kt

## UsageServlet Documentation

`UsageServlet` is a Java servlet class designed to handle HTTP GET requests and present the usage summary of OpenAI
models for a specific session or user. It is part of a web application that manages sessions, users, and their
interactions with various OpenAI models. This servlet extends `HttpServlet` and overrides the `doGet` method to serve
HTML content displaying usage statistics.

### Package

```plaintext
com.simiacryptus.skyenet.webui.servlet
```

### Import Statements

The class imports necessary packages and classes for handling HTTP requests and responses, interacting with the
application's core platform services, and working with OpenAI model usage data.

### Class Definition

```kotlin
class UsageServlet : HttpServlet()
```

`UsageServlet` inherits from `HttpServlet`, making it a servlet capable of handling HTTP requests.

### Overridden Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

- **Parameters**:
    - `req`: The `HttpServletRequest` object that contains the request the client has made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet sends to the client.

- **Functionality**: This method processes HTTP GET requests. It checks if the request contains a `sessionId` parameter;
  if so, it fetches the session's usage summary. Otherwise, it attempts to retrieve the user's information based on a
  cookie and, if successful, fetches the user's usage summary. It then calls the `serve` method to generate and send the
  HTML response.

- **Response**: Sets the content type to `text/html` and the HTTP status code appropriately based on the request
  processing outcome. It generates an HTML page displaying the usage summary.

### Private Methods

#### serve(HttpServletResponse resp, Map<OpenAIModel, com.simiacryptus.jopenai.ApiModel.Usage> usage)

- **Parameters**:
    - `resp`: The `HttpServletResponse` object for sending the response.
    - `usage`: A map containing usage data for OpenAI models.

- **Functionality**: This method generates an HTML document that summarizes the usage of OpenAI models. It calculates
  total prompt tokens, completion tokens, and cost from the given usage data. It then constructs an HTML table
  displaying these statistics for each model and the totals.

### Companion Object

Contains a logger for logging purposes, which can be used for debugging or tracking the operations of the servlet.

### Usage Example

This servlet is intended to be deployed as part of a web application. It can be mapped to a specific URL pattern within
the application's web descriptor (web.xml) or through annotations, allowing it to respond to HTTP GET requests at that
URL.

When a user visits the corresponding URL, the servlet processes the request based on the presence of a `sessionId`
parameter or a user cookie, and generates an HTML page displaying the usage statistics of OpenAI models for the session
or user.

### Conclusion

`UsageServlet` provides a straightforward way to monitor and display the usage of OpenAI models by sessions or users
within a web application. By extending `HttpServlet` and overriding the `doGet` method, it serves dynamic content based
on the application's usage data, making it a useful component for applications that require tracking and displaying
usage statistics.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\ToolServlet.kt

## ToolServlet Class Documentation

The `ToolServlet` class is an abstract servlet that serves as a foundation for building web tools within the Skyenet web
application. It dynamically loads and manages tools defined by users, enabling the creation, editing, deletion, and
viewing of these tools through a web interface. This documentation provides an overview of its functionality, structure,
and usage.

### Overview

The `ToolServlet` class extends `HttpServlet` and is designed to handle HTTP requests for managing and interacting with
tools. Tools are defined by users and consist of a path, an OpenAPI description, an interpreter string, and servlet
code. The class supports operations such as displaying a list of tools, showing details for a specific tool, editing
tools, and deleting tools.

#### Key Features:

- Dynamic tool management: Add, edit, and delete tools through a web interface.
- Tool execution: Execute tool-specific code within a secure and isolated environment.
- User authentication and authorization: Ensure that only authorized users can manage and execute tools.

### Class Structure

#### Properties

- `app`: An instance of `ApplicationDirectory`, representing the application's directory structure.
- `tools`: A lazily initialized list of `Tool` objects, loaded from a JSON file.
- `userRoot`: A `File` object representing the root directory for storing tool-related data.

#### Inner Classes

- `Tool`: A data class representing a tool, including its path, OpenAPI description, interpreter string, and servlet
  code.

#### Methods

##### Constructors

- `ToolServlet(app: ApplicationDirectory)`: Initializes a new instance of `ToolServlet` with the specified application
  directory.

##### Overridden Methods

- `doGet(req: HttpServletRequest?, resp: HttpServletResponse?)`: Handles GET requests by displaying tool listings, tool
  details, or serving edit pages based on query parameters.
- `doPost(req: HttpServletRequest?, resp: HttpServletResponse?)`: Handles POST requests for editing tool details.
- `service(req: HttpServletRequest?, resp: HttpServletResponse?)`: Overrides the default service method to provide
  custom routing and execution of tools.

##### Private Methods

- `indexPage()`: Generates the HTML content for the tool listing page.
- `toolDetailsPage(tool: Tool)`: Generates the HTML content for a tool's detail page.
- `serveEditPage(req: HttpServletRequest, resp: HttpServletResponse, tool: Tool)`: Serves the HTML form for editing a
  tool.
- `construct(user: User, tool: Tool)`: Dynamically constructs and returns an instance of `HttpServlet` based on the
  tool's code and interpreter.

### Usage

1. **Initialization**: An instance of `ToolServlet` is created and initialized with an `ApplicationDirectory` object.
2. **Handling Requests**: The servlet listens for HTTP GET and POST requests.
    - GET requests can be used to view the list of tools, details of a specific tool, or the edit page for a tool.
    - POST requests are used to submit changes to an existing tool.
3. **Tool Management**: Through the web interface, users can add new tools, edit existing tools, or delete tools.
4. **Tool Execution**: Users can execute tools by navigating to the specific path associated with each tool. Execution
   is handled securely, with support for user authentication and authorization.

### Security Considerations

- The `ToolServlet` class includes mechanisms for user authentication and authorization to ensure that only authorized
  users can manage and execute tools.
- Tool execution is isolated to mitigate security risks associated with executing user-defined code.

### Conclusion

The `ToolServlet` class provides a flexible and dynamic framework for managing and executing tools within the Skyenet
web application. It leverages Kotlin's capabilities for dynamic code execution and integrates with the application's
authentication and authorization mechanisms to ensure secure operation.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\UserInfoServlet.kt

## UserInfoServlet Documentation

The `UserInfoServlet` class extends the `HttpServlet` class to provide a specific implementation for handling HTTP GET
requests. This servlet is designed to return information about the user based on a cookie provided in the request. The
response is formatted in JSON.

### Package

This servlet is part of the `com.simiacryptus.skyenet.webui.servlet` package, which likely contains other servlets and
utilities for web UI interaction within the Skyenet framework.

### Import Statements

The servlet imports various classes for its functionality:

- **com.simiacryptus.jopenai.util.JsonUtil**: Used for converting the `User` object to a JSON string.
- **com.simiacryptus.skyenet.core.platform.ApplicationServices**: Provides access to application-wide services, such as
  authentication.
- **com.simiacryptus.skyenet.core.platform.User**: Represents the user entity.
- **com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie**: Utility method to extract
  cookies from the request.
- **jakarta.servlet.http.HttpServlet, HttpServletRequest, HttpServletResponse**: Standard servlet API classes for
  handling HTTP requests and responses.

### Class Definition

`class UserInfoServlet : HttpServlet()` - This class is a servlet that handles user information retrieval. It inherits
from `HttpServlet`.

### Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

This method overrides `HttpServlet`'s `doGet` method to implement the logic for handling HTTP GET requests.

- **Parameters**:
    - `HttpServletRequest req`: The request object, providing information such as parameters and cookies.
    - `HttpServletResponse resp`: The response object, used to send data back to the client.

- **Functionality**:
    1. Sets the content type of the response to `text/json`.
    2. Sets the HTTP status code of the response to `SC_OK` (200).
    3. Attempts to retrieve the user based on a cookie from the request. The method `getCookie()` is used to extract the
       cookie, and `ApplicationServices.authenticationManager.getUser()` is used to retrieve the user.
    4. If a user is found, their information is converted to a JSON string
       using `JsonUtil.objectMapper().writeValueAsString(user)` and written to the response.
    5. If no user is found, an empty JSON object (`{}`) is written to the response.

### Usage

To use this servlet, it must be properly mapped in the web application's configuration (e.g., web.xml or through
annotations) so that it can receive HTTP GET requests. When a request is sent to the servlet's URL with the appropriate
cookie, it will return the user's information in JSON format if the user is authenticated, or an empty JSON object if
not.

This servlet is useful in scenarios where client-side scripts or applications need to fetch information about the
currently authenticated user without requiring a full page reload or additional authentication mechanisms.

### Security Considerations

Ensure that sensitive user information is not exposed unintentionally through this servlet. It's important to consider
what user data is serialized into JSON and sent in the response. Additionally, proper validation and sanitation of
cookies and other inputs should be implemented to prevent security vulnerabilities.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\SessionThreadsServlet.kt

### SessionThreadsServlet Documentation

The `SessionThreadsServlet` class is part of the web UI component of an application, designed to display detailed
information about thread pools associated with a particular session. It extends the `HttpServlet` class, making it a
servlet that can respond to HTTP GET requests with session-specific thread pool statistics and stack traces.

#### Package

The servlet is located in the `com.simiacryptus.skyenet.webui.servlet` package, which suggests it is part of the web UI
module of the Skyenet application.

#### Dependencies

This class relies on various components from the `com.simiacryptus.skyenet.core.platform` package,
including `ApplicationServices`, `ClientManager`, and `Session`. It also utilizes the `ApplicationServer` class from
the `com.simiacryptus.skyenet.webui.application` package for server-related functionalities.

#### Constructor

The constructor of the `SessionThreadsServlet` class requires an `ApplicationServer` instance. This dependency is
injected at the time of servlet instantiation and is used to access application-level services and configurations.

```java
SessionThreadsServlet(ApplicationServer server)
```

#### doGet Method

The `doGet` method is overridden to handle HTTP GET requests. It is designed to output HTML content that displays thread
pool statistics and stack traces for a given session.

##### Parameters

- `HttpServletRequest req`: The request object, which provides request information for HTTP servlets.
- `HttpServletResponse resp`: The response object, which servlets use to respond to requests.

##### Functionality

1. **Content Type and Status**: Sets the response content type to `text/html` and the status
   to `HttpServletResponse.SC_OK`.

2. **Session ID Check**: Checks if the request contains a `sessionId` parameter. If not, it returns a `400 Bad Request`
   response with a message indicating that a session ID is required.

3. **Session and User Identification**: Creates a `Session` object using the provided `sessionId` and retrieves the
   current user by using a cookie from the request.

4. **Thread Pool Retrieval**: Obtains a thread pool associated with the session and user through the `clientManager`.

5. **HTML Response Generation**: Generates an HTML page displaying statistics about the thread pool (such as active
   thread count, queue size, completed task count, etc.) and the stack traces of all alive threads in the pool. The HTML
   includes CSS styles for better readability.

##### HTML Structure

The response HTML is structured into two main sections:

- **Pool Stats**: Displays general statistics about the thread pool, including session ID, user, pool details, active
  and total thread counts, queue size, completed task count, and total task count.

- **Thread Stacks**: Lists all alive threads in the pool, showing each thread's name and stack trace. Each stack trace
  element is displayed in a monospaced font for clarity.

#### Usage

To deploy this servlet, it needs to be registered in the web application's servlet context, typically defined in
the `web.xml` file or through annotations. Once deployed, it can be accessed by making a GET request to the servlet's
URL with a `sessionId` parameter. This could be useful for administrators or developers for debugging and monitoring
purposes, providing insights into the application's thread usage and potential bottlenecks at the session level.

#### Security Considerations

Given that this servlet exposes sensitive information about the application's internal state, it's crucial to implement
appropriate authentication and authorization checks to ensure that only authorized users can access this information.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\UserSettingsServlet.kt

## UserSettingsServlet Documentation

`UserSettingsServlet` is a servlet class designed to manage user settings within a web application. This class
extends `HttpServlet` and overrides the `doGet` and `doPost` methods to handle HTTP GET and POST requests, respectively.
It is part of the `com.simiacryptus.skyenet.webui.servlet` package.

### Overview

The servlet serves two main purposes:

1. **Display User Settings**: Through the `doGet` method, it presents the user's current settings in an editable form.
2. **Update User Settings**: Through the `doPost` method, it allows users to submit changes to their settings which are
   then persisted.

### Key Components

- **Authentication**: Determines the user's identity based on a cookie.
- **User Settings Management**: Fetches and updates user settings data.
- **Security**: Masks sensitive information like API keys in the user interface.

### Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

Handles HTTP GET requests by displaying the user's current settings in a web form.

##### Parameters:

- `HttpServletRequest req`: The request object containing the request data.
- `HttpServletResponse resp`: The response object used to send data back to the client.

##### Behavior:

1. Sets the response content type to `text/html`.
2. Fetches user information based on a cookie.
3. If the user is not authenticated, sends a 400 Bad Request response.
4. Otherwise, retrieves the user's settings, masks sensitive information, and renders a form with the settings as
   editable JSON.

#### doPost(HttpServletRequest req, HttpServletResponse resp)

Handles HTTP POST requests for updating user settings.

##### Parameters:

- `HttpServletRequest req`: The request object containing the form data.
- `HttpServletResponse resp`: The response object used to send data back to the client.

##### Behavior:

1. Fetches user information based on a cookie.
2. If the user is not authenticated, sends a 400 Bad Request response.
3. Otherwise, parses the updated settings from the request, reconciles them with existing settings (ensuring sensitive
   information like API keys are correctly handled), and updates the user's settings.
4. Redirects the user to the home page.

### Security Considerations

- The servlet masks API keys when displaying them to the user to prevent accidental exposure.
- It uses cookies for authentication, which requires proper cookie security settings (e.g., HttpOnly, Secure attributes)
  to mitigate risks like CSRF or session hijacking.

### Dependencies

- `com.simiacryptus.jopenai.util.JsonUtil`: For JSON serialization and deserialization.
- `com.simiacryptus.skyenet.core.platform.*`: For application services like authentication and user settings management.
- `jakarta.servlet.http.HttpServlet`: Base class for handling HTTP requests.

### Usage Example

This servlet is mapped to a URL (e.g., `/userSettings/`) in the web application. Users can view and edit their settings
by navigating to this URL. The servlet ensures that only authenticated users can access and modify their settings.

```java

@WebServlet("/userSettings/")
public class UserSettingsServlet extends HttpServlet {
    // Implementation details...
}
```

**Note**: Actual deployment and URL mapping require additional configuration in the web application (e.g., `web.xml` or
annotations).

### Logging

The servlet includes a static logger for logging important events or errors. This can be utilized for debugging or
monitoring the servlet's operations.

```java
private static final Logger log = LoggerFactory.getLogger(UserSettingsServlet.class);
```

Ensure proper logging levels are set in your logging framework configuration to capture necessary log messages.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\ZipServlet.kt

## ZipServlet Class Documentation

The `ZipServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends the `HttpServlet`
class to provide a specific functionality for zipping files and directories located on the server, based on the client's
request. This servlet is designed to handle HTTP GET requests to dynamically generate and serve ZIP archives of files or
directories specified in the request parameters.

### Overview

- **Package:** `com.simiacryptus.skyenet.webui.servlet`
- **Extended Class:** `HttpServlet`
- **Purpose:** To serve ZIP archives of files or directories specified in client requests.

### Constructor

#### ZipServlet(StorageInterface dataStorage)

Initializes a new instance of the `ZipServlet` class with a specified `StorageInterface` for accessing file storage.

- **Parameters:**
    - `dataStorage`: An instance of `StorageInterface` used to access the server's file storage system.

### Public Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

Handles HTTP GET requests by generating and serving a ZIP archive based on parameters provided in the request.

- **Parameters:**
    - `req`: An `HttpServletRequest` object that contains the request the client has made to the servlet.
    - `resp`: An `HttpServletResponse` object that contains the response the servlet sends to the client.
- **Throws:** This method does not explicitly throw exceptions, but underlying operations may throw runtime exceptions
  such as `IOException`.

### Private Methods

#### write(File basePath, File file, ZipOutputStream zip)

Recursively writes files and directories into the provided `ZipOutputStream`. If the provided `File` object is a
directory, this method will recursively call itself for each file within the directory, excluding files starting
with ".".

- **Parameters:**
    - `basePath`: The base path used to calculate relative paths for ZIP entries.
    - `file`: The file or directory to be written to the ZIP archive.
    - `zip`: The `ZipOutputStream` to which files and directories are written.
- **Visibility:** Private. This method is an implementation detail and not intended for external use.

### Usage Example

This class is typically used as part of a web application to dynamically generate ZIP archives of files stored on the
server. It is intended to be mapped to a specific URL pattern within a web application's servlet configuration.

```java

@WebServlet("/zip")
public class ZipServlet extends HttpServlet {
    private StorageInterface dataStorage;

    public ZipServlet() {
        this.dataStorage = /* Initialize your StorageInterface implementation here */;
    }

    // Implementation of doGet and other methods
}
```

### Important Considerations

- The servlet performs basic path validation but does not implement comprehensive security checks. Ensure that proper
  authentication and authorization mechanisms are in place to prevent unauthorized access to files.
- Large files or directories may consume significant server resources during the zipping process. Consider implementing
  size limits or asynchronous processing to mitigate resource exhaustion.
- Temporary files are created during the zipping process and are deleted after serving. Ensure that the server has
  sufficient disk space and permissions to manage these temporary files.

This documentation provides a comprehensive overview of the `ZipServlet` class, its functionality, and usage within a
web application context.

# main\kotlin\com\simiacryptus\skyenet\webui\session\MutableSessionHandler.kt

## MutableSessionHandler Class Documentation

The `MutableSessionHandler` class, part of the `com.simiacryptus.skyenet.webui.session` package, is a flexible WebSocket
session manager designed to handle dynamic delegation of socket management tasks. It extends the `SocketManager`
interface, allowing it to manage WebSocket sessions and delegate these tasks to other `SocketManager` instances as
needed.

### Overview

The `MutableSessionHandler` is designed to manage WebSocket connections dynamically. It allows changing the
delegate `SocketManager` at runtime, effectively changing the behavior of how WebSocket messages are handled, sockets
are added or removed, and session replays are obtained. This is particularly useful in scenarios where the handling
logic needs to change based on runtime conditions or configurations.

### Constructor

- `MutableSessionHandler(initialDelegate: SocketManager?)`:
  Initializes a new instance of `MutableSessionHandler` with an optional initial delegate.

### Properties

- `private var priorDelegates: MutableList<SocketManager>`: A list to keep track of all previous delegates.
- `private var currentDelegate: SocketManager?`: The current delegate that is actively managing the WebSocket sessions.
- `private val sockets: MutableMap<ChatSocket, Session>`: A map that keeps track of the active WebSocket sessions.

### Methods

#### setDelegate

```kotlin
fun setDelegate(delegate: SocketManager)
```

Sets a new delegate for managing WebSocket sessions. The current delegate, if any, is added to the list of prior
delegates, and the new delegate takes over. All existing WebSocket sessions are transferred to the new delegate.

#### removeSocket

```kotlin
override fun removeSocket(socket: ChatSocket)
```

Removes a WebSocket session. The session is removed from the internal map and also from the current delegate, if any.

#### addSocket

```kotlin
override fun addSocket(socket: ChatSocket, session: Session)
```

Adds a new WebSocket session. The session is added to the internal map and also to the current delegate, if any.

#### getReplay

```kotlin
override fun getReplay(): List<String>
```

Returns a list of messages that represent the replay of all sessions managed by this handler. This includes replays from
all prior delegates as well as the current delegate.

#### onWebSocketText

```kotlin
override fun onWebSocketText(socket: ChatSocket, message: String)
```

Handles an incoming text message on a WebSocket. This method delegates the handling of the message to the current
delegate, if any.

### Usage Example

```kotlin
val mutableSessionHandler = MutableSessionHandler(initialDelegate)
mutableSessionHandler.addSocket(chatSocket, session)
mutableSessionHandler.setDelegate(newDelegate)
```

This example demonstrates creating a `MutableSessionHandler` with an initial delegate, adding a WebSocket session, and
then changing the delegate.

### Conclusion

The `MutableSessionHandler` class provides a flexible and dynamic way to manage WebSocket sessions in applications where
the handling logic needs to adapt at runtime. Its ability to switch delegates on-the-fly and keep track of session
replays makes it a powerful tool for complex WebSocket management scenarios.

# main\kotlin\com\simiacryptus\skyenet\webui\servlet\WelcomeServlet.kt

## WelcomeServlet Class Documentation

The `WelcomeServlet` class is part of the SkyeNet web application framework, designed to serve as the entry point for
user interactions with the web application. This class extends `HttpServlet` and provides customized handling for HTTP
GET and POST requests, primarily focusing on rendering the application's homepage, user information, settings, and
facilitating logout operations. It also dynamically generates links to web applications and sessions based on user
authorization.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.servlet`
- **Dependencies**:
    - SkyeNet Core Platform classes (`ApplicationServices`, `AuthorizationInterface`, etc.)
    - Java Servlet API (`HttpServlet`, `HttpServletRequest`, `HttpServletResponse`)
    - Other utility classes within the SkyeNet framework

### Constructor

#### WelcomeServlet(ApplicationDirectory parent)

Initializes a new instance of the `WelcomeServlet` class.

- **Parameters**:
    - `parent`: An instance of `ApplicationDirectory` that represents the parent directory of the web application.

### Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

Handles HTTP GET requests by routing to specific functionalities based on the requested URI.

- **Parameters**:
    - `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet returns to the client.

#### doPost(HttpServletRequest req, HttpServletResponse resp)

Handles HTTP POST requests, specifically for user settings updates.

- **Parameters**:
    - `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet returns to the client.

#### homepage(User user)

Generates the HTML content for the application's homepage.

- **Parameters**:
    - `user`: The `User` object representing the currently authenticated user.
- **Returns**: A `String` containing the HTML content.

#### appRow(ApplicationDirectory.ChildWebApp app, User user)

Generates an HTML row for each application, including action links based on the user's authorization.

- **Parameters**:
    - `app`: An instance of `ApplicationDirectory.ChildWebApp` representing a child web application.
    - `user`: The `User` object representing the currently authenticated user.
- **Returns**: A `String` containing the HTML table row for the specified application.

### Protected Fields

- **welcomeMarkdown**: A Markdown `String` that can be overridden to customize the welcome message on the homepage.
- **postAppMarkdown**: A Markdown `String` that can be overridden to provide additional content or instructions after
  the list of applications on the homepage.

### Usage

To utilize the `WelcomeServlet`, it must be registered and mapped to a URL pattern within the web application's servlet
context. This is typically done in the application's initialization code or through configuration in the `web.xml` file.

### Example

```java

@WebServlet("/welcome")
public class CustomWelcomeServlet extends WelcomeServlet {
    public CustomWelcomeServlet() {
        super(new ApplicationDirectory(...));
    }

    @Override
    protected String getWelcomeMarkdown() {
        return "# Welcome to Our Custom Application\nEnjoy our features!";
    }
}
```

This example demonstrates how to extend `WelcomeServlet` to customize the welcome message. The custom servlet is mapped
to the `/welcome` URL pattern.

# main\kotlin\com\simiacryptus\skyenet\webui\session\SessionTask.kt

## SessionTask Class Documentation

The `SessionTask` class is an abstract class designed to facilitate the handling and displaying of task-related messages
and data within a web UI session. It provides a structured way to append messages, errors, images, and other HTML
elements to the task output, manage file saving, and control the visibility of a loading spinner.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.session`
- **Imports**: Various, including utilities for handling images, markdown rendering, and logging.
- **Dependencies**: Requires classes such as `ApplicationInterface`, `CodingActor`, and `ValidatedObject` from the
  project's modules.

### Key Methods

#### `add`

Adds a message to the task output with customizable HTML tag and CSS class. It optionally shows a spinner.

- **Parameters**:
    - `message`: The message to add.
    - `showSpinner`: Whether to show the spinner for the task (default: true).
    - `tag`: The HTML tag to wrap the message in (default: div).
    - `className`: The CSS class to apply to the message (default: response-message).

#### `hideable`

Similar to `add`, but includes a close button to hide the message. Returns a `StringBuilder` instance that can be
cleared to remove the message from the UI.

- **Parameters**:
    - `ui`: The application interface instance.
    - All other parameters are similar to `add`.

#### `echo`

Echos a user message to the task output, specifically styled as a "user-message".

- **Parameters**: Similar to `add`.

#### `header`

Adds a header-styled message to the task output.

- **Parameters**: Similar to `add`, but with a default classname of "response-header".

#### `verbose`

Adds a verbose message to the task output. Verbose messages are hidden by default.

- **Parameters**: Similar to `add`, but with a default tag of "pre" and classname of "verbose".

#### `error`

Displays an error in the task output, formatted based on the type of error. It can handle and format specific exceptions
differently.

- **Parameters**:
    - `ui`: The application interface instance.
    - `e`: The throwable/error to display.
    - All other parameters are similar to `add`.

#### `complete`

Displays a final message in the task output and hides the spinner.

- **Parameters**: Similar to `add`, but does not show the spinner by default.

#### `image`

Displays an image in the task output by converting it to PNG, saving it, and embedding it as an `<img>` tag.

- **Parameters**:
    - `image`: The `BufferedImage` to display.

#### `saveFile`

Saves the given data to a file and returns the URL of the file. This method is abstract and must be implemented by
subclasses.

- **Parameters**:
    - `relativePath`: The name of the file to save.
    - `data`: The data to save.

### Utility Methods and Fields

- `toPng`: A companion object method that converts a `BufferedImage` to a PNG byte array.
- `spinner`: A constant HTML string for displaying a loading spinner.
- `log`: A logger instance for logging purposes.

### Usage Example

To use `SessionTask`, one must extend this abstract class and implement the abstract methods such as `send`
and `saveFile`. Once implemented, the various public methods can be used to manage task output in a web UI session
effectively.

```kotlin
class MySessionTask : SessionTask() {
  override fun send(html: String) {
    // Implementation to send HTML to the client
  }

  override fun saveFile(relativePath: String, data: ByteArray): String {
    // Implementation to save file and return its URL
  }
}
```

In this setup, `MySessionTask` would be capable of handling task outputs, including messages, errors, images, and more,
providing a rich interactive experience in a web UI session.

# main\kotlin\com\simiacryptus\skyenet\webui\session\SocketManager.kt

## SocketManager Interface Documentation

The `SocketManager` interface is an essential component within the `com.simiacryptus.skyenet.webui.session` package,
designed to manage WebSocket connections for chat functionalities. It provides a structured way to handle chat sockets,
ensuring that messages are correctly managed, and sessions are maintained appropriately. Below is a detailed overview of
its functionalities.

### Overview

The `SocketManager` interface defines methods for managing `ChatSocket` instances. These methods allow for the addition
and removal of chat sockets, handling incoming text messages on the WebSocket, and retrieving a replay of chat messages.
Implementing this interface is crucial for managing chat functionalities within a web application, ensuring that chat
sessions are handled efficiently.

### Methods

#### `fun removeSocket(socket: ChatSocket)`

Removes a specified `ChatSocket` from the manager. This method is typically called when a chat socket is closed or is no
longer needed. It ensures that resources are freed and that the socket is properly cleaned up.

- **Parameters:**
    - `socket`: The `ChatSocket` instance to be removed.

#### `fun addSocket(socket: ChatSocket, session: Session)`

Adds a new `ChatSocket` to the manager, associating it with a specific `Session`. This method is essential for
initializing chat functionalities for a new user session, ensuring that messages can be correctly routed to and from the
user.

- **Parameters:**
    - `socket`: The `ChatSocket` instance to be added.
    - `session`: The `Session` instance associated with the `ChatSocket`.

#### `fun getReplay(): List<String>`

Retrieves a list of chat messages that can be used to replay chat history. This is particularly useful for providing
users with context when they join an ongoing chat session, allowing them to catch up on the conversation.

- **Returns:** A `List<String>` containing chat messages.

#### `fun onWebSocketText(socket: ChatSocket, message: String)`

Handles incoming chat messages received over a WebSocket. This method should implement the logic for processing text
messages sent by users, such as broadcasting the message to other participants or performing command actions.

- **Parameters:**
    - `socket`: The `ChatSocket` through which the message was received.
    - `message`: The text message received over the WebSocket.

### Usage

Implement the `SocketManager` interface in a class to manage chat sockets within your web application. Ensure that each
method is implemented to handle the addition and removal of sockets, process incoming messages, and manage chat history
appropriately.

```kotlin
class MySocketManager : SocketManager {
  // Implement methods here
}
```

By adhering to the `SocketManager` interface, your application can efficiently manage chat functionalities, providing a
robust and interactive user experience.

# main\kotlin\com\simiacryptus\skyenet\webui\test\CodingActorTestApp.kt

## CodingActorTestApp Class Documentation

The `CodingActorTestApp` class extends the functionality of an `ApplicationServer` to provide a specialized application
server that interacts with a `CodingActor`. This application server is designed to process user messages, execute code
provided by the `CodingActor`, and render the output in a user-friendly format. It is part of the
package `com.simiacryptus.skyenet.webui.test`.

### Constructor

```kotlin
CodingActorTestApp(
  private val actor : CodingActor,
applicationName: String = "CodingActorTest_" + actor.name,
temperature: Double = 0.3,
)
```

#### Parameters:

- `actor: CodingActor`: The coding actor instance that will be used to generate and execute code.
- `applicationName: String`: The name of the application, defaulting to "CodingActorTest_" followed by the name of the
  actor.
- `temperature: Double`: A parameter influencing the randomness of the responses from the actor, with a default value of
  0.3.

### Methods

#### `userMessage`

```kotlin
override fun userMessage(
  session: Session,
  user: User?,
  userMessage: String,
  ui: ApplicationInterface,
  api: API
)
```

Handles messages from users. It processes the user's input, interacts with the `CodingActor` to generate code based on
the input, and formats the response for display in the application's UI.

##### Parameters:

- `session: Session`: The current session object.
- `user: User?`: The user object, which can be `null`.
- `userMessage: String`: The message input from the user.
- `ui: ApplicationInterface`: The interface to interact with the application's UI.
- `api: API`: The API client used for external requests.

##### Functionality:

1. Sets a budget for the API client.
2. Renders the user's message as markdown and displays it.
3. Requests the `CodingActor` to generate a response based on the user's message.
4. Checks if the user is authorized to execute code.
5. If authorized, provides a link to execute the generated code and displays the execution result.
6. In case of errors, logs the error and displays an error message to the user.

### Companion Object

Contains a logger for the `CodingActorTestApp` class.

```kotlin
companion object {
  private val log = LoggerFactory.getLogger(CodingActorTestApp::class.java)
}
```

### Usage Example

To use `CodingActorTestApp`, you need to instantiate it with a specific `CodingActor` and optionally customize the
application name and temperature. After instantiation, the `CodingActorTestApp` can be attached to an application server
to start processing user messages.

```kotlin
val codingActor = CodingActor(/* parameters */)
val codingActorTestApp = CodingActorTestApp(actor = codingActor)
applicationServer.attach(codingActorTestApp)
```

This class is a part of a larger system designed to facilitate interactive coding challenges or tutorials through a web
UI, leveraging the capabilities of coding actors to generate and execute code dynamically based on user input.

# main\kotlin\com\simiacryptus\skyenet\webui\test\ParsedActorTestApp.kt

## ParsedActorTestApp Class Documentation

The `ParsedActorTestApp` class is designed to test parsed actors within a web UI context. It extends
the `ApplicationServer` class, providing a specific implementation focused on interacting with `ParsedActor` instances.
This class allows users to send messages through a web interface and receive responses generated by the
specified `ParsedActor`.

### Generic Type Parameter

- `T`: The type parameter `T` represents the type of data the `ParsedActor` works with. It ensures type safety and
  consistency across the interactions with the actor.

### Constructor

The constructor initializes a new instance of the `ParsedActorTestApp` class.

#### Parameters

- `actor`: The `ParsedActor` instance to be tested. It should be an instance of a class that extends `ParsedActor<T>`.
- `applicationName` (optional): The name of the application. If not provided, it defaults to "ParsedActorTest_" followed
  by the simple name of the parser class used by the actor.
- `temperature` (optional): A parameter influencing the randomness of the actor's responses. Defaults to 0.3.

### Methods

#### userMessage

This method handles messages from users.

##### Parameters

- `session`: The `Session` object representing the current session.
- `user`: An optional `User` instance representing the user sending the message. Can be `null`.
- `userMessage`: The message sent by the user.
- `ui`: The `ApplicationInterface` instance used to interact with the application's UI.
- `api`: An `API` instance used for making external API calls.

##### Functionality

1. Sets the budget for API calls to $2.00.
2. Creates a new task in the UI to handle the user's message.
3. Renders the user's message as markdown and echoes it back to the UI.
4. Calls the `answer` method of the `ParsedActor` with the user's message, generating a response.
5. Renders the response as markdown, including a code block with the JSON representation of the response object, and
   completes the task with this output.
6. Catches and logs any errors that occur during processing, displaying an error message in the UI.

### Companion Object

#### Properties

- `log`: A logger instance for logging warnings and errors.

### Usage Example

To use `ParsedActorTestApp`, you would typically instantiate it with a specific implementation of `ParsedActor` and
optionally specify the application name and temperature. After instantiation, the application can handle user messages,
leveraging the provided `ParsedActor` to generate and display responses.

```kotlin
val myActor = MyParsedActorImplementation()
val testApp = ParsedActorTestApp(myActor)
```

This class is particularly useful for developers looking to test and debug `ParsedActor` implementations in a simulated
web UI environment, allowing for interactive testing and immediate feedback.

# main\kotlin\com\simiacryptus\skyenet\webui\test\ImageActorTestApp.kt

## ImageActorTestApp Class Documentation

The `ImageActorTestApp` class extends the functionality of `ApplicationServer` to create a specialized server
application designed for testing image actors. This class integrates various components from the project's architecture
to facilitate user interaction with image actors through a web UI.

### Package

```plaintext
com.simiacryptus.skyenet.webui.test
```

### Imports

The class uses several imports from both the project's modules and external libraries, including logging, markdown
rendering, and OpenAI's API wrapper.

### Constructor

```kotlin
ImageActorTestApp(
  private val actor : ImageActor,
applicationName: String = "ImageActorTest_" + actor.javaClass.simpleName,
temperature: Double = 0.3,
)
```

- `actor`: The `ImageActor` instance to be tested.
- `applicationName`: Optional. The name of the application, defaulting to "ImageActorTest_" followed by the simple name
  of the actor's class.
- `temperature`: Optional. A parameter influencing the randomness of responses, with a default value of 0.3.

### Settings Class

```kotlin
data class Settings(
  val actor: ImageActor? = null,
)
```

A data class used to encapsulate settings specific to the `ImageActorTestApp`. Currently, it holds an
optional `ImageActor` instance.

### Overridden Members

#### settingsClass

```kotlin
override val settingsClass: Class<*>
```

Returns the `Class` object associated with the `Settings` data class.

#### initSettings

```kotlin
@Suppress("UNCHECKED_CAST")
override fun <T : Any> initSettings(session: Session): T?
```

Initializes and returns the settings for a given session. The settings are cast to the generic type `T`.

#### userMessage

```kotlin
override fun userMessage(
  session: Session,
  user: User?,
  userMessage: String,
  ui: ApplicationInterface,
  api: API
)
```

Handles messages from users. It sets a budget for API usage, processes the user's message through the image actor, and
manages the response, including any errors.

### Companion Object

```kotlin
companion object {
  private val log = LoggerFactory.getLogger(ImageActorTestApp::class.java)
}
```

Contains a logger for the class, facilitating logging throughout the class's methods.

### Usage

To use `ImageActorTestApp`, instantiate it with a specific `ImageActor` implementation. The application can then be
started to allow users to interact with the actor through a web interface. Messages sent by users are processed by
the `userMessage` method, which communicates with the actor and handles the output.

### Error Handling

Errors encountered during the processing of user messages are logged and reported back to the user through the web UI.

---

This documentation provides an overview of the `ImageActorTestApp` class, its constructor, methods, and usage within the
context of a server application designed for testing image actors.

# main\kotlin\com\simiacryptus\skyenet\webui\session\SocketManagerBase.kt

## SocketManagerBase Class Documentation

The `SocketManagerBase` class is an abstract class designed to manage WebSocket connections for a chat application,
handling messaging, tasks, and file operations within a session. It serves as a core component for managing chat
sessions, including sending messages, handling commands, and managing user interactions through WebSockets.

### Overview

- **Package:** `com.simiacryptus.skyenet.webui.session`
- **Imports:** Utilizes various classes from the `com.simiacryptus.skyenet.core.platform` package, chat utilities, and
  standard Java utilities.

### Key Components

#### Properties

- `session`: Represents the current user session.
- `dataStorage`: Interface for storage operations, nullable.
- `owner`: The user who owns the session, nullable.
- `messageStates`: LinkedHashMap storing message states.
- `applicationClass`: Class object used for authorization checks.
- `sockets`: MutableMap managing ChatSocket to WebSocket session mappings.
- `sendQueues`: MutableMap for storing message queues per ChatSocket.
- `messageVersions`: HashMap tracking version numbers for messages.

#### Constructor

The constructor initializes the class with the session, dataStorage, owner, messageStates based on the session and
owner, and the applicationClass for authorization purposes.

#### Methods

##### WebSocket Management

- `addSocket(socket: ChatSocket, session: org.eclipse.jetty.websocket.api.Session)`: Adds a WebSocket connection to the
  manager after performing authorization checks.
- `removeSocket(socket: ChatSocket)`: Removes a WebSocket connection from the manager.

##### Messaging

- `send(out: String)`: Processes and sends a message to all connected WebSockets.
- `publish(out: String)`: Distributes a message to all connected sockets.
- `getReplay()`: Retrieves a list of all messages with their versions for replaying to a newly connected client.

##### Task and Command Handling

- `newTask(cancelable: Boolean = false)`: Initiates a new task, returning a `SessionTask` instance.
- `onWebSocketText(socket: ChatSocket, message: String)`: Handles incoming WebSocket text messages, processing commands
  or user messages.

##### Utility Methods

- `canWrite(user: User?)`: Checks if the given user has write access.
- `hrefLink(linkText: String, classname: String, handler: Consumer<Unit>)`: Generates an HTML anchor tag for link
  interactions.
- `textInput(handler: Consumer<String>)`: Generates an HTML form for text input.

#### Inner Classes

- `SessionTaskImpl`: Represents a task within the session, capable of sending messages and saving files.

### Usage

This class is designed to be extended by specific implementations that handle chat functionalities for different types
of sessions. Implementors will need to define the abstract method `onRun(userMessage: String, socket: ChatSocket)`,
which handles user messages.

#### Example

```kotlin
class ChatSocketManager(session: Session, dataStorage: StorageInterface?) : SocketManagerBase(session, dataStorage) {
  override fun onRun(userMessage: String, socket: ChatSocket) {
    // Implementation for handling user messages
  }
}
```

This class is a critical component for managing chat functionalities, ensuring message delivery, session management, and
command processing within a web application.

# main\kotlin\com\simiacryptus\skyenet\webui\test\SimpleActorTestApp.kt

## SimpleActorTestApp Developer Documentation

The `SimpleActorTestApp` class is a part of the com.simiacryptus.skyenet.webui.test package, designed to integrate
a `SimpleActor` into a web application. This document provides an overview of its functionality, setup, and usage within
the context of the application.

### Overview

`SimpleActorTestApp` extends `ApplicationServer`, making it a web application that can interact with users and process
their messages through a given `SimpleActor`. It is designed to demonstrate and test the capabilities of `SimpleActor`
instances within a web UI.

### Key Features

- **Actor Integration**: Incorporates a `SimpleActor` to process user messages.
- **Customizable Application Name**: Allows specifying an application name, defaulting to "SimpleActorTest_" followed by
  the simple name of the `SimpleActor` class.
- **Message Processing**: Handles user messages, processes them through the `SimpleActor`, and displays the responses.
- **Markdown Rendering**: Supports rendering of messages and responses in Markdown format.

### Components

#### Settings

A data class that holds the configuration for the application, primarily the `SimpleActor` instance to be used.

#### initSettings

Initializes settings for a user session, setting up the specified `SimpleActor`.

#### userMessage

Handles messages from users. It sets a budget for the `ClientManager.MonitoredClient`, processes the user message
through the `SimpleActor`, and sends back the response.

### Usage

To use `SimpleActorTestApp`, instantiate it with a `SimpleActor` and optionally specify the application name and
temperature. The application can then be started and will be accessible at the path "/simpleActorTest".

#### Example

```kotlin
val simpleActor = SimpleActor(/* configuration */)
val app = SimpleActorTestApp(simpleActor)
app.start()
```

This will start the application server, making the `SimpleActorTestApp` accessible to users. Users can send messages
which will be processed by the specified `SimpleActor` and the responses will be displayed in the web UI.

### Logging

`SimpleActorTestApp` uses SLF4J for logging, allowing developers to track its operations and any issues that arise.

### Conclusion

`SimpleActorTestApp` provides a straightforward way to integrate `SimpleActor` instances into a web UI for testing and
demonstration purposes. It showcases the capabilities of `SimpleActor` in processing and responding to user messages,
with support for markdown rendering and customizable settings.

For further customization and integration, developers can extend `SimpleActorTestApp` or modify its components as needed
to fit the requirements of their specific use case.

# main\kotlin\com\simiacryptus\skyenet\webui\util\EncryptFiles.kt

## EncryptFiles Utility Documentation

### Overview

The `EncryptFiles` utility is designed to encrypt files using AWS KMS (Key Management Service) and write the encrypted
data to a specified location on the disk. This utility is part of the `com.simiacryptus.skyenet.webui.util` package and
leverages the `ApplicationServices` for encryption services. It is a Kotlin-based utility that provides a
straightforward approach to encrypting strings (file contents) and saving them for further use.

### Prerequisites

Before using the `EncryptFiles` utility, ensure that you have the following prerequisites met:

- AWS Account: You need an AWS account and access to the AWS Key Management Service (KMS) to create and manage
  encryption keys.
- AWS Credentials: Your AWS credentials must be configured properly on the machine where you intend to run this utility.
  This is often done via the AWS CLI configuration or by setting environment variables.
- Kotlin Environment: Since this utility is written in Kotlin, ensure that you have a Kotlin compiler installed or an
  IDE that supports Kotlin (e.g., IntelliJ IDEA).

### Usage

The `EncryptFiles` utility is designed to be executed from the command line or within an IDE. Here's a brief guide on
how to use it:

1. **Prepare the Input String**: The utility is currently set up to encrypt a hardcoded empty string `""`. You will need
   to modify the input string within the `main` function to the content you wish to encrypt.

2. **Set the Key ID**: Replace the placeholder key
   ID (`"arn:aws:kms:us-east-1:470240306861:key/a1340b89-64e6-480c-a44c-e7bc0c70dcb1"`) with your actual AWS KMS key ARN
   that you intend to use for encryption.

3. **Specify the Output Path**: Change the output
   path (`"""C:\Users\andre\code\SkyenetApps\src\main\resources\patreon.json.kms"""`) to the desired location on your
   disk where the encrypted file should be saved.

4. **Run the Utility**: Execute the utility from your command line or IDE. The specified string will be encrypted using
   the provided AWS KMS key and written to the specified output path.

### Extension Methods

The utility includes two extension methods for the `String` class:

- `encrypt(keyId: String)`: Encrypts the string (interpreted as file content) using the specified AWS KMS key ID. Throws
  a `RuntimeException` if encryption fails.

- `write(outpath: String)`: Writes the string (interpreted as file content) to the specified path on the disk.

### Error Handling

The utility throws a `RuntimeException` if encryption fails, ensuring that you are alerted to issues during the
encryption process. Ensure to catch this exception appropriately when using this utility in a larger application.

### Security Considerations

- **Key Management**: Always follow best practices for managing your AWS KMS keys. Limit access to those keys to only
  the necessary personnel and applications.

- **Sensitive Data**: Be cautious about the data you choose to encrypt and where you save the encrypted files. Ensure
  that the output path is secure and accessible only to authorized users.

### Conclusion

The `EncryptFiles` utility provides a simple yet powerful way to encrypt data using AWS KMS and save it locally. By
modifying the utility to suit your needs, you can easily incorporate file encryption into your applications or
workflows.

# main\kotlin\com\simiacryptus\skyenet\webui\util\MarkdownUtil.kt

## MarkdownUtil Documentation

The `MarkdownUtil` object provides a utility function for rendering Markdown content into HTML. It leverages
the `flexmark-java` library to parse and render Markdown. This utility class is particularly useful when working with
web applications that need to display user-generated content or documentation in HTML format while allowing the content
to be written in Markdown for ease of use.

### Usage

To use the `MarkdownUtil` object, you need to call the `renderMarkdown` function, passing the Markdown content as a
string. Optionally, you can pass a `MutableDataSet` containing custom options for the Markdown parser and renderer. If
no options are provided, default options are used which currently only include support for Markdown tables.

#### Example

```kotlin
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import com.vladsch.flexmark.util.data.MutableDataSet

fun main() {
  val markdownContent = """
        # Heading
        This is a paragraph with **bold** text and *italic* text.
        
        | Header 1 | Header 2 |
        |----------|----------|
        | Row 1    | Data     |
        | Row 2    | Data     |
    """.trimIndent()

  // Render Markdown content with default options
  val htmlContentDefault = MarkdownUtil.renderMarkdown(markdownContent)
  println(htmlContentDefault)

  // Render Markdown content with custom options (e.g., enabling strikethrough)
  val customOptions = MutableDataSet().apply {
    // Add custom options here
  }
  val htmlContentCustom = MarkdownUtil.renderMarkdown(markdownContent, customOptions)
  println(htmlContentCustom)
}
```

### Functions

#### renderMarkdown

```kotlin
fun renderMarkdown(response: String, options: MutableDataSet = defaultOptions()): String
```

Renders the given Markdown content (`response`) into HTML. If `options` are not provided, default options are used.

##### Parameters

- `response`: The Markdown content to be rendered as a `String`.
- `options`: Optional. A `MutableDataSet` containing parser/renderer options. Defaults to the result
  of `defaultOptions()` if not provided.

##### Returns

A `String` containing the HTML representation of the given Markdown content.

#### defaultOptions

```kotlin
private fun defaultOptions(): MutableDataSet
```

Provides default options for the Markdown parser and renderer. Currently, this includes enabling support for tables via
the `TablesExtension`.

##### Returns

A `MutableDataSet` with the default options set.

### Dependencies

This utility relies on the `flexmark-java` library for parsing and rendering Markdown. Ensure you have
included `flexmark-java` and its extensions in your project dependencies.

---

This documentation provides an overview of how to use the `MarkdownUtil` object within your Kotlin projects to render
Markdown content into HTML. Adjustments and enhancements to the rendering options can be made by modifying or extending
the `defaultOptions` function or by providing a custom `MutableDataSet` when calling `renderMarkdown`.

# main\kotlin\com\simiacryptus\skyenet\webui\util\TensorflowProjector.kt

## TensorflowProjector Class Documentation

The `TensorflowProjector` class in the `com.simiacryptus.skyenet.webui.util` package is designed to facilitate the
generation of HTML content to visualize word embeddings using the TensorFlow Embedding Projector. It leverages the
OpenAI API for generating embeddings of words and stores these embeddings along with metadata to be visualized in the
TensorFlow Projector.

### Constructor Parameters

- `api`: Instance of `API` from `com.simiacryptus.jopenai` used for generating word embeddings.
- `dataStorage`: Interface of type `StorageInterface` for data storage operations.
- `sessionID`: Session identifier of type `Session`.
- `appPath`: Application path as a `String`.
- `host`: Hostname or IP address as a `String`.
- `session`: Application interface instance of type `ApplicationInterface`.
- `userId`: Optional user identifier of type `User`.

### Methods

#### `private fun toVectorMap(vararg words: String): Map<String, DoubleArray>`

Generates a map of words to their corresponding embeddings (as `DoubleArray`). This method is private and serves as a
utility for internal processing.

- **Parameters**: `words` - Vararg parameter of words to be converted into embeddings.
- **Returns**: A map where each key is a word and the value is the corresponding embedding as a `DoubleArray`.

#### `fun writeTensorflowEmbeddingProjectorHtml(vararg words: String): String`

Creates HTML content for visualizing the embeddings of the given words in the TensorFlow Embedding Projector. This
method processes the words to filter out blanks, generates embeddings, stores them along with metadata, and constructs
HTML content linking to the TensorFlow Projector with the generated data.

- **Parameters**: `words` - Vararg parameter of words to be visualized.
- **Returns**: HTML `String` that contains links to the projector configuration, vectors, metadata, and an iframe
  embedding of the TensorFlow Projector configured to visualize the generated embeddings.

### Usage Example

```kotlin
// Initialize API, storage, and other required parameters
val api = OpenAIClient(...)
val dataStorage: StorageInterface = ...
val sessionID = Session(...)
val appPath = "your/app/path"
val host = "your.host.com"
val session: ApplicationInterface = ...
val userId: User? = ...

// Create an instance of TensorflowProjector
val projector = TensorflowProjector(api, dataStorage, sessionID, appPath, host, session, userId)

// Generate HTML content for a set of words
val htmlContent = projector.writeTensorflowEmbeddingProjectorHtml("word1", "word2", "word3")

// htmlContent contains the HTML to be embedded or served to visualize the embeddings
```

### Remarks

This class demonstrates a practical integration of OpenAI's embedding API with TensorFlow's visualization tools,
abstracting complexities and providing a straightforward way to visualize word embeddings in web applications.

# main\kotlin\com\simiacryptus\skyenet\webui\util\Selenium2S3.kt

## Selenium2S3 Class Documentation

The `Selenium2S3` class is a comprehensive utility designed for web scraping, processing, and saving web content to an
S3 storage system. It leverages the Selenium WebDriver for browsing web pages and the Apache HttpAsyncClient for
fetching resources asynchronously. The class supports processing HTML and JSON content, media files, and performing link
replacements to ensure the saved content maintains its integrity when served from S3.

### Features

- **Web Scraping with Selenium**: Utilizes Selenium WebDriver for navigating and scraping web pages.
- **Asynchronous Resource Fetching**: Employs Apache HttpAsyncClient for efficient, non-blocking HTTP requests.
- **Cookie Management**: Supports setting cookies for both Selenium WebDriver and HttpAsyncClient, enabling session
  persistence across requests.
- **Content Processing and Saving**: Processes HTML and JSON content, fetches media files, and performs link
  replacements before saving to S3.
- **Thread Pool Management**: Executes tasks asynchronously using a configurable thread pool executor.

### Key Components

#### Properties

- `pool`: A `ThreadPoolExecutor` for managing asynchronous tasks.
- `cookies`: An array of servlet cookies to be used with requests.
- `driver`: A lazy-initialized `WebDriver` instance for web scraping.
- `cookieStore`: Stores cookies for use with the HttpAsyncClient.
- `httpClient`: An asynchronous HTTP client for fetching resources.
- `linkReplacements`: A map for managing link replacements within fetched content.
- `htmlPages`, `jsonPages`: Maps for storing processed HTML and JSON content.
- `links`: A list of links to be processed.

#### Methods

##### save(url: URL, currentFilename: String?, saveRoot: String)

Initiates the scraping and saving process for a given URL. It navigates to the URL using Selenium, sets cookies,
refreshes the page to ensure dynamic content is loaded, and then processes the page and any discovered links for saving.

##### process(url: URL, href: String, completionSemaphores: MutableList<Semaphore>, saveRoot: String): Boolean

Processes a single link by determining its content type and fetching the content accordingly. Supports HTML, JSON, and
various media types.

##### getHtml(href: String, htmlPages: MutableMap<String, String>, relative: String, links: MutableList<String>, saveRoot: String, semaphore: Semaphore)

Fetches HTML content asynchronously and processes it for saving.

##### getJson(href: String, jsonPages: MutableMap<String, String>, relative: String, semaphore: Semaphore)

Fetches JSON content asynchronously for processing and saving.

##### getMedia(href: String, mimeType: String, saveRoot: String, relative: String, semaphore: Semaphore)

Fetches media files asynchronously for saving to S3.

##### saveAll(saveRoot: String)

Saves all processed content to S3, including HTML, JSON, and media files.

##### setCookies(driver: WebDriver, cookies: Array<out jakarta.servlet.http.Cookie>?, domain: String?)

Sets cookies for the Selenium WebDriver instance.

#### Utility Methods

- Methods for link manipulation (`toAbsolute`, `toRelative`, `toArchivePath`) and content type
  determination (`mimeType`).
- `editPage(html: String)`: Processes an HTML page to remove unnecessary elements before saving.
- `validate(expected: String, actual: String, bytes: ByteArray)`: Validates the MIME type of fetched content.

### Usage Example

To use `Selenium2S3`, instantiate the class with optional cookies and a thread pool executor if needed. Then, call
the `save` method with the desired URL, filename, and S3 save root path. The class handles the rest, from web scraping
to processing and saving the content to S3.

```kotlin
val selenium2S3 = Selenium2S3()
selenium2S3.save(URL("http://example.com"), "examplePage.html", "myS3Bucket")
```

Ensure you have configured Selenium WebDriver and the Apache HttpAsyncClient appropriately for your environment,
including setting system properties for ChromeDriver if necessary.

### Closing Resources

The `close` method should be called when done to properly release resources, including closing the WebDriver and
HttpAsyncClient instances.

```kotlin
selenium2S3.close()
```

### Conclusion

The `Selenium2S3` class offers a powerful and flexible way to scrape, process, and save web content to S3, making it a
valuable tool for web archiving and content migration tasks.

# main\kotlin\com\simiacryptus\skyenet\webui\util\ClasspathResource.kt

## ClasspathResource Documentation

The `ClasspathResource` class is a custom implementation of the `org.eclipse.jetty.util.resource.Resource` abstract
class, specifically designed to handle resources located within the classpath. This implementation is particularly
useful for accessing resources bundled within JAR files.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.util`
- **Dependencies**: Requires the `org.eclipse.jetty` and `org.slf4j` libraries.

### Key Features

- **URL-based Resource Access**: Allows accessing resources through a given URL.
- **Classpath Integration**: Facilitates the retrieval of resources from the classpath, including those within JAR
  files.
- **Resource Existence Check**: Provides a method to check if a resource exists within the classpath.
- **InputStream Access**: Enables obtaining an `InputStream` for the resource, allowing for reading the resource's
  content.

### Constructor

```java
ClasspathResource(URL url, @Transient boolean useCaches =__defaultUseCaches)
```

- `url`: The URL of the resource to be accessed.
- `useCaches`: (Optional) A flag indicating whether to use caches. Defaults to `__defaultUseCaches` if not specified.

### Methods

#### `close()`

Closes the resource. This implementation does not require any action upon closing.

#### `exists() : Boolean`

Checks if the resource exists within the classpath.

#### `isDirectory() : Boolean`

Determines if the resource represents a directory. This is inferred from the URL ending with a slash (`/`).

#### `lastModified() : Long`

Returns the last modified timestamp of the resource. Always returns `-1` as this feature is not supported.

#### `length() : Long`

Calculates the length of the resource. Returns the size in bytes if available, otherwise `-1`.

#### `getURI() : URI`

Retrieves the URI of the resource. Throws a `RuntimeException` if the URL cannot be converted to a URI.

#### `getFile() : File?`

Returns a `File` object for the resource. Always returns `null` as this operation is not supported.

#### `getName() : String`

Gets the name of the resource, which is its URL in external form.

#### `getInputStream() : InputStream?`

Obtains an `InputStream` for reading the resource's content.

#### `getReadableByteChannel() : ReadableByteChannel?`

Returns a `ReadableByteChannel` for the resource. Always returns `null` as this operation is not supported.

#### `delete() : Boolean`

Deletes the resource. Always throws a `SecurityException` as deletion is not supported.

#### `renameTo(Resource dest) : Boolean`

Renames the resource. Always throws a `SecurityException` as renaming is not supported.

#### `list() : Array<String>?`

Lists the names of resources contained within this resource. Always returns `null` as this operation is not supported.

#### `addPath(String path) : Resource`

Creates a new `ClasspathResource` instance representing a resource at the given path relative to the current resource.

#### `isContainedIn(Resource containingResource) : Boolean`

Checks if the resource is contained within another resource. Always returns `false` as this operation is not supported.

### Overridden Methods

- `toString()`
- `hashCode()`
- `equals(Object other)`

### Usage Example

```java
URL resourceUrl = getClass().getResource("/example/resource.txt");
ClasspathResource resource = new ClasspathResource(resourceUrl);
if(resource.

exists()){
        try(
InputStream inputStream = resource.getInputStream()){
        // Process the resource
        }catch(
IOException e){
        // Handle exception
        }
        }
```

### Logging

The class utilizes SLF4J for logging, specifically to log instances where resources are not found.

### Limitations

- Does not support modification operations such as `delete()` or `renameTo()`.
- Does not provide direct support for obtaining a `File` or `ReadableByteChannel` representation of the resource.
- Length and last modification time are not reliably supported for all resource types.

# main\kotlin\com\simiacryptus\skyenet\webui\util\OpenAPI.kt

## Developer Documentation: OpenAPI Model and Serialization

This document provides an overview and usage guide for the OpenAPI model classes and serialization/deserialization
functions defined in the Kotlin package `com.simiacryptus.skyenet.webui.util`. These classes and functions are designed
to facilitate the creation, manipulation, and conversion of OpenAPI specifications within Kotlin applications.

### Overview

The OpenAPI model classes represent the structure of an OpenAPI specification, which is a standard, language-agnostic
interface to RESTful APIs. The model includes classes for the OpenAPI root document, API metadata, paths, operations,
responses, and various components that can be reused throughout the specification.

#### Key Classes

- `OpenAPI`: Represents the root document of an OpenAPI specification.
- `Info`: Contains metadata about the API, such as title, version, and contact information.
- `PathItem`: Represents an API endpoint and its available operations (GET, POST, etc.).
- `Operation`: Describes a single API operation on a path.
- `Response`: Represents a response from an API operation.
- `Components`: Holds reusable components for the OpenAPI specification, like schemas and parameters.
- `Schema`, `Parameter`, `RequestBody`, etc.: Represent specific components that can be reused in the API specification.

#### Serialization and Deserialization

Two utility functions are provided for converting between OpenAPI model instances and their JSON representations:

- `serializeOpenApiSpec(openApi: OpenAPI): String`: Converts an `OpenAPI` object into a JSON string.
- `deserializeOpenApiSpec(json: String): OpenAPI`: Parses a JSON string to produce an `OpenAPI` object.

### Usage

#### Creating an OpenAPI Specification

To create an OpenAPI specification, instantiate `OpenAPI` and related classes as needed, filling in properties to
describe your API:

```kotlin
val apiSpec = OpenAPI(
  info = Info(
    title = "Example API",
    version = "1.0.0",
    description = "This is a sample API"
  ),
  paths = mapOf(
    "/example" to PathItem(
      get = Operation(
        summary = "Get example",
        responses = mapOf(
          "200" to Response(description = "Success")
        )
      )
    )
  )
)
```

#### Merging OpenAPI Specifications

OpenAPI objects can be merged together, allowing for modular construction of API specifications:

```kotlin
val baseSpec = OpenAPI(...)
val extensionSpec = OpenAPI(...)
val mergedSpec = baseSpec.merge(extensionSpec)
```

#### Serialization

To convert an `OpenAPI` instance to a JSON string for storage or transmission:

```kotlin
val jsonSpec = serializeOpenApiSpec(apiSpec)
```

#### Deserialization

To load an OpenAPI specification from a JSON string:

```kotlin
val apiSpec = deserializeOpenApiSpec(jsonSpec)
```

### Conclusion

The OpenAPI model classes and serialization functions provide a Kotlin-friendly way to work with OpenAPI specifications.
By leveraging these tools, developers can programmatically create, modify, and share API definitions in a structured and
standardized format.

# main\resources\application\chat.js

## WebSocket Communication Module Documentation

This module provides a set of functions designed to facilitate WebSocket communication within a web application. It
encompasses establishing a WebSocket connection, handling session IDs, sending messages through the WebSocket, and
managing UI elements based on the connection status.

### Functions Overview

- **getSessionId()**: Retrieves or generates a session ID for the WebSocket connection.
- **send(message)**: Sends a message through the WebSocket.
- **connect(sessionId, customReceiveFunction)**: Establishes a WebSocket connection using a session ID and sets up event
  listeners.
- **showDisconnectedOverlay(show)**: Toggles UI elements based on the WebSocket connection status.

#### getSessionId()

This function checks if the current URL contains a hash (session ID). If not present, it requests a new session ID from
the server, updates the URL with the received session ID, and initiates a WebSocket connection using that ID. If a
session ID is already present in the URL, it returns the ID.

##### Usage

```javascript
const sessionId = getSessionId();
```

#### send(message)

Sends a specified message through the WebSocket. It first checks if the WebSocket connection is open; if not, it throws
an error.

##### Parameters

- **message**: The message to be sent through the WebSocket.

##### Usage

```javascript
send("Hello, World!");
```

#### connect(sessionId, customReceiveFunction)

Initiates a WebSocket connection with the server using a provided session ID. It constructs the WebSocket URL
dynamically based on the current page location and the session ID. This function also sets up event listeners
for `open`, `message`, `close`, and `error` events on the WebSocket. Optionally, a custom function can be provided to
handle incoming messages (`customReceiveFunction`).

##### Parameters

- **sessionId** (String): The session ID to be used for the WebSocket connection.
- **customReceiveFunction** (Function) (optional): A custom function to handle incoming WebSocket messages. If not
  provided, a default handler (`onWebSocketText`) is used.

##### Usage

```javascript
connect(sessionId, (event) => {
    console.log("Received message:", event.data);
});
```

#### showDisconnectedOverlay(show)

Controls the visibility of UI elements (e.g., overlays or disabled controls) based on the WebSocket connection status.
It is used internally to disable or enable UI controls when the WebSocket connection is closed or opened, respectively.

##### Parameters

- **show** (Boolean): Determines whether to show or hide the disconnected overlay. `true` to show (or disable
  controls), `false` to hide (or enable controls).

##### Usage

```javascript
// To disable controls
showDisconnectedOverlay(true);

// To enable controls
showDisconnectedOverlay(false);
```

### Example Workflow

1. **Start**: Call `getSessionId()` to retrieve or generate a session ID and establish a WebSocket connection.
2. **Send Messages**: Use `send(message)` to send messages through the WebSocket.
3. **Receive Messages**: Handle incoming messages using the custom function provided to `connect(...)`.
4. **Connection Status**: Use `showDisconnectedOverlay(show)` to manage UI elements based on the connection status.

This module provides a comprehensive solution for managing WebSocket communications, including session management,
message sending and receiving, and UI control based on connection status.

# main\resources\application\forest.scss

#### Developer Documentation: Importing Shared Styles in SASS

This document provides a guide on how to import shared styles into your SASS project, using specific examples from a
project structure. The focus is on importing styles from a `forest` theme and a main stylesheet located within a shared
directory.

##### Overview

To maintain consistency and reusability across a large project, it's common to have shared styles, themes, or utility
classes. These shared resources can be imported into individual SASS files to apply a cohesive look and feel or to reuse
common styles without duplication.

##### File Structure

For the purpose of this documentation, consider the following simplified project structure:

```
project-directory/
│
├── styles/
│   └── your-stylesheet.scss
│
└── shared/
    ├── schemes/
    │   └── forest.scss
    └── main.scss
```

- `forest.scss` contains specific theme styles for a forest-themed UI.
- `main.scss` includes global styles, variables, mixins, or functions used across the project.

##### Importing Shared Styles

To import these shared styles into your SASS file (e.g., `your-stylesheet.scss`), follow the steps below:

1. **Navigate to Your Stylesheet**: Open the SASS file where you want to import the shared styles. This file is
   typically part of your project's specific module or component.

2. **Use the `@import` Directive**: At the top of your file, use the `@import` directive to include the shared styles.
   The path provided should be relative to the location of the file you're editing.

   Example imports:
    ```scss
    @import '../shared/schemes/forest';
    @import '../shared/main';
    ```

    - The first line imports the `forest` theme styles.
    - The second line imports the global styles defined in `main.scss`.

##### Relative Paths

The `@import` directive requires a path to the file you want to import. In our examples, the `..` notation moves up one
directory level from the current file's location, allowing access to the `shared` directory. Adjust the path as
necessary based on your project's directory structure.

##### Best Practices

- **Order of Imports**: Import global styles (`main.scss`) before theme-specific styles (`forest.scss`) to ensure that
  theme styles can override global defaults if needed.
- **Minimal Imports**: Only import the necessary files to keep the compiled CSS size minimal. Avoid importing entire
  libraries if you only need a small subset of their functionality.
- **Use of Variables and Mixins**: Leverage variables and mixins defined in `main.scss` throughout your stylesheets to
  maintain consistency and simplify maintenance.

##### Conclusion

Importing shared styles in SASS is a straightforward but powerful feature to organize and maintain styles across a large
project. By following the outlined steps and best practices, you can ensure a consistent and efficient styling process.

# main\resources\application\favicon.svg

#### Developer Documentation for SVG Graphic

This SVG graphic is a complex illustration composed of multiple paths, styles, and colors. It is designed to be used in
web applications, digital media, or any platform that supports SVG format. Below is a detailed documentation intended
for developers who wish to integrate or modify this SVG graphic in their projects.

##### Overview

The SVG graphic is an intricate design, primarily composed of various shapes and colors, making it suitable for a wide
range of applications, from website decorations to digital art projects. It utilizes a combination of filled paths and
strokes to create a detailed illustration.

##### File Information

- **File Format:** SVG (Scalable Vector Graphics)
- **Encoding:** UTF-8
- **SVG Version:** 1.1

##### Implementation Guide

1. **Integration:**

   To integrate the SVG graphic into an HTML document, use the following tag:

   ```html
   <img src="path/to/svgfile.svg" alt="Description of the Graphic">
   ```

   Alternatively, you can directly embed the SVG code into your HTML file to further manipulate the SVG properties:

   ```html
   <!-- Paste the SVG code here -->
   ```

2. **Styling:**

   The SVG graphic comes with predefined styles within the `<style>` tag. If you wish to customize the appearance, you
   can override these styles in your CSS file or within the `<style>` tag itself.

   Example of overriding styles:

   ```css
   .st0 { fill: #newColor; }
   ```

3. **Scaling:**

   Being a vector graphic, it can be scaled to any size without loss of quality. To resize the SVG, you can modify
   the `width` and `height` attributes in the `<svg>` tag or through CSS.

   Example:

   ```html
   <svg version="1.1" width="200px" height="200px">
   ```

   Or using CSS:

   ```css
   svg { width: 200px; height: 200px; }
   ```

4. **Animation:**

   SVG elements can be animated using CSS animations or SMIL (Synchronized Multimedia Integration Language). For simple
   animations like hover effects, CSS is recommended.

   Example of a CSS hover effect:

   ```css
   .st0:hover { fill: #anotherColor; }
   ```

5. **Accessibility:**

   Ensure accessibility by providing descriptive titles and descriptions using the `<title>` and `<desc>` tags within
   the SVG. Additionally, when using the `<img>` tag to embed the SVG, always include an `alt` attribute.

   Example:

   ```html
   <svg>
     <title>Descriptive Title</title>
     <desc>Description of the graphic.</desc>
     ...
   </svg>
   ```

##### Optimization

For optimal performance, especially for web use, consider minimizing the SVG code. Tools like SVGO can help reduce file
size by removing unnecessary metadata, comments, and other redundant information without affecting the visual output.

##### Browser Support

SVG is supported across all modern web browsers, including Chrome, Firefox, Safari, Edge, and Internet Explorer 9 and
above. However, complex SVG graphics with filters and animations might have varying performance across different devices
and browsers. Always test your SVG graphics across multiple platforms for compatibility.

# main\resources\application\main.js

## Developer Documentation

This documentation provides an overview and detailed explanation of the JavaScript functions and event listeners used in
the web application. The code facilitates the dynamic interaction within the web application, including handling modals,
fetching data, WebSocket communication, toggling verbose mode, and managing user input.

### Functions Overview

#### showModal(endpoint, useSession = true)

- **Purpose**: Opens a modal window and fetches content from the specified endpoint. Optionally appends a session ID to
  the request.
- **Parameters**:
    - `endpoint` (String): The URL from which to fetch data for the modal content.
    - `useSession` (Boolean): Flag to determine if the session ID should be appended to the request (default is `true`).

#### closeModal()

- **Purpose**: Closes the currently open modal window.

#### async fetchData(endpoint, useSession = true)

- **Purpose**: Fetches data from a specified endpoint and displays it within the modal content area. Additionally,
  highlights syntax using Prism.js.
- **Parameters**:
    - `endpoint` (String): The URL from which to fetch data.
    - `useSession` (Boolean): Flag to determine if the session ID should be appended to the request (default is `true`).

#### onWebSocketText(event)

- **Purpose**: Handles incoming WebSocket text messages, updates the message container, and applies logic based
  on `singleInput` and `stickyInput` flags.
- **Parameters**:
    - `event` (Event): The WebSocket message event object.

#### toggleVerbose()

- **Purpose**: Toggles the visibility of elements with the `verbose` class based on their current state.

#### refreshReplyForms()

- **Purpose**: Adds event listeners to reply input fields to handle the submission process when the Enter key is
  pressed.

#### refreshVerbose()

- **Purpose**: Refreshes the state of verbose elements based on the toggle state.

### Event Listeners

The application sets up various event listeners upon DOM content load to handle user interactions such as clicks, form
submissions, and input field adjustments. Key event listeners include:

- **Theme Switching**: Listeners on theme selection buttons to dynamically change the application's theme.
- **Modal Triggers**: Buttons and links that open modals for different functionalities like settings, usage info, and
  more.
- **Close Modal**: A listener on the modal close button and a click-outside-modal listener to close the modal.
- **Input Field Auto-Resize**: Adjusts the height of the message input field based on the content.
- **WebSocket and Fetch**: Initialization of WebSocket connection and fetch requests for user and application data.

### Dynamic Content Management

- **WebSocket Message Handling**: The application listens for WebSocket messages and updates the UI accordingly,
  including handling new messages, updates, and command responses.
- **Fetch Data for Modals**: Fetches and displays data in modals dynamically based on the specified endpoint.

### User Interaction

- **Verbose Toggle**: Allows users to show or hide verbose information.
- **Reply Forms**: Enhances reply forms with keyboard shortcuts for submission.
- **Theme Selection**: Users can select different themes which are then applied to the application.

### Utility Functions

- **getSessionId()**: Retrieves the session ID from a predetermined source (not shown in the provided code).
- **send(message)**: Sends a message through a WebSocket connection (not shown in the provided code).
- **connect(sessionId, callback)**: Establishes a WebSocket connection using the provided session ID and callback
  function for message handling (not shown in the provided code).

This documentation serves as a guide for developers to understand and work with the provided JavaScript code,
facilitating the enhancement or maintenance of the web application's interactive features.

# main\resources\application\index.html

## WebSocket Client - Developer Documentation

This document provides a comprehensive guide for developers working with the WebSocket Client web application. The
application is designed to facilitate real-time communication using WebSockets and includes a user interface for sending
and receiving messages, managing sessions, and customizing themes.

### Overview

The WebSocket Client application is structured as a single HTML document (`index.html`) that includes references to
external CSS for styling and JavaScript files for functionality. It leverages the Prism library for syntax highlighting
and provides a modular approach for managing chat sessions and UI themes.

#### Key Features:

- Real-time messaging with WebSocket protocol.
- Dynamic theme switching for user interface.
- Session management with options for settings, files, usage, threads, sharing, cancellation, deletion, and verbose
  output.
- User authentication and settings management.
- Syntax highlighting using Prism library.

### File Structure

- `index.html` - The main HTML document containing the structure of the web application.
- `main.css` - Main stylesheet for custom styling.
- `main.js` - Main JavaScript file containing the core functionality.
- `chat.js` - JavaScript file dedicated to handling chat functionalities.
- External libraries and stylesheets for Prism and responsive design.

### HTML Structure

#### Head Section

- **Meta Tags**: Includes charset, viewport, and favicon for basic HTML setup.
- **External Stylesheets**: Links to Prism themes and plugins for syntax highlighting and additional styling for
  responsive design.
- **Local Stylesheets**: The main stylesheet (`main.css`) for custom styling.
- **JavaScript Files**: Includes both local (`main.js`, `chat.js`) and external Prism JavaScript files for functionality
  and syntax highlighting.

#### Body Content

- **Toolbar**: Contains navigation links and dropdown menus for accessing different sections of the application such as
  Home, App, Session, Themes, and About.
- **Namebar**: Displays user login status and provides access to user-specific settings and logout functionality.
- **Session**: The main chat interface where users can type and send messages. Also includes a display area for incoming
  messages.
- **Modal**: A generic modal component for displaying dynamic content such as forms or information dialogs.
- **Footer**: Contains links to external resources or acknowledgments.

### JavaScript Functionality

#### Main Features

- **WebSocket Connection**: Establishes and manages a WebSocket connection for real-time messaging.
- **UI Interactions**: Handles user interactions for sending messages, switching themes, and navigating through the
  application.
- **Session Management**: Allows users to manage their chat sessions with options like settings adjustment, file
  management, and session deletion.
- **Theme Switching**: Dynamically changes the UI theme based on user selection.
- **Prism Integration**: Utilizes Prism for syntax highlighting within chat messages or code snippets.

#### Key Functions

- Initialization of WebSocket connection and event listeners for UI elements.
- Functions to send and receive WebSocket messages.
- Event handlers for theme switching and modal interactions.
- Utility functions for session management and user settings.

### External Libraries

- **Prism**: Used for syntax highlighting. The application includes various Prism plugins for additional functionality
  like line numbers, match braces, and toolbar.
- **Responsive Design**: Meta viewport tag and responsive CSS ensure the application is accessible across different
  devices and screen sizes.

### Conclusion

This documentation provides an overview and detailed explanation of the WebSocket Client web application. Developers can
use this guide to understand the application's structure, functionality, and integration points for further development
or customization.

# main\resources\application\night.scss

### Developer Documentation: Importing Shared Styles

#### Overview

In order to maintain a consistent look and feel across various parts of your application or multiple applications, it's
common to share style definitions. This approach ensures that themes, colors, and common styles are centralized, making
them easier to update and manage. The code snippet provided demonstrates how to import shared style schemes and main
style definitions into your project.

#### Code Snippet Explanation

```scss
@import '../shared/schemes/night';
@import '../shared/main';
```

This snippet is written in SCSS, a preprocessor scripting language that is interpreted or compiled into Cascading Style
Sheets (CSS). SCSS files use the extension `.scss`.

##### Import Statements

- `@import '../shared/schemes/night';`
    - This line imports the "night" theme style definitions from a relative path. The `'../'` indicates that the file is
      located one directory up from the current directory, then inside the `shared/schemes` directory. This allows you
      to apply the night theme's color schemes, fonts, and other style properties to your current stylesheet.

- `@import '../shared/main';`
    - This line imports the main stylesheet from a relative path, similar to the first import statement. The `main` file
      is likely to contain global styles that are applicable across different parts of the application, such as reset
      styles, base typography, and utility classes.

#### Best Practices

- **Organize Shared Styles:** Keep shared styles, such as themes or utility classes, in a separate directory. This makes
  them easy to locate and import as needed.
- **Use Descriptive Names:** Name your style files descriptively to make it clear what they contain. For
  example, `night.scss` for a night theme.
- **Minimize Dependencies:** While sharing styles is beneficial, minimize the number of dependencies each file has to
  keep the styles modular and easy to maintain.
- **Use Version Control:** Keep your shared styles in a version control system. This allows you to track changes and
  roll back if necessary.

#### Conclusion

Importing shared styles is a powerful way to ensure consistency and reduce redundancy in your web development projects.
By organizing your styles into shared files and importing them where needed, you can streamline your development process
and make your styles easier to manage and update.

# main\resources\application\main.scss

#### Developer Documentation for Stylesheet Imports

This documentation provides an overview and guidance for developers on how to import shared styles into their projects
using the provided code snippet. The code demonstrates the importation of stylesheet files from a shared directory,
specifically targeting the `normal` scheme and main shared styles. This approach is essential for maintaining
consistency across different parts of the application and promoting code reusability.

##### Code Snippet:

```scss
@import '../shared/schemes/normal';
@import '../shared/main';
```

##### Overview

The provided code snippet is written in SCSS, a preprocessor scripting language that is interpreted or compiled into
Cascading Style Sheets (CSS). SCSS allows the use of variables, nested rules, mixins, functions, and more, making it a
powerful tool for writing efficient and maintainable stylesheets.

##### Importing Stylesheets

The `@import` rule in SCSS is used to import external stylesheets into the current stylesheet. This rule enables
developers to modularize their CSS by dividing it into smaller, maintainable pieces.

###### Syntax:

```scss
@import 'path/to/stylesheet';
```

- The path to the stylesheet can be relative or absolute.
- The file extension `.scss` is optional and can be omitted.

##### Specifics of the Provided Code

1. **Normal Scheme Stylesheet:**

    - `@import '../shared/schemes/normal';`
    - This line imports the stylesheet for the "normal" theme or scheme from the shared schemes directory. It's useful
      for applying a consistent look and feel across various components or pages that require this specific styling
      scheme.

2. **Main Shared Stylesheet:**

    - `@import '../shared/main';`
    - This line imports the main shared stylesheet, which likely contains common styles, variables, mixins, or functions
      that are used across the entire project. This ensures that all components or pages have access to these
      foundational styles, promoting uniformity and reducing code duplication.

##### Best Practices

- **Modularization:** Keep your stylesheets modular by separating them into logical groups (e.g., base, components,
  layouts, themes). This makes it easier to manage and understand the styles in your project.
- **Use Descriptive Names:** Choose clear and descriptive names for your stylesheet files to make it easier for other
  developers to understand what each file contains at a glance.
- **Relative Paths:** Use relative paths for importing styles to ensure portability and flexibility of your project
  structure.

##### Conclusion

Importing shared styles using SCSS is a powerful technique for managing styles across a project. By following the
provided example and adhering to best practices, developers can ensure a consistent and maintainable styling approach
that enhances the scalability and readability of their projects.

# main\resources\application\pony.scss

The provided code snippet demonstrates how to import resources from other files into the current stylesheet file. This
is a common practice in web development where modular CSS is used to organize and maintain styles across a large
project. Below is a detailed explanation of the code snippet provided:

```scss
@import '../shared/schemes/pony';
@import '../shared/main';
```

#### Overview

This code snippet is written in SCSS, a preprocessor scripting language that is interpreted or compiled into Cascading
Style Sheets (CSS). SCSS provides mechanisms like variables, nesting, and imports which help in writing more
maintainable and concise CSS code.

#### Import Directives

The `@import` directive is used to include the content of one file into another. This is particularly useful for
splitting your CSS into smaller, more manageable segments. When the SCSS file is processed, it takes the file(s) you’ve
included with `@import` and combines them with the file you’re working on to produce a single CSS output.

##### Syntax

```scss
@import 'path/to/file';
```

- The path to the file can be relative or absolute.
- The file extension `.scss` is optional and can be omitted.

##### Examples

1. Importing a Scheme File:

```scss
@import '../shared/schemes/pony';
```

This line imports a scheme file named `pony.scss` from a directory named `schemes` within a `shared` directory, which is
a sibling of the current directory (indicated by `..`).

2. Importing a Main Stylesheet:

```scss
@import '../shared/main';
```

This imports a main stylesheet named `main.scss` from a `shared` directory that is a sibling of the current directory.

#### Best Practices

- **Modularization:** Use `@import` to break down your styles into smaller, more manageable pieces. This improves
  readability and maintainability.
- **Order of Import:** The order in which you import files matters. Files are imported and processed in the order they
  appear. This means styles in later files can override those in earlier files.
- **Avoid Excessive Nesting:** While splitting your CSS into smaller files, be mindful of creating too deep a directory
  structure. It can make managing and locating files more challenging.
- **Limitation:** Each `@import` statement results in an HTTP request (in plain CSS). However, in SCSS, all imports are
  combined into a single CSS file during the compilation process, mitigating this issue.

#### Conclusion

The `@import` directive in SCSS is a powerful feature for organizing and managing stylesheets in a web development
project. By separating styles into different files and importing them as needed, developers can maintain cleaner and
more modular codebases.

# main\resources\shared\schemes\_night.scss

## Nighttime Theme Color Scheme Documentation

This documentation provides an overview of the color variables defined for the Nighttime theme. These variables are
designed to facilitate the development of web interfaces by providing a consistent and visually appealing dark theme.
The color scheme is composed of base colors, derived colors, and specific use-case colors for elements such as buttons,
messages, and modals.

### Base Colors

The foundation of the Nighttime theme, these colors are intended for use throughout the interface to maintain
consistency.

- `$base-night`: `#141414` - A very dark gray, almost black, used primarily for backgrounds.
- `$base-midnight-blue`: `#2c3e50` - A dark blue, suitable for headers and footers.
- `$base-twilight-blue`: `#34495e` - A slightly lighter shade of dark blue for accents.
- `$base-dark-grey`: `#7f8c8d` - A muted grey, optimized for readable text that isn't too harsh on the eyes.
- `$base-moonlit-aqua`: `#1abc9c` - A muted aqua for subtle highlights and accents.
- `$base-starlight-yellow`: `#f1c40f` - A soft yellow for important buttons or icons.
- `$base-cloudy-grey`: `#bdc3c7` - A lighter grey for background contrasts.
- `$base-meteorite-border`: `#95a5a6` - A medium grey for borders and lines.

### Derived Colors

Derived colors are calculated based on the base colors, intended for specific UI elements to ensure visual harmony
across the interface.

- Background, text, and element colors for secondary interfaces, application lists, dropdowns, messages, and more.
- Highlight colors for interactive elements like buttons, links, and form elements.
- Error, success, and information feedback colors.

### Specific Use-Case Colors

#### Buttons

- Background, text, and hover states for buttons are defined to ensure they stand out and provide feedback to users.

#### Messages and Modals

- Background and text colors for informational, success, and error messages ensure readability and context.
- Modals have specified background, text colors, and shadow effects to focus user attention.

### Styling Properties

- `$border-radius`, `$box-shadow`, and `$transition-speed` are utility variables to maintain consistent styling for
  elements like buttons, cards, and modals.

### Implementation Example

To use these colors in a Sass stylesheet, you can directly refer to these variables. For example, to style a button, you
might write:

```scss
.button {
  background-color: $button-bg-color;
  color: $button-text-color;
  border-radius: $button-border-radius;
  box-shadow: $button-box-shadow;

  &:hover {
    background-color: $button-hover-bg-color;
    color: $button-hover-text-color;
  }
}
```

This ensures that your button adheres to the Nighttime theme's color scheme and interactive feedback guidelines.

### Conclusion

The Nighttime theme's color scheme is designed to offer a visually appealing, consistent experience for dark mode
interfaces. By adhering to the defined color variables, developers can ensure a cohesive look and feel across their
applications.

# main\resources\shared\schemes\_forest.scss

## Forest Canopy Theme - Developer Documentation

The Forest Canopy theme is inspired by the rich and vibrant colors of a forest landscape. It's designed to provide a
natural, calming, and cohesive visual experience for web applications. This document outlines the theme's color palette,
typography, and UI components styling to assist developers in implementing the theme in their projects.

### Theme Color Palette

The theme's color palette draws from the various elements found in a forest canopy, from the forest floor to the
sunbeams that pierce through the leaves. Each color is carefully chosen to represent an aspect of the forest and to work
harmoniously with the rest of the palette.

#### Base Colors

- **Forest Floor (`$base-forest-floor`)**: #2E342D, a rich dark earth tone used for main backgrounds.
- **Tree Bark (`$base-tree-bark`)**: #8B5A2B, a warm brown for headers and footers.
- **Canopy Leaf (`$base-canopy-leaf`)**: #3A5F0B, a deep green for accents and active elements.
- **Sunbeam (`$base-sunbeam`)**: #F0E68C, a light khaki for highlights and special features.
- **Dew (`$base-dew`)**: #C5E3BF, a pale green for subtle background contrasts.
- **Underbrush (`$base-underbrush`)**: #2D3C0F, a dark olive green for secondary backgrounds and buttons.
- **Fern (`$base-fern`)**: #71A267, a muted medium green for borders and separators.
- **Acorn (`$base-acorn`)**: #C68A18, a nutty brown for text and icons.

#### Derived Colors

Derived colors utilize the base colors to create a cohesive theme across various UI components, ensuring consistency and
enhancing the user interface's visual appeal.

- **Secondary Background Color (`$secondary-bg-color`)**: Uses `$base-forest-floor`.
- **Applist Header Background (`$applist-header-bg`)**: Uses `$base-tree-bark`.
- **Link Color (`$link-color`)**: Uses `$base-dew`, with a lighter version for hover states.
- **Error Color (`$error-color`)**: Uses `$base-sunbeam` for error messages and indicators.
- **Success Color (`$success-color`)**: Uses `$base-canopy-leaf` for success messages and indicators.

### Typography

The theme specifies typography settings to ensure text is readable and aesthetically pleasing.

- **Primary Font Family (`$font-family-primary`)**: `fantasy`, for headings and emphasis.
- **Secondary Font Family (`$font-family-secondary`)**: `sans-serif`, for body text and UI components.
- **Base Font Size (`$font-size-base`)**: `1em`, with variations for large (`$font-size-large`) and
  small (`$font-size-small`) text.
- **Font Weights**: Normal (`$font-weight-normal`) and bold (`$font-weight-bold`).

### UI Components

Styling for UI components such as buttons, forms, and modals ensures they are consistent with the theme's overall
design.

#### Buttons

- Background, text, and hover colors are defined to ensure buttons are visually distinct and interactive.

#### Forms

- Input fields have specific border, background, and text colors, with focus states to improve usability.

#### Messages and Modals

- Information, success, and error messages have distinct background and text colors to distinguish them.
- Modals have a dedicated background color, text color, and shadow for consistency and readability.

### Usage

To use the Forest Canopy theme in your project, import the theme's stylesheet and apply the variables to your CSS
classes and IDs as needed. The theme is designed to be flexible, allowing you to mix and match colors and typography
settings to fit your project's specific requirements.

For further customization or to create variations of this theme, you can adjust the base and derived colors, typography
settings, and component styles as needed.

### Conclusion

The Forest Canopy theme provides a natural and cohesive visual experience for web applications. By following this
documentation, developers can easily implement the theme in their projects, ensuring a consistent and engaging user
interface.

# main\resources\shared\schemes\_normal.scss

## Developer Documentation: Styling Variables

This document outlines the set of SCSS variables defined for a web application's styling. These variables are
categorized into typography, base colors, derived colors, buttons, forms, and messages & modals. Utilizing these
variables ensures a consistent look and feel across the application while simplifying the maintenance and updating of
the styles.

### Typography Variables

- **`$font-family-primary`**: The primary font stack used for most text. Defaults to 'Helvetica Neue', Helvetica, Arial,
  sans-serif.
- **`$font-family-secondary`**: The secondary font stack, used where differentiation is needed. Defaults to 'Segoe UI',
  Tahoma, Geneva, Verdana, sans-serif.
- **`$font-size-base`**: The base font size for the application. Set at 1em.
- **`$font-size-large`**: A larger font size for headings or emphasized text. Set at 1.5px.
- **`$font-size-small`**: A smaller font size for less important or supplementary text. Set at 0.8px.
- **`$font-weight-normal`**: The normal font weight. Set at 400.
- **`$font-weight-bold`**: The bold font weight for emphasized text. Set at 700.

### Base Colors

Defines the palette of colors used throughout the application.

- **`$base-white`**: Pure white color, used for backgrounds and text in some cases.
- **`$base-dark`**: A dark color, primarily for text to ensure readability.
- **`$base-light-blue`**, **`$base-grey`**, **`$base-dark-blue`**, **`$base-light-grey`**, **`$base-blue`**, *
  *`$base-green`**, **`$base-red`**, **`$base-orange`**, **`$base-dark-grey`**: Various colors used throughout the
  application for backgrounds, text, buttons, links, etc., each serving specific design purposes as indicated by their
  names.

### Derived Colors

Variables derived from the base colors for specific UI elements, ensuring consistency and theme coherence.

- **`$secondary-bg-color`**, **`$secondary-text-color`**: Colors used for secondary backgrounds and text.
- **`$applist-header-bg`**, **`$applist-header-text`**, **`$applist-link-color`**, etc.: Specific UI elements like app
  lists, dropdowns, modals, and messages have their colors defined here, ensuring a unified look.
- **`$border-color`**, **`$border-radius`**, **`$box-shadow`**: Basic styling for borders and shadows.
- **`$link-color`**, **`$link-hover-color`**: Colors for links and their hover states.
- **`$modal-overlay-color`**: The color used for modal overlays.

### Buttons

- **`$button-bg-color`**, **`$button-text-color`**, **`$button-hover-bg-color`**, **`$button-hover-text-color`**:Defines
  the background and text colors for buttons, as well as their hover states.
- **`$button-border-radius`**, **`$button-box-shadow`**: Styling properties for button borders and shadows.

### Forms

Variables specific to form elements, ensuring inputs and forms have a consistent style.

- **`$input-border-color`**, **`$input-border-focus-color`**: Border colors for input fields, including the focus state.
- **`$input-bg-color`**, **`$input-text-color`**: Background and text colors for inputs.
- **`$input-padding`**: Padding within input fields for better text alignment and readability.

### Messages and Modals

Styling for informational messages, success or error notifications, and modal dialogs.

- **`$message-info-bg-color`**, **`$message-info-text-color`**, **`$message-success-bg-color`**, etc.: Background and
  text colors for different types of messages.
- **`$modal-content-bg-color`**, **`$modal-content-text-color`**, **`$modal-content-shadow`**: Defines the styling for
  modal content, ensuring it stands out appropriately against the overlay.

By leveraging these predefined SCSS variables, developers can maintain a consistent styling approach throughout the
application, making the UI design more unified and easier to manage.

# main\resources\shared\schemes\_pony.scss

## Developer Documentation: Styling Variables

This document outlines the styling variables defined for a web project. These variables are designed to ensure a
consistent theme across the application, making it easier to manage and update styles. The variables are categorized
into typography, base colors, derived colors, buttons, forms, and messages and modals.

### Typography Variables

- `$font-family-primary`: The primary font family used across the application. Set to `cursive`.
- `$font-family-secondary`: The secondary font family, also set to `cursive`.
- `$font-size-base`: The base font size, set to `1.2em`.
- `$font-size-large`: The large font size, set to `1.6px`.
- `$font-size-small`: The small font size, set to `0.9px`.
- `$font-weight-normal`: The normal font weight, set to `400`.
- `$font-weight-bold`: The bold font weight, set to `700`.

### Base Colors

These are the foundational colors used throughout the application:

- `$base-bubblegum`: A playful pink (`#ff77a9`) for primary elements.
- `$base-light-blue`: A vibrant blue (`#3471FF`) for links and buttons.
- `$base-candy-red`: A soft red (`#ff6b6b`) for calls to action and highlights.
- `$base-vanilla`: A soft off-white (`#f3f3f3`) for backgrounds, ensuring legibility.
- `$base-charcoal`: A dark grey (`#454545`) for text, softer than black for better reading.
- `$base-pastel-pink`, `$base-pastel-yellow`, `$base-pastel-red`, and `$base-soft-grey`: Used for subtle backgrounds,
  highlights, error states or warnings, and less important elements, respectively.

### Derived Colors

These are colors derived from the base colors, used specifically for certain UI elements:

- Background colors, text colors, and specific UI component colors like `$applist-header-bg`, `$secondary-bg-color`,
  and `$primary-bg-color`.
- Interaction states such as `$button-hover-bg-color`, `$link-hover-color`, and `$new-session-link-hover-bg`.
- Borders, shadows, and radius settings like `$border-color`, `$box-shadow`, and `$border-radius`.

### Buttons

- Button-specific variables including background, text, hover states, border radius, and box shadow settings.

### Forms

- Variables related to form elements, including border color, focus state, background color, text color, and padding.

### Messages and Modals

- Variables for styling information, success, and error messages, as well as modal dialog boxes. This includes
  background and text colors, as well as modal content shadow.

### Usage

To use these variables, simply include the SCSS file containing the variables at the beginning of your stylesheet. Then,
you can reference these variables throughout your stylesheets to maintain consistency and make future updates or theme
changes easier.

For example, to set the background color of a button, you would use:

```scss
.button {
  background-color: $button-bg-color;
  color: $button-text-color;
  border-radius: $button-border-radius;
  box-shadow: $button-box-shadow;
}
```

By adhering to these variables, developers can ensure a cohesive look and feel across the application while simplifying
the maintenance of the project's styles.

# main\resources\welcome\forest.scss

#### Developer Documentation for App Type Styles

This documentation outlines the CSS styles applied to elements with the class `.app-type` within our application. The
purpose of these styles is to create a consistent appearance for specific text elements that are meant to stand out with
a distinctive look, often used to label or categorize content within the app.

##### Style Imports

Before diving into the `.app-type` class, it's important to note the external style dependencies:

- `@import '../shared/schemes/forest';` - This line imports color schemes, fonts, or other styling elements from a
  shared directory named `forest`. This import suggests that the `.app-type` class may indirectly utilize styles or
  variables defined within the `forest` scheme.

- `@import '../shared/main';` - This import statement includes the main stylesheet from the shared directory. It likely
  contains global styles, variables, or mixins that are essential across the application, which the `.app-type` class
  might also utilize.

##### `.app-type` Class Styles

The `.app-type` class is designed to modify the appearance of text elements to which it is applied. Below is a breakdown
of each style property and its purpose:

- `text-transform: uppercase;` - Converts the text to uppercase, making it stand out and ensuring uniformity in the
  visual presentation.

- `font-size: 0.6em;` - Sets the font size to 0.6 times the size of the font of its parent element. This smaller font
  size helps differentiate the `.app-type` text from regular content text.

- `font-weight: 600;` - Applies a font weight of 600, making the text semi-bold. This increases readability and draws
  attention to the text.

- `background-color: rgba(0, 200, 255, 0.25);` - Assigns a background color with a low opacity to the element. The color
  used is a shade of light blue (`rgba(0, 200, 255, 0.25)`), providing a subtle highlight without overwhelming the text
  color.

- `border-radius: 5px;` - Rounds the corners of the element's border with a radius of 5 pixels, giving it a modern,
  pill-like shape.

- `padding: 2px 5px;` - Adds padding inside the element, with 2 pixels on the top and bottom and 5 pixels on the left
  and right. This creates space around the text, improving legibility and aesthetic appeal.

- `color: white;` - Sets the text color to white, ensuring high contrast against the semi-transparent light blue
  background, which aids in readability.

##### Summary

The `.app-type` class is a crucial part of our application's styling, designed to visually distinguish certain text
elements from the rest of the content. By applying these styles, developers can ensure that specific labels or
categories within the app are consistently formatted, enhancing the user interface's overall look and feel.

# main\resources\welcome\favicon.svg

### Developer Documentation for SVG Graphic

This SVG graphic is a complex illustration composed of multiple paths, styles, and colors. It's designed for web
developers and graphic designers who need to integrate vector-based images into their projects, particularly for web
pages or any application that supports SVG format.

#### Overview

The SVG (Scalable Vector Graphics) format is an XML-based file format for describing two-dimensional vector graphics.
This SVG graphic is defined with various elements and attributes that specify the shapes, colors, and paths within the
image.

#### Key Components

- **Paths (`<path>`):** The core elements of the SVG, defining the shapes and outlines. Each path element comes with
  a `d` attribute containing the drawing instructions.
- **Styles (`<style>`):** CSS styles defined within the SVG to style the paths. This includes fill colors, stroke
  widths, and more.
- **Groups (`<g>`):** Elements can be grouped together with the `<g>` element for easier styling or transformations as a
  single unit.

#### Integration

To integrate this SVG into a web page, you can directly embed the SVG code into your HTML document. Alternatively, you
can save the SVG code in an `.svg` file and reference it with an `<img>` tag, CSS `background-image`, or
the `<object>`, `<embed>`, or `<iframe>` tags for more complex interactions.

##### Embedding Directly in HTML

```html
<!-- Direct embedding of SVG code -->
<div class="svg-container">
    <!-- SVG code goes here -->
</div>
```

##### Referencing External SVG File

```html
<!-- Using <img> tag -->
<img src="path/to/your/svgfile.svg" alt="Description of the image">

<!-- As a background image in CSS -->
<style>
    .svg-background {
        background-image: url('path/to/your/svgfile.svg');
    }
</style>
<div class="svg-background"></div>

<!-- Using <object>, <embed>, or <iframe> -->
<object data="path/to/your/svgfile.svg" type="image/svg+xml"></object>
```

#### Manipulation and Styling

SVGs can be styled and manipulated using CSS and JavaScript, respectively. For instance, to change the fill color of a
path with a class `st0`, you can use:

```css
.st0 {
    fill: #FF0000; /* Changes the fill color to red */
}
```

Using JavaScript, you can add interactivity or dynamic changes to the SVG:

```javascript
document.querySelector('.st0').addEventListener('click', function () {
    this.style.fill = '#00FF00'; // Changes the fill color to green on click
});
```

#### Performance Considerations

While SVGs are excellent for scalability and resolution independence, it's essential to keep the file size in check for
web use, especially for complex graphics. Optimize SVGs using tools like SVGO to remove unnecessary code and reduce file
size without affecting the visual quality.

#### Conclusion

This SVG graphic provides a scalable and customizable way to incorporate high-quality illustrations into your digital
projects. By understanding the structure and integration methods, developers and designers can effectively leverage SVGs
to enhance their applications' visual appeal and user experience.

# main\resources\shared\_main.scss

## Developer Documentation: SCSS Mixins and Styles

This documentation outlines the SCSS mixins and styles used to create a cohesive and maintainable stylesheet for a web
application. The SCSS code provided defines a set of mixins for common CSS patterns, applies these mixins throughout the
stylesheet, and establishes a consistent theming approach using variables for colors, fonts, and other properties.

### SCSS Mixins

#### Typography Mixin

```scss
@mixin typography($font-family: $font-family-primary, $font-size: $font-size-base, $font-weight: $font-weight-normal) {
  font-family: $font-family;
  font-size: $font-size;
  font-weight: $font-weight;
}
```

This mixin allows for easy application of font styles, including family, size, and weight. Default values are set but
can be overridden when the mixin is included.

#### Flex Container Mixin

```scss
@mixin flex-container($direction: column) {
  display: flex;
  flex-direction: $direction;
}
```

Simplifies the creation of flex containers, with an optional direction parameter that defaults to `column`.

#### Fixed Full Mixin

```scss
@mixin fixed-full {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100%;
}
```

Applies a fixed, full-screen layout to an element, useful for overlays and modals.

#### Link Hover Transition Mixin

```scss
@mixin link-hover-transition {
  transition: color $transition-speed;
  &:hover {
    color: $link-hover-color;
  }
}
```

Facilitates a smooth color transition on hover for link elements, leveraging a variable for transition speed.

#### Message Style Mixin

```scss
@mixin message-style {
  padding: 10px;
  margin-bottom: 10px;
  border-radius: $border-radius;
}
```

Provides a consistent styling base for message elements, including padding, margin, and border-radius.

### Usage Examples

#### Applying a Mixin to an Element

```scss
body {
  @include typography($font-family-secondary);
  color: $primary-text-color;
  background-color: $primary-bg-color;
  margin: 0;
  padding: 30px 0 50px;
}
```

This example shows how to include the `typography` mixin with a custom font family for the `<body>` element, along with
additional styles.

#### Flex Container with Row Direction

```scss

#
#main-input,
.reply-form,
.code-execution {
  @include flex-container(row);
  padding: 5px;
  width: -webkit-fill-available;
  background-color: $primary-bg-color;
}
```

Demonstrates the use of the `flex-container` mixin with a `row` direction for layout alignment.

### Keyframes and Animations

Animations are defined using `@keyframes` for specific UI effects:

#### Bounce Animation

```scss
@keyframes bounce {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-10px);
  }
}
```

#### Spin Animation

```scss
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
```

These keyframes are used to create dynamic visual effects, such as a bouncing or spinning animation, and can be applied
to any element via the `animation` property.

### Conclusion

This documentation provides an overview of the SCSS mixins and styles used within the web application. By utilizing
mixins, variables, and consistent styling practices, the stylesheet remains maintainable and scalable. Remember to
replace the variable values with your project's specific design tokens and adjust the mixins as needed to fit your
application's requirements.

# main\resources\welcome\main.js

## Developer Documentation

### Overview

This script is designed to enhance user interaction within a web application by managing modal windows, fetching and
displaying user information, and handling theme changes dynamically. It leverages asynchronous JavaScript to fetch data
and modify the DOM in response to user actions.

#### Functions

##### `showModal(endpoint)`

Opens a modal window and fetches content from a specified endpoint to display within the modal.

- **Parameters**
    - `endpoint`: A string representing the URL from which to fetch content to be displayed in the modal.

##### `closeModal()`

Closes the modal window by setting its display style to 'none'.

##### `fetchData(endpoint)`

Fetches data asynchronously from a specified endpoint and displays it within the modal content area. It also handles
loading states and errors.

- **Parameters**
    - `endpoint`: A string representing the URL from which to fetch content.

##### Event Listeners

Sets up various event listeners once the DOM content is fully loaded, handling:

- Closing the modal when the close button or area outside the modal is clicked.
- Fetching and displaying user information.
- Handling theme changes based on user selection.

#### Usage

##### Modal Functionality

To open a modal with content fetched from a specific endpoint, call `showModal('/your-endpoint')`. To close the modal,
either call `closeModal()` directly, click the close button, or click outside the modal area.

##### Fetching User Information

Upon page load, the script automatically fetches user information from the 'userInfo' endpoint. If the user is logged
in (indicated by the presence of a name in the response data), it updates various elements to reflect the user's state (
e.g., displaying the username, showing logout link, and hiding the login link).

##### Theme Management

Users can switch themes by clicking on theme options. The script supports 'main', 'night', 'forest', and 'pony' themes.
The chosen theme is saved to `localStorage` and persisted across sessions.

#### Error Handling

Errors during data fetch operations are logged to the console to aid in debugging.

#### DOM Elements

The script manipulates various DOM elements identified by specific IDs (`modal`, `modal-content`, `theme_style`, etc.)
and classes (`.close`). Ensure these IDs and classes are present in your HTML to avoid script errors.

#### Best Practices

- Ensure all endpoints used with `showModal` are secure and authenticated as needed.
- For accessibility, consider adding ARIA attributes and roles to the modal and ensuring keyboard navigability.

#### Dependencies

This script requires a modern browser with support for ES6 features (e.g., `async/await`, `fetch` API, template
literals) and the DOM `ContentLoaded` event.

---
This documentation provides an overview of the script's functionality and usage. For more detailed integration and
customization, refer to the specific sections above.

# main\resources\welcome\index.html

## Developer Documentation: HTML Auto-Redirect Page

This document provides an overview and code example for setting up an HTML page that automatically redirects users to a
new page. This technique is useful for redirecting traffic from old URLs to new ones, maintaining user experience during
site restructuring, or simply guiding users directly to a main or updated page.

### Overview

The HTML meta refresh tag is utilized to implement an automatic redirect. This tag instructs the browser to refresh the
page and load a new URL after a specified number of seconds. In this example, the redirection is immediate (0 seconds).

### Code Example

```html
<!DOCTYPE html>
<html>
<head>
    <!-- Meta Refresh Tag for Redirect -->
    <meta http-equiv="refresh" content="0;url=/index.html">
</head>
<body>
<!-- Fallback Link -->
<p>If you are not redirected, <a href="/index.html">click here</a>.</p>
</body>
</html>
```

#### Components

- **DOCTYPE Declaration**: `<!DOCTYPE html>` specifies the document type and HTML version. This ensures the browser
  renders the page in standards mode.

- **Head Section**: Contains the meta refresh tag used for redirection.

    - **Meta Refresh Tag**: `<meta http-equiv="refresh" content="0;url=/index.html">` is placed within the `<head>`
      section. It has two main components:
        - `content="0"`: The number (in seconds) before the page is redirected. `0` makes the redirect instant.
        - `url=/index.html`: Specifies the destination URL. Replace `/index.html` with your target URL.

- **Body Section**: Provides a fallback for users whose browsers do not support meta refresh or have it disabled.

    - **Fallback Link**: A simple message with a hyperlink (`<a href="/index.html">click here</a>`) guiding users to
      manually click if the redirect does not work automatically.

### Usage

1. **Customization**: Replace `/index.html` in both the meta tag and the fallback hyperlink with the path to your new
   page.
2. **Deployment**: Save this code in an HTML file and upload it to your server or hosting environment in place of the
   old page you wish to redirect from.
3. **Testing**: Access the URL of the page where this redirect is set up to ensure it redirects immediately to the new
   page. Verify the fallback link works by disabling meta refresh in your browser settings and accessing the page again.

### Best Practices

- **SEO Considerations**: Frequent use of meta refresh redirects can affect your site's SEO negatively. For permanent
  redirects, a server-side 301 redirect is recommended.
- **Accessibility**: Ensure the fallback link is clearly visible and accessible for users who might need it.
- **Testing**: Test the redirect across different browsers and devices to ensure compatibility and user experience.

### Conclusion

The HTML meta refresh tag offers a simple method for page redirection. While useful for quick fixes or temporary
redirects, consider more SEO-friendly methods for permanent solutions. Always provide a clear fallback option for the
best user experience.

# main\resources\welcome\favicon.png

## Developer Documentation

### Overview

This document provides essential information for developers working on a project that involves handling PNG image files,
extracting XMP metadata embedded within them, and potentially modifying or utilizing this metadata for further
processing or analysis.

### PNG File Format

Portable Network Graphics (PNG) is a raster graphics file format that supports lossless data compression. PNG was
designed for transferring images on the Internet, not for professional-quality print graphics, and therefore does not
support non-RGB color spaces such as CMYK.

#### Structure

A PNG file consists of a PNG signature followed by a series of chunks. A chunk consists of four parts:

1. **Length**: A 4-byte field indicating the number of bytes in the chunk's data field.
2. **Chunk Type**: A 4-byte field defining the type of the chunk.
3. **Chunk Data**: The actual data of the chunk, of length specified by the length field.
4. **CRC**: A 4-byte field used to check for errors in the chunk.

The first chunk after the PNG signature must be the IHDR chunk, which contains basic information about the PNG image
such as width, height, bit depth, and color type.

### XMP Metadata in PNG

Extensible Metadata Platform (XMP) is a standard created by Adobe Systems for embedding metadata into digital media. In
PNG files, XMP metadata is stored in an iTXt, tEXt, or zTXt chunk with the keyword 'XML:com.adobe.xmp'.

#### Extracting XMP Metadata

To extract XMP metadata from a PNG file, follow these steps:

1. Read the PNG file and identify iTXt, tEXt, or zTXt chunks.
2. Look for chunks with the keyword 'XML:com.adobe.xmp'.
3. Extract the chunk data, which contains the XMP metadata in XML format.

#### Parsing XMP Metadata

Once you have extracted the XMP metadata, you can parse the XML to read the metadata fields. The metadata can include
information such as the creator, creation date, modification date, and more.

### Modifying XMP Metadata

To modify XMP metadata:

1. Parse the existing XMP metadata from the PNG file.
2. Modify the XML document according to your needs.
3. Encode the modified XML document back into the PNG file, replacing or adding to the existing metadata.

### Example Code

Below is a pseudocode example demonstrating how to extract XMP metadata from a PNG file:

```python
def extract_xmp_metadata(png_file_path):
    with open(png_file_path, 'rb') as file:
        content = file.read()
    
    # Look for the XMP metadata chunk
    start_keyword = b'XML:com.adobe.xmp'
    start_index = content.find(start_keyword)
    
    if start_index == -1:
        return None
    
    # Extract and return the XMP metadata
    metadata_start = start_index + len(start_keyword)
    metadata_end = content.find(b'\x00', metadata_start)
    xmp_metadata = content[metadata_start:metadata_end]
    
    return xmp_metadata


## Example usage
xmp_metadata = extract_xmp_metadata('example.png')
if xmp_metadata:
    print("XMP Metadata found:", xmp_metadata)
else:
    print("No XMP Metadata found.")
```

**Note:** This pseudocode is simplified for clarity and does not handle all aspects of PNG file parsing or XMP
extraction, such as dealing with compressed iTXt chunks or validating chunk CRCs.

### Conclusion

This document outlines the basic approach for handling PNG files and extracting, parsing, and modifying XMP metadata
within them. Developers can use this guide as a starting point for implementing more comprehensive solutions tailored to
their specific project requirements.

# main\resources\welcome\pony.scss

## Developer Documentation: Styling App-Type Elements

This guide provides an overview of the CSS styles applied to elements with the `.app-type` class. These styles are part
of a larger styling framework that may include imports from shared schemes and main stylesheets. Below you will find a
detailed description of each style property used and its impact on the appearance of `.app-type` elements.

### Style Overview

The `.app-type` class is designed to stylize specific text elements within an application, giving them a distinct
appearance that includes uppercase lettering, a specific font size and weight, a background color with transparency,
rounded corners, padding, and a text color set to white.

#### Code Snippet

```css
.app-type {
    text-transform: uppercase;
    font-size: 0.6em;
    font-weight: 600;
    background-color: rgba(0, 200, 255, 0.25);
    border-radius: 5px;
    padding: 2px 5px;
    color: white;
}
```

### Style Properties

- `text-transform: uppercase;` - This property transforms all the text of the element to uppercase, making it stand out
  and indicating a specific category or type within the application.

- `font-size: 0.6em;` - Sets the font size to 0.6 times the size of the font applicable to the element's parent, making
  the text smaller and ensuring it doesn't overpower other text or elements nearby.

- `font-weight: 600;` - Applies a font weight of 600, making the text semi-bold. This increases the text's prominence
  without making it as heavy as bold text.

- `background-color: rgba(0, 200, 255, 0.25);` - Specifies a background color using the RGBA color model. This color is
  a light shade of cyan with a 25% opacity, allowing for the background behind the text to be partially visible, adding
  depth to the design.

- `border-radius: 5px;` - Rounds the corners of the element's background with a radius of 5px, softening the overall
  look and making it more visually appealing.

- `padding: 2px 5px;` - Adds padding inside the element, with 2px on the top and bottom, and 5px on the left and right.
  This creates space around the text, making it more readable and aesthetically pleasing.

- `color: white;` - Sets the text color to white, ensuring high contrast against the semi-transparent cyan background
  for better readability.

### Usage

To apply these styles, add the `class="app-type"` attribute to any HTML element that you want to stylize accordingly.
Typically, this class is used for small, categorizing text elements within larger applications to denote different
sections or functionalities.

#### Example

```html
<span class="app-type">Free Version</span>
```

This will render the text "Free Version" with all the styles specified under the `.app-type` class, distinguishing it
from other text elements on the page.

### Conclusion

The `.app-type` class provides a set of predefined styles aimed at enhancing the visual hierarchy and readability of
specific types of text within an application. By following the guidelines outlined in this document, developers can
ensure consistent and effective application of these styles across different parts of their projects.

# main\resources\welcome\night.scss

### Documentation for App Type Styles

This section of the stylesheet is dedicated to defining the appearance of elements assigned with the `.app-type` class.
This class is tailored to modify textual elements, giving them a distinctive look that aids in their identification or
emphasis within the application. Below is a detailed breakdown of the `.app-type` class and its properties:

#### Import Statements

```css
@import '../shared/schemes/night';
@import '../shared/main';
```

Before defining the `.app-type` styles, the stylesheet imports two external files. These imports likely contain
variables, mixins, or foundational styles that are necessary for the `.app-type` class to align with the overall design
system of the application.

- **`../shared/schemes/night`**: This import suggests that the `.app-type` class is part of a theme or scheme, possibly
  tailored for a night or dark mode appearance.
- **`../shared/main`**: This import likely contains global styles, variables, and mixins used throughout the
  application, ensuring consistency in design.

#### `.app-type` Class Properties

```css
.app-type {
    text-transform: uppercase;
    font-size: 0.6em;
    font-weight: 600;
    background-color: rgba(0, 200, 255, 0.25);
    border-radius: 5px;
    padding: 2px 5px;
    color: white;
}
```

- **`text-transform: uppercase;`**: Converts the text of the element to uppercase, making it stand out and improving
  readability or emphasis.

- **`font-size: 0.6em;`**: Sets the font size to 0.6 times the size of the font of its parent element. This relative
  size ensures that the `.app-type` elements scale appropriately with their context.

- **`font-weight: 600;`**: Applies a font weight of 600, making the text semi-bold. This increases the text's prominence
  without making it overly bold.

- **`background-color: rgba(0, 200, 255, 0.25);`**: Assigns a semi-transparent light blue background color to the
  element. The use of RGBA color allows for transparency, letting the element's background blend with its surroundings
  while still maintaining a distinct appearance.

- **`border-radius: 5px;`**: Rounds the corners of the element with a radius of 5 pixels, softening its appearance and
  making it more visually appealing.

- **`padding: 2px 5px;`**: Adds padding inside the element, with 2 pixels on the top and bottom and 5 pixels on the
  sides. This creates space around the content, making it more legible and aesthetically pleasing.

- **`color: white;`**: Sets the text color to white, ensuring high contrast against the semi-transparent light blue
  background, which aids in readability.

#### Usage

To apply these styles, add the `.app-type` class to the desired HTML elements. This class is primarily intended for
textual elements that require emphasis or a distinctive appearance to stand out from other content. For example:

```html
<span class="app-type">Sample Text</span>
```

This will transform the text within the `<span>` tag according to the defined properties, making it uppercase,
semi-bold, and applying the specified background color, padding, and border radius.

#### Conclusion

The `.app-type` class is designed to create a specific visual identity for text elements within the application. By
defining a consistent style for these elements, the application maintains a cohesive look and feel, enhancing the user
experience.

# test\kotlin\com\simiacryptus\skyenet\webui\ActorTestAppServer.kt

## ActorTestAppServer Documentation

The `ActorTestAppServer` is a component of the SimiaCryptus Skyenet web application framework, designed to facilitate
the testing of various actor implementations within a web server context. This document provides an overview of its
functionality, setup, and usage.

### Overview

`ActorTestAppServer` extends the `ApplicationDirectory` class, setting up a web server environment on port `8082` by
default. It is primarily used for testing different types of actors,
including `SimpleActor`, `ParsedActor`, `ImageActor`, and various coding actors (`CodingActor`) with support for Scala,
Kotlin, and Groovy interpreters.

#### Key Components

- **TestJokeDataStructure**: A data class for storing joke structure including setup, punchline, and type.
- **JokeParser**: An interface extending `Function<String, TestJokeDataStructure>`, designed for parsing jokes.
- **childWebApps**: A lazy-initialized list of `ChildWebApp` instances, each corresponding to a specific test
  application for different actor types.
- **toolServlet**: A property returning `null`, indicating no specific tool servlet is used in this application.

#### Actors and Test Applications

- **SimpleActorTestApp**: Tests `SimpleActor` capabilities, such as translating user requests into Pig Latin.
- **ParsedActorTestApp**: Utilizes `ParsedActor` with a `JokeParser` for telling jokes.
- **ImageActorTestApp**: For testing `ImageActor` functionalities.
- **CodingActorTestApp**: Tests `CodingActor` with support for Scala, Kotlin, and Groovy interpreters.

### Setup and Running

To set up and run the `ActorTestAppServer`, follow these steps:

1. **Mock User Configuration**: A mock user is configured for authentication and authorization testing purposes.
2. **Authentication and Authorization Services**: `ApplicationServices` are set up with mock implementations
   for `authenticationManager` and `authorizationManager` to bypass actual authentication and authorization mechanisms
   for testing.
3. **Starting the Server**: The server is started by invoking the `main` method with necessary arguments, if any.

### Usage

Once the server is running, you can access the different test applications through their respective endpoints:

- `/test_simple`: Access the SimpleActor test application.
- `/test_parsed_joke`: Interact with the ParsedActor for jokes.
- `/images`: Test image processing capabilities with the ImageActor.
- `/test_coding_scala`, `/test_coding_kotlin`, `/test_coding_groovy`: Test coding actors with Scala, Kotlin, and Groovy
  support, respectively.

### Customization

To customize the `ActorTestAppServer`, consider modifying the `childWebApps` list to include additional test
applications or actors as needed. You can also implement custom authentication and authorization mechanisms by modifying
the `ApplicationServices.authenticationManager` and `ApplicationServices.authorizationManager` assignments in the `main`
method.

### Conclusion

The `ActorTestAppServer` provides a flexible and straightforward way to test various actor implementations within a web
server context. By following the setup and usage guidelines provided in this document, developers can effectively
leverage this tool to enhance the development and testing of actor-based applications within the SimiaCryptus Skyenet
framework.

# main\resources\welcome\main.scss

### Developer Documentation: App Type Styles

This section of the stylesheet is dedicated to defining the appearance of elements classified under the `.app-type`
class. The style rules specified here aim to create a distinctive look for these elements, making them stand out within
the application's interface. Below is a detailed breakdown of the `.app-type` class and its properties:

#### Import Statements

Before defining the `.app-type` styles, the stylesheet imports two other files:

1. **Normal Schemes**: `@import '../shared/schemes/normal';`
    - This import statement includes the normal color schemes and potentially other shared style properties used across
      the application. It ensures consistency in the visual design.

2. **Main Shared Styles**: `@import '../shared/main';`
    - This import includes the main shared styles that could consist of common typography, layout, or utility classes
      that are reused throughout the application.

#### `.app-type` Class

The `.app-type` class is designed to be applied to textual elements within the application that require emphasis or
differentiation from other text types. The styling attributes are as follows:

- **Text Transformation**: `text-transform: uppercase;`
    - Converts the text to uppercase for a more standout and uniform appearance.

- **Font Size**: `font-size: 0.6em;`
    - Sets the font size to 0.6 times the size of the font of the element's parent, making the text relatively smaller
      and suitable for labels or tags.

- **Font Weight**: `font-weight: 600;`
    - Applies a font weight of 600, making the text semi-bold. This enhances readability and draws attention.

- **Background Color**: `background-color: rgba(0, 200, 255, 0.25);`
    - Sets a semi-transparent background color using RGBA (Red, Green, Blue, Alpha) values. The color is a light shade
      of blue with 25% opacity, providing a subtle highlight without overwhelming the text color.

- **Border Radius**: `border-radius: 5px;`
    - Applies rounded corners with a radius of 5 pixels to the element, softening its appearance and making it more
      visually appealing.

- **Padding**: `padding: 2px 5px;`
    - Adds padding inside the element, with 2 pixels on the top and bottom and 5 pixels on the left and right. This
      creates space around the text, improving legibility and aesthetics.

- **Color**: `color: white;`
    - Sets the text color to white, ensuring high contrast against the semi-transparent blue background for optimal
      readability.

#### Usage

To apply these styles, add the `class="app-type"` attribute to the desired HTML element. Example:

```html
<span class="app-type">Free</span>
```

This will render the text "FREE" in uppercase, with a semi-bold font weight, smaller size, white color, and a light blue
background, enclosed within a box with rounded corners and padding for spacing.

#### Conclusion

The `.app-type` class provides a standardized way to highlight specific types of information within the application,
making them easily recognizable to users. By combining text transformations, color, and spacing, it creates a visually
appealing element that can be used across various parts of the interface.

# test\resources\logback.xml

## Logback Configuration Documentation

This document explains the structure and settings of a Logback XML configuration file used for logging in Java
applications. The provided XML configuration snippet specifies how log messages are formatted and where they are
outputted.

### Overview

Logback is a widely used logging framework in Java, offering a powerful mechanism for capturing and managing application
logs. The configuration file (`logback.xml` or `logback-test.xml`) is where you define the behavior of the logging
system, such as log levels, output formats, and destinations (console, files, etc.).

### Configuration Elements

#### `<configuration>`

The root element of the Logback configuration file. All configuration properties and appender definitions are nested
within this element.

#### Appender Configuration

Appenders are responsible for delivering log events to their destination. This configuration defines a console appender
named `STDOUT`.

##### `<appender>`

- `name`: A unique name for the appender, `STDOUT` in this case.
- `class`: The fully qualified name of the appender class. `ch.qos.logback.core.ConsoleAppender` is used here, meaning
  logs will be written to the console.

###### `<encoder>`

Encoders format log events before they are written to their destination.

- `<pattern>`: Defines the format of the log message. The pattern provided here includes:
    - `%d{HH:mm:ss.SSS}`: Timestamp in hours, minutes, seconds, and milliseconds.
    - `[%thread]`: Name of the thread that generated the log event.
    - `%-5level`: Log level (e.g., DEBUG, INFO) padded to 5 characters.
    - `%logger{36}`: Name of the logger, truncated or padded to 36 characters.
    - `- %msg%n`: The log message followed by a new line.

#### Root Logger Configuration

The root logger is the parent of all other loggers in the application, and it captures log messages from all levels
unless configured otherwise.

##### `<root>`

- `level`: The minimum level of messages that will be logged. Setting this to `debug` means that DEBUG, INFO, WARN,
  ERROR, and TRACE level messages will be logged.
- `<appender-ref ref="STDOUT" />`: This element links the root logger to the `STDOUT` appender, indicating that log
  messages accepted by the root logger should be outputted to the console as defined by the `STDOUT` appender.

### Conclusion

This Logback configuration provides a basic setup for console logging in a Java application. By adjusting the appender
configurations, log patterns, and log levels, developers can customize the logging behavior to suit their needs. This
setup is particularly useful for development environments where immediate feedback in the console is valuable for
troubleshooting and debugging.

# test\resources\client_secret_google_oauth.json.kms

#### Developer Documentation: Decoding and Understanding Encrypted Data

This documentation provides an overview and guide on how to decode and understand an encrypted data string. The provided
encrypted data is a complex string that requires a specific methodology to decode and interpret correctly. This document
is intended for developers who need to work with and understand encrypted data within their applications.

##### Overview

Encrypted data is used to secure information so that it is not accessible or readable by unauthorized users. The example
provided is an encrypted string that likely contains sensitive information. Decoding this data requires knowledge of the
encryption method used and access to the necessary decryption keys or tokens.

##### Prerequisites

- Familiarity with encryption and decryption concepts.
- Access to the decryption keys or tokens.
- Knowledge of the encryption algorithm used (e.g., AES, RSA).
- Required libraries or tools installed for decryption.

##### Decryption Process

The decryption process involves several steps, which are outlined below. Please note that the specific steps may vary
depending on the encryption algorithm and tools used.

1. **Identify the Encryption Algorithm**: Determine the encryption algorithm used for encrypting the data. This
   information is crucial for selecting the correct decryption tool or library.

2. **Access Decryption Keys/Tokens**: Ensure you have access to the necessary decryption keys or tokens. These keys are
   essential for decrypting the data successfully.

3. **Prepare the Environment**: Set up your development environment with the necessary decryption libraries or tools.
   For example, if the encryption algorithm is AES, ensure that you have an AES decryption library available.

4. **Decode the Encrypted String**: Use the decryption tool or library to decode the encrypted string. This step
   typically involves passing the encrypted data and the decryption key to the decryption function provided by the
   library.

   ```python
   from cryptography.fernet import Fernet

   # Example decryption code
   def decrypt_message(encrypted_message, key):
       f = Fernet(key)
       decrypted_message = f.decrypt(encrypted_message)
       return decrypted_message.decode()

   # Replace 'encrypted_data' and 'decryption_key' with the actual data and key
   decrypted_data = decrypt_message(encrypted_data, decryption_key)
   print("Decrypted message:", decrypted_data)
   ```

5. **Interpret the Decoded Data**: Once the data is decrypted, you will need to interpret or parse it based on its
   format (e.g., JSON, XML) and the structure of the information it contains.

6. **Implement Security Measures**: Ensure that the decrypted information is handled securely. Avoid logging sensitive
   information or exposing it to unauthorized users.

##### Best Practices

- **Key Management**: Securely manage decryption keys. Limit access to these keys to only those who absolutely need it.
- **Use Secure Channels**: When transmitting sensitive information, even in its encrypted form, always use secure
  channels (e.g., HTTPS).
- **Stay Updated**: Encryption algorithms and best practices evolve. Stay updated on the latest security advisories and
  update your encryption methods accordingly.

##### Conclusion

Working with encrypted data requires a thorough understanding of encryption algorithms, access to decryption keys, and
the use of appropriate tools or libraries. By following the steps outlined in this documentation, developers can decode
and understand encrypted data securely and effectively.

# test\kotlin\com\simiacryptus\skyenet\webui\TestOpenAPITool.kt

### Developer Documentation: OpenAPI Tool Testing with JUnit

This documentation provides a comprehensive guide on how to test OpenAPI Tool generation within a JUnit test
environment. The example code demonstrates how to generate OpenAPI client code in Java by using the OpenAPI Generator
tool programmatically within a test case.

#### Overview

The test case `TestOpenAPITool` is part of a larger project aimed at testing the functionality of the OpenAPI Generator
tool. The test involves creating a temporary OpenAPI specification file and using the OpenAPI Generator tool to generate
client code based on this specification.

#### Pre-requisites

- Java Development Kit (JDK) installed.
- OpenAPI Generator CLI installed or accessible via project dependencies.
- JUnit 5 test framework set up in the project.

#### Test Case: `TestOpenAPITool`

##### Step 1: Creating a Temporary OpenAPI Specification File

The test begins by creating a temporary file to store an OpenAPI specification. This specification defines a simple
API, "Gmail Labels API", which includes an endpoint for fetching Gmail labels.

```java
val tempFile = File.createTempFile("openapi", ".json").apply {

writeText(
    """
    {
      "openapi" : "3.0.0",
      ...
    }
    """.trimIndent()
  )

deleteOnExit()
}
```

This temporary file is set to be deleted when the JVM exits to ensure no residual files are left.

##### Step 2: Generating Client Code

The main part of the test involves invoking the OpenAPI Generator tool programmatically to generate client code based on
the temporary OpenAPI specification file created in Step 1.

```java
try{
val generator = "java"

File("C:/Users/andre/Downloads/openapi/build/openapi-$generator").

mkdirs()
  org.openapitools.codegen.OpenAPIGenerator.

main(
        arrayOf(
        "generate",
      "--skip-validate-spec",
                "-i","C:/Users/andre/Downloads/openapi.yaml",
                "-g",generator,
      "-o","C:/Users/andre/Downloads/openapi/build/openapi-$generator",
)
  )
          }catch(e:SpecValidationException){
        e.

printStackTrace()
}
```

In this snippet:

- A target directory for the generated code is created.
- The `OpenAPIGenerator.main` method is called with arguments to generate the client code. The arguments specify the
  input file (`-i`), the generator type (`-g`, in this case, `java`), and the output directory (`-o`).
- The `--skip-validate-spec` option is used to skip OpenAPI specification validation. This can be useful for testing or
  development purposes but should be used with caution.

##### Step 3: Handling Exceptions

The code generation process is wrapped in a try-catch block to handle `SpecValidationException`, which is thrown if the
OpenAPI specification is invalid. In a real-world scenario, additional exception handling might be necessary depending
on the requirements.

```java
}catch(e:SpecValidationException){
        e.

printStackTrace()
}
```

#### Conclusion

This documentation outlines the steps to create a JUnit test case for generating client code using the OpenAPI Generator
tool. It demonstrates creating a temporary OpenAPI specification file, invoking the OpenAPI Generator programmatically,
and handling exceptions.

This approach can be adapted and extended to test various OpenAPI specifications and generator configurations within a
continuous integration pipeline or a development workflow.

# test\resources\permissions\read.txt

To create effective developer documentation for a given piece of code, it's essential to include several key components.
These include a clear title or heading, a brief description of the functionality, any prerequisites or dependencies, a
detailed explanation of the code, usage examples, parameters or inputs descriptions, outputs or return values
explanations, and any additional notes or warnings. Without a specific code example from you, I'll outline a general
structure that you can follow to document any piece of code.

#### Developer Documentation Template

---

##### Title or Heading

*Provide a concise and descriptive title for the functionality or the piece of code.*

##### Brief Description

*Offer a short summary of what the code does and its purpose.*

##### Prerequisites or Dependencies

*List any required libraries, frameworks, or any other prerequisites needed to run the code.*

##### Detailed Explanation

*Explain how the code works in detail. This section can include the logic behind the code, the flow of execution, and
any important algorithms or techniques used.*

##### Code Example

```language
// Insert your code snippet here
```

##### Usage Examples

*Provide one or more examples of how to use the code in real-world scenarios. Include any variations in use cases if
applicable.*

##### Parameters or Inputs

*Detail each parameter or input the code accepts. Include data types, default values, and whether a parameter is
optional or required.*

- **param1** (`type`): Description. Default: `defaultValue`.
- **param2** (`type`, Optional): Description.

##### Outputs or Return Values

*Describe what the code returns after execution. Include data types and what each type represents.*

- **Return 1** (`type`): Description.
- **Return 2** (`type`): Description of when this value is returned.

##### Additional Notes or Warnings

*Include any additional information that might be helpful for the user, such as edge cases, limitations,
version-specific features, or upcoming deprecations.*

---

This template is a starting point, and depending on the complexity and nature of the code, some sections may need to be
expanded, or additional sections might be necessary. For more specific documentation, please provide the code snippet or
more details about the functionality you wish to document.

# test\resources\permissions\execute.txt

To create comprehensive developer documentation for a given piece of code, it's essential to break down the
documentation process into several key components. These components typically include an overview of the functionality,
prerequisites for using the code, a detailed explanation of the code's workings, examples of use cases, and any
additional notes or warnings that might be relevant. Below is a structured approach to documenting a hypothetical piece
of code:

#### Overview

The `calculateInterest` function is designed to calculate the interest accrued on a principal amount over a specified
period at a given interest rate. This function is useful for financial applications, banking software, and personal
finance tools where interest calculation is required.

#### Prerequisites

Before using the `calculateInterest` function, ensure that the following conditions are met:

- The programming environment supports JavaScript ES6 or later, as the function uses ES6 syntax.
- Basic understanding of interest calculation principles.

#### Function Signature

```javascript
function calculateInterest(principal, annualRate, timeInYears, compoundFrequency = 1) {
    // Function body
}
```

#### Parameters

- `principal` (Number): The initial amount of money on which the interest is calculated.
- `annualRate` (Number): The annual interest rate, expressed as a decimal (e.g., 0.05 for 5%).
- `timeInYears` (Number): The time period in years over which the interest is calculated.
- `compoundFrequency` (Number, optional): The number of times the interest is compounded per year. Defaults to 1 (simple
  interest).

#### Returns

- (Number): The amount of interest accrued over the specified period.

#### Example Usage

```javascript
// Calculating simple interest
const simpleInterest = calculateInterest(1000, 0.05, 1);
console.log(simpleInterest); // Output: 50

// Calculating compound interest
const compoundInterest = calculateInterest(1000, 0.05, 1, 12);
console.log(compoundInterest); // Output will vary based on the compound frequency
```

#### Detailed Explanation

The `calculateInterest` function calculates the interest by first determining if the interest is simple or compounded.
For simple interest (when `compoundFrequency` is 1), the formula used is:

\[ \text{Interest} = \text{Principal} \times \text{Rate} \times \text{Time} \]

For compound interest, the formula applied is:

\[ \text{Interest} = \text{Principal} \times \left(1 + \frac{\text{Rate}}{\text{Compound Frequency}}\right)
^{\text{Compound Frequency} \times \text{Time}} - \text{Principal} \]

The function then returns the calculated interest, excluding the principal amount.

#### Notes and Warnings

- Ensure that all numerical inputs are positive values to avoid unexpected results.
- The function does not validate input types; it assumes all inputs are of the correct type and format.
- The compound interest calculation assumes that compounding occurs uniformly over the specified time period.

#### Conclusion

The `calculateInterest` function is a versatile tool for calculating both simple and compound interest. By adjusting the
parameters, users can tailor the function to suit various financial scenarios. This documentation should provide all the
necessary information to understand and utilize the function effectively in your projects.

# test\resources\permissions\globalkey.txt

To create developer documentation for a piece of code, it's important to provide comprehensive, clear, and useful
information that can help other developers understand and use the code effectively. The documentation should include an
overview of what the code does, how it works, its dependencies, how to set it up, and examples of how to use it. Below
is a template for documenting a generic piece of code, followed by a specific example based on the assumption that
you're documenting a simple API.

#### Template for Developer Documentation

---

##### Name/Title

**[Name or title of the code/library/API]**

##### Overview

Provide a brief description of what the code does and its purpose. Explain in simple terms but be precise.

##### Features

List the key features and functionalities of the code.

##### Requirements

Mention any dependencies, environment requirements, or prerequisites needed to run the code.

##### Installation

Step-by-step guide on how to install or set up the environment and the code.

##### Configuration

Details on how to configure the code or system if necessary.

##### Usage

Provide examples of how to use the code. Include simple code snippets and explain the parameters and return values.

##### API Reference (if applicable)

Document the API endpoints, methods, request parameters, and response objects if you're documenting an API.

##### Examples

Provide more comprehensive examples that show how to use the code in real-world scenarios.

##### Troubleshooting

Common issues and their solutions or workarounds.

##### Contributing

Guidelines for contributing to the codebase, including code style, tests, and review process.

##### License

Specify the license under which the code is released.

---

#### Example: Simple API Documentation

---

##### Name/Title

**Simple Weather API**

##### Overview

The Simple Weather API allows developers to retrieve current weather information and forecasts. It supports multiple
cities worldwide.

##### Features

- Current weather data retrieval
- 5-day weather forecast
- Support for multiple units (metric, imperial)

##### Requirements

- Internet connection
- API key (obtainable after free registration)

##### Installation

This API is accessible over HTTP; no installation is required. Register at [website] to obtain your API key.

##### Configuration

Include your API key in the header of each request:

```
x-api-key: YOUR_API_KEY
```

##### Usage

**Get Current Weather:**

```
GET /weather/current?city={cityName}&units={units}
```

Parameters:

- `cityName` - Name of the city (e.g., "London")
- `units` - Units of measurement ("metric" or "imperial")

**Get 5-Day Forecast:**

```
GET /weather/forecast?city={cityName}&units={units}
```

##### Examples

**Request Current Weather in London (Metric):**

```
GET /weather/current?city=London&units=metric
```

**Response:**

```json
{
  "temperature": 15,
  "description": "Partly cloudy",
  "humidity": 73
}
```

##### Troubleshooting

- **Issue:** API key not recognized.  
  **Solution:** Ensure your API key is correct and included in the request header.

##### Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your changes. Ensure your code
adheres to the project's code style and includes tests.

##### License

This API is released under the MIT License.

---

This template and example should give you a good starting point for documenting your own code. Remember, the goal of
documentation is to make it easier for others (and your future self) to understand and use your code, so clarity and
completeness are key.

# test\resources\permissions\write.txt

To create comprehensive developer documentation for a piece of code, it's essential to include several key components
that will make it easier for other developers to understand, use, and possibly contribute to the code. These components
typically include an introduction or overview, installation instructions, usage examples, API documentation,
contribution guidelines, license information, and contact information for the maintainers. Below is a structured
approach to writing developer documentation, using a hypothetical piece of code as an example.

#### Example Code

```python
def add_numbers(a, b):
    """
    Adds two numbers together.

    Parameters:
    a (int): The first number.
    b (int): The second number.

    Returns:
    int: The sum of a and b.
    """
    return a + b
```

#### Developer Documentation Structure

##### Introduction

Start with an introduction to your code. Explain what it does, why it's useful, and any background information that
might be relevant.

**Example Introduction:**
> Welcome to the `NumberOperations` library, a simple Python module designed to perform basic arithmetic operations.
> This library is perfect for educational purposes or for small-scale projects that require basic mathematical operations
> without the overhead of more complex libraries.

##### Installation Instructions

Provide clear, step-by-step instructions on how to install your code. Include any prerequisites or dependencies if
applicable.

**Example Installation Instructions:**
> To install `NumberOperations`, you'll need Python installed on your system. This library has been tested with Python
> 3.6 and above. You can install it directly from the source by cloning this repository and running:
> ```
> python setup.py install
> ```

##### Usage Examples

Showcase how to use your code with simple examples. This helps users understand how to implement your code in their
projects.

**Example Usage:**
> Here's a quick example of how to use the `add_numbers` function from the `NumberOperations` library:
> ```python
> from number_operations import add_numbers
>
> result = add_numbers(5, 7)
> print(f"The sum is {result}")
> ```
> This will output: `The sum is 12`

##### API Documentation

Detail each function, class, or module provided by your code. Include parameters, return types, and a short description
of what each does.

**Example API Documentation:**
> **`add_numbers(a, b)`**
> - **Parameters:** `a` (int) - The first number, `b` (int) - The second number.
> - **Returns:** (int) - The sum of `a` and `b`.

##### Contribution Guidelines

Encourage contributions by providing guidelines on how others can contribute to your project. Include instructions for
submitting pull requests, coding standards, and how to report bugs.

**Example Contribution Guidelines:**
> Contributions to `NumberOperations` are welcome! Please submit pull requests on GitHub and ensure that your code
> follows the PEP 8 coding standards. For bug reports, please use the GitHub issues tab to report them.

##### License Information

Include the license under which your code is released. This informs users how they can legally use your code.

**Example License Information:**
> `NumberOperations` is released under the MIT License. See the LICENSE file for more details.

##### Contact Information

Provide a way for users to contact you or the project maintainers for further assistance or inquiries.

**Example Contact Information:**
> For any questions or concerns regarding `NumberOperations`, please contact us at example@email.com.

By following this structured approach and adapting each section to fit your specific project, you can create effective
and useful developer documentation that will help your project gain users and contributors.

# test\resources\permissions\admin.txt

Certainly! To create effective developer documentation for a given code example, it's essential to cover several key
aspects, including an overview of the functionality, prerequisites, how to set up the environment, a step-by-step guide
on how to use the code, and any additional notes that might help the developer understand the nuances of the code. Let's
proceed with an example code snippet you might want to document.

For demonstration purposes, let's assume the code snippet is a simple Python function that calculates and returns the
factorial of a number using recursion:

```python
def factorial(n):
    """Calculate the factorial of a number using recursion.

    Args:
        n (int): The number to calculate the factorial of.

    Returns:
        int: The factorial of the number.
    """
    if n == 1:
        return 1
    else:
        return n * factorial(n-1)
```

#### Developer Documentation for Factorial Function

##### Overview

This document provides details on the `factorial` function, a Python implementation that calculates the factorial of a
given number using recursion. The factorial of a number is the product of all positive integers less than or equal to
the number. It is denoted by n! and is defined for all positive integers and zero.

##### Prerequisites

- Python 3.x installed on the system.

##### Environment Setup

No additional environment setup is required for this function, as it uses Python's standard library.

##### Using the Code

1. **Import the Function**: First, ensure that the `factorial` function is accessible by your script. If it's defined in
   a separate module, you'll need to import it.

2. **Call the Function**: Use the function by passing an integer value to it. For example, `factorial(5)` will calculate
   the factorial of 5.

```python
result = factorial(5)
print(f"The factorial of 5 is {result}")
```

This will output:

```
The factorial of 5 is 120
```

##### Notes

- The function uses recursion to calculate the factorial. This means it calls itself with a decremented value of `n`
  until `n` equals 1, at which point it returns 1.
- Ensure that the input is a positive integer, as the factorial is not defined for negative numbers, and the function
  does not handle non-integer inputs or zero.
- The maximum value of `n` that can be handled is limited by Python's recursion depth limit, which can be checked or set
  using `sys.getrecursionlimit()` and `sys.setrecursionlimit(limit)` respectively.

##### Conclusion

The `factorial` function provides a straightforward and efficient method to calculate the factorial of a number using
recursion. It is a fundamental example of both recursion and the calculation of a mathematical series in computer
science.

---

This template can be adapted for documenting other code snippets by adjusting the sections to fit the context and
requirements of the new code.


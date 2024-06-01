## Overview of Base Application Classes

This document provides an overview of the base application classes used in the provided code snippets. These classes
form the foundation for various application functionalities, including coding assistance, web development, and testing
different types of actors.

### 1. `CodingAgent`

The `CodingAgent` class is designed to facilitate coding assistance using an interpreter. It interacts with the OpenAI
API to generate code based on user input and provides mechanisms to display and execute the generated code. The class
extends `ActorSystem` and utilizes a `CodingActor` to handle code generation.

**Key Features:**

- **Initialization:** Takes parameters like API, storage, session, user, UI, interpreter class, symbols, temperature,
  details, model, and main task.
- **Actor Management:** Manages a map of actors, with `CodingActor` being the primary actor.
- **Code Request Handling:** Generates code requests and handles the response.
- **Code Display and Feedback:** Displays the generated code and provides feedback mechanisms.
- **Execution:** Executes the generated code and handles execution errors.

### 2. `ShellToolAgent`

The `ShellToolAgent` class extends `CodingAgent` and adds functionalities specific to shell tool operations. It provides
mechanisms to export code prototypes, generate data schemas, and create servlets.

**Key Features:**

- **Tool Button Creation:** Adds a button to create tools from the generated code.
- **Schema Actor:** Generates data schemas from code prototypes.
- **Servlet Actor:** Converts code prototypes into servlets.
- **OpenAPI Integration:** Generates OpenAPI definitions for the servlets.
- **Test Page Generation:** Creates test pages for the generated servlets.

### 3. `WebDevApp`

The `WebDevApp` class is designed to assist in web development. It provides functionalities to translate user ideas into
detailed web application architectures and generate the necessary HTML, CSS, JavaScript, and image files.

**Key Features:**

- **Architecture Discussion:** Translates user ideas into detailed web application architectures.
- **File Drafting:** Drafts HTML, CSS, JavaScript, and image files based on the architecture.
- **Code Review:** Reviews and refines the generated code.
- **Task Management:** Manages tasks for drafting and reviewing code.

### 4. `ProcessInterpreter`

The `ProcessInterpreter` class provides an implementation of the `Interpreter` interface for running shell commands. It
wraps the provided code and executes it using a specified command.

**Key Features:**

- **Command Execution:** Executes shell commands based on the provided definitions.
- **Output Handling:** Handles the output and errors from the executed commands.
- **Code Wrapping:** Wraps the provided code before execution.

### 5. `SimpleActorTestApp`

The `SimpleActorTestApp` class is designed to test `SimpleActor` instances. It provides a simple interface for users to
interact with the actor and view the responses.

**Key Features:**

- **User Message Handling:** Handles user messages and passes them to the actor.
- **Response Display:** Displays the actor's responses in a user-friendly format.
- **Settings Management:** Manages settings for the actor.

### 6. `ParsedActorTestApp`

The `ParsedActorTestApp` class is designed to test `ParsedActor` instances. It provides functionalities to handle user
messages, parse the responses, and display them.

**Key Features:**

- **User Message Handling:** Handles user messages and passes them to the actor.
- **Response Parsing:** Parses the actor's responses and displays them in a structured format.
- **Settings Management:** Manages settings for the actor.

### 7. `ImageActorTestApp`

The `ImageActorTestApp` class is designed to test `ImageActor` instances. It provides functionalities to handle user
messages, generate images, and display them.

**Key Features:**

- **User Message Handling:** Handles user messages and passes them to the actor.
- **Image Generation:** Generates images based on the actor's responses.
- **Response Display:** Displays the generated images and the actor's responses.
- **Settings Management:** Manages settings for the actor.

### 8. `CodingActorTestApp`

The `CodingActorTestApp` class is designed to test `CodingActor` instances. It provides functionalities to handle user
messages, generate code, and execute it.

**Key Features:**

- **User Message Handling:** Handles user messages and passes them to the actor.
- **Code Generation:** Generates code based on the actor's responses.
- **Code Execution:** Executes the generated code and displays the results.
- **Settings Management:** Manages settings for the actor.

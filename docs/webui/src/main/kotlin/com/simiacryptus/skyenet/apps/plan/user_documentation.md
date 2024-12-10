<!-- TOC -->
* [AbstractTask.kt](#abstracttaskkt)
  * [AbstractTask Class Documentation](#abstracttask-class-documentation)
    * [Overview](#overview)
    * [Key Features](#key-features)
      * [Task Configuration and Settings](#task-configuration-and-settings)
      * [Task State Management](#task-state-management)
      * [Code File Management](#code-file-management)
      * [Task Dependencies](#task-dependencies)
      * [UI Integration](#ui-integration)
    * [Required Implementations](#required-implementations)
    * [Usage Example](#usage-example)
    * [Important Notes](#important-notes)
    * [See Also](#see-also)
* [CommandAutoFixTask.kt](#commandautofixtaskkt)
  * [CommandAutoFixTask Documentation](#commandautofixtask-documentation)
    * [Overview](#overview-1)
    * [Configuration](#configuration)
      * [CommandAutoFixTaskConfigData](#commandautofixtaskconfigdata)
      * [CommandWithWorkingDir](#commandwithworkingdir)
    * [Features](#features)
      * [Auto-Fix Capability](#auto-fix-capability)
      * [Command Resolution](#command-resolution)
      * [Working Directory](#working-directory)
    * [Usage Example](#usage-example-1)
    * [Output](#output)
    * [Error Handling](#error-handling)
    * [Best Practices](#best-practices)
    * [Limitations](#limitations)
* [CommandSessionTask.kt](#commandsessiontaskkt)
  * [CommandSessionTask Documentation](#commandsessiontask-documentation)
    * [Overview](#overview-2)
    * [Features](#features-1)
    * [Configuration Options](#configuration-options)
    * [Usage Examples](#usage-examples)
      * [Basic Command Execution](#basic-command-execution)
      * [Persistent Session](#persistent-session)
    * [Output Format](#output-format)
    * [Limitations](#limitations-1)
    * [Best Practices](#best-practices-1)
    * [Error Handling](#error-handling-1)
    * [Session Management](#session-management)
    * [Notes](#notes)
* [DisabledTaskException.kt](#disabledtaskexceptionkt)
  * [DisabledTaskException](#disabledtaskexception)
    * [Overview](#overview-3)
    * [Constructor](#constructor)
      * [Parameters](#parameters)
    * [Usage Example](#usage-example-2)
    * [Common Scenarios](#common-scenarios)
    * [Best Practices](#best-practices-2)
* [ForeachTask.kt](#foreachtaskkt)
  * [ForeachTask Documentation](#foreachtask-documentation)
    * [Overview](#overview-4)
    * [Configuration](#configuration-1)
      * [Required Parameters](#required-parameters)
      * [Optional Parameters](#optional-parameters)
    * [Example Configuration](#example-configuration)
    * [Behavior](#behavior)
    * [Use Cases](#use-cases)
    * [Notes](#notes-1)
    * [Error Handling](#error-handling-2)
* [GitHubSearchTask.kt](#githubsearchtaskkt)
  * [GitHub Search Task Documentation](#github-search-task-documentation)
    * [Overview](#overview-5)
    * [Features](#features-2)
    * [Configuration Parameters](#configuration-parameters)
      * [Required Parameters](#required-parameters-1)
      * [Optional Parameters](#optional-parameters-1)
    * [Result Format](#result-format)
      * [Type-Specific Result Details](#type-specific-result-details)
        * [Repositories](#repositories)
        * [Code](#code)
        * [Commits](#commits)
        * [Issues](#issues)
        * [Users](#users)
        * [Topics](#topics)
    * [Example Usage](#example-usage)
    * [Notes](#notes-2)
    * [Error Handling](#error-handling-3)
* [GoogleSearchTask.kt](#googlesearchtaskkt)
  * [GoogleSearchTask Documentation](#googlesearchtask-documentation)
    * [Overview](#overview-6)
    * [Configuration](#configuration-2)
      * [Required Settings](#required-settings)
      * [Task Configuration Parameters](#task-configuration-parameters)
    * [Usage Example](#usage-example-3)
    * [Output Format](#output-format-1)
  * [2. Title of Result](#2-title-of-result)
      * [Executing Plans](#executing-plans)
    * [Visual Components](#visual-components)
    * [Usage Example](#usage-example-4)
    * [Error Handling](#error-handling-4)
    * [Best Practices](#best-practices-3)
    * [Limitations](#limitations-2)
* [PlanProcessingState.kt](#planprocessingstatekt)
  * [PlanProcessingState Documentation](#planprocessingstate-documentation)
    * [Properties](#properties)
      * [subTasks](#subtasks)
      * [tasksByDescription](#tasksbydescription-)
      * [taskIdProcessingQueue](#taskidprocessingqueue)
      * [taskResult](#taskresult)
      * [completedTasks](#completedtasks)
      * [taskFutures](#taskfutures)
      * [uitaskMap](#uitaskmap)
    * [Usage](#usage)
* [PlanSettings.kt](#plansettingskt)
  * [PlanSettings Documentation](#plansettings-documentation)
    * [Overview](#overview-7)
    * [Key Properties](#key-properties)
      * [Model Settings](#model-settings)
      * [Command Execution](#command-execution)
      * [Integration Settings](#integration-settings)
      * [Task Settings](#task-settings)
    * [Usage Example](#usage-example-5)
    * [Task Planning](#task-planning)
    * [Customization](#customization)
    * [Best Practices](#best-practices-4)
* [PlanUtil.kt](#planutilkt)
  * [PlanUtil Documentation](#planutil-documentation)
    * [Key Features](#key-features-1)
      * [Task Visualization](#task-visualization)
      * [Task Management](#task-management)
      * [Visualization Styling](#visualization-styling)
    * [Usage Example](#usage-example-6)
    * [Notes](#notes-3)
    * [Error Handling](#error-handling-5)
* [Planner.kt](#plannerkt)
  * [Planner Class Documentation](#planner-class-documentation)
    * [Key Features](#key-features-2)
    * [Main Methods](#main-methods)
      * [initialPlan](#initialplan)
        * [Parameters:](#parameters-1)
      * [newPlan](#newplan)
        * [Parameters:](#parameters-2)
    * [Usage Example](#usage-example-7)
    * [Configuration](#configuration-3)
    * [Notes](#notes-4)
* [PlanningTask.kt](#planningtaskkt)
  * [PlanningTask Documentation](#planningtask-documentation)
    * [Overview](#overview-8)
    * [Key Features](#key-features-3)
      * [Task Planning Capabilities](#task-planning-capabilities)
      * [Task Configuration](#task-configuration)
      * [Execution Modes](#execution-modes)
        * [Blocking Mode](#blocking-mode)
        * [Auto Mode](#auto-mode)
    * [Usage Example](#usage-example-8)
    * [Task Breakdown Process](#task-breakdown-process)
    * [Best Practices](#best-practices-5)
    * [Notes](#notes-5)
    * [Error Handling](#error-handling-6)
* [RunShellCommandTask.kt](#runshellcommandtaskkt)
  * [RunShellCommandTask Documentation](#runshellcommandtask-documentation)
    * [Overview](#overview-9)
    * [Configuration](#configuration-4)
      * [RunShellCommandTaskConfigData](#runshellcommandtaskconfigdata)
    * [Usage](#usage-1)
      * [Basic Example](#basic-example)
      * [Features](#features-3)
    * [Best Practices](#best-practices-6)
    * [Limitations](#limitations-3)
    * [Security Considerations](#security-considerations)
    * [Notes](#notes-6)
* [SearchTask.kt](#searchtaskkt)
  * [SearchTask Documentation](#searchtask-documentation)
    * [Overview](#overview-10)
    * [Configuration Options](#configuration-options-1)
      * [SearchTaskConfigData](#searchtaskconfigdata)
    * [Usage Example](#usage-example-9)
    * [Output Format](#output-format-2)
    * [Features](#features-4)
    * [Notes](#notes-7)
* [SeleniumSessionTask.kt](#seleniumsessiontaskkt)
  * [SeleniumSessionTask Documentation](#seleniumsessiontask-documentation)
    * [Overview](#overview-11)
    * [Features](#features-5)
    * [Configuration Options](#configuration-options-2)
      * [SeleniumSessionTaskConfigData Parameters](#seleniumsessiontaskconfigdata-parameters)
    * [Example JavaScript Commands](#example-javascript-commands)
    * [Usage Notes](#usage-notes)
    * [Best Practices](#best-practices-7)
    * [Limitations](#limitations-4)
    * [Example Usage](#example-usage-1)
* [TaskBreakdownWithPrompt.kt](#taskbreakdownwithpromptkt)
  * [TaskBreakdownWithPrompt](#taskbreakdownwithprompt)
    * [Overview](#overview-12)
    * [Properties](#properties-1)
    * [Usage](#usage-2)
    * [Example](#example)
* [TaskConfigBase.kt](#taskconfigbasekt)
  * [TaskConfigBase Documentation](#taskconfigbase-documentation)
    * [Overview](#overview-13)
    * [Key Properties](#key-properties-1)
      * [task_type](#task_type)
      * [task_description](#task_description)
      * [task_dependencies](#task_dependencies)
      * [state](#state)
    * [JSON Serialization](#json-serialization)
    * [Usage Example](#usage-example-10)
    * [Notes](#notes-8)
    * [See Also](#see-also-1)
* [TaskSettingsBase.kt](#tasksettingsbasekt)
  * [TaskSettingsBase Documentation](#tasksettingsbase-documentation)
    * [Overview](#overview-14)
    * [Properties](#properties-2)
    * [Usage](#usage-3)
    * [JSON Serialization](#json-serialization-1)
    * [Type Resolution](#type-resolution)
    * [Notes](#notes-9)
* [TaskType.kt](#tasktypekt)
  * [TaskType Documentation](#tasktype-documentation)
    * [Overview](#overview-15)
    * [Task Types](#task-types)
      * [Planning Tasks](#planning-tasks)
      * [File Operations](#file-operations)
      * [Search and Knowledge](#search-and-knowledge)
      * [Command and Shell Operations](#command-and-shell-operations)
      * [Web Operations](#web-operations)
    * [Usage](#usage-4)
      * [Getting Available Task Types](#getting-available-task-types)
      * [Creating a Task Implementation](#creating-a-task-implementation)
      * [Checking if a Task Type is Enabled](#checking-if-a-task-type-is-enabled)
    * [Important Notes](#important-notes-1)
    * [Error Handling](#error-handling-7)
* [WebFetchAndTransformTask.kt](#webfetchandtransformtaskkt)
  * [WebFetchAndTransformTask Documentation](#webfetchandtransformtask-documentation)
    * [Overview](#overview-16)
    * [Configuration](#configuration-5)
      * [Required Parameters](#required-parameters-2)
      * [Optional Parameters](#optional-parameters-2)
    * [Usage Example](#usage-example-11)
    * [How It Works](#how-it-works)
    * [Features](#features-6)
    * [Best Practices](#best-practices-8)
    * [Error Handling](#error-handling-8)
    * [Integration](#integration)
    * [Limitations](#limitations-5)
* [file\AbstractAnalysisTask.kt](#fileabstractanalysistaskkt)
  * [AbstractAnalysisTask Documentation](#abstractanalysistask-documentation)
    * [Overview](#overview-17)
    * [Key Features](#key-features-4)
    * [Usage](#usage-5)
      * [Creating a Custom Analysis Task](#creating-a-custom-analysis-task)
      * [Required Implementations](#required-implementations-1)
      * [Configuration](#configuration-6)
    * [Execution Flow](#execution-flow)
    * [Status Messages](#status-messages)
    * [Dependencies](#dependencies)
    * [Error Handling](#error-handling-9)
    * [Best Practices](#best-practices-9)
* [file\AbstractFileTask.kt](#fileabstractfiletaskkt)
  * [AbstractFileTask Documentation](#abstractfiletask-documentation)
    * [Overview](#overview-18)
    * [Key Features](#key-features-5)
      * [File Configuration](#file-configuration)
      * [File Pattern Matching](#file-pattern-matching)
      * [File Content Processing](#file-content-processing)
    * [Usage Example](#usage-example-12)
    * [Configuration Parameters](#configuration-parameters-1)
      * [FileTaskConfigBase](#filetaskconfigbase)
    * [File Processing](#file-processing)
    * [Best Practices](#best-practices-10)
    * [Error Handling](#error-handling-10)
* [file\CodeOptimizationTask.kt](#filecodeoptimizationtaskkt)
  * [CodeOptimizationTask Documentation](#codeoptimizationtask-documentation)
    * [Overview](#overview-19)
    * [Configuration](#configuration-7)
      * [CodeOptimizationTaskConfigData Parameters](#codeoptimizationtaskconfigdata-parameters)
    * [Features](#features-7)
    * [Output Format](#output-format-3)
    * [Example Usage](#example-usage-2)
    * [Best Practices](#best-practices-11)
    * [Notes](#notes-10)
* [file\CodeReviewTask.kt](#filecodereviewtaskkt)
  * [CodeReviewTask Documentation](#codereviewtask-documentation)
    * [Overview](#overview-20)
    * [Configuration](#configuration-8)
      * [Required Parameters](#required-parameters-3)
      * [Configuration Data (CodeReviewTaskConfigData)](#configuration-data-codereviewtaskconfigdata)
    * [Features](#features-8)
    * [Usage Example](#usage-example-13)
    * [Output](#output-1)
    * [Notes](#notes-11)
* [file\DocumentationTask.kt](#filedocumentationtaskkt)
  * [DocumentationTask](#documentationtask)
    * [Purpose](#purpose)
    * [Key Components](#key-components)
      * [DocumentationTaskConfigData](#documentationtaskconfigdata)
      * [Documentation Generator Actor](#documentation-generator-actor)
    * [Usage](#usage-6)
    * [Configuration Options](#configuration-options-3)
    * [Key Methods](#key-methods)
      * [run()](#run)
      * [promptSegment()](#promptsegment)
    * [Integration](#integration-1)
    * [Error Handling](#error-handling-11)
    * [Notes](#notes-12)
* [file\FileModificationTask.kt](#filefilemodificationtaskkt)
  * [FileModificationTask Documentation](#filemodificationtask-documentation)
    * [Overview](#overview-21)
    * [Key Features](#key-features-6)
    * [Configuration](#configuration-9)
      * [FileModificationTaskConfigData Parameters](#filemodificationtaskconfigdata-parameters)
    * [Usage](#usage-7)
      * [Basic Example](#basic-example-1)
      * [Auto-Fix Mode](#auto-fix-mode)
      * [Manual Mode](#manual-mode)
    * [Output Format](#output-format-4)
    * [Best Practices](#best-practices-12)
    * [Error Handling](#error-handling-12)
    * [Dependencies](#dependencies-1)
    * [Notes](#notes-13)
* [file\InquiryTask.kt](#fileinquirytaskkt)
  * [InquiryTask Documentation](#inquirytask-documentation)
    * [Overview](#overview-22)
    * [Configuration](#configuration-10)
      * [Required Configuration Parameters](#required-configuration-parameters)
      * [Optional Configuration Parameters](#optional-configuration-parameters)
    * [Usage Examples](#usage-examples-1)
    * [Features](#features-9)
      * [Interactive Mode](#interactive-mode)
      * [Non-Interactive Mode](#non-interactive-mode)
      * [File Analysis](#file-analysis)
    * [Best Practices](#best-practices-13)
    * [Limitations](#limitations-6)
    * [Error Handling](#error-handling-13)
    * [Integration](#integration-2)
    * [Output](#output-2)
* [file\PerformanceAnalysisTask.kt](#fileperformanceanalysistaskkt)
  * [Performance Analysis Task Documentation](#performance-analysis-task-documentation)
    * [Overview](#overview-23)
    * [Configuration](#configuration-11)
      * [Required Parameters](#required-parameters-4)
      * [Example Configuration](#example-configuration-1)
    * [Analysis Focus Areas](#analysis-focus-areas)
    * [Output Format](#output-format-5)
    * [Usage Notes](#usage-notes-1)
    * [Example Output](#example-output)
    * [Dependencies](#dependencies-2)
* [file\RefactorTask.kt](#filerefactortaskkt)
  * [RefactorTask Documentation](#refactortask-documentation)
    * [Overview](#overview-24)
    * [Configuration](#configuration-12)
      * [RefactorTaskConfigData](#refactortaskconfigdata)
    * [Features](#features-10)
    * [Output Format](#output-format-6)
    * [Example Usage](#example-usage-3)
    * [Best Practices](#best-practices-14)
    * [Notes](#notes-14)
* [file\SecurityAuditTask.kt](#filesecurityaudittaskkt)
  * [SecurityAuditTask Documentation](#securityaudittask-documentation)
    * [Overview](#overview-25)
    * [Features](#features-11)
    * [Configuration](#configuration-13)
    * [Analysis Areas](#analysis-areas)
    * [Output Format](#output-format-7)
    * [Usage Example](#usage-example-14)
    * [Best Practices](#best-practices-15)
* [file\TestGenerationTask.kt](#filetestgenerationtaskkt)
  * [TestGenerationTask Documentation](#testgenerationtask-documentation)
    * [Overview](#overview-26)
    * [Configuration](#configuration-14)
      * [TestGenerationTaskConfigData Parameters](#testgenerationtaskconfigdata-parameters)
    * [Features](#features-12)
    * [Output Format](#output-format-8)
    * [Example Usage](#example-usage-4)
    * [Generated Test Structure](#generated-test-structure)
    * [Best Practices](#best-practices-16)
* [knowledge\EmbeddingSearchTask.kt](#knowledgeembeddingsearchtaskkt)
  * [EmbeddingSearchTask Documentation](#embeddingsearchtask-documentation)
    * [Overview](#overview-27)
    * [Configuration Options](#configuration-options-4)
      * [Required Parameters](#required-parameters-5)
      * [Optional Parameters](#optional-parameters-3)
    * [Output Format](#output-format-9)
    * [Example Usage](#example-usage-5)
    * [Best Practices](#best-practices-17)
    * [Notes](#notes-15)
* [knowledge\KnowledgeIndexingTask.kt](#knowledgeknowledgeindexingtaskkt)
  * [Knowledge Indexing Task Documentation](#knowledge-indexing-task-documentation)
    * [Overview](#overview-28)
    * [Configuration Options](#configuration-options-5)
      * [Required Parameters](#required-parameters-6)
      * [Optional Parameters](#optional-parameters-4)
    * [Usage Example](#usage-example-15)
    * [Process Flow](#process-flow)
    * [Output](#output-3)
    * [Notes](#notes-16)
    * [Error Handling](#error-handling-14)
* [knowledge\WebSearchAndIndexTask.kt](#knowledgewebsearchandindextaskkt)
  * [WebSearchAndIndexTask Documentation](#websearchandindextask-documentation)
    * [Overview](#overview-29)
    * [Configuration Parameters](#configuration-parameters-2)
      * [Required Parameters](#required-parameters-7)
      * [Optional Parameters](#optional-parameters-5)
    * [Prerequisites](#prerequisites)
    * [How It Works](#how-it-works-1)
    * [Example Usage](#example-usage-6)
    * [Output](#output-4)
    * [Error Handling](#error-handling-15)
    * [Limitations](#limitations-7)
    * [Best Practices](#best-practices-18)
    * [Related Components](#related-components)
<!-- TOC -->

# AbstractTask.kt

Here's the user documentation for the AbstractTask class:


## AbstractTask Class Documentation


### Overview
`AbstractTask` is an abstract base class for implementing task execution units within a planning system. It provides a framework for tasks that can be executed as part of a larger plan, with support for state management, code file handling, and task dependencies.


### Key Features


#### Task Configuration and Settings
- Takes a `PlanSettings` object and generic task configuration of type `T` (extending `TaskConfigBase`)
- Manages task state through an enum (`Pending`, `InProgress`, `Completed`)
- Handles working directory configuration through the `root` property


#### Task State Management
```kotlin
var state: TaskState? = TaskState.Pending
```
Tracks the current state of the task through the `TaskState` enum:
- `Pending`: Task has not started
- `InProgress`: Task is currently executing
- `Completed`: Task has finished execution


#### Code File Management
```kotlin
protected val codeFiles = mutableMapOf<Path, String>()
```
Maintains a map of file paths to code content for handling source files during task execution.


#### Task Dependencies
The `getPriorCode` method retrieves code from dependent tasks, formatting them with task names as headers.


#### UI Integration
The `acceptButtonFooter` method provides a standard way to add an acceptance button to task output, with callback handling.


### Required Implementations

Subclasses must implement:

1. `promptSegment()`: Define the prompt text for the task
2. `run()`: Implement the main task execution logic


### Usage Example

```kotlin
class MyCustomTask(
    planSettings: PlanSettings,
    taskConfig: MyTaskConfig
) : AbstractTask<MyTaskConfig>(planSettings, taskConfig) {
    
    override fun promptSegment(): String {
        return "Custom task prompt text"
    }
    
    override fun run(
        agent: PlanCoordinator,
        messages: List<String>,
        task: SessionTask,
        api: ChatClient,
        resultFn: (String) -> Unit,
        api2: OpenAIClient,
        planSettings: PlanSettings
    ) {
        // Task implementation
    }
}
```


### Important Notes

- Tasks should properly manage their state transitions
- Working directory must be set in PlanSettings for root path resolution
- Task dependencies should be properly configured in the task configuration
- UI callbacks should be handled safely with proper error handling


### See Also

- `PlanSettings`
- `TaskConfigBase`
- `PlanCoordinator`
- `SessionTask`

# CommandAutoFixTask.kt

Here's the user documentation for the CommandAutoFixTask class:


## CommandAutoFixTask Documentation


### Overview
The `CommandAutoFixTask` is a specialized task that executes commands and automatically attempts to fix any issues that arise during execution. It's particularly useful for automated build and test processes where common issues can be automatically resolved.


### Configuration


#### CommandAutoFixTaskConfigData
The task is configured using `CommandAutoFixTaskConfigData` which contains:

- `commands`: A list of commands to be executed, each with their working directory
- `task_description`: Optional description of the task
- `task_dependencies`: Optional list of dependent task IDs
- `state`: Current state of the task


#### CommandWithWorkingDir
Each command is specified using `CommandWithWorkingDir` which contains:

- `command`: List of strings representing the command and its arguments
- `workingDir`: Optional relative path to the working directory (relative to root directory)


### Features


#### Auto-Fix Capability
- When `autoFix` is enabled in plan settings, the task will automatically attempt to fix issues
- Up to 5 retry attempts are made when auto-fix is enabled
- Without auto-fix, the task will only execute commands once


#### Command Resolution
- Commands can be specified using aliases or full paths
- The task will search for matching executables in the configured command directories
- Throws an IllegalArgumentException if the command cannot be found


#### Working Directory
- Each command can specify its own working directory
- If no working directory is specified, the root directory is used
- Working directories are automatically created if they don't exist


### Usage Example

```kotlin
val task = CommandAutoFixTask(
    planSettings = PlanSettings(
        autoFix = true,
        commandAutoFixCommands = listOf("/path/to/commands")
    ),
    planTask = CommandAutoFixTaskConfigData(
        commands = listOf(
            CommandWithWorkingDir(
                command = listOf("build", "--clean"),
                workingDir = "src/main"
            )
        ),
        task_description = "Clean build with auto-fix"
    )
)
```


### Output
The task provides detailed feedback through the UI:
- Progress updates for each command execution
- Success/failure status for each command
- Auto-fix application notifications when enabled
- Overall task completion status


### Error Handling
- Failed commands are clearly indicated in the UI
- With auto-fix enabled, the task will automatically retry failed commands
- Users can manually accept results even if some commands fail
- Detailed logs are maintained for troubleshooting


### Best Practices
1. Provide clear command aliases for better readability
2. Use relative working directories for portability
3. Enable auto-fix for automated workflows
4. Include descriptive task descriptions for better tracking
5. Monitor logs for persistent failures that may need manual intervention


### Limitations
- Auto-fix capability depends on the specific command implementation
- Maximum of 5 retry attempts with auto-fix enabled
- Commands must be available in the configured command directories
- Working directories must be within the project scope

# CommandSessionTask.kt

Here's the user documentation for the CommandSessionTask class:


## CommandSessionTask Documentation


### Overview
The CommandSessionTask class provides functionality to create and manage interactive command-line sessions within the application. It allows you to execute commands, maintain session state, and handle multiple concurrent sessions.


### Features
- Create interactive command sessions
- Send multiple commands to a session
- Maintain session state between commands
- Reuse existing sessions via session IDs
- Automatic session cleanup and management
- Configurable command timeouts
- Session limiting to prevent resource exhaustion


### Configuration Options
The task is configured using `CommandSessionTaskConfigData` with the following parameters:

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| command | List<String> | The command and arguments to start the session | Required |
| inputs | List<String> | Commands to send to the session | Empty list |
| sessionId | String? | ID to reuse or create a named session | null |
| timeout | Long | Timeout in milliseconds for each command | 30000 (30s) |
| closeSession | Boolean | Whether to close the session after execution | false |


### Usage Examples


#### Basic Command Execution
```kotlin
val config = CommandSessionTaskConfigData(
    command = listOf("bash"),
    inputs = listOf("ls -l", "pwd"),
    timeout = 5000
)
```


#### Persistent Session
```kotlin
val config = CommandSessionTaskConfigData(
    command = listOf("python"),
    inputs = listOf("import math", "print(math.pi)"),
    sessionId = "python-session",
    closeSession = false
)
```


### Output Format
The task produces formatted output containing:
- Command used to start the session
- Session ID
- Timeout setting
- Results for each input command, including:
  - The input command
  - The command output (limited to 5000 characters)


### Limitations
- Maximum of 10 concurrent sessions
- 30-second default timeout per command
- Output is truncated to 5000 characters per command


### Best Practices
1. Always use sessionId for long-running sessions
2. Close sessions when no longer needed
3. Use appropriate timeouts for your commands
4. Handle potential errors in command execution
5. Clean up sessions using closeSession=true when finished


### Error Handling
The task handles various error conditions:
- Session limit exceeded
- Command execution failures
- Timeout conditions
- Process termination issues


### Session Management
Sessions can be managed using static methods:
- `closeSession(sessionId)`: Close a specific session
- `closeAllSessions()`: Close all active sessions
- `getActiveSessionCount()`: Get number of active sessions


### Notes
- Sessions without IDs are temporary and cleaned up after use
- Error messages are included in the output if commands fail
- The task automatically cleans up inactive sessions
- All session output is captured, including stderr

# DisabledTaskException.kt

Here's the documentation for the DisabledTaskException class:


## DisabledTaskException

A custom exception class that is thrown when attempting to use a disabled task type in the SkyeNet planning system.


### Overview

`DisabledTaskException` extends the standard Java `Exception` class and is used to indicate that a specific task type has been disabled and cannot be executed.


### Constructor

```kotlin
DisabledTaskException(taskType: TaskType<*, *>)
```


#### Parameters

- `taskType`: The `TaskType` instance that is disabled. The task type's string representation will be included in the exception message.


### Usage Example

```kotlin
try {
    // Attempt to use a disabled task
    if (taskType.isDisabled) {
        throw DisabledTaskException(taskType)
    }
    // Task execution code...
} catch (e: DisabledTaskException) {
    // Handle the disabled task case
    println(e.message) // Will print: "Task type [taskType] is disabled"
}
```


### Common Scenarios

This exception is typically thrown when:

1. A task type has been explicitly disabled in the system configuration
2. A task type is not available in the current execution context
3. Access to a particular task type has been restricted


### Best Practices

- Always catch and handle `DisabledTaskException` appropriately in your code
- Provide meaningful feedback to users when a task cannot be executed due to being disabled
- Consider offering alternative task types or workflows when a preferred task type is disabled

# ForeachTask.kt

Here's the user documentation for the ForeachTask class:


## ForeachTask Documentation


### Overview
The ForeachTask is a specialized task type that allows you to execute a set of subtasks for each item in a given list. This is particularly useful when you need to perform the same operations on multiple items sequentially.


### Configuration


#### Required Parameters
- `foreach_items`: A list of strings representing the items to iterate over
- `foreach_subplan`: A map defining the subtasks to execute for each item


#### Optional Parameters
- `task_description`: A description of the foreach task
- `task_dependencies`: List of task IDs that must complete before this task starts
- `state`: Current state of the task


### Example Configuration
```kotlin
ForeachTaskConfigData(
    foreach_items = listOf("item1", "item2", "item3"),
    foreach_subplan = mapOf(
        "subtask1" to TaskConfigBase(...),
        "subtask2" to TaskConfigBase(...)
    ),
    task_description = "Process multiple items",
    task_dependencies = listOf("previousTask")
)
```


### Behavior
1. The task iterates through each item in `foreach_items`
2. For each item:
   - Creates a copy of the subtasks defined in `foreach_subplan`
   - Modifies each subtask's description to include the current item
   - Executes all subtasks according to their dependencies
3. Progress is tracked and displayed in the UI
4. Task completes when all items have been processed


### Use Cases
- Processing multiple files with the same workflow
- Applying transformations to a list of items
- Executing the same analysis on different data sets


### Notes
- All subtasks must be properly configured with valid task types
- Dependencies between subtasks are maintained for each iteration
- Task execution is sequential across items but may be parallel within each item's subtasks
- Progress can be monitored through the UI interface


### Error Handling
The task will throw exceptions if:
- No items are specified (`foreach_items` is null)
- No subtasks are specified (`foreach_subplan` is null)

# GitHubSearchTask.kt


## GitHub Search Task Documentation


### Overview
The GitHub Search Task is a powerful tool that allows you to search across GitHub's vast repository of code, commits, issues, repositories, topics, and users. This task integrates directly with GitHub's API to provide comprehensive search results in a well-formatted, readable output.


### Features
- Search across multiple GitHub content types:
  - Repositories
  - Code
  - Commits
  - Issues
  - Users
  - Topics
- Customizable search parameters
- Formatted results with relevant details
- Support for sorting and ordering results


### Configuration Parameters


#### Required Parameters
- `search_query`: The search term or query you want to look for
- `search_type`: The type of GitHub content to search. Valid options:
  - `code`
  - `commits`
  - `issues`
  - `repositories`
  - `topics`
  - `users`
- `per_page`: Number of results to return (maximum 100)


#### Optional Parameters
- `sort`: Sort results by specific criteria (varies by search type)
- `order`: Sort direction (`asc` or `desc`)


### Result Format

The search results are formatted in Markdown and include:
- Total count of results found
- Top results (up to 10) with detailed information based on the search type


#### Type-Specific Result Details


##### Repositories
- Repository name
- Description
- Star count
- Fork count
- GitHub link


##### Code
- Repository name
- File path
- Code snippet
- Link to file


##### Commits
- Repository name
- Commit message
- Author
- Date
- Link to commit


##### Issues
- Title
- State
- Comment count
- Creator
- Creation date
- Link to issue


##### Users
- Username
- Account type
- Public repository count
- Avatar
- Profile link


##### Topics
- Topic name
- Description
- Featured/Curated status
- Link to topic


### Example Usage

```kotlin
val searchTask = GitHubSearchTask(
    planSettings = myPlanSettings,
    planTask = GitHubSearchTaskConfigData(
        search_query = "language:kotlin stars:>1000",
        search_type = "repositories",
        per_page = 10,
        sort = "stars",
        order = "desc"
    )
)
```


### Notes
- Requires a valid GitHub API token in the plan settings
- API rate limits may apply based on your GitHub account type
- Search results are limited to the first 1000 matches by GitHub's API
- Complex search queries are supported using GitHub's search syntax


### Error Handling
The task will throw a RuntimeException if:
- The GitHub API request fails
- The response status code is not 200
- The API token is invalid or missing

For more information about GitHub's search syntax and capabilities, refer to the [GitHub Search Documentation](https://docs.github.com/en/search-github).

# GoogleSearchTask.kt

Here's the user documentation for the GoogleSearchTask class:


## GoogleSearchTask Documentation


### Overview
`GoogleSearchTask` is a task implementation that performs Google web searches and formats the results. It integrates with Google's Custom Search API to fetch search results programmatically.


### Configuration


#### Required Settings
The task requires the following settings in `PlanSettings`:
- `googleApiKey` - Your Google API key
- `googleSearchEngineId` - Your Google Custom Search Engine ID


#### Task Configuration Parameters
When creating a GoogleSearchTask, you need to specify:

- `search_query` (String): The search terms to query Google with
- `num_results` (Int): Number of results to return (maximum 10)
- `task_description` (String, optional): Description of the search task
- `task_dependencies` (List<String>, optional): List of dependent task IDs
- `state` (TaskState, optional): Current state of the task


### Usage Example

```kotlin
val taskConfig = GoogleSearchTaskConfigData(
    search_query = "artificial intelligence latest developments",
    num_results = 5
)

val searchTask = GoogleSearchTask(planSettings, taskConfig)
```


### Output Format
The task returns results in Markdown format with the following structure:

```markdown

## Google Search Results


## 1. [Title of Result](URL of Result)
Snippet of the search result
Pagemap:
```json
{pagemap data}
```


## 2. [Title of Result](URL of Result)
...
```


### Features
- Performs Google web searches using the Custom Search API
- Returns formatted results with titles, URLs, and snippets
- Includes pagemap metadata for each result
- Supports up to 10 results per search
- Results are rendered in Markdown format for easy display


### Error Handling
- Throws RuntimeException if the Google API request fails
- Handles cases where no results are found
- Properly encodes search queries for URL safety


### Dependencies
- Requires Jackson for JSON processing
- Uses Java's HttpClient for API requests
- Integrates with MarkdownUtil for result rendering


### Notes
- Ensure your Google API key and Search Engine ID are properly configured
- Be mindful of API usage limits and quotas
- The maximum number of results per request is 10

# PlanCoordinator.kt

Here's the user documentation for the PlanCoordinator class:


## PlanCoordinator Documentation

The PlanCoordinator class manages the execution of complex task plans in a coordinated and parallel manner. It handles task dependencies, execution state tracking, and provides visual feedback through a web UI.


### Overview

PlanCoordinator orchestrates the execution of task plans where:
- Tasks can have dependencies on other tasks
- Tasks are executed in parallel when possible
- Progress is displayed visually through a web UI
- Task execution is logged and monitored


### Key Components


#### Constructor Parameters

- `user`: The user executing the plan
- `session`: The current session context
- `dataStorage`: Interface for data storage operations
- `ui`: Interface for UI interactions
- `planSettings`: Configuration settings for plan execution
- `root`: Root path for file operations


#### Key Properties

- `files`: Array of files in the root directory
- `codeFiles`: Map of relative paths to file contents
- `pool`: Thread pool for executing tasks


### Main Operations


#### Executing Task Breakdowns

```kotlin
fun executeTaskBreakdownWithPrompt(
    jsonInput: String,
    api: API,
    api2: OpenAIClient, 
    task: SessionTask
)
```

Executes a task breakdown from JSON input that includes:
- A prompt describing the overall task
- A plan text describing the breakdown
- The actual task plan to execute


#### Executing Plans

```kotlin
fun executePlan(
    plan: Map<String, TaskConfigBase>,
    task: SessionTask,
    userMessage: String,
    api: API,
    api2: OpenAIClient
): PlanProcessingState
```

Executes a task plan with:
- Visual feedback through a dependency graph
- Parallel task execution
- Progress tracking
- Error handling


### Visual Components

The coordinator provides visual feedback through:

1. Task Dependency Graph
   - Shows relationships between tasks
   - Updates as tasks complete
   
2. Task Tabs Interface
   - Shows individual task status
   - Displays task details and progress
   - Indicates task state (pending, in progress, completed)


### Usage Example

```kotlin
val coordinator = PlanCoordinator(
    user = currentUser,
    session = currentSession,
    dataStorage = storageImpl,
    ui = uiImpl,
    planSettings = settings,
    root = projectRoot
)

// Execute a task breakdown
coordinator.executeTaskBreakdownWithPrompt(
    jsonInput = taskJson,
    api = openAIApi,
    api2 = openAIClient,
    task = sessionTask
)
```


### Error Handling

The coordinator:
- Logs errors during task execution
- Displays errors in the UI
- Continues executing other tasks when possible
- Maintains overall plan state even if individual tasks fail


### Best Practices

1. Ensure task dependencies are correctly specified
2. Monitor the task dependency graph for execution flow
3. Check individual task logs for detailed progress
4. Use appropriate timeout settings for long-running plans
5. Handle task failures appropriately in the calling code


### Limitations

- Tasks must complete within configured timeout periods
- All task dependencies must be explicitly declared
- Memory usage scales with number of concurrent tasks

This documentation provides an overview of the PlanCoordinator's capabilities and usage. For specific implementation details, refer to the code comments and API documentation.

# PlanProcessingState.kt

Here's the user documentation for the `PlanProcessingState` data class:


## PlanProcessingState Documentation

`PlanProcessingState` is a data class that manages the state of a task execution plan. It keeps track of tasks, their execution status, results, and dependencies.


### Properties


#### subTasks
- Type: `Map<String, TaskConfigBase>`
- A map of task IDs to their corresponding task configurations


#### tasksByDescription  
- Type: `MutableMap<String?, TaskConfigBase>`
- Maps task descriptions to their configurations
- Initialized from subTasks entries


#### taskIdProcessingQueue
- Type: `MutableList<String>` 
- Ordered list of task IDs representing the execution sequence
- Generated based on task dependencies using `PlanUtil.executionOrder()`


#### taskResult
- Type: `MutableMap<String, String>`
- Stores the results/output of completed tasks by task ID


#### completedTasks
- Type: `MutableList<String>`
- List of task IDs that have finished execution


#### taskFutures
- Type: `MutableMap<String, Future<*>>`
- Maps task IDs to their corresponding Future objects for async execution tracking


#### uitaskMap
- Type: `MutableMap<String, SessionTask>`
- Maps task IDs to their UI session task representations


### Usage

This class is used to:
- Track the execution state of a set of interdependent tasks
- Maintain execution order based on dependencies
- Store task results and completion status
- Link tasks to their UI representations
- Enable asynchronous task execution monitoring

The state is typically managed by a task coordinator/executor that updates the various collections as tasks progress through their lifecycle.

Example:
```kotlin
val state = PlanProcessingState(
    subTasks = mapOf(
        "task1" to Task1Config(),
        "task2" to Task2Config()
    )
)

// Track task completion
state.completedTasks.add("task1")
state.taskResult["task1"] = "Task 1 output"
```

# PlanSettings.kt

Here's the user documentation for the PlanSettings class:


## PlanSettings Documentation


### Overview
`PlanSettings` is a configuration class that manages settings for AI-powered task planning and execution. It controls various aspects like model selection, command execution, task types, and integration settings.


### Key Properties


#### Model Settings
- `defaultModel`: The default ChatModel to use for general tasks
- `parsingModel`: The ChatModel used specifically for parsing
- `temperature`: Controls randomness in model outputs (0.0-1.0, default 0.2)
- `budget`: Maximum cost allowance for operations (default 2.0)


#### Command Execution
- `command`: Shell command to use (`powershell` on Windows, `bash` on Unix)
- `autoFix`: Enable/disable automatic fixing of command issues
- `allowBlocking`: Allow/prevent blocking operations
- `commandAutoFixCommands`: List of commands used for auto-fixing
- `env`: Environment variables for command execution
- `workingDir`: Working directory for commands
- `language`: Shell language to use (defaults to `powershell`/`bash`)


#### Integration Settings
- `githubToken`: GitHub API authentication token
- `googleApiKey`: Google API authentication key
- `googleSearchEngineId`: Google Custom Search Engine ID


#### Task Settings
- `taskSettings`: Map of task type names to their settings
- Each task type can be individually configured with:
  - Enabled/disabled status
  - Custom model overrides
  - Task-specific parameters


### Usage Example

```kotlin
val settings = PlanSettings(
    defaultModel = ChatModel("gpt-4"),
    parsingModel = ChatModel("gpt-3.5-turbo"),
    temperature = 0.3,
    autoFix = true,
    workingDir = "./project",
    env = mapOf(
        "NODE_ENV" to "development"
    )
)

// Configure specific task types
settings.setTaskSettings(
    TaskType.FileModification,
    TaskSettingsBase("FileModification", enabled = true)
)

// Create a planning actor
val planningActor = settings.planningActor()
```


### Task Planning

The `planningActor()` method creates an AI actor that can break down user requests into smaller, actionable tasks. The planner:

1. Analyzes the user request
2. Identifies required subtasks
3. Determines dependencies between tasks
4. Specifies input/output files
5. Provides implementation details

The planner supports various task types including:
- File modifications
- Command execution
- Inquiries
- Custom task types


### Customization

You can extend `PlanSettings` to:
- Add new task types
- Customize the YAML description format
- Modify planning behavior
- Add additional configuration options

Use the `copy()` method to create modified instances while preserving immutability.


### Best Practices

1. Set appropriate temperature values:
   - Lower (0.1-0.3) for deterministic tasks
   - Higher (0.4-0.8) for creative tasks

2. Configure task settings based on project needs:
   - Disable unnecessary task types
   - Enable required capabilities
   - Set appropriate models per task type

3. Manage authentication tokens securely:
   - Use environment variables
   - Implement proper secret management
   - Rotate tokens regularly

4. Monitor budget usage to prevent unexpected costs

5. Test configurations in non-production environments first

# PlanUtil.kt

Here's the user documentation for the PlanUtil object:


## PlanUtil Documentation

`PlanUtil` is a utility object that provides functionality for handling task planning and visualization in the SkyeNet application.


### Key Features


#### Task Visualization

1. `diagram(ui: ApplicationInterface, taskMap: Map<String, TaskConfigBase>)`
   - Generates a Mermaid diagram visualization of task dependencies
   - Returns rendered markdown containing the diagram
   - Parameters:
     - `ui`: Application interface for rendering
     - `taskMap`: Map of task IDs to task configurations

2. `render(withPrompt: TaskBreakdownWithPrompt, ui: ApplicationInterface)`
   - Creates a tabbed display showing:
     - Text view of the plan
     - JSON representation
     - Mermaid diagram visualization
   - Parameters:
     - `withPrompt`: Task breakdown with associated prompt
     - `ui`: Application interface for rendering


#### Task Management

1. `executionOrder(tasks: Map<String, TaskConfigBase>)`
   - Determines the correct execution order for tasks based on dependencies
   - Returns a list of task IDs in execution order
   - Throws RuntimeException if circular dependencies are detected

2. `filterPlan(retries: Int = 3, fn: () -> Map<String, TaskConfigBase>?)`
   - Filters and validates a task plan
   - Handles:
     - Removal of invalid dependencies
     - Validation of TaskPlanning tasks
     - Circular dependency detection
   - Parameters:
     - `retries`: Number of retry attempts (default: 3)
     - `fn`: Function providing the task map to filter

3. `getAllDependencies(subPlanTask: TaskConfigBase, subTasks: Map<String, TaskConfigBase>, visited: MutableSet<String>)`
   - Recursively collects all dependencies for a given task
   - Parameters:
     - `subPlanTask`: Task to analyze
     - `subTasks`: Map of all available tasks
     - `visited`: Set of already visited task IDs


#### Visualization Styling

The system supports different task states and types with distinct visual styling:

- Task States:
  - Completed: Green fill
  - In Progress: Orange fill
  - Pending: Default styling

- Task Types:
  - NewFile: Light blue
  - EditFile: Light green
  - Documentation: Light yellow
  - Inquiry: Orange
  - TaskPlanning: Light grey


### Usage Example

```kotlin
// Create task map
val tasks = mapOf(
    "task1" to TaskConfigBase(task_type = "NewFile", task_description = "Create config file"),
    "task2" to TaskConfigBase(task_type = "EditFile", task_description = "Update settings", 
                            task_dependencies = listOf("task1"))
)

// Generate visualization
val diagram = PlanUtil.diagram(applicationInterface, tasks)

// Get execution order
val order = PlanUtil.executionOrder(tasks)
```


### Notes

- The system uses Mermaid diagrams for visualization
- Task dependencies are represented as directed graphs
- The system includes caching for diagram generation
- Windows-specific handling is included via the `isWindows` property


### Error Handling

- Circular dependencies are detected and will throw exceptions
- Invalid task configurations are filtered out
- The system includes retry mechanisms for handling temporary issues
- Caching includes both successful results and exceptions

This utility is essential for managing and visualizing task dependencies in the SkyeNet application, providing both practical task management and clear visual representation of task relationships.

# Planner.kt

Here's the user documentation for the Planner class:


## Planner Class Documentation

The `Planner` class is responsible for creating and managing task plans based on user input and code files. It provides functionality to break down tasks into manageable subtasks and generate execution plans.


### Key Features

- Creates initial task plans based on user messages and code files
- Supports both blocking and non-blocking plan generation
- Integrates with chat-based AI models for plan generation
- Handles file-based inputs and logging


### Main Methods


#### initialPlan

```kotlin
fun initialPlan(
    codeFiles: Map<Path, String>,
    files: Array<File>, 
    root: Path,
    task: SessionTask,
    userMessage: String,
    ui: ApplicationInterface,
    planSettings: PlanSettings,
    api: API
): TaskBreakdownWithPrompt
```

Creates an initial task breakdown plan based on:
- Code files and their contents
- User's task description
- Planning settings
- API configuration


##### Parameters:
- `codeFiles`: Map of file paths to their contents
- `files`: Array of files to be processed
- `root`: Root path for relative file references
- `task`: Current session task
- `userMessage`: User's input message describing the task
- `ui`: Application interface for rendering
- `planSettings`: Configuration settings for planning
- `api`: API client for AI model interaction


#### newPlan

```kotlin
fun newPlan(
    api: API,
    planSettings: PlanSettings,
    inStrings: List<String>
): ParsedResponse<Map<String, TaskConfigBase>>
```

Generates a new plan using the configured planning actor.


##### Parameters:
- `api`: API client for model interaction
- `planSettings`: Planning configuration settings
- `inStrings`: List of input strings to process


### Usage Example

```kotlin
val planner = Planner()
val plan = planner.initialPlan(
    codeFiles = mapOf(/* file paths to contents */),
    files = arrayOf(/* files */),
    root = Path.of("/project"),
    task = sessionTask,
    userMessage = "Refactor the authentication module",
    ui = applicationInterface,
    planSettings = PlanSettings(allowBlocking = true),
    api = apiClient
)
```


### Configuration

The planner can be configured through `PlanSettings` to:
- Enable/disable blocking execution
- Customize planning actor behavior
- Configure API interactions


### Notes

- Logs are automatically created for API interactions
- Plans can be discussed and revised when blocking mode is enabled
- File contents are included in plan generation context when relevant

For more detailed information about specific configurations and advanced usage, please refer to the API documentation.

# PlanningTask.kt

Here's the user documentation for the PlanningTask class:


## PlanningTask Documentation


### Overview
The `PlanningTask` class is responsible for breaking down complex tasks into smaller, manageable subtasks and coordinating their execution. It's a crucial component for task planning and orchestration within the system.


### Key Features


#### Task Planning Capabilities
- Decomposes large goals into smaller actionable tasks
- Establishes dependencies between tasks
- Optimizes for parallel execution where possible
- Dynamically refines plans based on new information
- Ensures proper information flow between tasks


#### Task Configuration
The task is configured using `PlanningTaskConfigData` which includes:
- Task description
- Task dependencies
- Task state (default: Pending)


#### Execution Modes


##### Blocking Mode
When `allowBlocking` is true and `autoFix` is false:
- Enables interactive planning
- Allows user review and modification of the plan
- Provides discussion capabilities for plan refinement


##### Auto Mode
When `allowBlocking` is false or `autoFix` is true:
- Automatically generates and executes the plan
- No user intervention required
- Faster execution but less control


### Usage Example

```kotlin
// Create planning settings
val planSettings = PlanSettings(
    allowBlocking = true,
    autoFix = false
    // ... other settings
)

// Create task configuration
val taskConfig = PlanningTaskConfigData(
    task_description = "Build a web application",
    task_dependencies = listOf("setup_environment", "design_database")
)

// Initialize planning task
val planningTask = PlanningTask(planSettings, taskConfig)

// Execute the task through a PlanCoordinator
planCoordinator.execute(planningTask)
```


### Task Breakdown Process

1. **Initial Planning**
   - Analyzes the main task description
   - Identifies key components and dependencies
   - Creates a preliminary task structure

2. **Dependency Management**
   - Establishes relationships between tasks
   - Ensures proper information flow
   - Optimizes for parallel execution

3. **Execution Coordination**
   - Manages subtask execution order
   - Monitors task completion
   - Handles task state transitions


### Best Practices

1. **Task Description**
   - Be specific and clear about the goal
   - Include relevant context
   - Specify any constraints or requirements

2. **Dependencies**
   - Clearly define task prerequisites
   - Avoid circular dependencies
   - Keep dependency chains manageable

3. **Planning Strategy**
   - Use blocking mode for complex tasks requiring oversight
   - Use auto mode for well-defined, routine tasks
   - Monitor task execution progress


### Notes
- Task planning is focused on organization and structure, not execution
- Each subtask should have clear inputs and outputs
- The system supports dynamic plan refinement based on new information
- Visual diagrams are generated to show task relationships


### Error Handling
- Task failures are logged and can be reviewed
- The system supports task retries and plan modifications
- Error states are preserved for debugging

This documentation provides a high-level overview of the PlanningTask functionality. For more detailed technical information, please refer to the code comments and API documentation.

# RunShellCommandTask.kt

Here's the user documentation for the RunShellCommandTask class:


## RunShellCommandTask Documentation


### Overview
The `RunShellCommandTask` class is responsible for executing shell commands within the application. It provides a safe and controlled environment for running system commands while handling outputs and errors gracefully.


### Configuration


#### RunShellCommandTaskConfigData
When creating a shell command task, you can configure it using the following parameters:

- `command` (String): The shell command to be executed
- `workingDir` (String): The relative file path of the working directory where the command will be executed
- `task_description` (String): A description of what the task should accomplish
- `task_dependencies` (List<String>): List of tasks that must be completed before this task runs
- `state` (TaskState): Current state of the task


### Usage


#### Basic Example
```kotlin
val taskConfig = RunShellCommandTaskConfigData(
    command = "ls -la",
    workingDir = "./myproject",
    task_description = "List all files in the project directory"
)

val task = RunShellCommandTask(planSettings, taskConfig)
```


#### Features

1. **Command Execution**
   - Executes shell commands in a controlled environment
   - Captures and displays command output
   - Handles errors gracefully

2. **Working Directory**
   - Allows specification of working directory for command execution
   - Supports both relative and absolute paths

3. **Interactive UI**
   - Provides interactive buttons for command execution
   - Displays command output in formatted text
   - Offers options to accept or revise results

4. **Safety Features**
   - Designed for running simple and safe commands
   - Includes built-in safeguards against harmful commands
   - Executes in isolated environment


### Best Practices

1. **Command Safety**
   - Only execute trusted and necessary commands
   - Avoid commands that could harm the system
   - Don't include sensitive information in commands

2. **Working Directory**
   - Always specify a working directory when needed
   - Use relative paths when possible for portability
   - Ensure the working directory exists before execution

3. **Error Handling**
   - Check command output for errors
   - Handle exceptions appropriately
   - Validate input parameters before execution


### Limitations

- Designed for simple command execution only
- May have restricted access to system resources
- Not suitable for long-running processes
- Should not be used for security-sensitive operations


### Security Considerations

- Commands are executed with limited privileges
- Avoid executing commands with elevated permissions
- Don't use for sensitive operations or data access
- Validate and sanitize all command inputs


### Notes

- The task runs asynchronously and provides feedback through the UI
- Results can be accepted or revised through the interactive interface
- Task dependencies must be completed before execution
- Supports various shell languages based on system configuration

Remember to always validate and test commands in a safe environment before executing them in production.

# SearchTask.kt

Here's the user documentation for the SearchTask class:


## SearchTask Documentation


### Overview
SearchTask is a utility class that performs pattern-based searches across files in a project directory. It can search using either simple substrings or regular expressions and provides contextual lines around each match.


### Configuration Options

The search task can be configured with the following parameters:


#### SearchTaskConfigData
- `search_pattern` (Required): The text pattern to search for in files
- `is_regex` (Optional): Boolean flag indicating if the search pattern is a regular expression (default: false)
- `context_lines` (Optional): Number of lines to show before and after each match (default: 2)
- `input_files` (Optional): List of file patterns to search within (uses glob syntax)


### Usage Example

```kotlin
val searchConfig = SearchTaskConfigData(
    search_pattern = "TODO:",           // Search for TODO comments
    is_regex = false,                   // Using simple substring match
    context_lines = 3,                  // Show 3 lines before and after
    input_files = listOf("src/**/*.kt") // Search Kotlin files in src directory
)

val searchTask = SearchTask(planSettings, searchConfig)
```


### Output Format

The search results are formatted in Markdown with the following structure:

```markdown

## Search Results


### [File Path]


#### Line [Number]

```
 [Line Number - Context]: [Content]
 [Line Number - Context]: [Content]
>[Line Number - Match]: [Matching Line]
 [Line Number - Context]: [Content]
 [Line Number - Context]: [Content]
```
```

- Results are grouped by file
- Each match shows the line number and surrounding context
- The matching line is prefixed with '>' for easy identification
- Context lines are prefixed with spaces


### Features

1. **Flexible Search Options**
   - Simple substring matching
   - Regular expression support
   - Configurable context display

2. **File Filtering**
   - Glob pattern support for file selection
   - Built-in filtering for binary and non-text files
   - Relative path display for cleaner output

3. **Organized Results**
   - Hierarchical output structure
   - File-based grouping
   - Line number references
   - Context preservation


### Notes

- The search is case-sensitive
- Only text files that are suitable for LLM processing are included
- File paths are displayed relative to the project root
- The search respects the project's file validation rules

This task is particularly useful for code review, refactoring planning, and documentation audits.

# SeleniumSessionTask.kt


## SeleniumSessionTask Documentation


### Overview
The `SeleniumSessionTask` class provides functionality to create and manage browser automation sessions using Selenium WebDriver. It allows you to execute JavaScript commands in a browser context, either in a new session or by reusing an existing one.


### Features
- Headless Chrome browser automation
- JavaScript command execution
- Session management (create new/reuse existing)
- Configurable timeouts
- Automatic session cleanup
- Detailed execution results


### Configuration Options


#### SeleniumSessionTaskConfigData Parameters
- `url` (String): The URL to navigate to (optional if reusing existing session)
- `commands` (List<String>): JavaScript commands to execute sequentially
- `sessionId` (String?): ID for reusing existing sessions (optional)
- `timeout` (Long): Command execution timeout in milliseconds (default: 30000)
- `closeSession` (Boolean): Whether to close the session after execution


### Example JavaScript Commands

```javascript
// Get page title
return document.title;

// Get element text
return document.querySelector('.my-class').textContent;

// Get all links
return Array.from(document.querySelectorAll('a')).map(a => a.href);

// Click an element
document.querySelector('#my-button').click();

// Scroll to bottom
window.scrollTo(0, document.body.scrollHeight);

// Get entire page HTML
return document.documentElement.outerHTML;

// Async operation with timeout
return new Promise(r => setTimeout(() => r(document.title), 1000));
```


### Usage Notes

1. **Session Management**:
   - Maximum 10 concurrent sessions allowed
   - Sessions can be reused by specifying a sessionId
   - Inactive sessions are automatically cleaned up

2. **Error Handling**:
   - Commands should include proper error handling
   - Use appropriate waits for dynamic content
   - Failed commands return error messages with stack traces

3. **Results**:
   - Each command execution produces detailed results
   - Results include command output, page source, and session info
   - Large outputs are automatically truncated for readability


### Best Practices

1. Always use appropriate timeouts for dynamic content
2. Close sessions when no longer needed
3. Handle asynchronous operations properly
4. Use descriptive sessionIds when planning to reuse sessions
5. Include error handling in JavaScript commands


### Limitations

- Headless Chrome browser only
- Maximum 10 concurrent sessions
- Command results and page source are truncated for large outputs
- Sessions may be cleaned up if inactive


### Example Usage

```kotlin
val config = SeleniumSessionTaskConfigData(
    url = "https://example.com",
    commands = listOf(
        "return document.title;",
        "document.querySelector('.button').click();",
        "return document.querySelector('.result').textContent;"
    ),
    timeout = 5000,
    closeSession = true
)
```

# TaskBreakdownWithPrompt.kt

Here's the user documentation for the TaskBreakdownWithPrompt data class:


## TaskBreakdownWithPrompt

A data class that represents a task breakdown structure with an associated prompt and plan details.


### Overview

`TaskBreakdownWithPrompt` is used to store the components of a task breakdown, including:

- The original prompt that generated the plan
- A structured plan mapping task names to configurations 
- A text representation of the plan


### Properties

| Property | Type | Description |
|----------|------|-------------|
| prompt | String | The original prompt or instruction that was used to generate this task breakdown |
| plan | Map<String, TaskConfigBase> | A map containing task names as keys and their corresponding task configurations as values |
| planText | String | A human-readable text representation of the plan |


### Usage

This class is typically used in planning and task management contexts where you need to:

- Store both the original instructions (prompt) and resulting plan together
- Maintain a structured representation of tasks (via the plan map)
- Keep a human-readable version of the plan (planText)


### Example

```kotlin
val taskBreakdown = TaskBreakdownWithPrompt(
    prompt = "Create a web application",
    plan = mapOf(
        "Setup" to TaskConfig(...),
        "Implementation" to TaskConfig(...)
    ),
    planText = """
        1. Setup
           - Initialize project
           - Configure environment
        2. Implementation
           - Create components
           - Add features
    """.trimIndent()
)
```

This data class is immutable and provides standard data class functionality like equality checking and string representation.

# TaskConfigBase.kt

Here's the user documentation for the TaskConfigBase class:


## TaskConfigBase Documentation


### Overview
`TaskConfigBase` is an abstract base class that provides the foundation for task configuration in a task planning system. It implements custom JSON type handling and includes essential properties for task management and dependency tracking.


### Key Properties


#### task_type
- Type: String
- Description: Specifies the type of task to be executed
- Must correspond to a value from the TaskType enumeration
- Required field


#### task_description
- Type: String
- Description: Detailed description of the specific task to be performed
- Should include:
  - The task's role in the overall plan
  - Dependencies and relationships with other tasks
- Optional field


#### task_dependencies
- Type: List<String>
- Description: List of task IDs that must be completed before this task
- Used to:
  - Define upstream dependencies
  - Ensure proper task execution order
  - Manage information flow between tasks
- Optional field


#### state
- Type: AbstractTask.TaskState
- Description: Tracks the current execution state of the task
- Used for:
  - Task coordination
  - Dependency management
- Optional field


### JSON Serialization
The class uses custom JSON type handling through:
- `@JsonTypeIdResolver` annotation with `PlanTaskTypeIdResolver`
- `@JsonTypeInfo` annotation for custom type identification
- Type information is stored in the "task_type" property


### Usage Example
```json
{
  "task_type": "EXAMPLE_TYPE",
  "task_description": "Process data from upstream tasks and generate report",
  "task_dependencies": ["task-001", "task-002"],
  "state": "PENDING"
}
```


### Notes
- All tasks must specify a valid task_type that corresponds to the TaskType enum
- Dependencies should be carefully managed to avoid circular references
- State management is handled automatically by the task execution system


### See Also
- TaskType enumeration
- AbstractTask class
- Task execution system documentation

# TaskSettingsBase.kt

Here's the user documentation for the `TaskSettingsBase` class:


## TaskSettingsBase Documentation


### Overview
`TaskSettingsBase` is a base class for task settings in the SkyeNet planning system. It provides core functionality for task configuration and type resolution through JSON serialization/deserialization.


### Properties

| Property | Type | Description |
|----------|------|-------------|
| task_type | String | Identifies the type of task these settings are for |
| enabled | Boolean | Controls whether the task is enabled (default: false) |
| model | ChatModel? | The chat model to be used for this task (optional) |


### Usage

The `TaskSettingsBase` class is meant to be extended by specific task setting implementations. Here's an example:

```kotlin
class MyTaskSettings(
    task_type: String,
    enabled: Boolean = false,
    model: ChatModel? = null,
    // Additional task-specific settings...
) : TaskSettingsBase(task_type, enabled, model)
```


### JSON Serialization

The class uses custom JSON type handling through the `@JsonTypeIdResolver` and `@JsonTypeInfo` annotations. This allows for proper serialization/deserialization of different task setting types based on the `task_type` field.

Example JSON:
```json
{
  "task_type": "MY_TASK",
  "enabled": true,
  "model": {
    // ChatModel properties
  }
}
```


### Type Resolution

The included `PlanTaskTypeIdResolver` handles type resolution for JSON serialization/deserialization by:
1. Converting task types to/from string IDs
2. Mapping string IDs to appropriate Java types using the `TaskType` enum
3. Supporting custom type resolution mechanisms


### Notes

- Task types must be defined in the `TaskType` enum
- The `enabled` flag can be used to conditionally activate/deactivate tasks
- The `model` property is optional and can be null
- Type resolution errors will throw `IllegalArgumentException`

This class is fundamental to the task configuration system and should be used as the base for all task-specific setting classes.

# TaskType.kt

Here's the user documentation for the TaskType class:


## TaskType Documentation


### Overview
`TaskType` is a class that represents different types of tasks that can be executed within the planning system. It provides a type-safe way to define, register, and manage various task types along with their configuration and settings.


### Task Types
The system supports the following task types:


#### Planning Tasks
- **TaskPlanning**: Handles task planning and organization
- **ForeachTask**: Executes operations on multiple items


#### File Operations
- **FileModification**: Handles file modification operations
- **Documentation**: Generates and manages documentation
- **CodeReview**: Performs code review tasks
- **TestGeneration**: Generates test cases
- **Optimization**: Handles code optimization
- **SecurityAudit**: Performs security auditing
- **PerformanceAnalysis**: Analyzes performance
- **RefactorTask**: Handles code refactoring


#### Search and Knowledge
- **Search**: General search functionality
- **EmbeddingSearch**: Searches using embeddings
- **GitHubSearch**: Searches GitHub repositories
- **GoogleSearch**: Performs Google searches
- **KnowledgeIndexing**: Indexes knowledge bases
- **WebSearchAndIndex**: Combined web search and indexing


#### Command and Shell Operations
- **RunShellCommand**: Executes shell commands
- **CommandAutoFix**: Automatically fixes command issues
- **CommandSession**: Manages command sessions
- **SeleniumSession**: Handles Selenium browser automation sessions


#### Web Operations
- **WebFetchAndTransform**: Fetches and transforms web content
- **Inquiry**: Handles inquiry-based tasks


### Usage


#### Getting Available Task Types
```kotlin
val availableTypes = TaskType.getAvailableTaskTypes(planSettings)
```


#### Creating a Task Implementation
```kotlin
val taskImpl = TaskType.getImpl(planSettings, taskType, planTask)
```


#### Checking if a Task Type is Enabled
```kotlin
if (planSettings.getTaskSettings(taskType).enabled) {
    // Task type is enabled
}
```


### Important Notes
- Tasks must be enabled in the plan settings before they can be used
- Each task type has associated configuration and settings classes
- Attempting to use a disabled task will throw a `DisabledTaskException`
- Task types are registered at initialization and cannot be modified at runtime


### Error Handling
- Invalid task types will throw a `RuntimeException`
- Disabled tasks will throw a `DisabledTaskException`
- Missing task type specifications will throw a `RuntimeException`

This documentation provides a high-level overview of the TaskType system. For specific implementation details of each task type, please refer to their individual documentation.

# WebFetchAndTransformTask.kt

Here's the user documentation for the WebFetchAndTransformTask class:


## WebFetchAndTransformTask Documentation


### Overview
The WebFetchAndTransformTask is a specialized task that fetches content from a web URL, strips the HTML formatting, and transforms the content according to specified goals. This is useful for extracting and reformatting web content for various purposes.


### Configuration


#### Required Parameters
- `url`: The web URL from which to fetch content
- `transformationGoal`: A description of how you want the content to be transformed or formatted


#### Optional Parameters
- `task_description`: A description of the task (optional)
- `task_dependencies`: List of dependent task IDs (optional)
- `state`: Current state of the task (optional)


### Usage Example

```kotlin
val taskConfig = WebFetchAndTransformTaskConfigData(
    url = "https://example.com/article",
    transformationGoal = "Summarize the main points in bullet format"
)

val task = WebFetchAndTransformTask(
    planSettings = yourPlanSettings,
    planTask = taskConfig
)
```


### How It Works

1. **Web Content Fetching**: 
   - The task fetches the HTML content from the specified URL
   - Uses Apache HttpClient for reliable web requests

2. **HTML Processing**:
   - Strips unnecessary HTML elements (scripts, styles, iframes, etc.)
   - Removes comments and data attributes
   - Cleans up empty elements
   - Converts relative URLs to absolute URLs
   - Limits content size to prevent memory issues (max 100KB by default)

3. **Content Transformation**:
   - Processes the cleaned content according to the specified transformation goal
   - Uses AI model specified in plan settings to transform the content
   - Returns formatted results based on the transformation goal


### Features

- **Smart HTML Cleaning**: Intelligently removes unnecessary HTML while preserving important content
- **Content Size Management**: Automatically handles large content through truncation
- **Flexible Transformation**: Can transform content into various formats based on the specified goal
- **Progress Tracking**: Integrates with the session task system for progress monitoring


### Best Practices

1. **URL Selection**:
   - Ensure the URL is accessible and contains the desired content
   - Verify the website allows content scraping

2. **Transformation Goals**:
   - Be specific about the desired output format
   - Consider the content type when specifying transformation goals

3. **Content Size**:
   - Be aware of the 100KB content limit
   - For large websites, consider focusing on specific sections


### Error Handling

The task includes error handling for:
- Invalid URLs
- Network connection issues
- Malformed HTML content
- Content size limitations


### Integration

This task integrates with:
- Plan Coordinator system
- Session management
- Markdown rendering
- AI transformation services


### Limitations

- Maximum content size: 100KB
- Requires active internet connection
- May be affected by website's robots.txt policies
- Some dynamic content may not be captured

# file\AbstractAnalysisTask.kt

Here's the user documentation for the AbstractAnalysisTask class:


## AbstractAnalysisTask Documentation


### Overview
`AbstractAnalysisTask` is an abstract base class for file analysis tasks in the SkyeNet planning system. It provides a framework for analyzing code files and applying suggested changes based on the analysis results.


### Key Features
- Performs code analysis using AI-powered actors
- Automatically applies suggested changes to files
- Configurable model and temperature settings
- Integrated with the planning system


### Usage


#### Creating a Custom Analysis Task
To create a custom analysis task, extend `AbstractAnalysisTask` and implement the required abstract members:

```kotlin
class MyAnalysisTask(
    planSettings: PlanSettings,
    planTask: FileTaskConfigBase?
) : AbstractAnalysisTask<FileTaskConfigBase>(planSettings, planTask) {
    override val actorName = "My Analysis"
    override val actorPrompt = "Analyze the following code..."
    
    override fun getAnalysisInstruction(): String {
        return "Please analyze this code for..."
    }
}
```


#### Required Implementations

1. `actorName`: String - Name of the analysis actor
2. `actorPrompt`: String - Base prompt for the AI analysis
3. `getAnalysisInstruction()`: String - Specific instructions for the analysis


#### Configuration

The task uses several settings from the parent `PlanSettings`:

- Model selection from task settings or default model
- Temperature settings for AI responses
- AutoFix settings for applying changes


### Execution Flow

1. The task runs the analysis using an AI actor
2. Analysis results are generated based on the input file code
3. Results are passed to a command patch system
4. Changes are automatically applied if possible
5. Status messages are added to the task


### Status Messages

The task provides feedback through status messages:

- Success: "[ActorName] completed and suggestions have been applied successfully."
- Failure: "[ActorName] completed, but failed to apply suggestions. Exit code: [code]"


### Dependencies

- Requires OpenAI API client
- Integrates with SkyeNet planning system
- Uses CommandPatchApp for applying changes


### Error Handling

- Failed changes are reported with exit codes
- Logging is available through SLF4J


### Best Practices

1. Provide clear, specific analysis instructions
2. Set appropriate temperature for desired analysis precision
3. Test analysis results before enabling auto-fix
4. Monitor task execution through provided status messages

This class is typically used as part of a larger planning system and should be configured according to project-specific requirements.

# file\AbstractFileTask.kt

Here's the user documentation for the AbstractFileTask class:


## AbstractFileTask Documentation


### Overview
`AbstractFileTask` is an abstract base class for tasks that operate on files within a project. It provides functionality for handling file inputs and outputs as part of a larger task planning system.


### Key Features


#### File Configuration
The class uses `FileTaskConfigBase` to configure:
- Input files: Files to be processed by the task
- Output files: Files to be generated by the task
- Task dependencies and state management


#### File Pattern Matching
- Supports glob pattern matching for file selection
- Example: "*.kt" would match all Kotlin files
- Handles both input and output file patterns


#### File Content Processing
- Automatically reads and formats file contents
- Filters for LLM-compatible files
- Provides organized code blocks with file paths as headers


### Usage Example

```kotlin
class MyFileTask(
    planSettings: PlanSettings,
    config: FileTaskConfigBase
) : AbstractFileTask<FileTaskConfigBase>(planSettings, config) {
    // Implement task-specific logic here
}

// Configuration example
val config = FileTaskConfigBase(
    task_type = "my_file_task",
    input_files = listOf("src/**/*.kt"),
    output_files = listOf("build/**/*.class"),
    task_description = "Process Kotlin files"
)
```


### Configuration Parameters


#### FileTaskConfigBase
- `task_type`: String - Unique identifier for the task type
- `task_description`: String? - Optional description of the task
- `task_dependencies`: List<String>? - Optional list of dependent task IDs
- `input_files`: List<String>? - Glob patterns for input files
- `output_files`: List<String>? - Glob patterns for output files
- `state`: TaskState? - Current state of the task (default: Pending)


### File Processing
- Files are processed relative to the project root directory
- Non-readable or invalid files are gracefully handled with logging
- File contents are formatted with markdown-style code blocks


### Best Practices
1. Always specify clear file patterns to limit scope
2. Handle both input and output files appropriately
3. Consider file validation requirements
4. Monitor logs for file processing issues


### Error Handling
- File reading errors are logged but won't crash the task
- Invalid file patterns are safely handled
- Non-LLM compatible files are automatically filtered out

This class is designed to be extended for specific file-based tasks while providing common file handling functionality.

# file\CodeOptimizationTask.kt

Here's the user documentation for the CodeOptimizationTask class:


## CodeOptimizationTask Documentation


### Overview
The `CodeOptimizationTask` class is a specialized task for analyzing and optimizing code files. It focuses on improving code quality through structural improvements, enhanced readability, and better maintainability.


### Configuration


#### CodeOptimizationTaskConfigData Parameters
- `filesToOptimize`: List of file paths that need to be optimized
- `optimizationFocus`: Optional list of specific areas to focus optimization on
- `task_description`: Description of the optimization task
- `task_dependencies`: List of tasks that must be completed before this task
- `input_files`: List of input files needed for the task
- `output_files`: List of files that will be modified/created
- `state`: Current state of the task


### Features

The task analyzes code focusing on:
1. Code structure and organization
2. Readability improvements
3. Maintainability enhancements
4. Proper use of language-specific features
5. Application of design patterns


### Output Format

The task generates output in markdown format including:
- Detailed explanations for each optimization
- Reasons for suggested changes
- Expected benefits
- Potential trade-offs
- Code snippets in diff format showing proposed changes


### Example Usage

```kotlin
val taskConfig = CodeOptimizationTaskConfigData(
    filesToOptimize = listOf("src/main/kotlin/MyClass.kt"),
    optimizationFocus = listOf("code structure", "design patterns"),
    task_description = "Optimize authentication module",
    input_files = listOf("src/main/kotlin/MyClass.kt"),
    output_files = listOf("src/main/kotlin/MyClass.kt")
)

val optimizationTask = CodeOptimizationTask(planSettings, taskConfig)
```


### Best Practices
- Specify clear optimization goals in `optimizationFocus`
- Include all relevant files in `filesToOptimize`
- Review suggested changes carefully before implementation
- Consider dependencies and potential impacts on other code


### Notes
- The task is part of a larger planning system
- Changes are suggested in diff format for easy review
- Task execution depends on configured plan settings

# file\CodeReviewTask.kt

Here's the user documentation for the CodeReviewTask class:


## CodeReviewTask Documentation


### Overview
The `CodeReviewTask` class is a specialized task for performing automated code reviews. It analyzes code files for quality, potential issues, and provides detailed recommendations for improvements.


### Configuration


#### Required Parameters
- `planSettings`: General settings for the planning system
- `planTask`: Configuration data specific to the code review task (optional)


#### Configuration Data (CodeReviewTaskConfigData)
- `filesToReview`: List of specific files to be reviewed
- `focusAreas`: Optional list of specific areas to focus on during the review
- `task_description`: Description of the task
- `task_dependencies`: List of dependent tasks
- `input_files`: List of input files
- `output_files`: List of output files
- `state`: Current state of the task


### Features

The code review analyzes:
1. Code quality and readability
2. Potential bugs or errors 
3. Performance issues
4. Security vulnerabilities
5. Adherence to best practices
6. Areas for improvement/optimization


### Usage Example

```kotlin
val config = CodeReviewTaskConfigData(
    filesToReview = listOf("src/main/kotlin/MyClass.kt", "src/main/kotlin/OtherClass.kt"),
    focusAreas = listOf("performance", "security"),
    task_description = "Review core business logic classes"
)

val reviewTask = CodeReviewTask(planSettings, config)
```


### Output

The task generates a detailed markdown document containing:
- Analysis of specified files
- Issues found with code examples
- Specific recommendations for improvements
- Organized sections for different types of findings


### Notes

- If no specific files are provided, all available files will be reviewed
- Focus areas are optional - if not specified, the review will be comprehensive
- The review follows standard code review best practices and coding standards

The output is designed to be readable and actionable, with clear examples and explanations for each identified issue or recommendation.

# file\DocumentationTask.kt

Here's the documentation for the DocumentationTask class:


## DocumentationTask

`DocumentationTask` is a specialized task class for generating documentation for code files. It extends `AbstractFileTask` and is designed to work within a planning system for automated code documentation generation.


### Purpose

The main purpose of this class is to:
- Generate detailed documentation for specified code files
- Handle both inline code comments and separate markdown documentation files
- Support automated application of documentation changes
- Provide interactive user approval for documentation changes


### Key Components


#### DocumentationTaskConfigData

Configuration data class that specifies:
- `topics`: List of specific topics to document
- Input/output files to process
- Task dependencies and state
- Task description


#### Documentation Generator Actor

Uses a specialized prompt to generate documentation that includes:
- Purpose and functionality explanations
- Input/output specifications
- Design decisions and rationale
- Known issues and limitations
- Code examples where relevant


### Usage

1. Create a new DocumentationTask instance with:
   ```kotlin
   val task = DocumentationTask(
       planSettings = yourPlanSettings,
       planTask = DocumentationTaskConfigData(
           topics = listOf("API", "Configuration"),
           input_files = listOf("src/main/kotlin/MyClass.kt"),
           output_files = listOf("docs/MyClass.md")
       )
   )
   ```

2. The task will:
   - Analyze specified input files
   - Generate documentation based on configured topics
   - Present changes for approval (if autoFix is disabled)
   - Apply documentation changes to files


### Configuration Options

- `autoFix`: When enabled, automatically applies documentation changes without user confirmation
- `model`: Configurable AI model for documentation generation
- `temperature`: Controls creativity/determinism of generated documentation


### Key Methods


#### run()
Main execution method that:
- Validates input/output files
- Generates documentation using AI
- Handles user interaction for approving changes
- Applies approved documentation changes


#### promptSegment()
Defines the task's prompt format for the planning system.


### Integration

The class integrates with:
- PlanCoordinator for task management
- UI system for user interaction
- File diff system for showing/applying changes
- Markdown rendering for documentation display


### Error Handling

Uses the Retryable pattern for resilient execution and includes semaphore-based synchronization for task completion.


### Notes

- Documentation changes are presented as diffs for existing files
- New documentation is created as separate markdown files
- Supports both automated and manual approval workflows
- Includes file linking and navigation in the UI

# file\FileModificationTask.kt

Here's the user documentation for the FileModificationTask class:


## FileModificationTask Documentation


### Overview
The `FileModificationTask` class is a specialized task handler for modifying existing files or creating new files within a project. It provides an AI-powered interface for making code changes while maintaining code quality and project consistency.


### Key Features
- Modifies existing files with proper diff formatting
- Creates new files following project conventions
- Maintains code quality and readability
- Integrates with project coding standards
- Provides clear documentation of changes


### Configuration


#### FileModificationTaskConfigData Parameters
- `input_files`: List of source files to be examined/modified
- `output_files`: List of target files to be created/modified 
- `modifications`: Specific changes to be made to the files
- `task_description`: Description of what needs to be done
- `task_dependencies`: List of tasks that must complete before this one
- `state`: Current state of the task


### Usage


#### Basic Example
```kotlin
val taskConfig = FileModificationTaskConfigData(
    input_files = listOf("src/main/kotlin/MyFile.kt"),
    output_files = listOf("src/main/kotlin/MyNewFile.kt"),
    task_description = "Add logging functionality to MyFile.kt and create a new utility class"
)

val task = FileModificationTask(planSettings, taskConfig)
```


#### Auto-Fix Mode
When `autoFix` is enabled in plan settings:
- Changes are automatically applied without user confirmation
- Task completes immediately after changes are made
- Links to modified files are provided in the output


#### Manual Mode
When `autoFix` is disabled:
- Changes require user confirmation before being applied
- An "Accept" button is shown with the proposed changes
- User can review diffs before applying changes


### Output Format
The task generates output in markdown format with:
- File paths as headers
- Diff blocks showing changes to existing files
- Code blocks showing content of new files
- Links to modified/created files
- Context and rationale for changes


### Best Practices
1. Provide clear task descriptions
2. List all relevant input files
3. Specify output files when creating new files
4. Review changes before accepting in manual mode
5. Verify file paths are correct


### Error Handling
- Validates file configurations before execution
- Returns configuration error if no input files specified
- Supports retry functionality for failed operations
- Logs errors for troubleshooting


### Dependencies
- Requires a valid ChatClient for AI operations
- Needs proper file system access permissions
- Must have appropriate project context


### Notes
- Changes are made relative to the project root directory
- Follows project coding standards automatically
- Maintains existing code formatting
- Provides context for all modifications
- Supports multiple file operations in single task

This task is ideal for automated code modifications while maintaining code quality and providing clear documentation of changes.

# file\InquiryTask.kt

Here's the user documentation for the InquiryTask class:


## InquiryTask Documentation


### Overview
The InquiryTask is a specialized task type that allows users to ask questions and gather insights about specific files or aspects of a codebase. It analyzes provided files and generates comprehensive answers and reports based on user-specified questions and goals.


### Configuration


#### Required Configuration Parameters
- `inquiry_questions`: A list of specific questions to be addressed
- `inquiry_goal`: The overall purpose or objective of the inquiry
- `input_files`: List of files or file patterns to analyze (supports glob patterns)


#### Optional Configuration Parameters
- `task_description`: General description of the task
- `task_dependencies`: List of other tasks that must complete before this one
- `state`: Current state of the task


### Usage Examples

```json
{
  "inquiry_questions": [
    "What is the main purpose of the authentication system?",
    "How are user permissions handled?",
    "What security measures are implemented?"
  ],
  "inquiry_goal": "Understand the security architecture of the application",
  "input_files": [
    "src/main/kotlin/auth/*.kt",
    "src/main/kotlin/security/*.kt"
  ]
}
```


### Features


#### Interactive Mode
When `allowBlocking` is enabled in plan settings:
- Provides interactive discussion capability
- Users can review and refine the generated insights
- Supports iterative improvement through user feedback


#### Non-Interactive Mode
When `allowBlocking` is disabled:
- Generates a single comprehensive report
- No user interaction required
- Faster execution for automated workflows


#### File Analysis
- Supports glob patterns for file selection
- Automatically filters out non-relevant file types
- Handles large codebases efficiently


### Best Practices

1. **Question Formulation**
   - Make questions specific and focused
   - Break down complex inquiries into smaller questions
   - Ensure questions align with the inquiry goal

2. **File Selection**
   - Use specific file patterns to limit scope
   - Include all relevant files for comprehensive analysis
   - Avoid including unnecessary files that may add noise

3. **Goal Setting**
   - Define clear, measurable goals
   - Align goals with project objectives
   - Keep scope manageable


### Limitations

- File size limitations may apply based on model constraints
- Processing time increases with number of input files
- Some file types may be automatically excluded for analysis


### Error Handling

The task includes robust error handling for:
- File reading issues
- Invalid file patterns
- Model processing errors

Errors are logged and reported appropriately without causing task failure.


### Integration

The InquiryTask integrates with:
- Project planning systems
- Code analysis workflows
- Documentation generation pipelines


### Output

The task generates:
- Detailed answers to specified questions
- Comprehensive analysis based on input files
- Markdown-formatted reports
- Interactive discussion threads (when in interactive mode)

This documentation should help users effectively utilize the InquiryTask for code analysis and documentation purposes.

# file\PerformanceAnalysisTask.kt

Here's the user documentation for the PerformanceAnalysisTask class:


## Performance Analysis Task Documentation


### Overview
The PerformanceAnalysisTask is a specialized task for analyzing code performance issues and bottlenecks. It provides detailed analysis focusing on various performance aspects of your codebase.


### Configuration


#### Required Parameters
- `files_to_analyze`: List of file paths to be analyzed for performance issues
- `analysis_focus` (optional): Specific areas to focus the analysis on, such as:
  - Time complexity
  - Memory usage
  - I/O operations


#### Example Configuration
```kotlin
val config = PerformanceAnalysisTaskConfigData(
    files_to_analyze = listOf("src/main/kotlin/MyClass.kt"),
    analysis_focus = listOf("time complexity", "memory usage")
)
```


### Analysis Focus Areas
The task analyzes code for:
1. Time complexity of algorithms
2. Memory usage and potential memory leaks
3. I/O operations and network calls
4. Concurrency and parallelism opportunities
5. Caching and memoization possibilities


### Output Format
The analysis is provided as a markdown document containing:
- Detailed explanations of identified performance issues
- Reasons why each issue is a performance concern
- Potential impact on system performance
- Quantitative performance impact estimates where possible


### Usage Notes
- The task focuses on analysis and recommendations rather than providing code changes
- Each identified issue includes detailed context and explanation
- Analysis can be customized by specifying focus areas in the configuration


### Example Output
```markdown

## Performance Analysis Report


### Time Complexity Issues
- Method `processData()` has O(n²) complexity
  - Impact: Significant slowdown with large datasets
  - Estimated impact: 100ms delay per 1000 items


### Memory Usage
- Potential memory leak in cache implementation
  - Impact: Growing memory usage over time
  - Recommendation: Implement cache eviction policy
```


### Dependencies
This task is part of the planning system and requires:
- Valid PlanSettings configuration
- Access to the specified source files
- Proper file system permissions

For more detailed technical information, please refer to the API documentation.

# file\RefactorTask.kt

Here's the user documentation for the RefactorTask class:


## RefactorTask Documentation


### Overview
The `RefactorTask` class is a specialized task for analyzing and refactoring code to improve its structure, readability, and maintainability. It extends `AbstractAnalysisTask` and provides functionality to suggest code improvements based on best practices.


### Configuration


#### RefactorTaskConfigData
When creating a RefactorTask, you can provide the following configuration parameters:

- `filesToRefactor`: List of specific files that need to be refactored
- `refactoringFocus`: List of specific areas to focus on during refactoring, such as:
  - Modularity
  - Design patterns
  - Naming conventions
- `task_description`: Description of the refactoring task
- `task_dependencies`: List of tasks that must be completed before this task
- `input_files`: List of input files to be analyzed
- `output_files`: List of files that will be modified
- `state`: Current state of the task


### Features

The RefactorTask analyzes code and provides suggestions for improvements in the following areas:

1. Code organization
2. Code duplication reduction
3. Modularity enhancement
4. Design pattern implementation
5. Naming convention improvements
6. Complex logic simplification


### Output Format

The task generates output in markdown format, including:

- Detailed explanations for each suggested refactoring
- Reasons for the proposed changes
- Expected benefits
- Potential trade-offs or considerations
- Code snippets in diff format showing the proposed changes


### Example Usage

```kotlin
val refactorTask = RefactorTask(
    planSettings = PlanSettings(),
    planTask = RefactorTaskConfigData(
        filesToRefactor = listOf("src/main/kotlin/MyClass.kt"),
        refactoringFocus = listOf("modularity", "naming conventions"),
        task_description = "Improve code organization and naming in MyClass.kt"
    )
)
```


### Best Practices

1. Specify clear focus areas in `refactoringFocus` to get more targeted suggestions
2. Review all suggested changes carefully before implementation
3. Consider the impact on dependent code when applying refactoring suggestions
4. Test thoroughly after implementing refactoring changes


### Notes

- The task uses an AI-powered analysis system to generate refactoring suggestions
- All suggestions should be reviewed and validated by developers before implementation
- The task can be integrated into larger automated workflows using task dependencies

# file\SecurityAuditTask.kt

Here's the user documentation for the SecurityAuditTask class:


## SecurityAuditTask Documentation


### Overview
The SecurityAuditTask class is a specialized task for performing automated security audits on code files. It analyzes code for potential security vulnerabilities and provides recommendations for improvements.


### Features
- Comprehensive security analysis of specified code files
- Configurable focus areas for targeted auditing
- Detailed audit reports in markdown format
- Code fix suggestions using diff format


### Configuration
The task is configured using `SecurityAuditTaskConfigData` which accepts the following parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| filesToAudit | List<String> | List of files to be analyzed for security issues |
| focusAreas | List<String> | Specific security aspects to focus on during the audit |
| task_description | String | Custom description of the audit task |
| task_dependencies | List<String> | Other tasks that must complete before this audit |
| input_files | List<String> | Source files required for the audit |
| output_files | List<String> | Files where audit results will be saved |
| state | TaskState | Current state of the audit task |


### Analysis Areas
The security audit covers:
1. Security vulnerabilities
2. Insecure coding practices 
3. Security standards compliance
4. Sensitive data handling
5. Authentication/authorization issues
6. Input validation and sanitization


### Output Format
The audit results are provided in markdown format with:
- Organized sections and headings
- Code snippets highlighting issues
- Diff-style suggestions for fixes
- Detailed recommendations for each finding


### Usage Example
```kotlin
val auditTask = SecurityAuditTask(
    planSettings = myPlanSettings,
    planTask = SecurityAuditTaskConfigData(
        filesToAudit = listOf("src/main/kotlin/MyApp.kt"),
        focusAreas = listOf("authentication", "data encryption")
    )
)
```


### Best Practices
- Specify all files that need security review in filesToAudit
- Use focusAreas to prioritize critical security concerns
- Review the complete audit report before implementing changes
- Test all suggested security fixes thoroughly

This task is part of the automated planning system and integrates with other task types for comprehensive code analysis and improvement.

# file\TestGenerationTask.kt

Here's the user documentation for the TestGenerationTask class:


## TestGenerationTask Documentation


### Overview
The TestGenerationTask class is responsible for automatically generating unit tests for specified code files. It is designed to create comprehensive test suites that follow testing best practices and ensure good code coverage.


### Configuration


#### TestGenerationTaskConfigData Parameters
- `filesToTest` (List<String>): List of source code files that need test generation
- `inputReferences` (List<String>): Additional files or tasks to consider when generating tests
- `task_description` (String): Optional description of the test generation task
- `task_dependencies` (List<String>): List of tasks that must complete before test generation
- `input_files` (List<String>): Source files needed for test generation
- `output_files` (List<String>): Generated test files
- `state` (TaskState): Current state of the test generation task


### Features

The test generator creates tests that:
1. Cover all public methods and functions
2. Include both positive and negative test cases
3. Test edge cases and boundary conditions 
4. Aim for high code coverage
5. Follow language-specific testing best practices


### Output Format

Generated tests will:
- Be placed in a `test` directory parallel to source files
- Use appropriate testing frameworks for the target language
- Include setup/teardown methods when needed
- Contain comments explaining test cases
- Be provided as complete, runnable test files


### Example Usage

```kotlin
val config = TestGenerationTaskConfigData(
    filesToTest = listOf("src/main/kotlin/com/example/Utils.kt"),
    inputReferences = listOf("src/main/kotlin/com/example/Types.kt"),
    task_description = "Generate tests for Utils class"
)

val task = TestGenerationTask(planSettings, config)
task.execute()
```


### Generated Test Structure

The generated tests will follow this general pattern:

```java
// Test file example
public class UtilsTest {
    @Test
    public void testExampleFunction() {
        // Happy path test
        assertEquals(expected, actual);
        
        // Edge case tests
        assertEquals(0, Utils.method(0));
        
        // Error condition tests
        assertThrows(Exception.class, () -> {
            // Test code
        });
    }
}
```


### Best Practices
- Specify all files that need testing in `filesToTest`
- Include dependent files in `inputReferences` for better context
- Review generated tests and adjust as needed
- Add task dependencies if tests depend on other tasks completing first

The task will automatically handle test file organization and framework selection based on the source files' language and structure.

# knowledge\EmbeddingSearchTask.kt

Here's the user documentation for the EmbeddingSearchTask:


## EmbeddingSearchTask Documentation


### Overview
The EmbeddingSearchTask is a specialized task that performs semantic search using OpenAI embeddings across indexed documents. It allows you to find content that is semantically similar to your search queries while also supporting negative queries to filter out unwanted results.


### Configuration Options


#### Required Parameters
- `positive_queries`: List of search queries to find semantically similar content
  - Example: `["machine learning algorithms", "neural networks"]`


#### Optional Parameters
- `negative_queries`: List of queries to filter out semantically similar content
  - Default: empty list
  - Example: `["basic statistics", "simple math"]`

- `distance_type`: Method used to calculate similarity between embeddings
  - Options: `Euclidean`, `Manhattan`, `Cosine`
  - Default: `Cosine`

- `count`: Number of top results to return
  - Default: 5
  - Example: `10`

- `min_length`: Minimum character length for content to be considered
  - Default: 0
  - Example: `100`

- `required_regexes`: List of regular expressions that must match in the content
  - Default: empty list
  - Example: `[".*algorithm.*", ".*data.*"]`


### Output Format

The task returns results in markdown format with the following structure:

```markdown

## Embedding Search Results


### Result 1
* Distance: 0.123
* File: path/to/file
[Context Summary in JSON]
Metadata: [JSON metadata]


### Result 2
...
```

Each result includes:
- Numerical distance score (lower is better)
- Source file path
- Context summary showing relevant JSON structure
- Associated metadata


### Example Usage

```json
{
  "positive_queries": ["deep learning architectures"],
  "negative_queries": ["basic neural networks"],
  "distance_type": "Cosine",
  "count": 3,
  "min_length": 200,
  "required_regexes": [".*neural.*", ".*layer.*"]
}
```

This configuration will:
1. Search for content similar to "deep learning architectures"
2. Filter out content similar to "basic neural networks"
3. Return the top 3 results
4. Only consider content with at least 200 characters
5. Require matches to contain words containing "neural" and "layer"


### Best Practices

1. Use specific positive queries for better results
2. Leverage negative queries to filter out unwanted content
3. Adjust min_length to avoid short, irrelevant matches
4. Use required_regexes for additional filtering precision
5. Start with a higher count and narrow down if needed


### Notes

- At least one positive query is required
- Results are sorted by distance (lower is better)
- The task reads from .index.data files in the specified directory
- Context summaries include relevant parent and sibling information from the JSON structure

# knowledge\KnowledgeIndexingTask.kt

Here's the user documentation for the KnowledgeIndexingTask:


## Knowledge Indexing Task Documentation


### Overview
The Knowledge Indexing Task is designed to process and index files for semantic search capabilities. It can handle both document and code files, breaking them down into searchable chunks that can be used for semantic retrieval later.


### Configuration Options


#### Required Parameters
- `file_paths`: A list of file paths to process and index
  - Example: `["/path/to/file1.txt", "/path/to/file2.md"]`
  - Files must exist to be processed


#### Optional Parameters
- `parsing_type`: The type of parsing to use (default: "document")
  - Options:
    - `"document"`: For general document parsing
    - `"code"`: For source code parsing
- `chunk_size`: Controls how documents are split into chunks (default: 0.1)
  - Range: 0.0 to 1.0
  - Smaller values create more granular chunks


### Usage Example
```json
{
  "file_paths": [
    "docs/readme.md",
    "src/main/kotlin/Example.kt"
  ],
  "parsing_type": "code",
  "chunk_size": 0.2
}
```


### Process Flow
1. The task validates the provided file paths
2. Creates a thread pool for parallel processing
3. Selects appropriate parsing model based on parsing_type
4. Processes files and generates searchable chunks
5. Displays progress as percentage complete
6. Outputs summary of processed files


### Output
The task provides:
- Real-time progress updates during processing
- Final summary report listing all processed files
- Indexed content ready for semantic search


### Notes
- Invalid file paths are automatically filtered out
- Processing is done in parallel for better performance
- Progress updates are shown in percentage increments
- The task uses a thread pool of 8 threads for processing


### Error Handling
- Empty or invalid file paths will result in early termination
- Progress updates help identify any processing issues
- Thread pool is properly shutdown even if errors occur

This task is particularly useful for preparing documentation, code bases, or other text content for semantic search capabilities within your application.

# knowledge\WebSearchAndIndexTask.kt

Here's the user documentation for the WebSearchAndIndexTask:


## WebSearchAndIndexTask Documentation


### Overview
The WebSearchAndIndexTask is a specialized task that performs web searches, downloads content, and indexes it for future embedding-based searches. This task is useful for gathering and processing web content related to specific topics that you want to search through later.


### Configuration Parameters


#### Required Parameters
- `search_query` (String): The search query to use for web search
- `num_results` (Int): The number of search results to process (maximum 10)
- `output_directory` (String): The directory where downloaded and indexed content will be stored


#### Optional Parameters
- `task_description` (String): Optional description of the task
- `task_dependencies` (List<String>): List of tasks that must complete before this task runs
- `state` (TaskState): Current state of the task


### Prerequisites
To use this task, you need:
1. A valid Google API key
2. A Google Custom Search Engine ID
3. Sufficient storage space for downloaded content


### How It Works

The task executes in three main phases:

1. **Web Search**
   - Performs a Google Custom Search using the provided query
   - Retrieves the specified number of search results (up to 10)

2. **Content Download**
   - Downloads HTML content from each search result URL
   - Saves the content to files in the specified output directory
   - Files are named based on the search result titles

3. **Content Indexing**
   - Processes downloaded files for indexing
   - Creates binary index files for future embedding searches
   - Uses parallel processing for improved performance


### Example Usage

```kotlin
val taskConfig = WebSearchAndIndexTaskConfigData(
    search_query = "artificial intelligence latest developments",
    num_results = 5,
    output_directory = "ai_research_data"
)

val task = WebSearchAndIndexTask(planSettings, taskConfig)
```


### Output

The task produces:
1. Downloaded HTML files in the specified output directory
2. Indexed binary files for embedding search
3. A summary report containing:
   - The original search query
   - List of successfully downloaded and indexed files


### Error Handling
- Failed downloads are skipped and logged
- Invalid URLs are safely handled
- Network errors are logged and won't crash the task


### Limitations
- Maximum of 10 search results per query
- Requires valid Google API credentials
- Only processes HTML content
- File names are sanitized and limited to 50 characters


### Best Practices
1. Use specific search queries for better results
2. Start with a small number of results for testing
3. Ensure sufficient disk space in output directory
4. Monitor logs for any download or indexing issues


### Related Components
- DocumentRecord: Handles content parsing and indexing
- PlanCoordinator: Manages task execution
- ChatClient: Handles API communication
      TaskType.TaskPlanning -> "Break down complex tasks into manageable subtasks"
      TaskType.Inquiry -> "Analyze and answer questions about code"
      TaskType.FileModification -> "Create or modify source code files"
      TaskType.Documentation -> "Create comprehensive code documentation"
      TaskType.CodeReview -> "Analyze code quality and suggest improvements"
      TaskType.TestGeneration -> "Create automated test suites"
      TaskType.Optimization -> "Improve code performance and efficiency"
      TaskType.SecurityAudit -> "Identify security vulnerabilities"
      TaskType.RefactorTask -> "Improve code structure and maintainability"
          Intelligently breaks down complex development tasks into manageable subtasks with clear dependencies.
            <li>Analyzes project requirements and constraints</li>
            <li>Creates optimal task sequences with dependencies</li>
            <li>Estimates task complexity and effort</li>
            <li>Identifies critical path and bottlenecks</li>
            <li>Suggests parallel execution opportunities</li>
          Acts as an intelligent assistant to analyze code and answer questions about implementation details.
            <li>Explains complex code structures and patterns</li>
            <li>Clarifies implementation decisions and trade-offs</li>
            <li>Identifies dependencies and relationships</li>
            <li>Suggests best practices and improvements</li>
            <li>Provides context-aware technical guidance</li>
          Creates new files or modifies existing ones while maintaining code quality and consistency.
            <li>Implements code changes with proper error handling</li>
            <li>Maintains consistent coding style and patterns</li>
            <li>Updates imports and dependencies automatically</li>
            <li>Provides detailed change documentation</li>
            <li>Supports version control best practices</li>
          Creates detailed, maintainable documentation for code and APIs.
            <li>Generates clear API documentation with examples</li>
            <li>Creates comprehensive usage guides</li>
            <li>Documents architecture decisions and patterns</li>
            <li>Maintains consistency across documentation</li>
            <li>Includes diagrams and visual aids when helpful</li>
          Conducts thorough code reviews focusing on quality, maintainability, and best practices.
            <li>Analyzes code quality and complexity</li>
            <li>Identifies potential bugs and edge cases</li>
            <li>Suggests specific improvements with examples</li>
            <li>Ensures adherence to best practices</li>
            <li>Provides actionable, prioritized feedback</li>
          Generates comprehensive test suites to ensure code reliability and correctness.
            <li>Creates unit, integration, and edge case tests</li>
            <li>Generates meaningful test data sets</li>
            <li>Implements proper test setup and teardown</li>
            <li>Ensures high code coverage</li>
            <li>Follows testing best practices</li>
          Identifies and implements performance optimizations while maintaining code clarity.
            <li>Analyzes performance bottlenecks</li>
            <li>Optimizes algorithms and data structures</li>
            <li>Improves resource utilization</li>
            <li>Provides before/after performance metrics</li>
            <li>Maintains code readability while optimizing</li>
          Performs comprehensive security analysis to identify and mitigate vulnerabilities.
            <li>Identifies security vulnerabilities</li>
            <li>Checks for common attack vectors</li>
            <li>Analyzes authentication and authorization</li>
            <li>Reviews data handling and encryption</li>
            <li>Provides detailed security recommendations</li>
          Restructures code to improve maintainability while preserving functionality.
            <li>Applies appropriate design patterns</li>
            <li>Reduces code complexity and duplication</li>
            <li>Improves naming and organization</li>
            <li>Enhances code modularity</li>
            <li>Maintains backwards compatibility</li>
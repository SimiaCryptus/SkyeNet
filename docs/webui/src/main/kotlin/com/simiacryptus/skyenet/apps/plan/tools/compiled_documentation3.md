# CommandAutoFixTask.kt

## CommandAutoFixTask Documentation

### Overview

CommandAutoFixTask is a specialized task type designed to execute commands and automatically fix any issues that arise during execution. It provides automated
error detection and correction capabilities within a specified working directory context.

### Configuration Parameters

#### TaskSettings

```kotlin
class CommandAutoFixTaskSettings(
  task_type: String,
  enabled: Boolean = false,
  model: ChatModel? = null,
  commandAutoFixCommands: List<String>? = listOf()
)
```

| Parameter              | Type         | Required | Description                     |
|------------------------|--------------|----------|---------------------------------|
| task_type              | String       | Yes      | Task identifier                 |
| enabled                | Boolean      | No       | Enables/disables task execution |
| model                  | ChatModel    | No       | AI model for fixes              |
| commandAutoFixCommands | List<String> | No       | Allowed command executables     |

#### Task Configuration

```kotlin
class CommandAutoFixTaskConfigData(
  commands: List<CommandWithWorkingDir>? = null,
  task_description: String? = null,
  task_dependencies: List<String>? = null,
  state: TaskState? = null
)
```

| Parameter         | Type                        | Required | Description         |
|-------------------|-----------------------------|----------|---------------------|
| commands          | List<CommandWithWorkingDir> | No       | Commands to execute |
| task_description  | String                      | No       | Task description    |
| task_dependencies | List<String>                | No       | Required tasks      |
| state             | TaskState                   | No       | Current task state  |

### Features and Capabilities

- Executes multiple commands sequentially
- Supports custom working directories
- Automatic error detection
- Configurable retry mechanism
- Integration with AI-powered fixes
- Progress tracking and reporting

### Usage Example

```kotlin
val task = CommandAutoFixTask(
  planSettings = PlanSettings(),
  planTask = CommandAutoFixTaskConfigData(
    commands = listOf(
      CommandWithWorkingDir(
        command = listOf("npm", "test"),
        workingDir = "frontend"
      )
    )
  )
)
```

### Error Handling

- Automatic retry mechanism (up to 5 retries with autoFix enabled)
- Error state tracking via AtomicBoolean
- Detailed error reporting in task output
- Semaphore-based completion tracking

### Integration Points

- Integrates with PlanCoordinator for task management
- Uses ChatClient for AI-powered fixes
- Interfaces with file system for command execution
- Supports markdown rendering for output

### Limitations

- Limited to pre-configured command executables
- Requires appropriate file system permissions
- Dependent on AI model availability
- Sequential command execution only

### Best Practices

1. Configure appropriate working directories
2. Use full command paths when possible
3. Implement proper error handling
4. Monitor retry counts
5. Validate commands before execution

### Output Format

The task produces structured markdown output including:

- Command execution results
- Error status
- Fix attempts
- Completion status

### Technical Details

#### Implementation Notes

- Uses semaphore for synchronization
- Implements Retryable pattern
- Integrates with CmdPatchApp for execution
- Supports markdown rendering
- Thread-safe error handling

#### Security Considerations

- Validates executable paths
- Restricts to configured commands
- Requires appropriate permissions
- Working directory validation

### Support

For issues and debugging:

- Check command permissions
- Verify working directory paths
- Monitor task logs
- Review error messages
- Validate configuration settings

# CommandSessionTask.kt

Here's the documentation for the CommandSessionTask:

## CommandSessionTask Documentation

### Table of Contents

1. [Overview](#overview)
2. [Configuration Parameters](#configuration-parameters)
3. [Usage Examples](#usage-examples)
4. [Features and Capabilities](#features-and-capabilities)
5. [Limitations and Constraints](#limitations-and-constraints)
6. [Best Practices](#best-practices)
7. [Error Handling](#error-handling)
8. [Integration Details](#integration-details)

### Overview

CommandSessionTask manages interactive command-line sessions, allowing execution of multiple commands within a persistent session context. It supports both
temporary and reusable sessions, with configurable timeouts and session management capabilities.

### Configuration Parameters

#### Required Parameters

| Parameter | Type         | Description                                                |
|-----------|--------------|------------------------------------------------------------|
| command   | List<String> | The command and arguments to start the interactive session |

#### Optional Parameters

| Parameter    | Type         | Default | Description                              |
|--------------|--------------|---------|------------------------------------------|
| inputs       | List<String> | []      | Commands to send to the session          |
| sessionId    | String?      | null    | Unique identifier for reusing sessions   |
| timeout      | Long         | 30000   | Command timeout in milliseconds          |
| closeSession | Boolean      | false   | Whether to close session after execution |

### Usage Examples

#### Basic Usage

```kotlin
val config = CommandSessionTaskConfigData(
  command = listOf("python", "-i"),
  inputs = listOf(
    "print('Hello World')",
    "2 + 2"
  )
)
```

#### Persistent Session

```kotlin
val config = CommandSessionTaskConfigData(
  command = listOf("bash"),
  inputs = listOf("echo $PATH"),
  sessionId = "my-bash-session",
  closeSession = false
)
```

### Features and Capabilities

- Persistent interactive sessions
- Session reuse via sessionId
- Automatic session cleanup
- Configurable command timeouts
- Concurrent session management
- Combined stdout/stderr capture
- Session lifecycle management

### Limitations and Constraints

- Maximum 10 concurrent sessions
- 30-second default timeout per command
- 5-second graceful shutdown period
- Output limited to 5000 characters per command
- Process streams must be text-based

### Best Practices

1. **Session Management**
  - Use sessionId for long-running sessions
  - Close sessions when no longer needed
  - Implement proper error handling

2. **Resource Management**
  - Monitor active session count
  - Use appropriate timeouts
  - Clean up sessions explicitly

3. **Error Handling**
  - Implement timeout handling
  - Check for process termination
  - Handle stream closure properly

### Error Handling

Common errors and solutions:

1. **Session Limit Exceeded**

```
Error: Maximum number of concurrent sessions (10) reached
Solution: Close unused sessions or wait for sessions to complete
```

2. **Timeout Errors**

```
Error: Command execution timeout
Solution: Increase timeout value or optimize command execution
```

### Integration Details

#### Dependencies

- Requires ChatClient for agent communication
- Integrates with PlanCoordinator
- Uses SessionTask for result handling

#### System Requirements

- JVM environment
- Access to process execution
- Sufficient system resources for concurrent sessions

#### Output Format

Results are formatted in Markdown:

```markdown

### Command Session Results

Command: <command string>
Session ID: <id or "temporary">
Timeout: <timeout>ms

Command Results:

#### Input 1
```

<input command>
```
Output:
```
<command output>
```
```

The task integrates with the broader planning system through the AbstractTask framework and supports both standalone and integrated operation within the
planning ecosystem.

# RunShellCommandTask.kt

## RunShellCommandTask Documentation

### Overview

The RunShellCommandTask is a specialized task implementation for executing shell commands within the application environment. It provides a secure and
controlled way to run system commands with configurable working directories and environment variables.

### Configuration Parameters

#### Required Parameters

| Parameter | Type   | Description                      |
|-----------|--------|----------------------------------|
| command   | String | The shell command to be executed |

#### Optional Parameters

| Parameter         | Type         | Default                 | Description                                      |
|-------------------|--------------|-------------------------|--------------------------------------------------|
| workingDir        | String       | planSettings.workingDir | Relative file path for command execution         |
| task_description  | String       | null                    | Description of the task's purpose                |
| task_dependencies | List<String> | null                    | List of tasks that must complete before this one |
| state             | TaskState    | null                    | Current state of the task                        |

### Features and Capabilities

- Executes shell commands in a controlled environment
- Supports custom working directories
- Integrates with process interpreter
- Provides real-time command output
- Includes error handling and output capture
- Supports environment variable configuration

### Usage Examples

#### Basic Command Execution

```kotlin
val task = RunShellCommandTask(
  planSettings = PlanSettings(),
  planTask = RunShellCommandTaskConfigData(
    command = "ls -la",
    workingDir = "./project"
  )
)
```

#### With Dependencies

```kotlin
val task = RunShellCommandTask(
  planSettings = PlanSettings(),
  planTask = RunShellCommandTaskConfigData(
    command = "npm install",
    workingDir = "./frontend",
    task_dependencies = listOf("BuildProject", "TestSetup")
  )
)
```

### Integration Points

- Works with `PlanCoordinator` for task orchestration
- Integrates with `ProcessInterpreter` for command execution
- Uses `CodingActor` for command processing
- Supports `SessionTask` for UI feedback

### Error Handling

- Captures and displays command execution errors
- Provides semaphore-based synchronization
- Includes logging for troubleshooting
- Supports graceful error recovery
- Displays formatted error output in UI

### Limitations and Constraints

- Executes in the context of system permissions
- Limited to single command execution per task
- Requires proper working directory configuration
- Subject to system resource constraints
- Must handle command timeouts appropriately

### Best Practices

1. Always specify absolute or well-defined relative paths
2. Include proper error handling in commands
3. Use task dependencies for command sequencing
4. Validate commands before execution
5. Monitor command output for errors

### Security Considerations

- Validate command input to prevent injection
- Use restricted permissions where possible
- Avoid executing sensitive commands
- Sanitize command output
- Control environment variable access

### Output Format

The task outputs command results in a structured format:

```

### Shell Command Output

~~~
[Command executed]
~~~

~~~
[Command output/results]
~~~
```

### Technical Details

- Implements `AbstractTask` for task management
- Uses semaphore for execution synchronization
- Supports custom environment variables
- Integrates with UI feedback system
- Provides command output formatting

### Troubleshooting

1. Check working directory permissions
2. Verify command syntax
3. Review environment variables
4. Check system logs for errors
5. Validate task dependencies

For additional support, consult the system logs or contact the development team.

# SeleniumSessionTask.kt

Based on the code provided, I'll create comprehensive documentation for the SeleniumSessionTask type following the specified guidelines.

## SeleniumSessionTask Documentation

### Table of Contents

1. [Overview](#overview)
2. [Configuration](#configuration)
3. [Features and Capabilities](#features-and-capabilities)
4. [Usage Examples](#usage-examples)
5. [Limitations and Constraints](#limitations-and-constraints)
6. [Error Handling](#error-handling)
7. [Best Practices](#best-practices)
8. [Technical Details](#technical-details)

### Overview

SeleniumSessionTask provides a powerful interface for browser automation and web scraping using Selenium WebDriver. It enables creation and management of
stateful browser sessions with support for executing JavaScript commands, navigation, and session persistence.

#### Key Features

- Headless Chrome browser automation
- Stateful session management
- JavaScript command execution
- Session reuse capabilities
- Automatic resource cleanup
- Configurable timeouts

### Configuration

#### SeleniumSessionTaskConfigData Parameters

| Parameter    | Type         | Required | Default | Description                              |
|--------------|--------------|----------|---------|------------------------------------------|
| url          | String       | No       | ""      | Initial URL to navigate to               |
| commands     | List<String> | Yes      | []      | JavaScript commands to execute           |
| sessionId    | String?      | No       | null    | ID for session reuse                     |
| timeout      | Long         | No       | 30000   | Command timeout in milliseconds          |
| closeSession | Boolean      | No       | false   | Whether to close session after execution |

### Features and Capabilities

#### Session Management

- Concurrent session support (max 10 sessions)
- Session reuse via sessionId
- Automatic cleanup of inactive sessions
- Session state persistence between commands

#### JavaScript Execution

```javascript
// Supported command types:
return document.title;                    // Get page title
return document.querySelector('.class');  // DOM manipulation
window.scrollTo(0, 1000);                // Browser control
return new Promise(...);                 // Async operations
```

#### Browser Control

- Headless Chrome operation
- Navigation capabilities
- Screenshot capture
- Page source access
- Console log access

### Usage Examples

#### Basic Usage

```kotlin
val config = SeleniumSessionTaskConfigData(
  url = "https://example.com",
  commands = listOf(
    "return document.title;",
    "return document.querySelector('h1').textContent;"
  )
)
```

#### Session Reuse

```kotlin
val config = SeleniumSessionTaskConfigData(
  sessionId = "session1",
  commands = listOf(
    "window.scrollTo(0, document.body.scrollHeight);",
    "return document.documentElement.outerHTML;"
  ),
  closeSession = false
)
```

### Limitations and Constraints

- Maximum 10 concurrent sessions
- 30-second default command timeout
- Headless mode only
- Chrome browser only
- Memory constraints based on system resources

### Error Handling

#### Common Errors

- Session creation failures
- Command timeout exceptions
- JavaScript execution errors
- Browser connection issues

#### Error Response Format

```
Error: [error message]
[stack trace - first 3 lines]
Failed command: [command that caused error]
```

### Best Practices

#### Session Management

1. Always close sessions when no longer needed
2. Use session reuse for related operations
3. Implement proper error handling
4. Set appropriate timeouts for operations

#### Command Execution

1. Use proper waits for dynamic content
2. Implement error checking in JavaScript
3. Keep commands focused and atomic
4. Handle asynchronous operations properly

### Technical Details

#### Implementation Notes

- Uses ChromeDriver with WebDriverManager
- Implements automatic resource cleanup
- Supports concurrent execution
- Provides detailed logging
- Implements force quit fallback

#### Security Considerations

- Runs in sandbox mode
- Disabled GPU access
- No-sandbox operation
- Dev-shm-usage disabled

#### Output Format

```markdown

### Selenium Session Results

Initial URL: [url]
Session ID: [id]
Final URL: [current_url]
Timeout: [timeout]ms
Browser Info: [browser_details]

Command Results:

#### Command 1

[command details and results]

Final Page Source:
[HTML content]
```

This documentation provides a comprehensive overview of the SeleniumSessionTask functionality while following the specified documentation guidelines. It
includes all essential information for developers to effectively use and integrate the task into their applications.

# file\AbstractFileTask.kt

Here's the documentation for the AbstractFileTask class:

## AbstractFileTask Documentation

### Overview

AbstractFileTask is a base class for file-based tasks in the planning system. It provides core functionality for handling file inputs and outputs, with support
for glob pattern matching and file content processing.

### Configuration Parameters

#### FileTaskConfigBase

| Parameter         | Type         | Required | Description                           |
|-------------------|--------------|----------|---------------------------------------|
| task_type         | String       | Yes      | Identifier for the task type          |
| task_description  | String       | No       | Description of the task's purpose     |
| task_dependencies | List<String> | No       | List of dependent task IDs            |
| input_files       | List<String> | No       | Glob patterns for input files         |
| output_files      | List<String> | No       | Glob patterns for output files        |
| state             | TaskState    | No       | Current task state (default: Pending) |

### Features and Capabilities

#### File Pattern Matching

- Supports glob pattern syntax for file selection
- Handles both input and output file specifications
- Automatically filters for LLM-includable files
- Maintains relative path relationships

#### Content Processing

- Reads and formats file contents with path information
- Handles file reading errors gracefully
- Supports markdown-style code block formatting
- Preserves file ordering based on path

### Usage Example

```kotlin
class CustomFileTask(
  planSettings: PlanSettings,
  config: FileTaskConfigBase
) : AbstractFileTask<FileTaskConfigBase>(planSettings, config) {

  fun processFiles() {
    val fileContents = getInputFileCode()
    // Process file contents
  }
}

// Configuration example
val config = FileTaskConfigBase(
  task_type = "custom_file_task",
  input_files = listOf("src/**/*.kt"),
  output_files = listOf("build/**/*.class")
)
```

### Integration Points

- Extends AbstractTask for planning system integration
- Works with FileValidationUtils for file filtering
- Integrates with filesystem operations via java.nio
- Supports logging via SLF4J

### Best Practices

1. File Pattern Usage
  - Use specific glob patterns to limit file selection
  - Avoid overly broad patterns that may include unwanted files
  - Consider file size limitations when selecting files

2. Error Handling
  - Monitor logs for file reading errors
  - Implement appropriate error recovery in derived classes
  - Validate file patterns before execution

### Limitations and Constraints

- Only processes files that pass FileValidationUtils.isLLMIncludableFile
- File content is loaded into memory, consider memory constraints
- Relies on proper file system permissions

### Error Handling

The class includes built-in error handling for:

- File reading errors (logged with warning)
- Invalid file paths
- Pattern matching failures

### Technical Details

#### File Processing Flow

1. Combines input and output file patterns
2. Resolves glob patterns against filesystem
3. Filters files using FileValidationUtils
4. Reads and formats file contents
5. Combines into single string with markdown formatting

#### Performance Considerations

- File reading is done sequentially
- All file contents are held in memory
- Pattern matching occurs for all files in path

### Logging

Uses SLF4J logging framework:

- Warnings for file reading errors
- Debug level logging available for troubleshooting

### Support

For issues related to file processing:

1. Check file permissions
2. Verify glob patterns
3. Monitor memory usage
4. Review log output
5. Validate file content compatibility

# file\CodeOptimizationTask.kt

Here's the documentation for the CodeOptimizationTask:

## Code Optimization Task

### Overview

The CodeOptimizationTask is a specialized analysis task designed to improve code quality through automated optimization suggestions. It focuses on enhancing
code structure, readability, maintainability, and adherence to language-specific best practices.

### Configuration

#### Required Parameters

| Parameter    | Type         | Description                                         |
|--------------|--------------|-----------------------------------------------------|
| planSettings | PlanSettings | Core configuration settings for the planning system |

#### Optional Parameters

| Parameter         | Type         | Description                                  | Default |
|-------------------|--------------|----------------------------------------------|---------|
| optimizationFocus | List<String> | Specific areas to focus optimization efforts | null    |
| task_description  | String       | Custom description of the task               | null    |
| task_dependencies | List<String> | Tasks that must complete before this one     | null    |
| input_files       | List<String> | Source files to be optimized                 | null    |
| output_files      | List<String> | Destination for optimization results         | null    |
| state             | TaskState    | Current state of the task                    | null    |

### Features and Capabilities

The task analyzes code for improvements in five key areas:

1. Code structure and organization
2. Readability improvements
3. Maintainability enhancements
4. Language-specific best practices
5. Design pattern applications

### Usage Example

```kotlin
val task = CodeOptimizationTask(
  planSettings = PlanSettings(),
  planTask = CodeOptimizationTaskConfigData(
    optimizationFocus = listOf("readability", "design patterns"),
    input_files = listOf("src/main/kotlin/MyClass.kt"),
    output_files = listOf("optimization-report.md")
  )
)
```

### Output Format

The task generates a markdown document containing:

- Detailed explanations for each suggested optimization
- Reasoning behind each suggestion
- Expected benefits
- Potential trade-offs
- Code snippets in diff format showing proposed changes

### Integration

#### System Context

- Extends AbstractAnalysisTask
- Integrates with the broader planning system
- Uses OpenAI's API for analysis

#### Dependencies

- Requires com.simiacryptus.jopenai for API integration
- Depends on core planning system components

### Best Practices

1. Input Files
  - Provide focused, related code files
  - Limit scope to manageable chunks
  - Include context files when necessary

2. Optimization Focus
  - Specify clear optimization goals
  - Prioritize critical improvements
  - Consider project-specific needs

### Limitations and Constraints

- Analysis depth depends on OpenAI API capabilities
- Large codebases may require splitting into multiple tasks
- Language-specific optimizations limited to supported languages

### Error Handling

The task inherits error handling from AbstractAnalysisTask, including:

- Input validation
- API communication errors
- File system access issues

### Technical Details

#### Actor Configuration

- Name: "CodeOptimization"
- Custom prompt template focusing on code analysis
- Structured output format in markdown

#### Implementation Notes

- Extends AbstractAnalysisTask for core functionality
- Uses SLF4J for logging
- Implements TaskType.Optimization

### Support and Troubleshooting

#### Common Issues

1. Missing input files
2. Invalid optimization focus
3. API rate limiting

#### Logging

- Uses SLF4J logging framework
- Log level configurable via logback.xml

For additional support, refer to the project documentation or contact the development team.

# file\CodeReviewTask.kt

Here's the documentation for the CodeReviewTask:

## CodeReviewTask Documentation

### Overview

CodeReviewTask is an automated code review tool that performs comprehensive analysis of source code files. It extends AbstractAnalysisTask and provides detailed
feedback on code quality, potential issues, and suggested improvements.

### Configuration Parameters

#### CodeReviewTaskConfigData Class

| Parameter         | Type         | Required | Description                                 | Default |
|-------------------|--------------|----------|---------------------------------------------|---------|
| focusAreas        | List<String> | No       | Specific areas to focus the code review on  | null    |
| task_description  | String       | No       | Description of the review task              | null    |
| task_dependencies | List<String> | No       | Tasks that must complete before this review | null    |
| input_files       | List<String> | No       | Files to be reviewed                        | null    |
| output_files      | List<String> | No       | Output files for review results             | null    |
| state             | TaskState    | No       | Current state of the task                   | null    |

### Features and Capabilities

The code review analyzes:

- Code quality and readability
- Potential bugs and errors
- Performance issues
- Security vulnerabilities
- Adherence to best practices
- Areas for improvement

### Usage Example

```kotlin
val reviewTask = CodeReviewTask(
  planSettings = PlanSettings(),
  planTask = CodeReviewTaskConfigData(
    focusAreas = listOf("security", "performance"),
    input_files = listOf("src/main/kotlin/MyClass.kt")
  )
)
```

### Output Format

The review is provided as a markdown document containing:

- Structured sections for different review aspects
- Code snippets highlighting issues
- Specific recommendations for improvements
- Examples and explanations

### Integration

#### System Requirements

- Requires configuration via PlanSettings
- Integrates with the broader task planning system
- Works with the AbstractAnalysisTask framework

#### Dependencies

- Extends AbstractAnalysisTask
- Uses JOpenAI for analysis
- Requires SLF4J for logging

### Best Practices

1. Input Files
  - Provide specific files rather than reviewing all files
  - Group related files for contextual review
  - Limit review scope for better focus

2. Focus Areas
  - Specify focus areas for targeted reviews
  - Combine multiple focus areas logically
  - Prioritize critical areas first

### Limitations

- Review quality depends on input code clarity
- May not catch all runtime issues
- Performance varies with codebase size
- Limited to static code analysis

### Error Handling

The task uses SLF4J logging for error tracking:

```kotlin
private val log = LoggerFactory.getLogger(CodeReviewTask::class.java)
```

### Technical Details

#### Actor Configuration

- Name: "CodeReview"
- Custom prompt template for consistent analysis
- Configurable focus areas for specialized review

#### Analysis Process

1. Collects input files
2. Applies focus areas if specified
3. Generates analysis instructions
4. Performs comprehensive review
5. Formats results in markdown

### Support

For issues or questions:

- Check logs for error messages
- Review task configuration
- Verify input file accessibility
- Ensure proper task dependencies

### Version Information

- Part of SkyeNet platform
- Compatible with AbstractAnalysisTask framework
- Requires Kotlin runtime

# file\DocumentationTask.kt

I'll document the DocumentationTask class following the provided guidelines.

```kotlin
/**
 * # DocumentationTask
 *
 * ## Overview
 * DocumentationTask is a specialized file task that generates and manages documentation for code files.
 * It supports both inline code documentation and separate markdown documentation files, providing
 * structured and comprehensive documentation generation capabilities.
 *
 * ## Configuration
 * The task is configured through [DocumentationTaskConfigData] which includes:
 * - topics: List of specific topics to document
 * - task_description: Description of the documentation task
 * - input_files: Source files to be documented
 * - output_files: Target files for documentation
 *
 * ## Features
 * - Automated documentation generation for existing code
 * - Support for both inline comments and separate markdown files
 * - Integration with diff-based code updates
 * - Configurable auto-apply functionality
 * - Retry mechanism for reliability
 *
 * ## Usage Example
 * ```kotlin
 * val docTask = DocumentationTask(
 *   planSettings = myPlanSettings,
 *   planTask = DocumentationTaskConfigData(
 *     topics = listOf("API", "Configuration"),
 *     input_files = listOf("src/main/kotlin/MyClass.kt"),
 *     output_files = listOf("docs/MyClass.md")
 *   )
 * )
 * ```

*
* ## Integration
*
  - Integrates with [PlanCoordinator] for task management
*
  - Uses [SimpleActor] for documentation generation
*
  - Supports [ChatClient] and [OpenAIClient] for AI-powered documentation
*
* ## Error Handling
*
  - Implements [Retryable] for automatic retry on failures
*
  - Validates input/output file configurations
*
  - Provides semaphore-based completion synchronization
*
* @property documentationGeneratorActor Lazy-initialized actor for generating documentation
* @property planSettings Configuration settings for the planning system
* @property taskConfig Specific configuration for this documentation task
  */
  class DocumentationTask(
  planSettings: PlanSettings,
  planTask: DocumentationTaskConfigData?
  ) : AbstractFileTask<DocumentationTaskConfigData>(planSettings, planTask) {

  /**
  * Configuration data class for documentation tasks.
  *
  * @property topics Optional list of specific topics to document
  * @property task_description Description of the documentation task
  * @property task_dependencies List of dependent tasks
  * @property input_files List of source files to document
  * @property output_files List of target documentation files
  * @property state Current state of the task
    */
    class DocumentationTaskConfigData(
    @Description("List topics to document")
    val topics: List<String>? = null,
    task_description: String? = null,
    task_dependencies: List<String>? = null,
    input_files: List<String>? = null,
    output_files: List<String>? = null,
    state: TaskState? = null
    ) : FileTaskConfigBase(
    task_type = TaskType.Documentation.name,
    task_description = task_description,
    task_dependencies = task_dependencies,
    input_files = input_files,
    output_files = output_files,
    state = state
    )

  /**
  * Provides the prompt segment for documentation generation.
  * @return String containing the documentation prompt template
    */
    override fun promptSegment() = """
    Documentation - Generate documentation
    ** List input file names and tasks to be examined
    ** List topics to document
    ** List output files to be modified or created with documentation
    """.trimIndent()

  /**
  * Lazy-initialized documentation generator actor.
  * Configured with specific prompts and settings for documentation generation.
    */
    val documentationGeneratorActor by lazy {
    SimpleActor(
    name = "DocumentationGenerator",
    prompt = // ... (existing prompt)
    model = planSettings.getTaskSettings(TaskType.Documentation).model ?: planSettings.defaultModel,
    temperature = planSettings.temperature,
    )
    }

  /**
  * Executes the documentation task.
  *
  * @param agent PlanCoordinator managing the task
  * @param messages List of input messages
  * @param task Current session task
  * @param api ChatClient instance
  * @param resultFn Callback for results
  * @param api2 OpenAIClient instance
  * @param planSettings Task planning settings
  *
  * @throws IllegalStateException if no input or output files are specified
    */
    override fun run(
    agent: PlanCoordinator,
    messages: List<String>,
    task: SessionTask,
    api: ChatClient,
    resultFn: (String) -> Unit,
    api2: OpenAIClient,
    planSettings: PlanSettings
    ) {
    // ... (existing implementation)
    }

  companion object {
  private val log = LoggerFactory.getLogger(DocumentationTask::class.java)
  }
  }

```

This documentation provides a comprehensive overview of the DocumentationTask class, including its purpose, configuration options, usage examples, and technical details. It follows Kotlin documentation conventions while maintaining readability and accessibility. The documentation is structured to help developers quickly understand and effectively use the DocumentationTask functionality.

# file\FileModificationTask.kt


## FileModificationTask Documentation


### Overview
FileModificationTask is a specialized task type designed to handle code modifications and file creation within a project. It provides capabilities for making precise code changes to existing files and creating new files while maintaining project consistency and coding standards.


### Configuration Parameters


#### FileModificationTaskConfigData Class
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| input_files | List<String>? | No | Source files to be modified |
| output_files | List<String>? | No | Target files for modifications |
| modifications | Any? | No | Specific changes to be made |
| task_description | String? | No | Description of the modification task |
| task_dependencies | List<String>? | No | Dependencies on other tasks |
| state | TaskState? | No | Current state of the task |


### Features and Capabilities


#### Code Modification
- Supports both file modifications and new file creation
- Maintains context awareness with existing codebase
- Provides diff-style change presentation
- Supports multiple file modifications in a single task
- Includes auto-apply functionality when enabled


#### Integration
- Works with PlanCoordinator for task management
- Integrates with ChatClient for AI assistance
- Supports markdown rendering for output
- Includes WebSocket integration for UI updates


### Usage Example

```kotlin
val task = FileModificationTask(
    planSettings = PlanSettings(),
    planTask = FileModificationTaskConfigData(
        input_files = listOf("src/main/kotlin/Example.kt"),
        task_description = "Add logging functionality"
    )
)
```

### Best Practices

1. **Input Files**
  - Always specify input files when modifying existing code
  - Use relative paths for file references
  - Include minimal required files for context

2. **Task Description**
  - Provide clear, specific modification requirements
  - Include context for why changes are needed
  - Specify any constraints or requirements

3. **Error Handling**
  - Implement proper error handling for file operations
  - Validate file paths before modifications
  - Use the retry mechanism for resilience

### Limitations and Constraints

- Requires valid file paths relative to project root
- Auto-fix functionality dependent on planSettings.autoFix
- Single default file handling when only one file is specified

### Output Format

The task generates markdown-formatted output containing:

- File modification diffs
- New file contents
- Interactive UI elements for applying changes
- Status updates and completion messages

#### Example Output

```markdown

#### src/main/kotlin/Example.kt

```diff
- existing code
+ modified code
```

### Error Handling

1. **Configuration Validation**
  - Checks for missing input files
  - Validates file paths
  - Ensures proper task configuration

2. **Execution Safety**
  - Uses semaphore for synchronization
  - Implements retry mechanism
  - Logs errors for troubleshooting

### Technical Details

#### Components

- SimpleActor for AI interaction
- MarkdownUtil for output formatting
- WebSocket integration for UI updates
- Semaphore for execution control

#### Process Flow

1. Validates configuration
2. Prepares input context
3. Generates modifications
4. Renders results
5. Handles user interaction
6. Applies changes (if auto-fix enabled)

### Integration Points

- PlanCoordinator: Task management and coordination
- ChatClient: AI interaction
- SessionTask: Progress tracking
- WebSocket: UI updates
- FileSystem: File operations

### Troubleshooting

Common issues and solutions:

1. Missing input files
  - Ensure proper file paths
  - Verify file existence
2. Failed modifications
  - Check file permissions
  - Verify file content format
3. UI update issues
  - Verify WebSocket connection
  - Check UI manager state

# file\FileSearchTask.kt

## FileSearchTask Documentation

### Overview

FileSearchTask is a specialized task implementation for searching patterns within files in a project directory. It supports both simple substring and
regex-based searches with configurable context lines around matches.

### Configuration Parameters

| Parameter         | Type          | Default | Description                                  |
|-------------------|---------------|---------|----------------------------------------------|
| search_pattern    | String        | ""      | Pattern to search for in files               |
| is_regex          | Boolean       | false   | Whether to treat search_pattern as regex     |
| context_lines     | Int           | 2       | Number of lines to show before/after matches |
| input_files       | List<String>? | null    | Files or glob patterns to search             |
| task_description  | String?       | null    | Optional task description                    |
| task_dependencies | List<String>? | null    | Optional task dependencies                   |
| state             | TaskState?    | null    | Current task state                           |

### Features and Capabilities

- Supports both regex and substring search patterns
- Configurable context lines around matches
- File filtering using glob patterns
- Automatic file type validation for LLM processing
- Grouped results by file with line numbers
- Markdown-formatted output

### Usage Examples

#### Basic Substring Search

```kotlin
val task = FileSearchTask(
  planSettings, SearchTaskConfigData(
    search_pattern = "TODO",
    is_regex = false,
    context_lines = 2,
    input_files = listOf("src/**/*.kt")
  )
)
```

#### Regex Pattern Search

```kotlin
val task = FileSearchTask(
  planSettings, SearchTaskConfigData(
    search_pattern = "class.*\\{",
    is_regex = true,
    context_lines = 3,
    input_files = listOf("**/*.kt", "**/*.java")
  )
)
```

### Output Format

Results are formatted in Markdown with the following structure:

```markdown

## Search Results

### {filename}

#### Line {number}

```

> {line_number}: {matched_line}
> {line_number}: {context_line}
> {line_number}: {context_line}

```
```

### Integration Points

- Integrates with PlanCoordinator for task execution
- Uses ChatClient and OpenAIClient for API interactions
- Leverages MarkdownUtil for output rendering
- Compatible with SessionTask for progress tracking

### Limitations and Constraints

- Only searches files deemed suitable for LLM processing
- Memory usage scales with file size and number of matches
- Performance depends on file system access speed
- Large context line settings may impact performance

### Best Practices

1. Use specific file patterns to limit search scope
2. Keep regex patterns simple and efficient
3. Adjust context lines based on need vs performance
4. Consider file size when searching large codebases
5. Use substring search when exact patterns are known

### Error Handling

- Validates file paths and patterns
- Handles file access permissions
- Manages regex pattern compilation errors
- Gracefully handles invalid UTF-8 content

### Technical Details

- Uses Java NIO for file operations
- Implements glob pattern matching
- Groups results by file for better organization
- Supports relative path resolution
- Thread-safe implementation

### Security Considerations

- Respects file system permissions
- Validates file types before processing
- Sanitizes output for markdown rendering
- Handles path traversal attempts

# file\InquiryTask.kt

## Inquiry Task Documentation

### Table of Contents

- [Overview](#overview)
- [Configuration](#configuration)
- [Features and Capabilities](#features-and-capabilities)
- [Usage Examples](#usage-examples)
- [Integration](#integration)
- [Best Practices](#best-practices)
- [Limitations](#limitations)
- [Error Handling](#error-handling)
- [Technical Details](#technical-details)

### Overview

The Inquiry Task is designed to analyze files and answer specific questions by providing comprehensive summaries and insights. It supports both interactive (
blocking) and non-interactive modes, allowing users to review and discuss findings before proceeding.

### Configuration

#### InquiryTaskConfigData Parameters

| Parameter         | Type         | Required | Description                        | Default |
|-------------------|--------------|----------|------------------------------------|---------|
| inquiry_questions | List<String> | No       | Specific questions to be addressed | null    |
| inquiry_goal      | String       | No       | Overall purpose of the inquiry     | null    |
| input_files       | List<String> | No       | File patterns for analysis         | null    |
| task_description  | String       | No       | Description of the task            | null    |
| task_dependencies | List<String> | No       | Required predecessor tasks         | null    |
| state             | TaskState    | No       | Current task state                 | null    |

### Features and Capabilities

- File pattern matching for input selection
- Support for interactive discussion of findings
- Markdown rendering of results
- Integration with OpenAI API
- Concurrent execution management
- File content validation and filtering

### Usage Examples

```kotlin
val inquiryTask = InquiryTask(
  planSettings = PlanSettings(
    allowBlocking = true,
    temperature = 0.7
  ),
  planTask = InquiryTaskConfigData(
    inquiry_questions = listOf(
      "What are the main components?",
      "How are they interconnected?"
    ),
    inquiry_goal = "Understand system architecture",
    input_files = listOf("src/**/*.kt")
  )
)
```

### Integration

- Integrates with PlanCoordinator for task management
- Uses ChatClient and OpenAIClient for AI interactions
- Supports SessionTask for progress tracking
- Compatible with MarkdownUtil for result rendering

### Best Practices

- Specify focused, clear questions
- Use specific file patterns to limit scope
- Enable blocking mode for critical analyses
- Provide clear inquiry goals
- Review results in interactive mode when accuracy is crucial

### Limitations

- File size and type restrictions apply
- Performance depends on API response times
- Limited to text-based file analysis
- Memory constraints for large file sets

### Error Handling

- Logs file reading errors with warning level
- Graceful handling of invalid file patterns
- Concurrent execution protection via semaphores
- Robust API interaction error handling

### Technical Details

#### Implementation Notes

- Uses glob pattern matching for file selection
- Implements AbstractTask for core functionality
- Supports both blocking and non-blocking execution
- Utilizes SimpleActor for AI interactions
- Implements Discussable for interactive reviews

#### Performance Considerations

- File reading is done sequentially
- Results are cached for efficiency
- Concurrent access is managed via semaphores
- Memory usage scales with input file size

#### Security

- Validates files before processing
- Respects system file access permissions
- Implements API authentication
- Supports user authorization checks

For additional support or questions, please refer to the project documentation or contact the development team.

# file\PerformanceAnalysisTask.kt

Here's the documentation for the PerformanceAnalysisTask:

## Performance Analysis Task

### Overview

The Performance Analysis Task is designed to analyze code for performance issues and bottlenecks, providing detailed insights into potential performance
improvements. It focuses on key performance aspects including algorithmic complexity, memory usage, I/O operations, concurrency, and caching opportunities.

### Configuration Parameters

#### PerformanceAnalysisTaskConfigData

| Parameter         | Type         | Required | Description                              | Default |
|-------------------|--------------|----------|------------------------------------------|---------|
| analysis_focus    | List<String> | No       | Specific areas to focus analysis on      | null    |
| task_description  | String       | No       | Description of the analysis task         | null    |
| task_dependencies | List<String> | No       | Tasks that must complete before this one | null    |
| input_files       | List<String> | No       | Files to be analyzed                     | null    |
| output_files      | List<String> | No       | Files to store analysis results          | null    |
| state             | TaskState    | No       | Current state of the task                | null    |

### Features and Capabilities

#### Analysis Areas

1. Time Complexity Analysis
  - Algorithm efficiency evaluation
  - Computational complexity assessment

2. Memory Usage Analysis
  - Memory leak detection
  - Heap usage patterns

3. I/O Operations Review
  - File operations efficiency
  - Network call patterns

4. Concurrency Assessment
  - Parallelization opportunities
  - Thread usage analysis

5. Optimization Opportunities
  - Caching recommendations
  - Memoization possibilities

### Usage Example

```kotlin
val task = PerformanceAnalysisTask(
  planSettings = PlanSettings(),
  planTask = PerformanceAnalysisTaskConfigData(
    analysis_focus = listOf("time complexity", "memory usage"),
    input_files = listOf("src/main/kotlin/MyClass.kt"),
    task_description = "Analyze performance of data processing logic"
  )
)
```

### Output Format

The analysis is provided as a markdown document containing:

- Identified performance issues
- Detailed explanations of concerns
- Impact assessments
- Quantitative performance estimates where applicable
- Recommendations for improvements

### Integration Points

- Extends `AbstractAnalysisTask`
- Compatible with the task planning system
- Can be chained with other analysis tasks

### Best Practices

1. Specify focused analysis areas for more detailed results
2. Include all relevant files in the analysis
3. Provide clear task descriptions for better context
4. Review dependencies before running analysis

### Limitations and Constraints

- Does not automatically implement improvements
- Analysis depth depends on code documentation quality
- Performance estimates are approximations
- Limited to static code analysis

### Error Handling

- Logs errors through SLF4J
- Maintains task state for tracking
- Supports task retry mechanisms

### Technical Details

- Actor Name: "PerformanceAnalysis"
- Task Type: PerformanceAnalysis
- Implementation: Kotlin
- Logging: SLF4J

### Support

- Logging through LoggerFactory
- Task state tracking
- Integration with planning system for coordination

This task is part of the larger code analysis and improvement system, focusing specifically on performance optimization opportunities through static analysis.

# file\RefactorTask.kt

## RefactorTask Documentation

### Overview

RefactorTask is a specialized code analysis task that focuses on improving existing code through systematic refactoring. It analyzes source code and provides
detailed recommendations for enhancing code quality, maintainability, and structure.

### Configuration Parameters

#### RefactorTaskConfigData Class

| Parameter         | Type         | Required | Default | Description                                                                     |
|-------------------|--------------|----------|---------|---------------------------------------------------------------------------------|
| refactoringFocus  | List<String> | No       | null    | Specific areas to focus refactoring efforts (e.g., modularity, design patterns) |
| task_description  | String       | No       | null    | Description of the refactoring task                                             |
| task_dependencies | List<String> | No       | null    | List of tasks that must complete before this task                               |
| input_files       | List<String> | No       | null    | Source files to be refactored                                                   |
| output_files      | List<String> | No       | null    | Files that will contain refactored code                                         |
| state             | TaskState    | No       | null    | Current state of the task                                                       |

### Features and Capabilities

The task analyzes code for improvements in:

- Code organization and structure
- Code duplication reduction
- Modularity enhancement
- Design pattern implementation
- Naming convention consistency
- Logic simplification

### Usage Example

```kotlin
val refactorTask = RefactorTask(
  planSettings = PlanSettings(),
  planTask = RefactorTaskConfigData(
    refactoringFocus = listOf("modularity", "design patterns"),
    input_files = listOf("src/main/kotlin/MyClass.kt"),
    output_files = listOf("src/main/kotlin/RefactoredClass.kt")
  )
)
```

### Output Format

The task generates a markdown document containing:

- Detailed refactoring suggestions
- Explanations for each proposed change
- Benefits and trade-offs analysis
- Code snippets in diff format showing proposed changes

Example output:

```markdown

## Refactoring Analysis

### 1. Code Organization

- **Current Issue**: Method X is too long and handles multiple responsibilities
- **Suggested Change**:

```diff
- public void longMethod() {
+ public void handleValidation() {
+    // validation logic
+ }
+ 
+ public void processData() {
+    // processing logic
+ }
```

### Integration Points

- Works within the broader task planning system
- Integrates with source code management systems
- Can be chained with other code analysis tasks
- Supports multiple programming languages

### Best Practices

1. **Input Files**
  - Provide complete file paths
  - Group related files together
  - Include all dependent files

2. **Refactoring Focus**
  - Specify clear focus areas
  - Prioritize high-impact changes
  - Consider dependencies between changes

### Limitations and Constraints

- Cannot automatically apply changes
- May require manual review of suggestions
- Performance depends on code complexity
- Limited to supported programming languages

### Error Handling

The task handles common scenarios including:

- Invalid file paths
- Malformed source code
- Missing dependencies
- Configuration errors

### Technical Details

- Extends AbstractAnalysisTask
- Uses AI-powered code analysis
- Generates diff-formatted suggestions
- Supports markdown output formatting

### Logging and Debugging

- Uses SLF4J logging framework
- Logs analysis progress and errors
- Supports detailed debugging output
- Maintains operation history

### Support and Maintenance

For issues or questions:

- Check the logs for detailed error messages
- Review input configuration
- Verify file permissions and paths
- Contact system administrator for persistent issues

# file\SecurityAuditTask.kt

Here's the documentation for the SecurityAuditTask:

## Security Audit Task

### Overview

The SecurityAuditTask performs automated security audits on code files, identifying potential vulnerabilities and providing recommendations for security
improvements. It analyzes code for common security issues, compliance with best practices, and potential risks.

### Configuration Parameters

#### SecurityAuditTaskConfigData

| Parameter         | Type         | Required | Default | Description                                   |
|-------------------|--------------|----------|---------|-----------------------------------------------|
| focusAreas        | List<String> | No       | null    | Specific security areas to focus the audit on |
| task_description  | String       | No       | null    | Custom description of the audit task          |
| task_dependencies | List<String> | No       | null    | Tasks that must complete before this audit    |
| input_files       | List<String> | No       | null    | Files to be audited                           |
| output_files      | List<String> | No       | null    | Files where audit results will be saved       |
| state             | TaskState    | No       | null    | Current state of the task                     |

### Features and Capabilities

The security audit analyzes code for:

- Potential security vulnerabilities
- Insecure coding practices
- Compliance with security standards
- Proper handling of sensitive data
- Authentication and authorization issues
- Input validation and sanitization

### Usage Example

```kotlin
val auditTask = SecurityAuditTask(
  planSettings = PlanSettings(),
  planTask = SecurityAuditTaskConfigData(
    focusAreas = listOf("authentication", "data-encryption"),
    input_files = listOf("src/main/kotlin/auth/LoginService.kt"),
    output_files = listOf("security-audit-report.md")
  )
)
```

### Output Format

The audit generates a markdown-formatted report containing:

- Detailed analysis of security issues
- Code examples highlighting vulnerabilities
- Recommended fixes in diff format
- Severity levels for identified issues
- Best practice recommendations

### Integration Points

- Extends AbstractAnalysisTask for file analysis capabilities
- Integrates with the broader task planning system
- Can be chained with other security-related tasks
- Supports the TaskType.SecurityAudit classification

### Best Practices

1. Specify focused areas for more detailed analysis
2. Include all related files in a single audit
3. Review audit reports promptly
4. Implement recommended security fixes
5. Run regular security audits as part of CI/CD

### Limitations

- Analysis depth depends on code complexity
- May require manual verification of findings
- Cannot detect all possible security vulnerabilities
- Performance may vary with codebase size

### Error Handling

- Logs errors through SLF4J logger
- Maintains task state for failure recovery
- Supports task retry mechanisms
- Reports analysis failures in output

### Technical Details

- Actor Name: "SecurityAudit"
- Task Type: TaskType.SecurityAudit
- Implementation: Kotlin
- Extends: AbstractAnalysisTask
- Output: Markdown formatted reports

### Support

- Logging: Uses SLF4J for diagnostic logging
- Error Reporting: Through standard task error handling
- Documentation: Inline code comments and markdown reports

This task is essential for maintaining code security and should be integrated into regular development workflows.

# file\TestGenerationTask.kt

Here's the documentation for the TestGenerationTask:

## Test Generation Task

### Overview

The TestGenerationTask is a specialized task for automatically generating comprehensive unit tests for specified code files. It analyzes source code and
produces corresponding test files with thorough test coverage.

### Configuration Parameters

#### TestGenerationTaskConfigData Class

| Parameter         | Type          | Required | Description                                       |
|-------------------|---------------|----------|---------------------------------------------------|
| task_description  | String?       | No       | Custom description of the test generation task    |
| task_dependencies | List<String>? | No       | List of tasks that must complete before this task |
| input_files       | List<String>? | No       | Source files to analyze for test generation       |
| output_files      | List<String>? | No       | Generated test file paths                         |
| state             | TaskState?    | No       | Current state of the task                         |

### Features and Capabilities

The task generates tests that:

- Cover all public methods and functions
- Include both positive and negative test cases
- Test edge cases and boundary conditions
- Ensure high code coverage
- Follow language-specific testing best practices

### Usage Example

```kotlin
val testTask = TestGenerationTask(
  planSettings = PlanSettings(),
  planTask = TestGenerationTaskConfigData(
    input_files = listOf("src/main/kotlin/com/example/Utils.kt"),
    task_description = "Generate tests for Utils class"
  )
)
```

### Output Format

Generated tests are provided in code blocks with:

- File path headers
- Language-specific syntax highlighting
- Proper test framework imports
- Setup/teardown methods where needed
- Documented test cases

Example output:

```java

#### src/test/java/com/example/UtilsTest.java
        ```java
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {
    @Test
    public void testExampleFunction() {
        // Test cases with assertions...
    }
}
```

### Best Practices

1. Input Files:
  - Provide complete source files for accurate analysis
  - Include related dependencies for proper context
  - Use absolute or project-relative paths

2. Test Organization:
  - Tests are generated in parallel test directories
  - Follow standard test naming conventions
  - Maintain consistent test structure

### Integration Points

- Integrates with project build systems
- Works with common testing frameworks
- Supports multiple programming languages
- Compatible with continuous integration workflows

### Limitations

- Requires valid source code input
- May need manual refinement for complex scenarios
- Language support depends on available testing frameworks
- Generated tests require review for business logic accuracy

### Error Handling

The task handles common errors including:

- Invalid file paths
- Malformed source code
- Missing dependencies
- Framework compatibility issues

### Technical Details

- Uses language-specific AST analysis
- Implements intelligent test case generation
- Supports multiple testing frameworks
- Generates setup/teardown code automatically

### Support

- Logs errors using SLF4J
- Provides detailed task state information
- Supports task dependency management
- Includes progress tracking

For issues or questions, refer to the project documentation or contact the development team.

# knowledge\EmbeddingSearchTask.kt

## EmbeddingSearchTask Documentation

### Overview

The EmbeddingSearchTask performs semantic search operations using embeddings to find relevant content in indexed documents. It supports both positive and
negative search queries, allowing for refined search results based on semantic similarity and dissimilarity.

### Configuration Parameters

#### Required Parameters

| Parameter        | Type         | Description                                         |
|------------------|--------------|-----------------------------------------------------|
| positive_queries | List<String> | Search queries to find semantically similar content |

#### Optional Parameters

| Parameter        | Type         | Default | Description                                                  |
|------------------|--------------|---------|--------------------------------------------------------------|
| negative_queries | List<String> | []      | Queries to filter out semantically similar content           |
| distance_type    | DistanceType | Cosine  | Metric for comparing embeddings (Euclidean/Manhattan/Cosine) |
| count            | Int          | 5       | Number of top results to return                              |
| min_length       | Int          | 0       | Minimum content length threshold                             |
| required_regexes | List<String> | []      | Regex patterns that must match in content                    |

### Usage Examples

#### Basic Search

```kotlin
val task = EmbeddingSearchTask(
  planSettings, EmbeddingSearchTaskConfigData(
    positive_queries = listOf("artificial intelligence", "machine learning"),
    count = 3
  )
)
```

#### Advanced Search with Filters

```kotlin
val task = EmbeddingSearchTask(
  planSettings, EmbeddingSearchTaskConfigData(
    positive_queries = listOf("security vulnerabilities"),
    negative_queries = listOf("false positives"),
    distance_type = DistanceType.Cosine,
    count = 5,
    min_length = 100,
    required_regexes = listOf("CVE-\\d+")
  )
)
```

### Features and Capabilities

- Semantic search using OpenAI embeddings
- Combined positive/negative query filtering
- Multiple distance metrics support
- Regex pattern matching
- Content length filtering
- JSON context summarization

### Output Format

Results are returned in markdown format containing:

- Distance score
- Source file path
- Context summary in JSON
- Associated metadata

Example output:

```markdown

## Embedding Search Results

### Result 1

* Distance: 0.123
* File: documents/security/report.json
* Metadata: {...}
```

### Integration Points

- Requires OpenAI API client
- Works with indexed document storage
- Integrates with PlanCoordinator framework
- Compatible with SessionTask system

### Limitations and Constraints

- Requires pre-indexed documents (.index.data files)
- Limited by OpenAI API rate limits
- Memory usage scales with embedding size
- Performance depends on index size

### Error Handling

- Validates minimum required parameters
- Handles missing or corrupt index files
- Graceful handling of API failures
- Reports errors through logging system

### Best Practices

- Use specific, focused search queries
- Combine positive/negative queries for precision
- Limit result count for better performance
- Use appropriate distance metric for use case
- Include relevant regex patterns when possible

### Technical Details

- Uses OpenAI's text-embedding-ada-002 model
- Supports hierarchical JSON context extraction
- Implements multiple distance metrics
- Performs parallel embedding computation
- Uses binary serialization for efficiency

### Support

- Logs detailed debug information
- Provides structured error messages
- Includes stack traces for debugging
- Reports API-related issues clearly

For additional support, consult the application logs or contact the development team.

# knowledge\KnowledgeIndexingTask.kt

## Knowledge Indexing Task Documentation

### Table of Contents

1. [Overview](#overview)
2. [Configuration](#configuration)
3. [Features and Capabilities](#features-and-capabilities)
4. [Usage Examples](#usage-examples)
5. [Integration](#integration)
6. [Limitations and Performance](#limitations-and-performance)
7. [Error Handling](#error-handling)
8. [Best Practices](#best-practices)

### Overview

The KnowledgeIndexingTask processes and indexes files for semantic search capabilities. It supports both document and code parsing, breaking content into
manageable chunks for efficient searching and analysis.

### Configuration

#### Required Parameters

| Parameter  | Type         | Description                             |
|------------|--------------|-----------------------------------------|
| file_paths | List<String> | List of file paths to process and index |

#### Optional Parameters

| Parameter         | Type          | Default    | Description                                   |
|-------------------|---------------|------------|-----------------------------------------------|
| parsing_type      | String        | "document" | Type of parsing to use ("document" or "code") |
| chunk_size        | Double        | 0.1        | Size of content chunks for processing         |
| task_description  | String?       | null       | Custom description of the task                |
| task_dependencies | List<String>? | null       | List of dependent task IDs                    |
| state             | TaskState?    | null       | Current state of the task                     |

### Features and Capabilities

- Multi-file processing with parallel execution
- Support for both document and code parsing
- Progress tracking and reporting
- Configurable chunk sizing
- Integration with OpenAI API
- Binary storage of processed results

### Usage Examples

```kotlin
// Basic document indexing
val task = KnowledgeIndexingTask(
  planSettings, KnowledgeIndexingTaskConfigData(
    file_paths = listOf("docs/readme.md", "docs/api.md"),
    parsing_type = "document",
    chunk_size = 0.1
  )
)

// Code indexing with custom chunk size
val task = KnowledgeIndexingTask(
  planSettings, KnowledgeIndexingTaskConfigData(
    file_paths = listOf("src/main/kotlin"),
    parsing_type = "code",
    chunk_size = 0.2
  )
)
```

### Integration

- Integrates with PlanCoordinator for task management
- Uses OpenAIClient for content processing
- Supports SessionTask for UI updates
- Compatible with both document and code parsing models

### Limitations and Performance

- Processing speed depends on file sizes and available threads
- Uses fixed thread pool of 8 threads
- Memory usage scales with chunk size and number of files
- Requires valid file paths and existing files

### Error Handling

- Validates file existence before processing
- Reports empty file lists
- Provides progress updates through UI
- Ensures thread pool cleanup on completion

### Best Practices

1. Choose appropriate parsing type for content
2. Adjust chunk size based on content complexity
3. Monitor progress for large file sets
4. Ensure sufficient system resources
5. Use appropriate file paths for your environment

### Output Format

```markdown

## Knowledge Indexing Complete

Processed 3 files:

* document1.md
* document2.md
* code.kt
```

The task provides real-time progress updates and a final summary of processed files.

# knowledge\WebSearchAndIndexTask.kt

## WebSearchAndIndexTask Documentation

### Overview

The WebSearchAndIndexTask performs web searches using Google's Custom Search API, downloads content from search results, and indexes it for future embedding
search operations. This task is useful for gathering and processing web content for knowledge bases or research purposes.

### Configuration Parameters

| Parameter         | Type         | Required | Default | Description                                            |
|-------------------|--------------|----------|---------|--------------------------------------------------------|
| search_query      | String       | Yes      | -       | The search query to execute                            |
| num_results       | Int          | No       | 5       | Number of search results to process (max 10)           |
| output_directory  | String       | Yes      | -       | Directory path to store downloaded and indexed content |
| task_description  | String       | No       | null    | Optional description of the task instance              |
| task_dependencies | List<String> | No       | null    | List of dependent task IDs                             |
| state             | TaskState    | No       | null    | Current state of the task                              |

### Features and Capabilities

- Performs Google Custom Search API queries
- Downloads web content from search results
- Converts downloaded content to indexed binary format
- Supports parallel processing of downloads
- Sanitizes filenames for storage
- Provides progress tracking and error handling

### Usage Example

```kotlin
val taskConfig = WebSearchAndIndexTaskConfigData(
  search_query = "artificial intelligence latest developments",
  num_results = 5,
  output_directory = "ai_research_content"
)

val task = WebSearchAndIndexTask(planSettings, taskConfig)
task.run(agent, messages, sessionTask, api, ::handleResult, api2, planSettings)
```

### Integration Points

- Requires Google API credentials in PlanSettings
- Integrates with DocumentRecord for content indexing
- Uses OpenAI client for processing
- Compatible with PlanCoordinator for task management

### Limitations and Constraints

- Maximum 10 search results per query
- Requires valid Google API key and Search Engine ID
- Network-dependent operation
- Storage space requirements for downloaded content
- Rate limits based on API quotas

### Error Handling

The task implements several error handling mechanisms:

- Search API error detection and reporting
- Download failure recovery
- File system error management
- Thread pool resource management

### Output Format

Returns a markdown-formatted summary containing:

```markdown

## Web Search and Index Results

### Search Query: [search_query]

### Downloaded and Indexed Files:

1. [filename1]
2. [filename2]
   ...
```

### Best Practices

1. Set appropriate num_results based on needs
2. Use specific search queries for better results
3. Ensure sufficient storage space
4. Monitor API usage and rate limits
5. Implement proper error handling for results

### Technical Details

- Uses HttpClient for web requests
- Implements parallel processing via ExecutorService
- Performs URL encoding for search queries
- Sanitizes filenames for cross-platform compatibility
- Manages thread pool resources

### Dependencies

- Google Custom Search API
- OpenAI API
- Apache Commons IO
- Java HTTP Client
- SLF4J for logging

### Security Considerations

- API keys must be secured
- URL validation recommended
- Content scanning for malicious data advised
- File system permissions management required
- Network security protocols enforced

# online\GitHubSearchTask.kt

## GitHub Search Task Documentation

### Table of Contents

1. [Overview](#overview)
2. [Configuration Parameters](#configuration-parameters)
3. [Usage Examples](#usage-examples)
4. [Features and Capabilities](#features-and-capabilities)
5. [Limitations and Constraints](#limitations-and-constraints)
6. [Integration Details](#integration-details)
7. [Error Handling](#error-handling)
8. [Output Format](#output-format)

### Overview

The GitHubSearchTask enables programmatic searching of GitHub's various resources including repositories, code, commits, issues, users, and topics. It
integrates with GitHub's REST API to perform searches and format results in a readable markdown format.

### Configuration Parameters

| Parameter    | Type   | Required | Default        | Description                                                    |
|--------------|--------|----------|----------------|----------------------------------------------------------------|
| search_query | String | Yes      | ""             | The search query to execute on GitHub                          |
| search_type  | String | Yes      | "repositories" | Type of search (code/commits/issues/repositories/topics/users) |
| per_page     | Int    | No       | 30             | Number of results per page (max 100)                           |
| sort         | String | No       | null           | Sort field for results                                         |
| order        | String | No       | null           | Sort direction (asc/desc)                                      |

### Usage Examples

```kotlin
val task = GitHubSearchTask(
  planSettings = PlanSettings(),
  planTask = GitHubSearchTaskConfigData(
    search_query = "language:kotlin stars:>1000",
    search_type = "repositories",
    per_page = 50,
    sort = "stars",
    order = "desc"
  )
)
```

### Features and Capabilities

- Supports multiple search types:
  - Repositories
  - Code
  - Commits
  - Issues
  - Users
  - Topics
- Customizable result formatting for each search type
- Markdown output with clickable links
- Pagination support
- Custom sorting options

### Limitations and Constraints

- Maximum 100 results per request
- Requires GitHub API token
- Subject to GitHub API rate limiting
- Search complexity limitations based on GitHub's API constraints

### Integration Details

#### Dependencies

- Requires Jackson for JSON processing
- Needs HTTP client support
- Integrates with PlanCoordinator system

#### Configuration Requirements

- GitHub API token must be configured in PlanSettings
- Valid GitHub API version header required

### Error Handling

The task implements robust error handling:

- Validates HTTP response codes
- Throws RuntimeException for API failures
- Includes detailed error messages
- Handles malformed JSON responses

### Output Format

Results are formatted in Markdown with different templates for each search type:

#### Repository Results

```markdown

#### repository-name

Description
Stars: X | Forks: Y
[View on GitHub](url)
```

#### Code Results

```markdown

#### repository-name

File: path
```code snippet```
```

#### User Results

```markdown

#### username

Type: user-type | Repos: count
![Avatar](avatar-url)
```

# online\SearchAndAnalyzeTask.kt

Here's the documentation for the SearchAndAnalyzeTask:

## SearchAndAnalyzeTask Documentation

### Overview

SearchAndAnalyzeTask is a specialized task that performs Google searches, retrieves web content from search results, and analyzes the content based on specified
goals. It combines web search, content fetching, and AI-powered analysis into a single automated workflow.

### Configuration Parameters

| Parameter     | Type   | Default | Description                                 | Required |
|---------------|--------|---------|---------------------------------------------|----------|
| search_query  | String | ""      | The Google search query to execute          | Yes      |
| num_results   | Int    | 3       | Number of search results to analyze (max 5) | No       |
| analysis_goal | String | ""      | The objective or focus for content analysis | Yes      |

### Features and Capabilities

- Performs Google Custom Search API queries
- Fetches web content from search results
- Scrubs HTML content for clean text analysis
- AI-powered content analysis using specified goals
- Markdown formatting of analysis results
- Error handling for failed requests

### Usage Example

```kotlin
val taskConfig = SearchAndAnalyzeTaskConfigData(
  search_query = "kotlin coroutines best practices",
  num_results = 3,
  analysis_goal = "Identify key recommendations and common pitfalls"
)

val task = SearchAndAnalyzeTask(planSettings, taskConfig)
```

### Output Format

The task produces markdown-formatted output containing:

- Numbered list of search results
- Title and link for each result
- Detailed analysis of each page's content
- Error messages for failed retrievals/analysis

Example output:

```markdown

## Analysis of Search Results

### 1. [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

[Analysis content...]

### 2. [Best Practices for Coroutines](https://example.com/article)

[Analysis content...]
```

### Limitations and Constraints

- Maximum 5 search results per query
- Requires valid Google API key and Search Engine ID
- Web content must be publicly accessible
- HTML content scrubbing may not handle all formats perfectly
- Rate limits apply to Google API usage

### Integration Points

- Requires PlanSettings with:
  - googleApiKey
  - googleSearchEngineId
  - defaultModel (for AI analysis)
- Integrates with SimpleActor for content analysis
- Uses WebFetchAndTransformTask for HTML processing

### Error Handling

- Handles HTTP errors from Google API
- Catches and logs exceptions during content fetching
- Provides error messages in output for failed analyses
- Continues processing remaining results if one fails

### Best Practices

1. **Search Queries**
  - Use specific, focused search terms
  - Include relevant keywords
  - Consider using site-specific searches when appropriate

2. **Analysis Goals**
  - Define clear, specific analysis objectives
  - Break complex analyses into multiple runs
  - Use consistent goal formatting

3. **Performance**
  - Limit number of results for faster processing
  - Consider caching frequently accessed content
  - Monitor API usage and rate limits

### Technical Details

- Uses HttpClient for web requests
- Implements URL encoding for search queries
- Processes JSON responses from Google API
- Supports markdown rendering for UI display
- Handles concurrent processing of results

### Security Considerations

- Requires secure storage of API keys
- Validates URLs before fetching
- Sanitizes HTML content
- Handles sensitive data appropriately
- Uses HTTPS for all external requests

### Troubleshooting

1. **API Errors**
  - Verify API key validity
  - Check search engine ID
  - Monitor rate limits
  - Verify network connectivity

2. **Content Errors**
  - Check URL accessibility
  - Verify content format
  - Monitor response sizes
  - Check character encodings

# online\SimpleGoogleSearchTask.kt

Here's the documentation for the SimpleGoogleSearchTask:

## SimpleGoogleSearchTask Documentation

### Overview

SimpleGoogleSearchTask is a task implementation that performs Google web searches using the Google Custom Search API. It enables programmatic access to Google
search results within the application workflow.

### Configuration Parameters

#### Required Parameters

| Parameter    | Type   | Description                              |
|--------------|--------|------------------------------------------|
| search_query | String | The search query to execute on Google    |
| num_results  | Int    | Number of results to return (maximum 10) |

#### Optional Parameters

| Parameter         | Type          | Description                    | Default |
|-------------------|---------------|--------------------------------|---------|
| task_description  | String?       | Custom description of the task | null    |
| task_dependencies | List<String>? | List of dependent task IDs     | null    |
| state             | TaskState?    | Current state of the task      | null    |

### Features and Capabilities

- Executes Google web searches programmatically
- Returns formatted search results including titles, links, and snippets
- Supports configurable number of results (up to 10)
- Includes pagemap metadata for each result
- Formats results in Markdown for easy display

### Usage Example

```kotlin
val searchTask = SimpleGoogleSearchTask(
  planSettings = PlanSettings(
    googleApiKey = "your-api-key",
    googleSearchEngineId = "your-search-engine-id"
  ),
  planTask = GoogleSearchTaskConfigData(
    search_query = "kotlin programming",
    num_results = 5
  )
)
```

### Output Format

The task returns results in Markdown format:

```markdown

## Google Search Results

## 1. [Result Title](https://result-url.com)

Result snippet text...
Pagemap:

```json
{
    // Pagemap metadata
}
```

```


### Limitations and Constraints
- Maximum 10 results per search query
- Requires valid Google API key and Custom Search Engine ID
- Subject to Google API usage quotas and rate limits
- Results limited to web pages indexed by Google


### Error Handling
- Throws RuntimeException for non-200 HTTP responses
- Includes error message and status code in exception
- Handles null search results gracefully
- Validates configuration parameters before execution


### Integration Points
- Integrates with PlanCoordinator for task management
- Uses ChatClient for communication
- Supports MarkdownUtil for result formatting
- Compatible with SessionTask for progress tracking


### Best Practices
1. Cache results when possible to minimize API calls
2. Use specific search queries for better results
3. Handle rate limiting with appropriate delays
4. Validate API credentials before execution
5. Monitor API usage to stay within quotas


### Technical Details
- Uses Java HttpClient for API requests
- Implements Jackson for JSON parsing
- Supports UTF-8 encoding for queries
- Follows RESTful API patterns
- Implements proper resource cleanup


### Dependencies
- Google Custom Search API
- Jackson ObjectMapper
- MarkdownUtil
- HttpClient
- ChatClient


### Security Considerations
- Requires secure storage of API credentials
- Implements URL encoding for search queries
- Validates input parameters
- Handles sensitive data appropriately


### Troubleshooting
1. Verify API credentials are valid
2. Check search engine ID configuration
3. Monitor API response codes
4. Verify network connectivity
5. Check query encoding for special characters

For additional support or questions, consult the application documentation or contact the development team.

# online\WebFetchAndTransformTask.kt


## WebFetchAndTransformTask Documentation


### Overview
WebFetchAndTransformTask is a specialized task that fetches web content from a specified URL, strips HTML formatting, and transforms the content according to user-defined goals. This task is particularly useful for content aggregation, data extraction, and content transformation workflows.


### Configuration Parameters


#### Required Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| url | String | The target URL to fetch content from |
| transformationGoal | String | Specification of how the content should be transformed |


#### Optional Parameters
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| task_description | String? | null | Custom description of the task instance |
| task_dependencies | List<String>? | null | List of dependent task IDs |
| state | TaskState? | null | Current state of the task |


### Features and Capabilities


#### HTML Processing
- Removes unnecessary HTML elements (scripts, styles, iframes)
- Strips data attributes and comments
- Preserves important attributes (href, src, alt, title)
- Handles relative to absolute URL conversion
- Implements content length limits


#### Content Transformation
- Uses AI-powered content transformation via SimpleActor
- Supports custom transformation goals
- Maintains markdown formatting in output
- Integrates with UI rendering system


### Usage Examples


#### Basic Usage
```kotlin
val task = WebFetchAndTransformTask(
    planSettings = PlanSettings(),
    planTask = WebFetchAndTransformTaskConfigData(
        url = "https://example.com",
        transformationGoal = "Summarize the main points"
    )
)
```

#### Advanced Usage with Dependencies

```kotlin
val task = WebFetchAndTransformTask(
  planSettings = PlanSettings(),
  planTask = WebFetchAndTransformTaskConfigData(
    url = "https://example.com",
    transformationGoal = "Extract product specifications",
    task_dependencies = listOf("previous_task_id"),
    task_description = "Extract and format product details"
  )
)
```

### Limitations and Constraints

- Maximum content length: 100KB
- Requires valid HTTP URLs
- JavaScript-rendered content not supported
- Rate limiting may apply to web requests
- Transformation quality depends on AI model capabilities

### Error Handling

#### Common Errors

- Invalid URLs
- Network connectivity issues
- Content too large
- HTML parsing failures
- API rate limits

#### Error Prevention

```kotlin
try {
  val content = fetchAndStripHtml(url)
  // Process content
} catch (e: Exception) {
  log.error("Failed to fetch content", e)
  // Handle error appropriately
}
```

### Integration Points

- PlanCoordinator: Task execution coordination
- ChatClient: AI model integration
- MarkdownUtil: Output formatting
- SessionTask: Progress tracking
- UI System: Result rendering

### Best Practices

1. URL Validation
  - Verify URL format before fetching
  - Implement timeouts for requests
  - Handle redirects appropriately

2. Content Processing
  - Set appropriate size limits
  - Implement retry logic for failed requests
  - Cache results when appropriate

3. Transformation Goals
  - Be specific in transformation requirements
  - Consider content type in goals
  - Validate transformed output

### Technical Details

#### HTML Processing Pipeline

1. Fetch raw content
2. Parse HTML using Jsoup
3. Remove unnecessary elements
4. Clean attributes
5. Convert URLs
6. Optimize output

#### Content Transformation

1. Prepare cleaned content
2. Generate transformation prompt
3. Process via AI model
4. Format output
5. Render results

### Performance Considerations

- Implements content length limits
- Optimizes HTML processing
- Uses efficient HTTP client
- Manages memory usage
- Implements filtering pipeline

### Security Considerations

- URL validation
- Content sanitization
- HTML security filtering
- API key protection
- Rate limiting support

# plan\ForeachTask.kt

## ForeachTask Documentation

### Overview

The ForeachTask is a specialized task type that enables iterative execution of subtasks over a list of items. It provides a structured way to process multiple
items using the same task configuration, making it ideal for batch processing and parallel execution scenarios.

### Configuration Parameters

#### Required Parameters

| Parameter       | Type                        | Description                                            |
|-----------------|-----------------------------|--------------------------------------------------------|
| foreach_items   | List<String>                | The list of items to iterate over                      |
| foreach_subplan | Map<String, TaskConfigBase> | Map of subtask configurations to execute for each item |

#### Optional Parameters

| Parameter         | Type         | Default | Description                           |
|-------------------|--------------|---------|---------------------------------------|
| task_description  | String       | null    | Description of the task's purpose     |
| task_dependencies | List<String> | null    | List of task IDs this task depends on |
| state             | TaskState    | null    | Current state of the task             |

### Usage Example

```kotlin
val foreachConfig = ForeachTaskConfigData(
  foreach_items = listOf("item1", "item2", "item3"),
  foreach_subplan = mapOf(
    "subtask1" to TaskConfigBase(
      task_type = "ProcessingTask",
      task_description = "Process item"
    )
  ),
  task_description = "Process multiple items sequentially"
)

val foreachTask = ForeachTask(planSettings, foreachConfig)
```

### Features and Capabilities

- Parallel processing of multiple items
- Automatic subtask creation and management
- Progress tracking for each item
- Integrated error handling
- Visual progress representation through diagrams

### Integration Points

#### System Components

- Integrates with PlanCoordinator for task management
- Uses ChatClient for API communication
- Supports OpenAIClient for AI operations
- Works with SessionTask for UI updates

#### Data Flow

1. Receives input items list
2. Creates subtasks for each item
3. Executes subtasks in sequence
4. Reports progress through UI
5. Aggregates results

### Error Handling

- Validates required parameters before execution
- Throws RuntimeException for missing configuration
- Provides detailed error messages for troubleshooting
- Maintains execution state for recovery

### Best Practices

1. **Item Management**
  - Keep item list sizes manageable
  - Ensure items are properly formatted
  - Use meaningful item descriptions

2. **Subtask Configuration**
  - Design subtasks for independent execution
  - Include clear task descriptions
  - Maintain consistent naming conventions

3. **Performance**
  - Monitor resource usage during execution
  - Consider batch size limitations
  - Plan for parallel execution overhead

### Limitations and Constraints

- Requires non-null foreach_items and foreach_subplan
- Subtasks must be compatible with parallel execution
- Memory usage scales with number of items
- UI updates may impact performance with large item sets

### Output Format

The task produces:

- Progress updates for each item
- Completion status for subtasks
- Final completion message with item count
- Visual diagram of execution progress

### Technical Details

#### Implementation Notes

- Uses Kotlin coroutines for concurrent execution
- Maintains task state through PlanProcessingState
- Supports dynamic task description updates
- Integrates with logging framework

#### Performance Characteristics

- Execution time scales linearly with item count
- Memory usage depends on subtask complexity
- UI updates may affect overall performance

### Support Information

#### Logging

```kotlin
private val log = LoggerFactory.getLogger(ForeachTask::class.java)
```

#### Troubleshooting

1. Check foreach_items is properly populated
2. Verify foreach_subplan configuration
3. Monitor task execution through logs
4. Review UI updates for progress

#### Related Components

- PlanCoordinator
- SessionTask
- PlanUtil
- TaskConfigBase

# plan\PlanningTask.kt

## Task Planning Task Documentation

### Table of Contents

1. [Overview](#overview)
2. [Configuration](#configuration)
3. [Features and Capabilities](#features-and-capabilities)
4. [Usage Examples](#usage-examples)
5. [Integration Points](#integration-points)
6. [Best Practices](#best-practices)
7. [Limitations](#limitations)
8. [Error Handling](#error-handling)

### Overview

The Planning Task is responsible for high-level task organization and decomposition within the system. It breaks down complex goals into manageable, actionable
tasks while ensuring proper information flow and dependencies between tasks.

#### Key Functions

- Task decomposition and organization
- Dependency management
- Parallel execution optimization
- Dynamic task refinement
- Information flow coordination

### Configuration

#### PlanningTaskConfigData Class

```kotlin
class PlanningTaskConfigData(
  task_description: String? = null,        // Description of the planning task
  task_dependencies: List<String>? = null, // List of dependent task IDs
  state: TaskState? = TaskState.Pending,   // Current task state
)
```

#### TaskBreakdownResult Class

```kotlin
data class TaskBreakdownResult(
  val tasksByID: Map<String, TaskConfigBase>? = null // Map of task IDs to task configurations
)
```

### Features and Capabilities

#### Task Planning Features

- High-level goal decomposition
- Dynamic task breakdown
- Dependency management
- Parallel execution optimization
- Information flow coordination

#### Task Coordination

- Manages upstream and downstream task relationships
- Ensures efficient information transfer
- Optimizes task execution order
- Supports parallel task execution where possible

### Usage Examples

#### Basic Planning Task Setup

```kotlin
val planningTask = PlanningTask(
  planSettings = PlanSettings(),
  planTask = PlanningTaskConfigData(
    task_description = "Implement new feature X",
    task_dependencies = listOf("task1", "task2")
  )
)
```

### Integration Points

#### System Components

- Integrates with `PlanCoordinator` for task execution
- Works with `ApplicationInterface` for UI updates
- Uses `ChatClient` and `OpenAIClient` for AI interactions
- Coordinates with other tasks through the planning system

#### Data Flow

1. Receives input through messages
2. Processes through AI planning
3. Creates subtasks
4. Manages execution through coordinator
5. Updates UI with progress

### Best Practices

#### Planning Optimization

- Break down complex tasks into manageable units
- Ensure clear task dependencies
- Maximize parallel execution opportunities
- Maintain clear information flow between tasks

#### Task Design

- Define clear task boundaries
- Specify explicit dependencies
- Ensure proper information transfer
- Consider execution efficiency

### Limitations

#### System Constraints

- Dependent on AI service availability
- Limited by planning model capabilities
- Requires proper configuration setup
- May have performance implications with large task sets

### Error Handling

#### Common Issues

- AI service connectivity issues
- Invalid task configurations
- Dependency resolution failures
- Execution coordination errors

#### Error Recovery

- Implements logging for debugging
- Supports task state management
- Allows for task revision and refinement
- Provides error feedback through UI

#### Logging

```kotlin
private val log = LoggerFactory.getLogger(PlanningTask::class.java)
```

This documentation provides a comprehensive overview of the Planning Task component, its configuration, usage, and best practices. For specific implementation
details or additional support, refer to the codebase or contact the development team.
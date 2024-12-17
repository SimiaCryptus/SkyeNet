# CommandAutoFixTask.kt

Here's the user documentation for the CommandAutoFixTask:


## CommandAutoFixTask Documentation


### Overview
The CommandAutoFixTask is a specialized task designed to execute commands and automatically fix any issues that arise during execution. It provides a robust framework for running commands with automatic error detection and correction capabilities.


### Key Features
- Execute multiple commands in specified working directories
- Automatic error detection and retry mechanism
- Support for configurable auto-fix attempts
- Integration with the broader planning system


### Configuration


#### Task Settings
The task is configured using `CommandAutoFixTaskSettings`:

```kotlin
class CommandAutoFixTaskSettings(
    task_type: String,
    enabled: Boolean = false,
    model: ChatModel? = null,
    commandAutoFixCommands: List<String>? = listOf()
)
```

- `enabled`: Enables/disables the task
- `model`: Specifies the chat model to use
- `commandAutoFixCommands`: List of allowed command executables for auto-fixing


#### Task Configuration Data
Commands are specified using `CommandAutoFixTaskConfigData`:

```kotlin
class CommandAutoFixTaskConfigData(
    commands: List<CommandWithWorkingDir>? = null,
    task_description: String? = null,
    task_dependencies: List<String>? = null,
    state: TaskState? = null
)
```

Each command is defined by:
```kotlin
data class CommandWithWorkingDir(
    command: List<String> = emptyList(),
    workingDir: String? = null
)
```


### Usage

1. **Define Commands**: Specify the commands to be executed along with their working directories:
```json
{
    "commands": [
        {
            "command": ["executable", "arg1", "arg2"],
            "workingDir": "path/to/working/dir"
        }
    ]
}
```

2. **Configure Auto-Fix**: Set `autoFix` in plan settings to enable automatic fixing (default: 5 retries)

3. **Monitor Execution**: The task will:
   - Execute each command in sequence
   - Report success/failure for each command
   - Attempt auto-fixes if enabled
   - Provide detailed execution results


### Error Handling

- Failed commands trigger the auto-fix mechanism if enabled
- The task will retry up to 5 times with auto-fix enabled
- Detailed error messages and fix attempts are logged
- Manual intervention possible through UI accept buttons


### Example

```json
{
    "task_type": "CommandAutoFix",
    "commands": [
        {
            "command": ["gradle", "build"],
            "workingDir": "project/backend"
        },
        {
            "command": ["npm", "install"],
            "workingDir": "project/frontend"
        }
    ],
    "task_description": "Build and install project dependencies"
}
```


### Best Practices

1. Always specify absolute or well-defined relative paths for working directories
2. List commands in logical execution order
3. Use appropriate task dependencies when part of a larger plan
4. Monitor auto-fix results to ensure desired outcomes
5. Keep command lists in settings up-to-date


### Limitations

- Auto-fix capability depends on the configured model's ability to understand and fix issues
- Some errors may require manual intervention
- Working directories must exist or be creatable
- Commands must be in the allowed list specified in settings


### Integration

The task integrates with:
- Plan Coordinator for overall execution flow
- UI system for progress display
- File system for working directory management
- Chat models for error analysis and fixing

For more detailed information about specific features or integration points, please consult the API documentation or contact support.

# CommandSessionTask.kt

Here's the user documentation for the CommandSessionTask class:


## CommandSessionTask Documentation


### Overview
CommandSessionTask is a specialized task handler that enables creation and management of interactive command-line sessions. It allows you to execute commands and maintain stateful interactions across multiple commands within a single session.


### Features
- Create and manage interactive command sessions
- Support for multiple concurrent sessions
- Session persistence between commands
- Configurable timeouts
- Session reuse via session IDs
- Automatic session cleanup


### Configuration Options


#### CommandSessionTaskConfigData Parameters

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| command | List<String> | The command and arguments to start the interactive session | Required |
| inputs | List<String> | List of commands to send to the session | Empty list |
| sessionId | String? | Unique identifier for session reuse | null |
| timeout | Long | Command execution timeout in milliseconds | 30000 (30s) |
| closeSession | Boolean | Whether to close the session after execution | false |


### Usage Examples


#### Basic Command Session
```kotlin
val config = CommandSessionTaskConfigData(
    command = listOf("python", "-i"),
    inputs = listOf(
        "print('Hello, World!')",
        "2 + 2"
    )
)
```


#### Persistent Session
```kotlin
val config = CommandSessionTaskConfigData(
    command = listOf("mysql", "-u", "user", "-p"),
    inputs = listOf("SHOW DATABASES;"),
    sessionId = "mysql-session-1",
    closeSession = false
)
```


#### Session with Custom Timeout
```kotlin
val config = CommandSessionTaskConfigData(
    command = listOf("node"),
    inputs = listOf("console.log('test')"),
    timeout = 60000 // 60 seconds
)
```


### Limitations and Constraints
- Maximum of 10 concurrent sessions
- Default timeout of 30 seconds per command
- Output is limited to 5000 characters per command
- Sessions without IDs are temporary and closed after execution


### Output Format
The task outputs results in a structured markdown format:
```markdown

### Command Session Results
Command: <command string>
Session ID: <session id or "temporary">
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


### Best Practices
1. Use session IDs for long-running or reusable sessions
2. Close sessions when no longer needed
3. Set appropriate timeouts based on expected command execution time
4. Monitor active sessions to prevent resource exhaustion
5. Handle potential errors in command execution


### Error Handling
- The task handles process cleanup on errors
- Failed commands return error messages in the output
- Session limits are enforced with appropriate error messages
- Timeouts prevent hanging on long-running commands


### Session Management
- Use `closeSession()` to manually close specific sessions
- `closeAllSessions()` available for cleanup
- Inactive sessions are automatically cleaned up
- Monitor session count with `getActiveSessionCount()`

This task is particularly useful for scenarios requiring:
- Interactive shell sessions
- Database client interactions
- REPL environments
- Multi-step command execution
- Stateful command-line tools

# RunShellCommandTask.kt

Here's the user documentation for the RunShellCommandTask class:


## RunShellCommandTask Documentation


### Overview
The `RunShellCommandTask` class is a specialized task implementation for executing shell commands within a planned workflow. It provides a safe and controlled way to run system commands and capture their output.


### Configuration


#### RunShellCommandTaskConfigData
When creating a RunShellCommandTask, you can configure it using the following parameters:

- `command` (String): The specific shell command to be executed
- `workingDir` (String): The relative file path where the command should be executed
- `task_description` (String): A description of what the task aims to accomplish
- `task_dependencies` (List<String>): List of other tasks that must complete before this one runs
- `state` (TaskState): The current state of the task


### Usage


#### Basic Example
```kotlin
val taskConfig = RunShellCommandTaskConfigData(
    command = "ls -la",
    workingDir = "./project",
    task_description = "List all files in project directory"
)

val task = RunShellCommandTask(planSettings, taskConfig)
```


#### Features

1. **Safe Command Execution**
   - Commands are executed in a controlled environment
   - Output is captured and formatted for review
   - Error handling is built-in

2. **Working Directory Support**
   - Specify custom working directory for command execution
   - Defaults to plan's working directory if not specified

3. **Interactive UI**
   - Provides accept/reject buttons for command output
   - Includes revision capabilities for failed commands
   - Shows command execution results in formatted display


#### Safety Considerations

The task is designed for running simple and safe commands. Users should:
- Avoid commands that could harm the system
- Not execute commands with elevated privileges
- Be cautious with commands that modify system files
- Validate commands before accepting execution


### Integration

The task integrates with:
- Plan coordination system
- Session management
- User interface components
- Logging system


### Output Format

Command execution results are formatted as:

```markdown

### Shell Command Output

```
<command executed>
```

```
<command output>
```
```


### Error Handling

- Failed commands are logged
- Users can revise and retry failed commands
- Execution errors are captured and displayed in the UI


### Best Practices

1. Always provide clear task descriptions
2. Specify dependencies when commands rely on previous tasks
3. Use relative paths in working directory specifications
4. Review command output before accepting results
5. Keep commands simple and focused on single operations


### Limitations

- Cannot execute privileged commands
- Limited to shell commands available in the execution environment
- May be restricted by system security policies


### Related Components

- PlanCoordinator
- SessionTask
- CodingAgent
- ProcessInterpreter

This task is part of a larger planning system and should be used within the context of a properly configured plan with appropriate settings and security measures.

# SeleniumSessionTask.kt


## SeleniumSessionTask Documentation


### Overview
The SeleniumSessionTask is a powerful tool for automating web browser interactions using Selenium WebDriver. It allows you to create and manage browser sessions, execute JavaScript commands, and perform web scraping or testing tasks.


### Features
- Create new browser sessions or reuse existing ones
- Execute JavaScript commands in sequence
- Navigate to specified URLs
- Configurable timeouts
- Session management with automatic cleanup
- Headless browser operation
- Detailed execution results and logging


### Configuration Options


#### SeleniumSessionTaskConfigData Parameters
- `url` (String): The URL to navigate to (optional if reusing existing session)
- `commands` (List<String>): List of JavaScript commands to execute
- `sessionId` (String?): Optional ID for reusing existing sessions
- `timeout` (Long): Timeout in milliseconds for commands (default: 30000ms)
- `closeSession` (Boolean): Whether to close the session after execution
- `task_description` (String?): Optional description of the task
- `task_dependencies` (List<String>?): Optional list of dependent tasks
- `state` (TaskState?): Current state of the task


### Example JavaScript Commands

```javascript
// Get page title
return document.title;

// Get element text by class
return document.querySelector('.my-class').textContent;

// Get all links on page
return Array.from(document.querySelectorAll('a')).map(a => a.href);

// Click a button
document.querySelector('#my-button').click();

// Scroll to bottom of page
window.scrollTo(0, document.body.scrollHeight);

// Get entire page HTML
return document.documentElement.outerHTML;

// Async operation with timeout
return new Promise(r => setTimeout(() => r(document.title), 1000));
```


### Usage Examples


#### Basic Navigation and Scraping
```kotlin
SeleniumSessionTaskConfigData(
    url = "https://example.com",
    commands = listOf(
        "return document.title;",
        "return Array.from(document.querySelectorAll('p')).map(p => p.textContent);"
    )
)
```


#### Reusing a Session
```kotlin
SeleniumSessionTaskConfigData(
    sessionId = "session1",
    commands = listOf(
        "document.querySelector('#search').value = 'query';",
        "document.querySelector('#submit').click();"
    ),
    timeout = 60000
)
```


#### Session with Cleanup
```kotlin
SeleniumSessionTaskConfigData(
    url = "https://example.com",
    commands = listOf("return document.title;"),
    closeSession = true
)
```


### Best Practices

1. **Session Management**
   - Use `sessionId` when you need to maintain state across multiple tasks
   - Set `closeSession = true` when you're done with the session
   - Monitor active sessions count to avoid resource exhaustion

2. **Error Handling**
   - Include appropriate waits for dynamic content
   - Use try-catch blocks in JavaScript commands
   - Check for element existence before interactions

3. **Performance**
   - Keep commands focused and minimal
   - Use headless mode for better performance
   - Clean up sessions when no longer needed

4. **Security**
   - Validate URLs before navigation
   - Sanitize JavaScript commands
   - Handle sensitive data appropriately


### Limitations

- Maximum of 10 concurrent sessions
- Default timeout of 30 seconds per command
- Headless Chrome browser only
- Results and page source are truncated in output


### Troubleshooting


#### Common Issues
1. **Session Creation Fails**
   - Check system resources
   - Verify WebDriver installation
   - Ensure no conflicting browser instances

2. **Command Timeout**
   - Increase timeout value
   - Check network connectivity
   - Verify page load completion

3. **Element Interaction Fails**
   - Verify element selectors
   - Add appropriate waits
   - Check element visibility/enabled state


#### Error Messages
Error messages include:
- Exception message
- Stack trace (first 3 lines)
- Failed command details


### Support
For issues and questions:
- Check the logs for detailed error messages
- Review active sessions status
- Verify JavaScript command syntax
- Ensure proper WebDriver setup

# file\AbstractAnalysisTask.kt

Here's the user documentation for the AbstractAnalysisTask class:


## AbstractAnalysisTask Documentation


### Overview
`AbstractAnalysisTask` is an abstract base class for implementing file analysis tasks in the SkyeNet platform. It extends `AbstractFileTask` and provides a framework for analyzing code files and applying suggested changes.


### Key Features
- Automated code analysis using AI
- Configurable analysis behavior through task settings
- Automatic application of suggested changes
- Integration with the plan coordination system


### Usage


#### Creating a New Analysis Task
To create a new analysis task, extend `AbstractAnalysisTask` and implement the required abstract members:

```kotlin
class MyAnalysisTask(
    planSettings: PlanSettings,
    planTask: MyTaskConfig?
) : AbstractAnalysisTask<MyTaskConfig>(planSettings, planTask) {
    override val actorName = "My Analysis Task"
    override val actorPrompt = "Instructions for the AI analyzer"
    
    override fun getAnalysisInstruction(): String {
        return "Specific instructions for analyzing the code"
    }
}
```


#### Required Implementations

1. `actorName`: String identifying the analysis task
2. `actorPrompt`: Instructions for the AI model performing the analysis
3. `getAnalysisInstruction()`: Method returning specific analysis instructions


#### Configuration

The task uses several configuration parameters from `PlanSettings`:
- Model selection
- Temperature settings
- Auto-fix behavior
- Task-specific settings


#### Execution Flow

1. The task analyzes input files using the configured AI model
2. Analysis results are processed
3. If changes are suggested, they are automatically applied (if auto-fix is enabled)
4. Results are reported back through the task system


#### Example Usage

```kotlin
val task = MyAnalysisTask(
    planSettings = myPlanSettings,
    planTask = myTaskConfig
)

task.run(
    agent = planCoordinator,
    messages = listOfMessages,
    task = sessionTask,
    api = chatClient,
    resultFn = { result -> println(result) },
    api2 = openAIClient,
    planSettings = myPlanSettings
)
```


### Error Handling

The task provides feedback through the task system:
- Success message when changes are applied successfully
- Error message with exit code when changes fail to apply


### Dependencies

- OpenAI API client
- SkyeNet platform components
- Plan coordination system


### Best Practices

1. Provide clear and specific analysis instructions
2. Test analysis tasks with various input types
3. Monitor execution results and error messages
4. Configure appropriate model and temperature settings


### Notes

- Abstract class - must be extended to create concrete implementations
- Integrates with the broader SkyeNet planning and execution system
- Supports automated code modification based on analysis results

This documentation provides a high-level overview of the AbstractAnalysisTask class and its usage. For specific implementations, refer to the concrete classes that extend this abstract base class.

# file\AbstractFileTask.kt

Here's the user documentation for the AbstractFileTask class:


## AbstractFileTask Documentation


### Overview
`AbstractFileTask` is an abstract base class for file-based tasks in the planning system. It provides common functionality for tasks that need to process input and output files based on glob patterns.


### Key Features


#### File Configuration
The class uses `FileTaskConfigBase` to configure:
- Input files: Files to be processed by the task
- Output files: Files to be generated by the task
- Task dependencies and state management


#### File Pattern Matching
- Supports glob pattern matching for file selection
- Automatically walks directory trees to find matching files
- Filters files based on LLM (Language Learning Model) compatibility


### Usage


#### Creating a Task
To create a file-based task, extend `AbstractFileTask`:

```kotlin
class MyFileTask(
    planSettings: PlanSettings,
    planTask: FileTaskConfigBase?
) : AbstractFileTask<FileTaskConfigBase>(planSettings, planTask)
```


#### Configuring File Patterns
Specify input and output file patterns in the task configuration:

```kotlin
val config = FileTaskConfigBase(
    task_type = "my_file_task",
    input_files = listOf("src/**/*.kt", "src/**/*.java"),
    output_files = listOf("build/**/*.class")
)
```


#### File Pattern Support
- Uses standard glob syntax
- Examples:
  - `**/*.kt`: All Kotlin files in any subdirectory
  - `src/*.java`: Java files directly in the src directory
  - `docs/**/*`: All files in the docs directory and subdirectories


#### Reading File Contents
The class provides `getInputFileCode()` which:
- Combines input and output file patterns
- Reads matching files
- Formats content with file paths as headers
- Handles reading errors gracefully


### Best Practices

1. **File Patterns**
   - Use specific patterns to limit file scope
   - Consider performance impact for large directories

2. **Error Handling**
   - Implement proper error handling for file operations
   - Check logs for file reading issues

3. **File Validation**
   - Respect LLM file compatibility checks
   - Validate file existence before processing


### Technical Details


#### Dependencies
- Requires Java FileSystems for path matching
- Uses Kotlin streams for file traversal
- Integrates with project's logging system


#### Limitations
- Only processes LLM-compatible files
- File reading errors are logged but not propagated


#### Performance Considerations
- Directory traversal may impact performance for large projects
- Consider limiting glob pattern scope for better performance


### Error Handling
The class logs warnings for file reading errors but continues processing other files. Check logs for:
- File access issues
- Invalid file contents
- Pattern matching problems

For more detailed information about implementing specific file tasks, refer to the API documentation or specific task implementations.

# file\CodeOptimizationTask.kt

Here's the user documentation for the CodeOptimizationTask class:


## Code Optimization Task

The Code Optimization Task is designed to analyze and optimize existing code to improve its quality, readability, and maintainability. This task uses AI-powered analysis to suggest improvements while following best practices and design patterns.


### Purpose

The main purpose of this task is to:
- Analyze existing code for potential improvements
- Suggest optimizations for better code quality
- Provide detailed explanations for suggested changes
- Focus on maintainability and readability


### Configuration


#### Required Parameters

- `input_files`: List of source code files to be analyzed and optimized
- `output_files`: List of files where optimized code will be saved


#### Optional Parameters

- `optimizationFocus`: List of specific areas to focus the optimization on
- `task_description`: Custom description of the optimization task
- `task_dependencies`: List of tasks that must be completed before this one
- `state`: Current state of the task


### Areas of Focus

The task analyzes code across five main areas:

1. Code structure and organization
2. Readability improvements
3. Maintainability enhancements
4. Language-specific features and best practices
5. Design pattern applications


### Output Format

The task generates output in markdown format, including:
- Detailed explanations for each suggested optimization
- Reasons for proposed changes
- Expected benefits
- Potential trade-offs
- Code snippets in diff format showing proposed changes


### Example Usage

```kotlin
val task = CodeOptimizationTask(
    planSettings = PlanSettings(),
    planTask = CodeOptimizationTaskConfigData(
        optimizationFocus = listOf("code structure", "design patterns"),
        input_files = listOf("src/main/kotlin/MyClass.kt"),
        output_files = listOf("src/main/kotlin/MyClass.optimized.kt")
    )
)
```


### Best Practices

1. Specify clear input and output files
2. Use the optimizationFocus parameter to target specific areas needing improvement
3. Review all suggested changes before implementing them
4. Consider the trade-offs mentioned in the analysis


### Task Type

This task is categorized as an `Optimization` task type in the system.


### Integration

The task integrates with the larger planning system and can be:
- Part of a larger optimization workflow
- Dependent on other tasks
- Used as a dependency for subsequent tasks

For more information about the planning system and task integration, refer to the main planning documentation.

# file\CodeReviewTask.kt

Here's the user documentation for the CodeReviewTask class:


## CodeReviewTask Documentation


### Overview
The `CodeReviewTask` class is a specialized task for performing automated code reviews. It extends `AbstractAnalysisTask` and provides functionality to analyze code files for quality, bugs, performance issues, and other important aspects of code review.


### Configuration


#### CodeReviewTaskConfigData
The task is configured using `CodeReviewTaskConfigData` which includes:

- `focusAreas`: Optional list of specific areas to focus on during the review
- `task_description`: Description of the task
- `task_dependencies`: List of tasks that must be completed before this one
- `input_files`: List of files to be reviewed
- `output_files`: List of files where review results will be saved
- `state`: Current state of the task


### Features

The code review analyzes:
1. Code quality and readability
2. Potential bugs or errors
3. Performance issues
4. Security vulnerabilities
5. Adherence to best practices and coding standards
6. Areas for improvement or optimization


### Usage Example

```kotlin
val reviewTask = CodeReviewTask(
    planSettings = PlanSettings(...),
    planTask = CodeReviewTaskConfigData(
        focusAreas = listOf("security", "performance"),
        input_files = listOf("src/main/kotlin/MyClass.kt"),
        task_description = "Review authentication module"
    )
)
```


### Output Format
The review results are formatted as a markdown document with:
- Organized sections using appropriate headings
- Code snippets highlighting specific issues
- Detailed recommendations for each finding


### Best Practices
1. Specify input files explicitly for targeted reviews
2. Use focusAreas to concentrate on specific aspects when needed
3. Review the generated markdown output for actionable insights
4. Consider task dependencies when integrating into larger workflows


### Notes
- The task uses an AI-powered actor named "CodeReview" for analysis
- Reviews are comprehensive by default if no focus areas are specified
- The task can be integrated into larger automated workflows

This task is ideal for maintaining code quality and identifying potential issues early in the development process.

# file\DocumentationTask.kt

Here's the user documentation for the DocumentationTask class:


## DocumentationTask

The `DocumentationTask` class is a specialized task implementation for generating documentation for code files in a project. It extends `AbstractFileTask` and provides functionality to automatically document code with comments and create markdown documentation files.


### Purpose

This task helps automate the documentation process by:
- Adding or updating code comments in existing files
- Creating new markdown documentation files
- Documenting specific topics or aspects of the codebase
- Maintaining consistent documentation format and style


### Configuration

The task is configured using `DocumentationTaskConfigData` which includes:

- `topics`: List of specific topics/aspects to document
- `input_files`: Source code files to analyze and document
- `output_files`: Files where documentation should be written
- `task_dependencies`: Other tasks that must complete before this one
- `task_description`: Description of what needs to be documented
- `state`: Current state of the task


### Usage

1. Create a new DocumentationTask instance:

```kotlin
val task = DocumentationTask(
    planSettings = yourPlanSettings,
    planTask = DocumentationTaskConfigData(
        topics = listOf("API", "Data Models"),
        input_files = listOf("src/main/kotlin/MyClass.kt"),
        output_files = listOf("docs/MyClass.md")
    )
)
```

2. The task will:
   - Analyze the input files
   - Generate appropriate documentation based on the specified topics
   - Add comments to existing code files and/or create new markdown files
   - Present changes for review/approval if autoFix is disabled
   - Automatically apply changes if autoFix is enabled


### Features

- **Smart Documentation Generation**: Uses AI to create detailed and clear documentation covering:
  - Purpose and functionality
  - Inputs and outputs
  - Assumptions and limitations
  - Code examples
  - Design decisions
  - Known issues
  - Maintenance guidance

- **Flexible Output Formats**:
  - In-code comments for existing files
  - Separate markdown files for new documentation
  - Consistent formatting and structure

- **Review Process**:
  - Shows diffs of proposed changes
  - Allows manual review and approval
  - Supports automatic application of changes
  - Provides links to modified files


### Settings

The task behavior can be controlled through PlanSettings:

- `autoFix`: When true, automatically applies documentation changes
- `model`: Specifies which AI model to use for documentation generation
- `temperature`: Controls creativity/variability in generated documentation


### Dependencies

- Requires a configured ChatClient for AI interaction
- Uses MarkdownUtil for rendering documentation
- Integrates with the project's UI system for displaying changes


### Error Handling

The task includes retry functionality for resilience against temporary failures and provides logging for troubleshooting.


### Example Output

```diff
// MyClass.kt
+ /**
+  * Handles user authentication and session management
+  * @param config Configuration parameters for authentication
+  */
  class MyClass(private val config: Config) {
```

```markdown

## MyClass Documentation


### Overview
Detailed description of MyClass functionality...
```


### Best Practices

1. Specify clear topics to focus the documentation effort
2. Review generated documentation for accuracy
3. Maintain consistent documentation style across files
4. Include both input and output files in configuration
5. Use task dependencies to ensure proper documentation order

# file\FileModificationTask.kt

Here's the user documentation for the FileModificationTask class:


## FileModificationTask Documentation


### Overview
The `FileModificationTask` class is a specialized task for modifying existing files or creating new files within a project. It provides an AI-powered interface for making code changes while maintaining code quality and project consistency.


### Features
- Modifies existing files with proper diff formatting
- Creates new files with appropriate structure and conventions
- Maintains context and provides rationale for changes
- Supports automatic or manual application of changes
- Integrates with project coding standards


### Configuration


#### FileModificationTaskConfigData Parameters
- `input_files`: List of source files to be examined/modified
- `output_files`: List of files to be created/modified
- `modifications`: Specific changes to be made to the files
- `task_description`: Description of the modification task
- `task_dependencies`: List of dependent tasks that must complete first
- `state`: Current state of the task


### Usage


#### Basic Setup
```kotlin
val taskConfig = FileModificationTaskConfigData(
    input_files = listOf("src/main/kotlin/MyFile.kt"),
    output_files = listOf("src/main/kotlin/NewFile.kt"),
    task_description = "Add logging functionality to MyFile.kt and create a new utility class"
)

val task = FileModificationTask(planSettings, taskConfig)
```


#### Task Execution
The task will:
1. Analyze input files and requirements
2. Generate appropriate code modifications
3. Present changes in a diff format for review
4. Apply changes automatically (if autoFix is enabled) or wait for manual approval


#### Output Format
Changes are presented in markdown format with:
- Diff blocks for existing file modifications
- Code blocks for new files
- File paths as headers
- Context lines around changes

Example output:
```markdown

#### src/main/kotlin/MyFile.kt
```diff
function existingMethod() {
-  return "old value"
+  logger.info("Returning new value")
+  return "new value"
}
```


#### src/main/kotlin/NewFile.kt
```kotlin
class LoggingUtil {
    private val logger = LoggerFactory.getLogger(LoggingUtil::class.java)
    // ... new code
}
```


### Configuration Options


#### Auto-Fix Mode
- Enable `autoFix` in plan settings for automatic application of changes
- Disable for manual review and approval process


#### Model Selection
- Uses task-specific model if configured in plan settings
- Falls back to default model if not specified


### Best Practices

1. **Input Files**
   - Specify all relevant files that need examination
   - Include files that might be affected by changes

2. **Task Description**
   - Be specific about required changes
   - Include context and rationale
   - Mention any constraints or requirements

3. **Review Process**
   - Review generated changes carefully before applying
   - Check for unintended side effects
   - Verify integration with existing code


### Error Handling

The task includes error checking for:
- Missing input/output files
- Invalid file paths
- Execution failures with retry capability


### Integration

The task integrates with:
- Project's UI system for displaying changes
- File system for applying modifications
- Source control systems (through file system changes)


### Limitations

- Requires proper file access permissions
- Changes are limited to specified files
- Complex refactoring may require multiple tasks


### See Also

- PlanCoordinator
- SessionTask
- SimpleActor
- TaskType.FileModification

This documentation should help users effectively utilize the FileModificationTask for code modifications and file creation within their projects.

# file\FileSearchTask.kt


## File Search Task Documentation


### Overview
The File Search Task is a utility that allows you to search through files in your project for specific patterns or text, providing matched results with surrounding context lines. This is useful for finding specific code snippets, text patterns, or occurrences of certain strings across multiple files.


### Configuration Options


#### Search Pattern
- **Parameter**: `search_pattern`
- **Description**: The text or pattern you want to search for in the files
- **Type**: String
- **Required**: Yes


#### Pattern Type
- **Parameter**: `is_regex`
- **Description**: Specifies whether the search pattern should be treated as:
  - Regular expression (true)
  - Literal text/substring (false)
- **Type**: Boolean
- **Default**: false


#### Context Lines
- **Parameter**: `context_lines`
- **Description**: Number of lines to display before and after each match
- **Type**: Integer
- **Default**: 2


#### Input Files
- **Parameter**: `input_files`
- **Description**: List of files or file patterns to search within
- **Type**: List of strings
- **Supports**: Glob patterns (e.g., "*.kt", "src/**/*.java")
- **Optional**: If not specified, no files will be searched


### Output Format

The search results are formatted in Markdown with the following structure:

```markdown

## Search Results


### [Filename]


#### Line [Number]

```
[Context lines before match]
> [Line number]: [Matching line]
[Context lines after match]
```
```


### Examples


#### Basic Text Search
```json
{
  "search_pattern": "TODO",
  "is_regex": false,
  "context_lines": 2,
  "input_files": ["src/**/*.kt"]
}
```


#### Regex Pattern Search
```json
{
  "search_pattern": "class\\s+\\w+Controller",
  "is_regex": true,
  "context_lines": 3,
  "input_files": ["src/main/kotlin/**/*.kt"]
}
```


### Notes
- The search is case-sensitive
- Only text-based files that are suitable for LLM processing will be included in the search
- Files are processed using the project's root directory as the base path
- Large files or binary files may be automatically excluded from the search


### Best Practices
1. Start with specific file patterns to narrow down the search scope
2. Use appropriate context lines based on your needs:
   - More lines for understanding code context
   - Fewer lines for quick pattern matching
3. Use regex mode when you need more complex pattern matching
4. Keep search patterns focused to avoid overwhelming results


### Error Handling
- Invalid regex patterns will cause the search to fail
- Inaccessible files will be skipped
- Binary files and very large files are automatically excluded

# file\InquiryTask.kt

Here's the user documentation for the InquiryTask class:


## InquiryTask Documentation


### Overview
The InquiryTask is a specialized task type that allows users to ask questions and gather information about specific files in a project. It analyzes the specified files and provides comprehensive answers and insights based on the given inquiry questions and goals.


### Configuration


#### Required Parameters
- `inquiry_questions`: A list of specific questions or topics to be addressed
- `inquiry_goal`: The overall goal or purpose of the inquiry
- `input_files`: List of files or file patterns to analyze (supports glob patterns)


#### Optional Parameters
- `task_description`: A description of the task
- `task_dependencies`: List of other tasks this task depends on
- `state`: Current state of the task


### Example Configuration
```json
{
  "inquiry_questions": [
    "What is the main purpose of this module?",
    "How are error cases handled?"
  ],
  "inquiry_goal": "Understand the core functionality and error handling patterns",
  "input_files": [
    "src/main/kotlin/**/*.kt",
    "src/test/kotlin/**/*Test.kt"
  ],
  "task_description": "Analyze core module implementation",
  "task_dependencies": ["setup_task"]
}
```


### Features


#### File Pattern Matching
- Supports glob patterns for file selection
- Automatically filters out non-readable or binary files
- Processes files in a sorted order


#### Interactive Mode
When `allowBlocking` is enabled in plan settings:
- Provides interactive discussion capability
- Allows users to refine and revise inquiries
- Supports iterative exploration of the codebase


#### Non-Interactive Mode
When `allowBlocking` is disabled:
- Generates a single comprehensive report
- Results are displayed directly in the task output


### Best Practices

1. **Question Formulation**
   - Make questions specific and focused
   - Break down complex inquiries into smaller, clear questions
   - Include context when necessary

2. **File Selection**
   - Use specific file patterns to limit scope
   - Include relevant test files for complete understanding
   - Avoid including unnecessary files that may dilute the analysis

3. **Goal Setting**
   - Define clear objectives for the inquiry
   - Align goals with overall project needs
   - Focus on actionable insights


### Limitations

- File size and number may impact processing time
- Binary files and certain file types are automatically excluded
- Response quality depends on the clarity of questions and goals


### Error Handling

The task handles various error cases:
- Invalid file paths
- Unreadable files
- File access errors

Errors are logged but don't halt task execution.


### Integration with Other Tasks

InquiryTask can be used to:
- Gather information before code modifications
- Validate assumptions about existing code
- Document system behavior
- Support decision-making for other tasks


### Example Usage

```kotlin
val inquiryTask = InquiryTask(
    planSettings = PlanSettings(),
    planTask = InquiryTaskConfigData(
        inquiry_questions = listOf(
            "What are the main classes in the authentication module?",
            "How is user session handling implemented?"
        ),
        inquiry_goal = "Understand authentication architecture",
        input_files = listOf("src/main/kotlin/auth/**/*.kt")
    )
)
```

This documentation should help users effectively utilize the InquiryTask for analyzing and understanding code within their projects.

# file\PerformanceAnalysisTask.kt

Here's the user documentation for the PerformanceAnalysisTask class:


## Performance Analysis Task

The Performance Analysis Task is a specialized tool designed to analyze code for performance issues and bottlenecks. It provides detailed insights into various performance aspects of your codebase.


### Overview

This task performs automated performance analysis of specified code files, focusing on:

1. Time complexity of algorithms
2. Memory usage and potential memory leaks
3. I/O operations and network calls
4. Concurrency and parallelism opportunities
5. Caching and memoization possibilities


### Configuration


#### Required Parameters

- `input_files`: List of file paths to be analyzed


#### Optional Parameters

- `analysis_focus`: List of specific areas to focus the analysis on (e.g., ["time complexity", "memory usage", "I/O operations"])
- `task_description`: Custom description of the analysis task
- `task_dependencies`: List of tasks that must be completed before this analysis
- `output_files`: List of files where the analysis results should be saved
- `state`: Current state of the task


### Usage Example

```kotlin
val task = PerformanceAnalysisTask(
    planSettings = PlanSettings(),
    planTask = PerformanceAnalysisTaskConfigData(
        input_files = listOf("src/main/kotlin/MyClass.kt"),
        analysis_focus = listOf("time complexity", "memory usage"),
        task_description = "Analyze performance of user authentication module"
    )
)
```


### Output Format

The analysis results are provided in a markdown document format with:

- Structured headings for different performance aspects
- Detailed explanations of identified issues
- Impact assessment of each performance concern
- Quantitative estimates where applicable
- Code snippets highlighting problematic areas


### Best Practices

1. Specify input files that are logically related to get more meaningful analysis
2. Use the `analysis_focus` parameter to target specific performance concerns
3. Review the complete analysis report before implementing any changes
4. Consider task dependencies if the analysis needs to be part of a larger workflow


### Limitations

- The task performs analysis only; it does not automatically implement improvements
- Analysis quality depends on the clarity and structure of the input code
- Quantitative estimates are approximations and should be validated in real-world scenarios


### Integration

This task can be integrated into larger workflows and can be used as part of a comprehensive code review process. It works well with other code analysis tasks and can be chained with implementation tasks for a complete performance optimization cycle.

# file\RefactorTask.kt

Here's the user documentation for the RefactorTask class:


## RefactorTask Documentation


### Overview
The RefactorTask is a specialized code analysis task that helps developers improve existing code through systematic refactoring. It analyzes source code and provides detailed suggestions for improving code structure, readability, and maintainability.


### Configuration


#### RefactorTaskConfigData Parameters
- `refactoringFocus`: Optional list of specific areas to focus the refactoring on, such as:
  - Modularity
  - Design patterns
  - Naming conventions
- `task_description`: Optional description of the refactoring task
- `task_dependencies`: Optional list of tasks that must be completed before this one
- `input_files`: List of source files to be analyzed and refactored
- `output_files`: List of files where refactored code should be saved
- `state`: Current state of the task


### Features

The task analyzes code focusing on:
1. Code organization improvements
2. Reduction of code duplication
3. Enhanced modularity
4. Strategic application of design patterns
5. Better naming conventions
6. Simplification of complex logic


### Output Format

The task generates a markdown document containing:
- Detailed refactoring suggestions
- Explanations for each suggested change
- Expected benefits
- Potential trade-offs
- Code snippets in diff format showing proposed changes


### Example Usage

```kotlin
val refactorTask = RefactorTask(
    planSettings = PlanSettings(),
    planTask = RefactorTaskConfigData(
        refactoringFocus = listOf("modularity", "naming conventions"),
        input_files = listOf("src/main/kotlin/MyClass.kt"),
        output_files = listOf("src/main/kotlin/RefactoredClass.kt")
    )
)
```


### Best Practices

1. Specify clear refactoring focus areas to get more targeted suggestions
2. Review all suggested changes carefully before implementation
3. Consider the impact on dependent code
4. Test thoroughly after applying refactoring changes


### Integration

The RefactorTask integrates with the broader task planning system and can be:
- Sequenced with other tasks
- Used as part of a larger code improvement initiative
- Incorporated into automated code review processes


### Notes

- The task uses AI-powered analysis to suggest improvements
- All suggestions should be reviewed by developers before implementation
- Consider running tests before and after applying refactoring changes

# file\SecurityAuditTask.kt

Here's the user documentation for the SecurityAuditTask class:


## SecurityAuditTask Documentation


### Overview
The SecurityAuditTask class is a specialized task for performing automated security audits on code files. It analyzes code for potential security vulnerabilities, insecure practices, and provides recommendations for improvements.


### Configuration


#### SecurityAuditTaskConfigData Parameters
- `focusAreas`: (Optional) List of specific security areas to focus on during the audit
- `task_description`: (Optional) Custom description of the audit task
- `task_dependencies`: (Optional) List of tasks that must be completed before this audit
- `input_files`: List of files to be audited
- `output_files`: (Optional) Files where audit results will be saved
- `state`: Current state of the task


### Features

The security audit analyzes code for:
1. Potential security vulnerabilities
2. Insecure coding practices 
3. Compliance with security standards
4. Proper handling of sensitive data
5. Authentication and authorization issues
6. Input validation and sanitization


### Output Format
- Results are formatted as a markdown document
- Includes section headings for different security aspects
- Contains code snippets highlighting issues
- Uses diff format to clearly show recommended security fixes


### Example Usage

```kotlin
val auditTask = SecurityAuditTask(
    planSettings = PlanSettings(),
    planTask = SecurityAuditTaskConfigData(
        focusAreas = listOf("authentication", "data encryption"),
        input_files = listOf("src/main/kotlin/auth/LoginService.kt"),
        task_description = "Audit authentication system security"
    )
)
```


### Best Practices
1. Specify input files that are logically related
2. Use focusAreas to target specific security concerns
3. Review the generated diff suggestions carefully before implementing
4. Consider running security audits regularly as part of development workflow


### Notes
- The task runs as part of a larger planning system
- Results are provided in a clear, actionable format
- Recommendations follow security best practices and standards

This task helps maintain code security by providing automated analysis and improvement suggestions for your codebase.

# file\TestGenerationTask.kt

Here's the user documentation for the TestGenerationTask class:


## Test Generation Task

The Test Generation Task is a specialized tool designed to automatically generate comprehensive unit tests for your code files. This task helps ensure code quality and maintainability by creating test suites that cover all aspects of your code.


### Features

- Generates complete, runnable unit tests for specified code files
- Creates tests for all public methods and functions
- Includes both positive and negative test cases
- Tests edge cases and boundary conditions
- Aims for high code coverage
- Follows language-specific testing best practices


### Configuration

To use the Test Generation Task, you need to configure the following parameters:


#### Required Fields

- `filesToTest`: List of source code files for which tests should be generated
- `inputReferences`: List of input files or tasks to be examined when generating tests


#### Optional Fields

- `task_description`: Custom description of the test generation task
- `task_dependencies`: List of tasks that must complete before this task runs
- `state`: Current state of the task


### Output

The task generates test files with the following characteristics:

- Test files are created in a `test` directory parallel to the source files
- Each test file includes:
  - Appropriate testing framework imports
  - Setup and teardown methods (if necessary)
  - Comprehensive test cases with comments
  - Assertions for expected behavior


### Example Usage

```kotlin
val testTask = TestGenerationTask(
    planSettings = PlanSettings(),
    planTask = TestGenerationTaskConfigData(
        input_files = listOf("src/main/kotlin/com/example/Utils.kt"),
        output_files = listOf("src/test/kotlin/com/example/UtilsTest.kt"),
        task_description = "Generate tests for Utils class"
    )
)
```


### Generated Test Format

The generated tests follow this structure:

```java
// Example test file structure
public class ExampleTest {
    @Test
    public void testMethod() {
        // Happy path test
        assertEquals(expected, actual);
        
        // Edge case test
        assertEquals(expected, actual);
        
        // Error condition test
        assertThrows(Exception.class, () -> {
            // Test code
        });
    }
}
```


### Best Practices

1. Specify all relevant source files in `filesToTest`
2. Include any dependent files in `inputReferences`
3. Review generated tests and adjust as needed
4. Run generated tests to ensure they work as expected
5. Maintain generated tests alongside your source code


### Task Type

This task is identified by the `TaskType.TestGeneration` type in the system.

# knowledge\EmbeddingSearchTask.kt


## EmbeddingSearchTask Documentation


### Overview
The EmbeddingSearchTask is a powerful tool for semantic search across indexed documents using OpenAI's embedding models. It allows you to find relevant content based on semantic similarity rather than just keyword matching.


### Features
- Semantic search using positive and negative queries
- Multiple distance metrics for comparison
- Configurable result count
- Content filtering by length and regex patterns
- Rich context display in search results


### Configuration Options


#### Required Parameters
- `positive_queries`: List of search queries to find similar content
  - Example: `["machine learning algorithms", "neural networks"]`


#### Optional Parameters
- `negative_queries`: List of queries to avoid in results (default: empty)
  - Example: `["statistical methods", "traditional algorithms"]`
  
- `distance_type`: Method for comparing embeddings (default: Cosine)
  - Options: `Euclidean`, `Manhattan`, `Cosine`
  
- `count`: Number of results to return (default: 5)
  - Example: `10`
  
- `min_length`: Minimum content length to consider (default: 0)
  - Example: `100`
  
- `required_regexes`: Regex patterns that must match content (default: empty)
  - Example: `[".*algorithm.*", ".*data.*"]`


### Example Usage

```json
{
  "task_type": "EmbeddingSearch",
  "positive_queries": ["artificial intelligence ethics"],
  "negative_queries": ["machine maintenance"],
  "distance_type": "Cosine",
  "count": 5,
  "min_length": 100,
  "required_regexes": ["ethics|moral|responsibility"]
}
```


### Output Format

The task returns results in markdown format with the following structure:

```markdown

## Embedding Search Results


### Result 1
* Distance: 0.123
* File: path/to/file.index.data
* Context: [JSON representation of relevant context]
* Metadata: [JSON metadata for the result]


### Result 2
...
```


### Notes
- At least one positive query is required
- Results are sorted by relevance (lowest distance score)
- The search looks for `.index.data` files in the specified directory
- Context includes both immediate content and relevant parent information


### Best Practices
1. Use specific, focused positive queries
2. Use negative queries to exclude unwanted topics
3. Adjust min_length to filter out short, less relevant content
4. Use required_regexes for additional filtering precision
5. Start with default distance_type (Cosine) unless you have specific needs


### Error Handling
- Will throw an error if no positive queries are provided
- Invalid regex patterns will cause search failures
- Missing or malformed index files will be skipped


### Performance Considerations
- Search time increases with the number of index files
- Large result sets may take longer to process
- Complex regex patterns can impact performance

# knowledge\KnowledgeIndexingTask.kt

Here's the user documentation for the KnowledgeIndexingTask:


## Knowledge Indexing Task Documentation


### Overview
The Knowledge Indexing Task is designed to process and index files for semantic search capabilities. It can handle both document and code files, breaking them down into searchable chunks that can be used for semantic retrieval later.


### Configuration Options


#### Required Parameters
- `file_paths`: A list of file paths to process and index
  - Example: `["docs/readme.md", "src/main.kt"]`
  - Files must exist on the system to be processed


#### Optional Parameters
- `parsing_type`: The type of parsing to use (default: "document")
  - Options:
    - `"document"`: For processing regular text documents
    - `"code"`: For processing source code files
- `chunk_size`: Controls how the documents are split into chunks (default: 0.1)
  - Range: 0.0 to 1.0
  - Smaller values create more granular chunks
  - Larger values create bigger chunks


### Usage Example

```json
{
  "file_paths": [
    "docs/user-guide.md",
    "src/main/kotlin/Example.kt"
  ],
  "parsing_type": "code",
  "chunk_size": 0.2
}
```


### Process Flow
1. The task validates the provided file paths and filters out any non-existent files
2. Based on the parsing_type, it initializes either a document or code parsing model
3. Files are processed in parallel using a thread pool
4. Progress is reported as a percentage during processing
5. Processed content is saved in a binary format for later retrieval
6. A completion summary is provided listing all processed files


### Progress Monitoring
- The task provides real-time progress updates as files are processed
- Progress is displayed as a percentage complete
- A final summary shows all successfully processed files


### Output
The task generates a markdown-formatted summary containing:
- Confirmation of completion
- Total number of files processed
- List of processed file names


### Error Handling
- If no valid files are found, the task will exit with an appropriate message
- Invalid file paths are automatically filtered out
- The thread pool is properly shutdown even if processing fails


### Best Practices
1. Ensure all file paths are valid before running the task
2. Choose the appropriate parsing_type based on your content
3. Adjust chunk_size based on your needs:
   - Smaller for more precise searching
   - Larger for more contextual chunks
4. Monitor progress for large file sets


### Technical Requirements
- Sufficient disk space for processed files
- Adequate memory for parallel processing
- Valid OpenAI API credentials


### Limitations
- Processing time depends on file sizes and quantity
- Memory usage increases with parallel processing
- API rate limits may affect processing speed

# knowledge\WebSearchAndIndexTask.kt

Here's the user documentation for the WebSearchAndIndexTask:


## WebSearchAndIndexTask Documentation


### Overview
The WebSearchAndIndexTask is a specialized task that performs web searches, downloads content, and indexes it for future embedding-based searches. This task is particularly useful when you need to gather and process information from multiple web sources for later reference or analysis.


### Configuration Parameters


#### Required Parameters
- `search_query` (String): The search query to use for web search
- `num_results` (Int): The number of search results to process (maximum 10)
- `output_directory` (String): The directory where downloaded and indexed content will be stored


#### Optional Parameters
- `task_description` (String): A description of the task (optional)
- `task_dependencies` (List<String>): List of tasks that must complete before this task starts (optional)


### Usage Example

```json
{
  "search_query": "artificial intelligence latest developments",
  "num_results": 5,
  "output_directory": "ai_research_data",
  "task_description": "Gather and index recent AI developments"
}
```


### Process Flow

1. **Web Search**
   - Performs a Google Custom Search using the provided search query
   - Retrieves the specified number of search results (up to 10)

2. **Content Download**
   - Downloads HTML content from each search result URL
   - Saves the content to files in the specified output directory
   - Files are named based on the page titles (sanitized for filesystem compatibility)

3. **Content Indexing**
   - Processes downloaded files using document parsing
   - Creates binary index files for future embedding-based searches
   - Uses parallel processing for improved performance


### Output

The task produces a summary report containing:
- The original search query
- A list of successfully downloaded and indexed files
- The files are stored in the specified output directory

Example output:
```

## Web Search and Index Results

### Search Query: artificial intelligence latest developments

### Downloaded and Indexed Files:
1. Latest_AI_Breakthroughs_2023.html
2. Understanding_Machine_Learning_Advances.html
3. AI_Research_Updates.html
```


### Requirements

- Valid Google API key
- Google Custom Search Engine ID
- Sufficient disk space for downloaded content
- Internet connection for web searches and downloads


### Error Handling

The task includes error handling for:
- Failed API requests
- Download errors
- Invalid file names
- Content processing issues

Failed downloads or processing attempts are logged but don't stop the overall task execution.


### Best Practices

1. Use specific search queries for better results
2. Start with a small number of results for testing
3. Ensure sufficient disk space in the output directory
4. Monitor the task logs for any issues
5. Use descriptive output directory names for better organization


### Limitations

- Maximum 10 search results per query
- Requires valid Google API credentials
- HTML content only
- File names are truncated to 50 characters
- Some websites may block automated downloads


### Support

For issues or questions:
- Check the application logs for detailed error messages
- Verify API credentials and permissions
- Ensure output directory is writable
- Contact system administrator for API-related issues

# online\GitHubSearchTask.kt

Here's the user documentation for the GitHubSearchTask:


## GitHub Search Task Documentation


### Overview
The GitHub Search Task allows you to search GitHub's vast repository of code, commits, issues, repositories, topics, and users through the GitHub API. This task provides a flexible way to query GitHub data and receive formatted results.


### Configuration Options


#### Required Parameters
- `search_query`: The search query string to find relevant content on GitHub
- `search_type`: The type of GitHub search to perform. Valid options include:
  - `code`: Search for code snippets
  - `commits`: Search for commits
  - `issues`: Search for issues and pull requests
  - `repositories`: Search for repositories
  - `topics`: Search for repository topics
  - `users`: Search for GitHub users
- `per_page`: Number of results to return (maximum 100)


#### Optional Parameters
- `sort`: Sort order for results (depends on search type)
  - For repositories: `stars`, `forks`, `updated`
  - For issues: `created`, `updated`, `comments`
  - For users: `followers`, `repositories`, `joined`
- `order`: Sort direction (`asc` or `desc`)


### Example Usage


#### Search for Popular Repositories
```json
{
  "search_query": "language:kotlin stars:>1000",
  "search_type": "repositories",
  "per_page": 10,
  "sort": "stars",
  "order": "desc"
}
```


#### Search for Code Examples
```json
{
  "search_query": "openai api language:kotlin",
  "search_type": "code",
  "per_page": 30
}
```


#### Search for Active Issues
```json
{
  "search_query": "is:open is:issue label:bug",
  "search_type": "issues",
  "per_page": 20,
  "sort": "updated",
  "order": "desc"
}
```


### Output Format

The task returns results in a formatted Markdown document containing:
1. Total count of results found
2. Detailed information for up to 10 top results
3. Type-specific formatting:
   - Repositories: Name, description, stars, forks, and link
   - Code: Repository name, file path, and matching code snippet
   - Commits: Repository, commit message, author, and date
   - Issues: Title, state, comments, creator, and creation date
   - Users: Username, type, public repos, and avatar
   - Topics: Name, description, and featured/curated status


### Requirements
- A valid GitHub API token must be configured in the plan settings
- The GitHub API has rate limits that may affect usage


### Error Handling
- The task will throw an exception if the GitHub API request fails
- Common errors include:
  - Invalid search syntax
  - Rate limiting
  - Authentication failures
  - Invalid search type


### Best Practices
1. Use specific search queries to narrow down results
2. Consider rate limits when determining request frequency
3. Use sorting and ordering for more relevant results
4. Keep per_page parameter reasonable to avoid performance issues


### Notes
- Search results are limited to the first 1000 items by GitHub's API
- Some search types may require additional permissions
- Complex searches may take longer to process
- Results are cached by GitHub for performance

# online\SearchAndAnalyzeTask.kt

Here's the user documentation for the SearchAndAnalyzeTask:


## SearchAndAnalyzeTask Documentation


### Overview
The SearchAndAnalyzeTask is a powerful tool that combines Google search with AI-powered content analysis. It allows you to search for specific topics, fetch multiple results, and automatically analyze the content according to your specified goals.


### Features
- Performs Google custom search queries
- Fetches and processes multiple search results (up to 5)
- Analyzes content based on user-defined goals
- Generates markdown-formatted reports


### Configuration Parameters


#### Required Parameters
1. `search_query` (String)
   - The search terms to use for Google search
   - Example: "machine learning best practices 2023"

2. `num_results` (Integer)
   - Number of search results to analyze
   - Maximum value: 5
   - Default value: 3

3. `analysis_goal` (String)
   - The specific focus or objective for analyzing the content
   - Example: "Compare and contrast different approaches to machine learning"


#### Optional Parameters
- `task_description`: Custom description of the task
- `task_dependencies`: List of tasks that must complete before this one
- `state`: Current state of the task


### Prerequisites
- Valid Google API Key
- Google Custom Search Engine ID
- Appropriate API access and permissions


### Output Format
The task generates a markdown-formatted report containing:
1. A header section
2. Numbered sections for each analyzed result including:
   - Title with link to original content
   - AI-generated analysis based on the specified goal
   - Error messages if any result fails to process


### Example Usage

```kotlin
val taskConfig = SearchAndAnalyzeTaskConfigData(
    search_query = "latest developments in quantum computing",
    num_results = 3,
    analysis_goal = "Identify key breakthroughs and potential applications"
)

val task = SearchAndAnalyzeTask(planSettings, taskConfig)
```


### Sample Output

```markdown

## Analysis of Search Results


### 1. [Latest Developments in Quantum Computing - Nature](<url>)
[Analysis of the content based on specified goal...]


### 2. [Quantum Computing Breakthroughs - Science Daily](<url>)
[Analysis of the content based on specified goal...]


### 3. [Future of Quantum Computing - MIT Technology Review](<url>)
[Analysis of the content based on specified goal...]
```


### Error Handling
- The task includes robust error handling for:
  - Failed Google API requests
  - Invalid URLs
  - Content fetching issues
  - Content analysis failures
- Errors are logged and included in the output report


### Limitations
1. Maximum of 5 search results can be analyzed
2. Requires valid Google API credentials
3. Subject to Google API rate limits and quotas
4. Content must be accessible via HTTP/HTTPS


### Best Practices
1. Use specific, focused search queries
2. Start with fewer results (2-3) for faster processing
3. Provide clear, specific analysis goals
4. Monitor API usage to stay within quotas


### Troubleshooting
If you encounter issues:
1. Verify API credentials are valid
2. Check network connectivity
3. Ensure search query is properly formatted
4. Review logs for detailed error messages

For additional support or questions, please refer to the main documentation or contact support.

# online\SimpleGoogleSearchTask.kt

Here's the user documentation for the SimpleGoogleSearchTask class:


## SimpleGoogleSearchTask Documentation


### Overview
SimpleGoogleSearchTask is a task implementation that allows you to perform Google searches and retrieve web results programmatically. It integrates with Google's Custom Search API to fetch search results and format them in a readable markdown format.


### Configuration


#### Required Settings
The task requires the following settings to be configured in your PlanSettings:

- `googleApiKey` - Your Google API key for accessing the Custom Search API
- `googleSearchEngineId` - Your Custom Search Engine ID (cx parameter)


#### Task Configuration Parameters
When creating a GoogleSearchTaskConfigData object, you can specify:

- `search_query` (String): The search terms to query Google with
- `num_results` (Int): Number of results to return (maximum 10)
- `task_description` (String, optional): Description of the search task
- `task_dependencies` (List<String>, optional): List of dependent task IDs
- `state` (TaskState, optional): Current state of the task


### Usage Example

```kotlin
val taskConfig = GoogleSearchTaskConfigData(
    search_query = "artificial intelligence latest developments",
    num_results = 5,
    task_description = "Search for recent AI developments"
)

val searchTask = SimpleGoogleSearchTask(planSettings, taskConfig)
```


### Output Format
The search results are formatted in markdown with the following structure:

```markdown

## Google Search Results


## 1. [Title of First Result](URL of First Result)
Snippet text from the search result
Pagemap:
```json
{pagemap data}
```


## 2. [Title of Second Result](URL of Second Result)
...
```


### Features

1. Performs Google Custom Search API queries
2. Supports configurable number of results (up to 10)
3. Returns formatted markdown output with:
   - Clickable result titles with links
   - Result snippets
   - Pagemap metadata in JSON format
4. Handles error cases and provides appropriate feedback


### Error Handling
The task will throw a RuntimeException if:
- The Google API request fails
- Invalid API credentials are provided
- The API returns a non-200 status code


### Limitations

1. Maximum of 10 results per search query
2. Requires valid Google API credentials
3. Subject to Google Custom Search API quotas and rate limits


### Integration
This task can be integrated into larger workflows by:
1. Including it in a task dependency chain
2. Using the search results as input for subsequent tasks
3. Incorporating it into automated research or data gathering processes

For more information about Google Custom Search API, visit the [Google Custom Search API Documentation](https://developers.google.com/custom-search/v1/overview).

# online\WebFetchAndTransformTask.kt

Here's the user documentation for the WebFetchAndTransformTask class:


## WebFetchAndTransformTask Documentation


### Overview
The WebFetchAndTransformTask is a specialized task that fetches content from a web URL, strips the HTML formatting, and transforms the content according to specified goals. This task is useful for extracting and reformatting web content for various purposes.


### Configuration


#### Required Parameters
The task requires two main parameters:

1. `url` (String): The web URL from which to fetch content
2. `transformationGoal` (String): Description of how the content should be transformed


#### Optional Parameters
- `task_description`: Custom description of the task
- `task_dependencies`: List of dependent task IDs
- `state`: Current state of the task


### Usage Example

```kotlin
val taskConfig = WebFetchAndTransformTaskConfigData(
    url = "https://example.com/article",
    transformationGoal = "Summarize the main points in bullet format"
)

val task = WebFetchAndTransformTask(planSettings, taskConfig)
```


### How It Works

The task performs three main operations:

1. **Web Content Fetching**
   - Retrieves HTML content from the specified URL
   - Uses HTTP GET request to fetch the content

2. **HTML Cleaning**
   - Removes unnecessary HTML elements (scripts, styles, iframes, etc.)
   - Strips unnecessary attributes
   - Cleans up empty elements
   - Converts relative URLs to absolute URLs
   - Limits content size to 100KB by default

3. **Content Transformation**
   - Processes the cleaned content according to the specified transformation goal
   - Uses AI model specified in plan settings to transform the content
   - Returns formatted results


### Features

- **HTML Sanitization**: Automatically removes potentially harmful elements
- **Content Optimization**: Removes unnecessary formatting while preserving important content
- **Size Management**: Implements content size limits to prevent memory issues
- **Flexible Transformation**: Supports various transformation goals through AI processing


### Limitations

- Maximum content size is limited to 100KB by default
- Requires active internet connection to fetch web content
- Transformation quality depends on the clarity of the transformation goal


### Best Practices

1. **URL Selection**
   - Ensure URLs are accessible and contain relevant content
   - Verify URLs are properly formatted and include protocol (http/https)

2. **Transformation Goals**
   - Be specific about desired transformation outcomes
   - Examples:
     - "Convert to bullet points"
     - "Summarize in three paragraphs"
     - "Extract key statistics"

3. **Error Handling**
   - Implement proper error handling for network issues
   - Consider content size limitations when selecting sources


### Example Transformation Goals

```kotlin
// Summarization
transformationGoal = "Provide a concise summary in 3 paragraphs"

// Format Conversion
transformationGoal = "Convert content into a bulleted list of key points"

// Information Extraction
transformationGoal = "Extract all dates and events mentioned"

// Analysis
transformationGoal = "Analyze the main arguments and provide pros and cons"
```


### Technical Details

- Uses JSoup for HTML parsing and cleaning
- Implements HTTP client for web requests
- Integrates with AI models for content transformation
- Supports markdown rendering for output


### Integration Notes

- Can be integrated into larger workflows through task dependencies
- Supports session-based task management
- Compatible with markdown rendering systems
- Works with various AI models through the plan settings

This task is particularly useful for content aggregation, research, and information processing workflows where web content needs to be transformed into specific formats or analyzed in particular ways.

# plan\ForeachTask.kt

Here's the user documentation for the ForeachTask class:


## ForeachTask Documentation


### Overview
The ForeachTask is a specialized task type that allows you to execute a set of subtasks for each item in a list. This is particularly useful when you need to perform the same operations on multiple items sequentially.


### Configuration


#### ForeachTaskConfigData Properties
- `foreach_items`: (Required) A list of strings representing the items to iterate over
- `foreach_subplan`: (Required) A map defining the subtasks to execute for each item
- `task_description`: (Optional) Description of the overall foreach task
- `task_dependencies`: (Optional) List of task IDs that must complete before this task starts
- `state`: (Optional) Current state of the task


### Usage Example

```kotlin
val foreachConfig = ForeachTaskConfigData(
    foreach_items = listOf("item1", "item2", "item3"),
    foreach_subplan = mapOf(
        "subtask1" to TaskConfigBase(
            task_type = "SomeTaskType",
            task_description = "Process item"
        )
    ),
    task_description = "Process multiple items sequentially",
    task_dependencies = listOf("previousTask")
)

val foreachTask = ForeachTask(planSettings, foreachConfig)
```


### How It Works

1. The task takes a list of items and a map of subtasks as input
2. For each item in the list:
   - Creates a copy of the subtasks
   - Appends the current item information to the task descriptions
   - Executes the subtasks with the modified context
3. Progress is tracked and displayed through the UI
4. The task completes when all items have been processed


### Best Practices

1. Ensure your foreach_items list is not empty
2. Define clear and specific subtasks in the foreach_subplan
3. Use meaningful task descriptions that will be helpful when reviewing logs
4. Consider dependencies carefully to ensure proper task ordering


### Error Handling

The task will throw RuntimeExceptions if:
- No items are specified (foreach_items is null)
- No subtasks are specified (foreach_subplan is null)


### Notes

- Each iteration's subtasks run in their own context
- Progress can be monitored through the UI
- Task execution order within each iteration follows standard plan execution rules
- Results from each iteration are preserved in the task history

This task type is ideal for scenarios where you need to apply the same processing steps to multiple items while maintaining organization and tracking of the overall process.

# plan\PlanningTask.kt

Here's the user documentation for the PlanningTask class:


## PlanningTask Documentation


### Overview
The PlanningTask class is a specialized task type that handles high-level planning and task organization within the system. It breaks down complex goals into smaller, manageable tasks while ensuring proper dependencies and information flow between them.


### Key Features


#### Task Planning Capabilities
- Decomposes overall goals into smaller actionable tasks
- Establishes task dependencies and relationships
- Optimizes for parallel execution where possible
- Dynamically adjusts plans based on new information
- Ensures efficient information transfer between tasks


#### Configuration


##### PlanningTaskConfigData
The task configuration includes:
- `task_description`: Description of the planning task
- `task_dependencies`: List of tasks this planning task depends on
- `state`: Current state of the task (defaults to Pending)


#### Task Breakdown Results

The planning process produces a `TaskBreakdownResult` containing:
- `tasksByID`: A map linking task IDs to their corresponding task configurations


### Usage


#### Basic Implementation
```kotlin
val planningTask = PlanningTask(
    planSettings = yourPlanSettings,
    planTask = PlanningTaskConfigData(
        task_description = "Plan project implementation phases",
        task_dependencies = listOf("requirements_gathering")
    )
)
```


#### Execution Flow
1. The task receives input messages
2. Creates a sub-plan based on the input
3. Executes sub-tasks according to dependencies
4. Monitors and manages task completion


#### Execution Modes


##### Blocking Mode
When `allowBlocking` is true and `autoFix` is false:
- Enables interactive discussion of the plan
- Allows manual refinement before execution
- Provides visual feedback through diagrams


##### Auto Mode
When blocking is disabled:
- Automatically generates and executes the plan
- Proceeds without manual intervention
- Optimizes for efficiency


### Best Practices

1. **Clear Task Descriptions**
   - Provide detailed task descriptions
   - Include specific goals and expected outcomes

2. **Dependency Management**
   - Carefully specify task dependencies
   - Ensure logical task ordering

3. **Information Flow**
   - Design tasks to properly utilize upstream outputs
   - Ensure outputs feed correctly into downstream tasks

4. **Parallel Execution**
   - Structure tasks to maximize parallel processing
   - Balance parallelism with dependency requirements


### Limitations and Considerations

- Planning tasks focus on organization and structure, not execution
- Task modifications should maintain dependency integrity
- Complex dependencies may limit parallelization opportunities


### Error Handling

The system includes logging for error tracking and debugging:
- Errors are logged using SLF4J
- Task state changes are tracked
- Execution failures are captured and reported


### Integration

The PlanningTask integrates with:
- Plan Coordinator for overall execution management
- Session management for task tracking
- UI components for visual representation
- API clients for communication


### Example

```kotlin
// Create a planning task
val planningTask = PlanningTask(
    planSettings = PlanSettings(
        allowBlocking = true,
        autoFix = false
    ),
    planTask = PlanningTaskConfigData(
        task_description = "Plan software release cycle",
        task_dependencies = listOf("requirements", "resource_allocation")
    )
)

// Execute the task
planningTask.run(
    agent = yourPlanCoordinator,
    messages = listOf("Plan next software release"),
    task = yourSessionTask,
    api = yourChatClient,
    resultFn = { result -> println(result) },
    api2 = yourOpenAIClient,
    planSettings = yourPlanSettings
)
```

This documentation provides a comprehensive overview of the PlanningTask class and its functionality. For specific implementation details or advanced usage scenarios, please consult the development team or refer to the source code.
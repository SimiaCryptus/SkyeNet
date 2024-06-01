Sure! Here is a detailed description of each utility provided in the code snippets:

### 1. **StringSplitter.kt**

The `StringSplitter` object provides a utility to split a string into two parts based on specified separators and their
associated weights. The main functionality is encapsulated in the `split` function.

- **split(text: String, separators: Map<String, Double>): Pair<String, String>**
    - **Parameters:**
        - `text`: The input string to be split.
        - `separators`: A map where keys are separator strings and values are their associated weights.
    - **Returns:**
        - A pair of strings, representing the two parts of the split text.
    - **Functionality:**
        - The function iterates over the provided separators and calculates potential split points based on the
          positions of these separators in the text.
        - For each separator, it computes a score using a logarithmic formula that takes into account the position of
          the separator and its weight.
        - The split point with the highest score is chosen, and the text is split at this point.
        - If no suitable split point is found, the text is split in the middle by default.

- **main(args: Array<String>)**
    - A sample main function to demonstrate the usage of the `split` function.
    - It prints the result of splitting a sample text using specified separators and their weights.

### 2. **Selenium.kt**

The `Selenium` interface defines a contract for saving web content and potentially setting cookies (commented out).

- **save(url: URL, currentFilename: String?, saveRoot: String)**
    - **Parameters:**
        - `url`: The URL of the web content to be saved.
        - `currentFilename`: The current filename for saving the content.
        - `saveRoot`: The root directory where the content should be saved.
    - **Functionality:**
        - This function is intended to save the web content from the specified URL to a file in the given directory.

- **AutoCloseable**
    - The interface extends `AutoCloseable`, indicating that any implementing class should provide a mechanism to
      release resources when no longer needed.

### 3. **OutputInterceptor.java**

The `OutputInterceptor` class provides a utility to intercept and capture standard output (`System.out`) and standard
error (`System.err`) streams.

- **setupInterceptor()**
    - Sets up the interceptor by redirecting `System.out` and `System.err` to custom `PrintStream` instances that route
      output to both the original streams and internal buffers.

- **getThreadOutputStream()**
    - Retrieves a thread-local output stream for capturing output specific to the current thread.

- **getThreadOutput()**
    - Returns the captured output for the current thread as a string.

- **clearThreadOutput()**
    - Clears the captured output for the current thread.

- **getGlobalOutput()**
    - Returns the globally captured output as a string.

- **clearGlobalOutput()**
    - Clears the globally captured output.

- **OutputStreamRouter**
    - A custom `ByteArrayOutputStream` that routes written data to both the original stream and internal buffers (global
      and thread-local).
    - Ensures that buffers do not exceed specified maximum sizes, resetting them if necessary.

### 4. **LoggingInterceptor.kt**

The `LoggingInterceptor` class provides a utility to intercept and capture logging output.

- **withIntercept(stringBuffer: StringBuffer, vararg loggerPrefixes: String, fn: () -> T): T**
    - A companion object function that temporarily replaces the appenders of specified loggers with a custom appender
      that writes to a `StringBuffer`.
    - Executes the provided function `fn` and restores the original loggers' state afterward.

- **append(event: ILoggingEvent)**
    - Appends the formatted log message and any associated throwable information to the `StringBuffer`.

- **getStringBuffer()**
    - Returns the `StringBuffer` containing the captured log messages.

### 5. **FunctionWrapper.kt**

The `FunctionWrapper` class provides a utility to wrap function calls and intercept their execution.

- **wrap(fn: () -> T)**
    - Wraps a no-argument function and intercepts its execution using the provided `FunctionInterceptor`.

- **wrap(p: P, fn: (P) -> T)**
    - Wraps a single-argument function and intercepts its execution.

- **wrap(p1: P1, p2: P2, fn: (P1, P2) -> T)**
    - Wraps a two-argument function and intercepts its execution.

- **wrap(p1: P1, p2: P2, p3: P3, fn: (P1, P2, P3) -> T)**
    - Wraps a three-argument function and intercepts its execution.

- **wrap(p1: P1, p2: P2, p3: P3, p4: P4, fn: (P1, P2, P3, P4) -> T)**
    - Wraps a four-argument function and intercepts its execution.

### 6. **FunctionInterceptor Interface**

Defines methods for intercepting function calls with varying numbers of parameters.

- **intercept(returnClazz: Class<T>, fn: () -> T)**
    - Intercepts a no-argument function.

- **intercept(params: P, returnClazz: Class<T>, fn: (P) -> T)**
    - Intercepts a single-argument function.

- **intercept(p1: P1, p2: P2, returnClazz: Class<T>, fn: (P1, P2) -> T)**
    - Intercepts a two-argument function.

- **intercept(p1: P1, p2: P2, p3: P3, returnClazz: Class<T>, fn: (P1, P2, P3) -> T)**
    - Intercepts a three-argument function.

- **intercept(p1: P1, p2: P2, p3: P3, p4: P4, returnClazz: Class<T>, fn: (P1, P2, P3, P4) -> T)**
    - Intercepts a four-argument function.

### 7. **NoopFunctionInterceptor**

A no-operation implementation of `FunctionInterceptor` that simply executes the provided function without any
interception logic.

### 8. **JsonFunctionRecorder**

A class that intercepts function calls and records their inputs and outputs in JSON format.

- **JsonFunctionRecorder(baseDir: File)**
    - Initializes the recorder with a base directory for storing JSON files.

- **intercept(returnClazz: Class<T>, fn: () -> T)**
    - Intercepts a no-argument function, records its output (or error) in JSON format, and returns the result.

- **intercept(params: P, returnClazz: Class<T>, fn: (P) -> T)**
    - Intercepts a single-argument function, records its input and output (or error) in JSON format, and returns the
      result.

- **operationDir()**
    - Creates a directory for storing JSON files for the current operation, based on a sequence ID and timestamp.

- **close()**
    - Closes the recorder (no resources to close in this implementation).

### 9. **getModel(modelName: String?): OpenAIModel?**

A utility function that retrieves an `OpenAIModel` instance based on the provided model name.

- **Parameters:**
    - `modelName`: The name of the model to retrieve.
- **Returns:**
    - An `OpenAIModel` instance if a matching model is found, or `null` otherwise.
- **Functionality:**
    - Searches through available `ChatModels`, `EmbeddingModels`, and `ImageModels` to find a model with the specified
      name.

These utilities provide a range of functionalities, including string splitting, web content saving, output interception,
logging interception, function call interception, and model retrieval. They are designed to be flexible and reusable
across different parts of an application.
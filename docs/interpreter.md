# Interpreter Subsystem Manual

## Overview

The interpreter subsystem provides a framework for executing and validating code snippets in various scripting
languages. It supports Scala, Kotlin, and Groovy, allowing dynamic execution of code with predefined variables and
functions.

## Components

### 1. Core Interfaces and Utilities

#### `Interpreter.kt`

This is the core interface that defines the contract for any interpreter implementation. It includes methods for running
and validating code, as well as utility methods for wrapping code and execution.

- **Methods:**
    - `getLanguage(): String`: Returns the language of the interpreter.
    - `getSymbols(): Map<String, Any>`: Returns the predefined symbols (variables and functions) available in the
      interpreter.
    - `run(code: String): Any?`: Executes the given code and returns the result.
    - `validate(code: String): Throwable?`: Validates the given code and returns any validation errors.
    - `wrapCode(code: String): String`: Wraps the code (default implementation returns the code as-is).
    - `<T : Any> wrapExecution(fn: java.util.function.Supplier<T?>): T?`: Wraps the execution of a function (default
      implementation simply executes the function).

- **Companion Object:**
    - Contains utility methods for testing interpreter implementations.

#### `InterpreterTestBase.kt`

This is an abstract base class for unit testing interpreter implementations. It provides a set of standardized tests to
ensure that interpreters conform to expected behavior.

- **Methods:**
    - `test run with valid code`
    - `test run with invalid code`
    - `test validate with valid code`
    - `test validate with invalid code`
    - `test run with variables`
    - `test validate with variables`
    - `test run with tool Any`
    - `test validate with tool Any`
    - `test run with tool Any and invalid code`
    - `test validate with tool Any and invalid code`
    - `test validate with undefined variable`

- **Abstract Method:**
    - `newInterpreter(map: Map<String, Any>): Interpreter`: Must be implemented by subclasses to provide an instance of
      the interpreter being tested.

### 2. Scala Interpreter

#### `ScalaLocalInterpreter.scala`

This class provides an implementation of the `Interpreter` interface for the Scala language. It uses the Scala REPL (
Read-Eval-Print Loop) to execute and validate code.

- **Components:**
    - **Object `ScalaLocalInterpreter`:**
        - `getTypeTag(value: Any): Type`: Utility method to get the type tag of a value.
    - **Class `ScalaLocalInterpreter`:**
        - Constructor: Accepts a map of predefined variables and functions.
        - `CustomReplReporter`: Custom reporter to handle errors and warnings during code execution.
        - `getClasspathFromManifest(jarPath: String): String`: Utility method to get classpath from a JAR manifest.
        - `run(code: String): Any`: Executes the given code and returns the result.
        - `validate(code: String): Exception`: Validates the given code and returns any validation errors.
        - `wrapCode(code: String): String`: Wraps the code (default implementation returns the code as-is).
        - `wrapExecution[T](fn: Supplier[T]): T`: Wraps the execution of a function (default implementation simply
          executes the function).
        - `getSymbols(): util.Map[String, AnyRef]`: Returns the predefined symbols (variables and functions) available
          in the interpreter.

### 3. Kotlin Interpreter

#### `KotlinInterpreter.kt`

This class provides an implementation of the `Interpreter` interface for the Kotlin language. It uses the Kotlin JSR223
scripting engine to execute and validate code.

- **Components:**
    - **Class `KotlinInterpreter`:**
        - Constructor: Accepts a map of predefined variables and functions.
        - `scriptEngine`: Provides the Kotlin scripting engine.
        - `validate(code: String): Throwable?`: Validates the given code and returns any validation errors.
        - `run(code: String): Any?`: Executes the given code and returns the result.
        - `wrapException(cause: ScriptException, wrappedCode: String, code: String): CodingActor.FailedToImplementException`:
          Wraps a script exception with additional information.
        - `wrapCode(code: String): String`: Wraps the code (default implementation returns the code as-is).
    - **Companion Object:**
        - `errorMessage(code: String, line: Int, column: Int, message: String)`: Utility method to format error
          messages.
        - `classLoader`: Class loader for the Kotlin interpreter.

### 4. Groovy Interpreter

#### `GroovyInterpreter.kt`

This class provides an implementation of the `Interpreter` interface for the Groovy language. It uses the GroovyShell to
execute and validate code.

- **Components:**
    - **Class `GroovyInterpreter`:**
        - Constructor: Accepts a map of predefined variables and functions.
        - `shell`: Provides the Groovy shell for executing code.
        - `run(code: String): Any?`: Executes the given code and returns the result.
        - `validate(code: String): Exception?`: Validates the given code and returns any validation errors.
        - `wrapCode(code: String): String`: Wraps the code (default implementation returns the code as-is).

## Usage

### Creating an Interpreter

To create an interpreter, you need to instantiate the appropriate class and provide a map of predefined variables and
functions.

#### Scala Interpreter

```scala
val scalaInterpreter = new ScalaLocalInterpreter(java.util.Map.of("x", 2.asInstanceOf[AnyRef], "y", 3.asInstanceOf[AnyRef]))
```

#### Kotlin Interpreter

```kotlin
val kotlinInterpreter = KotlinInterpreter(mapOf("x" to 2, "y" to 3))
```

#### Groovy Interpreter

```kotlin
val groovyInterpreter = GroovyInterpreter(java.util.Map.of("x", 2 as Any, "y", 3 as Any))
```

### Running Code

To run code using an interpreter, use the `run` method.

```scala
val result = scalaInterpreter.run("x * y")
println(result) // Output: 6
```

```kotlin
val result = kotlinInterpreter.run("x * y")
println(result) // Output: 6
```

```kotlin
val result = groovyInterpreter.run("x * y")
println(result) // Output: 6
```

### Validating Code

To validate code using an interpreter, use the `validate` method.

```scala
val error = scalaInterpreter.validate("x * y")
if (error != null) {
  println("Validation failed: " + error.getMessage)
} else {
  println("Validation succeeded")
}
```

```kotlin
val error = kotlinInterpreter.validate("x * y")
if (error != null) {
    println("Validation failed: " + error.message)
} else {
    println("Validation succeeded")
}
```

```kotlin
val error = groovyInterpreter.validate("x * y")
if (error != null) {
    println("Validation failed: " + error.message)
} else {
    println("Validation succeeded")
}
```

## Testing

To test an interpreter implementation, extend the `InterpreterTestBase` class and implement the `newInterpreter` method.

```kotlin
class ScalaInterpreterTest : InterpreterTestBase() {
    override fun newInterpreter(map: Map<String, Any>): Interpreter {
        return ScalaLocalInterpreter(map)
    }
}

class KotlinInterpreterTest : InterpreterTestBase() {
    override fun newInterpreter(map: Map<String, Any>): Interpreter {
        return KotlinInterpreter(map)
    }
}

class GroovyInterpreterTest : InterpreterTestBase() {
    override fun newInterpreter(map: Map<String, Any>): Interpreter {
        return GroovyInterpreter(map)
    }
}
```

Run the tests to ensure your interpreter implementation conforms to the expected behavior.

## Conclusion

The interpreter subsystem provides a flexible and extensible framework for executing and validating code snippets in
various scripting languages. By implementing the `Interpreter` interface, you can create custom interpreters for
additional languages and integrate them seamlessly into the existing framework.
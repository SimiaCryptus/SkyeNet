# Skyenet Core Actors Package Documentation

The Skyenet Core Actors package is a comprehensive framework designed to facilitate interaction with OpenAI's GPT models and other functionalities such as text-to-speech and image generation. This package is structured to provide a robust foundation for building applications that leverage OpenAI's capabilities, including conversational agents, code generators, and visual content creators.

## Package Overview

The package is organized into several key components, each serving a specific purpose within the framework. Below is an overview of the primary classes and their relationships.

### Core Classes

- **`BaseActor`**: Serves as the abstract base class for all actors. It defines the basic structure and functionalities common to all types of actors.
- **`SimpleActor`**: Extends `BaseActor` to provide simple interaction with OpenAI's GPT models, focusing on sending prompts and receiving text responses.
- **`ParsedActor`**: A specialized actor that parses responses into specific data types, extending the `BaseActor` functionalities.
- **`CodingActor`**: Designed for code generation and execution, this actor extends `BaseActor` and integrates with code interpreters.
- **`ImageActor`**: Focuses on generating images from textual prompts, extending the capabilities of `BaseActor` to support visual content creation.
- **`TextToSpeechActor`**: Converts text into speech, extending `BaseActor` to utilize OpenAI's text-to-speech models.

### Supporting Classes and Interfaces

- **`ActorSystem`**: Manages and interacts with various types of actors within a session, applying specific interceptors based on the actor type.
- **`ParsedResponse`**: Represents responses parsed from various sources in a structured manner.
- **Interceptors (`*Interceptor`)**: Specialized classes designed to intercept and potentially modify the behavior of actor instances (e.g., `CodingActorInterceptor`, `ImageActorInterceptor`).

### Documentation and Testing Base Classes

- **`ActorTestBase`**: An abstract class designed to facilitate the testing of actors within the system.
- **`ParsedActorTestBase`, `CodingActorTestBase`, `ImageActorTestBase`**: Extend `ActorTestBase` to provide testing functionalities tailored to specific actor types.


# BaseActor.kt


## User Documentation for BaseActor Class

The `BaseActor` class serves as an abstract foundation for creating actors that interact with OpenAI's GPT models via the `com.simiacryptus.jopenai` API. This class is designed to facilitate the development of applications that require conversational capabilities or complex decision-making processes based on the input provided to the model.


### Overview

The `BaseActor` class abstracts the process of sending prompts to an OpenAI model and receiving responses. It is designed to be extended by more specific actor classes that define the logic for generating prompts based on input and processing the model's responses.


### Key Features

- **Customizable Prompts and Models:** Allows specifying the prompt and the model (e.g., GPT-3.5 Turbo) to use for generating responses.
- **Temperature Control:** Enables setting the temperature parameter to control the randomness of the model's responses.
- **Convenient Response Handling:** Simplifies the process of sending requests to the OpenAI API and handling responses.


### Constructor Parameters

- `prompt`: The initial prompt or question to be sent to the model.
- `name`: An optional name for the actor. Useful for identification purposes in more complex scenarios.
- `model`: The OpenAI model to be used. Defaults to `ChatModels.GPT35Turbo`.
- `temperature`: Controls the randomness of the model's responses. Lower values make responses more deterministic.


### Methods


#### `respond(input: I, api: API, vararg messages: ApiModel.ChatMessage): R`

An abstract method that must be implemented by subclasses. It defines how the actor should respond to a given input, using the specified API and any additional chat messages.


#### `response(vararg input: ApiModel.ChatMessage, model: OpenAIModel = this.model, api: API): List<ApiModel.ChatMessage>`

Sends a chat request to the OpenAI API using the provided messages and returns the response. This method can be overridden if necessary.


#### `answer(input: I, api: API): R`

Generates a response based on the input by internally calling `respond`. It automatically handles the conversion of input to chat messages.


#### `chatMessages(questions: I): Array<ApiModel.ChatMessage>`

An abstract method that subclasses must implement. It should convert the input into an array of `ApiModel.ChatMessage` instances, which are then used to generate the model's response.


#### `withModel(model: ChatModels): BaseActor<I,R>`

An abstract method that should be implemented to return a new instance of the actor with the specified model.


### Usage Example

To use the `BaseActor` class, you must extend it and implement the abstract methods. Here's a simplified example:

```kotlin
class SimpleActor(prompt: String) : BaseActor<String, String>(prompt) {
    override fun respond(input: String, api: API, vararg messages: ApiModel.ChatMessage): String {
        val response = response(*messages, api = api)
        return response.joinToString(separator = "\n") { it.text }
    }

    override fun chatMessages(questions: String): Array<ApiModel.ChatMessage> {
        return arrayOf(ApiModel.ChatMessage(questions))
    }

    override fun withModel(model: ChatModels): BaseActor<String, String> {
        return SimpleActor(prompt).apply { this.model = model }
    }
}
```

This example demonstrates a basic actor that sends a single message to the model and returns the concatenated text of the responses.


### Conclusion

The `BaseActor` class provides a flexible and powerful foundation for building applications that interact with OpenAI's models. By extending this class and implementing its abstract methods, developers can create customized actors tailored to their specific needs.

# CodingActor.kt


## CodingActor Documentation

The `CodingActor` class is a versatile component designed to facilitate the interaction between natural language instructions and code execution within a specified programming language context. It leverages the capabilities of OpenAI's GPT models to translate instructions into executable code, evaluate the code, and provide feedback or corrections as necessary. This document provides an overview of the `CodingActor` class, its functionalities, and how to utilize it effectively.


### Overview

`CodingActor` extends the `BaseActor` class and specializes in processing coding requests (`CodeRequest`) and generating code results (`CodeResult`). It integrates with an interpreter to execute the generated code and handles the interaction with OpenAI's API for code generation and correction.


#### Key Features

- **Code Generation**: Translates natural language instructions into executable code.
- **Code Execution**: Executes the generated code within the context of a specified interpreter.
- **Error Handling and Correction**: Attempts to correct errors in the generated code through iterative feedback loops with the OpenAI API.
- **Customizable Code Formatting**: Supports customizable code formatting instructions.
- **Extensible**: Allows for the specification of additional symbols and runtime context to be used during code generation and execution.


### Usage


#### Initialization

To create an instance of `CodingActor`, you need to provide the following parameters:

- `interpreterClass`: The class of the interpreter to be used for code execution.
- `symbols`: A map of predefined symbols (variables, functions, etc.) available during code generation.
- `describer`: An instance of `TypeDescriber` used to describe the types of the provided symbols.
- `name`: An optional name for the actor.
- `details`: Optional additional details to be included in the prompt sent to the OpenAI API.
- `model`: The OpenAI model to be used for code generation (default is `ChatModels.GPT35Turbo`).
- `fallbackModel`: A fallback OpenAI model to be used in case of failure with the primary model (default is `ChatModels.GPT4Turbo`).
- `temperature`: The temperature parameter for the OpenAI API requests (default is `0.1`).
- `runtimeSymbols`: Additional symbols to be added at runtime.

Example:

```kotlin
val codingActor = CodingActor(
    interpreterClass = MyCustomInterpreter::class,
    symbols = mapOf("exampleSymbol" to Any()),
    name = "MyCodingActor",
    details = "This is a detailed description of what the actor does.",
    model = ChatModels.GPT35Turbo
)
```


#### Processing Requests

To process a coding request, create an instance of `CodeRequest` with the desired parameters, such as the list of messages (instructions and responses), code prefix, and whether to auto-evaluate the code. Then, call the `respond` method of your `CodingActor` instance with the request.

Example:

```kotlin
val request = CodingActor.CodeRequest(
    messages = listOf(Pair("Please write a function to add two numbers.", CodingActor.Role.user)),
    autoEvaluate = true
)

val result = codingActor.respond(request, apiInstance)
```


#### Handling Results

The result of a coding request is an instance of `CodeResult`, which includes the generated code, its execution status, the execution result, and a rendered response.

Example:

```kotlin
println("Status: ${result.status}")
println("Generated Code: ${result.code}")
println("Execution Result: ${result.result.resultValue}")
```


### Advanced Features

- **Custom Type Describers**: Implement custom `TypeDescriber` to control how symbols are described in the prompt.
- **Error Correction**: Utilize the error correction mechanism to automatically attempt to fix errors in the generated code.
- **Execution Result**: Access detailed execution results, including output and returned values.


### Conclusion

The `CodingActor` class provides a powerful interface for integrating natural language coding instructions with executable code, leveraging the capabilities of OpenAI's models. By customizing the interpreter, symbols, and other parameters, developers can tailor the coding actor to fit a wide range of coding tasks and workflows.

# ImageActor.kt


## ImageActor Documentation

The `ImageActor` class is a powerful tool designed to transform textual prompts into visually compelling images using OpenAI's image generation models. This class is part of the `com.simiacryptus.skyenet.core.actors` package and leverages the OpenAI API to fulfill image generation requests based on user input.


### Features

- **Customizable Prompts**: Allows setting a default prompt to guide the image generation process.
- **Flexible Image Models**: Supports different OpenAI image models, including DALL·E 2, for generating images.
- **Adjustable Image Dimensions**: Users can specify the desired width and height for the generated images.
- **Text-to-Image Transformation**: Converts user requests into image generation prompts and subsequently into images.
- **Model Switching**: Provides methods to switch between different chat and image models while retaining other settings.


### Usage


#### Initialization

To create an instance of `ImageActor`, you can use the following constructor:

```kotlin
val imageActor = ImageActor(
    prompt = "Transform the user request into an image generation prompt that the user will like",
    name = null,
    textModel = ChatModels.GPT35Turbo,
    imageModel = ImageModels.DallE2,
    temperature = 0.3,
    width = 1024,
    height = 1024
)
```


#### Generating Images

To generate an image based on a list of textual prompts, use the `respond` method:

```kotlin
val api: API = // Initialize your OpenAIClient here
val inputPrompts: List<String> = listOf("A futuristic cityscape", "under a purple sky")
val imageResponse: ImageResponse = imageActor.respond(inputPrompts, api)
```

The `respond` method processes the input prompts, interacts with the OpenAI API to generate a textual description for the image, and then generates the image based on this description.


#### Accessing the Generated Image

The `ImageResponse` object returned by the `respond` method contains both the textual description used for the image generation and the generated image itself:

```kotlin
val description: String = imageResponse.text
val image: BufferedImage = imageResponse.image
```


#### Customizing the Actor

You can customize the `ImageActor` by changing its model settings:

- To change the chat model:

```kotlin
val newChatModel: ChatModels = ChatModels.GPT4
val updatedActor = imageActor.withModel(newChatModel)
```

- To change the image model:

```kotlin
val newImageModel: ImageModels = ImageModels.Craiyon
val updatedActor = imageActor.withModel(newImageModel)
```


### Interfaces and Classes

- **`ImageActor`**: The main class responsible for transforming text prompts into images.
- **`ImageResponse`**: An interface representing the response from an image generation request, containing both the textual description and the generated image.


### Conclusion

The `ImageActor` class offers a convenient and flexible way to integrate OpenAI's image generation capabilities into your applications. By customizing prompts, models, and image dimensions, you can tailor the image generation process to meet a wide range of creative needs.

# ActorSystem.kt


## ActorSystem Documentation

The `ActorSystem` class is a core component of the framework designed to manage and interact with various types of actors within a session. It provides a structured way to access and utilize actors, ensuring that the necessary interceptors and wrappers are applied for enhanced functionality and monitoring. This document outlines the key features and usage of the `ActorSystem` class.


### Overview

An `ActorSystem` is initialized with a collection of actors, data storage interface, an optional user, and a session. It facilitates the retrieval and interaction with different types of actors, applying specific interceptors based on the actor type. This system is designed to work within the context of a session, providing a seamless way to manage actors' lifecycle and their interactions.


### Key Components

- **actors**: A map of actor identifiers to `BaseActor` instances. These are the actors managed by the `ActorSystem`.
- **dataStorage**: An implementation of `StorageInterface` to handle data storage and retrieval.
- **user**: An optional `User` instance representing the user interacting with the actors.
- **session**: A `Session` instance representing the current session.


### Features


#### Actor Retrieval

- **getActor(actor: T)**: Retrieves an instance of `BaseActor` for the specified actor identifier. If the actor has not been initialized yet, it applies the appropriate interceptor based on the actor's type and caches the result for future retrievals.


#### Interceptors

Depending on the actor type, one of the following interceptors is applied to enhance its functionality:

- `SimpleActorInterceptor`
- `ParsedActorInterceptor`
- `CodingActorInterceptor`
- `ImageActorInterceptor`
- `TextToSpeechActorInterceptor`

These interceptors wrap the original actor to provide additional features such as logging, monitoring, or custom behavior modifications.


#### Function Wrapping

For each actor, a `FunctionWrapper` is associated to enable recording and monitoring of function calls. This is facilitated through a `JsonFunctionRecorder` which stores the function call data in a structured JSON format for later analysis or debugging.


### Usage

To use the `ActorSystem`, you must first initialize it with the required components:

```kotlin
val actorSystem = ActorSystem(
    actors = mapOf(/* Actor identifiers and instances */),
    dataStorage = /* An implementation of StorageInterface */,
    user = /* An optional User instance */,
    session = /* A Session instance */
)
```

Once initialized, you can retrieve and interact with actors through the system:

```kotlin
val actor = actorSystem.getActor(/* Actor identifier */)
// Use the actor for its intended purpose
```


### Conclusion

The `ActorSystem` class provides a robust and flexible way to manage actors within a session. By abstracting the complexities of actor initialization and management, it allows developers to focus on the core logic and interactions of their application.

# opt\Expectation.kt


## Skyenet Core Expectation Module Documentation

The Skyenet Core Expectation module is a part of the Skyenet framework designed to facilitate the evaluation of responses from an AI model, specifically using the OpenAI API. This module provides a flexible way to define expectations for responses and score them based on different criteria. It includes two primary classes: `VectorMatch` and `ContainsMatch`, each serving a unique purpose in evaluating responses.


### Overview

The Expectation module is abstract and contains a companion object for logging purposes. It defines two abstract methods, `matches` and `score`, which must be implemented by subclasses to determine if a response meets a certain expectation and to assign a numerical score to the response, respectively.


#### VectorMatch Class

The `VectorMatch` class is designed to compare the semantic similarity between a given example text and a response text using embeddings. It utilizes the OpenAI API to generate embeddings for both texts and calculates the distance between them based on a specified metric.


##### Parameters:
- `example`: The example text to compare against.
- `metric`: The distance metric to use for comparison. Defaults to `DistanceType.Cosine`.


##### Methods:
- `matches(api: OpenAIClient, response: String)`: Always returns `true` as it is not used for matching but for scoring based on distance.
- `score(api: OpenAIClient, response: String)`: Calculates and returns the negative distance between the embeddings of the example text and the response text, effectively scoring the response based on its semantic similarity to the example.


#### ContainsMatch Class

The `ContainsMatch` class checks if a given pattern (regular expression) is contained within the response text. It is useful for scenarios where the presence of specific patterns or keywords is critical or of interest.


##### Parameters:
- `pattern`: The regular expression pattern to search for within the response.
- `critical`: A boolean indicating if the match is critical. If `false`, `matches` always returns `true`. Defaults to `true`.


##### Methods:
- `matches(api: OpenAIClient, response: String)`: Returns `true` if the pattern is found in the response and `critical` is `true`, otherwise returns `true` or `false` based on the `critical` flag.
- `score(api: OpenAIClient, response: String)`: Returns `1.0` if the pattern matches the response, otherwise returns `0.0`.


### Usage

To use the Expectation module, instantiate either `VectorMatch` or `ContainsMatch` with the appropriate parameters and call the `matches` and/or `score` methods with an instance of `OpenAIClient` and the response text to evaluate.


#### Example

```kotlin
val apiClient = OpenAIClient(...)
val expectation = VectorMatch("Example text")
val response = "Response text from the AI model"
val score = expectation.score(apiClient, response)
println("Score: $score")
```

This module provides a powerful and flexible way to evaluate AI model responses, enabling developers to implement custom logic for scoring and matching responses based on semantic similarity or the presence of specific patterns.

# ParsedActor.kt


## ParsedActor Class Documentation

The `ParsedActor` class is a specialized actor designed to parse responses from chat models into specific data types. It extends the functionality of a base actor to not only interact with chat models but also to process the responses into a more structured format. This class is particularly useful when working with chat models to perform specific tasks that require structured output.


### Features

- **Custom Parsing**: Allows for the specification of a custom parser to convert chat model responses into a desired data type.
- **Flexible Chat Model Interaction**: Supports interaction with different chat models, including specifying the model for parsing responses.
- **Error Handling**: Implements retry logic for deserialization to handle potential parsing errors gracefully.
- **Customizable Prompts**: Enables the use of custom prompts for initiating chat model conversations.


### Constructor Parameters

- `parserClass`: The class of the parser function used to convert chat model responses into the desired data type.
- `prompt`: The initial prompt to send to the chat model.
- `name`: An optional name for the actor. Defaults to the simple name of the parser class if not provided.
- `model`: The chat model to use for generating responses. Defaults to `ChatModels.GPT35Turbo`.
- `temperature`: The temperature setting for the chat model, affecting the randomness of responses. Defaults to `0.3`.
- `parsingModel`: The chat model to use specifically for parsing responses. Defaults to `ChatModels.GPT35Turbo`.
- `deserializerRetries`: The number of retries for deserialization in case of parsing errors. Defaults to `2`.


### Methods


#### `chatMessages(questions: List<String>)`
Generates an array of chat messages to be sent to the chat model, including the initial system prompt and user questions.


#### `getParser(api: API)`
Creates and returns a parser function based on the specified parser class and chat model settings.


#### `respond(input: List<String>, api: API, vararg messages: ApiModel.ChatMessage)`
Processes the given input questions and returns a `ParsedResponse` containing the parsed data type.


#### `withModel(model: ChatModels)`
Creates a new instance of `ParsedActor` with the specified chat model, retaining other settings.


### Inner Classes


#### `ParsedResponseImpl`
An implementation of `ParsedResponse` that holds the parsed object and the original text response from the chat model.


### Usage Example

```kotlin
// Define a parser function class that converts a String to a custom data type
class MyParser : Function<String, MyDataType> {
    override fun apply(t: String): MyDataType {
        // Implement parsing logic here
    }
}

// Initialize a ParsedActor with the custom parser and a prompt
val parsedActor = ParsedActor(
    parserClass = MyParser::class.java,
    prompt = "Please provide information about X."
)

// Use the actor to send questions and receive parsed responses
val response = parsedActor.respond(listOf("Question about X"), api)
```

This documentation provides an overview of the `ParsedActor` class and its capabilities. For more detailed information on specific methods and parameters, refer to the source code documentation.

# ParsedResponse.kt


## User Documentation for ParsedResponse Class


### Overview

The `ParsedResponse` class is an abstract class designed to handle and represent responses parsed from various sources in a structured manner. It is part of the `com.simiacryptus.skyenet.core.actors` package. This class is generic and can be used to represent any type of parsed data by specifying the type parameter `T` during implementation. The class is designed to encapsulate both the raw text of the response and its parsed object representation.


### Key Features

- **Type-Safe Responses**: By specifying the type parameter `T`, users can ensure that the responses handled by instances of `ParsedResponse` are type-safe, making the code more robust and easier to maintain.
- **Dual Representation**: The class stores both the raw text of the response (`text`) and its object representation (`obj`), allowing users to access the data in the format most convenient for their needs.
- **Abstract Design**: Being an abstract class, `ParsedResponse` serves as a template for creating more specific response handlers tailored to particular data types or parsing requirements.


### Properties

- `clazz: Class<T>`: The class literal of the type parameter `T`. This property is used to specify the type of the parsed object and is essential for type safety and reflection-based operations.
- `text: String`: An abstract property that should be overridden to return the raw text of the parsed response.
- `obj: T`: An abstract property that should be overridden to return the object representation of the parsed response.


### Methods

- `toString()`: Overrides the `toString` method to return the raw text of the response. This method provides a convenient way to access the raw response as a string.


### Usage

To use the `ParsedResponse` class, you need to extend it with a concrete implementation that specifies the type of the parsed object and provides implementations for the abstract properties `text` and `obj`.


#### Example

```kotlin
class JsonResponse(parsedText: String) : ParsedResponse<JSONObject>(JSONObject::class.java) {
    override val text: String = parsedText
    override val obj: JSONObject = JSONObject(parsedText)
    
    // Additional functionality specific to JSON responses can be added here
}
```

In this example, `JsonResponse` is a concrete implementation of `ParsedResponse` for handling JSON responses. It specifies `JSONObject` as the type parameter `T` and provides implementations for the `text` and `obj` properties. This class can now be used to handle JSON responses in a type-safe and structured manner.


### Conclusion

The `ParsedResponse` class provides a flexible and type-safe framework for handling parsed responses. By extending this class, developers can create customized response handlers that encapsulate both the raw and object representations of responses, tailored to specific data types or parsing requirements.

# record\CodingActorInterceptor.kt


## CodingActorInterceptor Documentation

The `CodingActorInterceptor` class is a specialized implementation of the `CodingActor` interface, designed to intercept and potentially modify the behavior of another `CodingActor` instance. This class is part of the `com.simiacryptus.skyenet.core.actors.record` package and is intended for use in scenarios where additional processing or logging of the interactions with a `CodingActor` is required.


### Features

- **Function Interception**: Allows for the interception and modification of the responses generated by the wrapped `CodingActor` instance.
- **Compatibility**: Seamlessly integrates with existing `CodingActor` instances without requiring changes to their implementation.
- **Customizable Behavior**: Through the use of a `FunctionWrapper`, users can define custom behavior to be executed before or after the wrapped `CodingActor`'s methods are invoked.


### Usage


#### Initialization

To use the `CodingActorInterceptor`, you must first instantiate it with a reference to an existing `CodingActor` instance and a `FunctionWrapper` that defines the interception logic.

```kotlin
val innerActor: CodingActor = // Initialize your CodingActor
val functionInterceptor: FunctionWrapper = // Define your FunctionWrapper
val interceptor = CodingActorInterceptor(innerActor, functionInterceptor)
```


#### Overridden Methods

The `CodingActorInterceptor` overrides several methods from the `CodingActor` interface, allowing it to intercept calls to these methods. The key methods are:

- `response(...)`: Intercepts the generation of responses to chat messages.
- `respond(...)`: Intercepts the generation of code responses to code requests.
- `execute(...)`: Intercepts the execution of code.


#### Example: Interception Logic

The interception logic is defined within the `FunctionWrapper` provided at initialization. This example demonstrates a simple logging wrapper:

```kotlin
val functionInterceptor = FunctionWrapper { args, proceed ->
    println("Before invocation with args: ${args.joinToString()}")
    val result = proceed(*args)
    println("After invocation, result: $result")
    result
}
```

This `FunctionWrapper` logs the arguments before the invocation of the wrapped method and the result after the invocation.


### Integration

To integrate the `CodingActorInterceptor` into your system, replace direct references to the original `CodingActor` instance with references to the `CodingActorInterceptor` instance. This allows all interactions with the `CodingActor` to be intercepted according to the logic defined in your `FunctionWrapper`.


### Conclusion

The `CodingActorInterceptor` provides a powerful mechanism for augmenting the behavior of `CodingActor` instances, enabling scenarios such as logging, monitoring, and dynamic behavior modification. By leveraging the `FunctionWrapper`, developers can easily inject custom logic into the processing pipeline of a `CodingActor`, enhancing its capabilities and facilitating advanced use cases.

# opt\ActorOptimization.kt


## ActorOptimization Class Documentation

The `ActorOptimization` class is designed to facilitate the optimization of actor responses using genetic algorithms and OpenAI's GPT models. This class allows for the generation, mutation, and recombination of prompts to produce optimized responses based on defined test cases and expectations.


### Features

- **Genetic Algorithm Implementation**: Utilizes genetic algorithms for optimizing prompts to achieve desired responses.
- **Flexible Actor Factory**: Supports any actor implementation that can generate responses based on prompts.
- **Customizable Mutation and Recombination**: Allows for the customization of mutation rates and types, as well as recombination logic.
- **Test Case Evaluation**: Supports the definition of test cases with expectations to guide the optimization process.


### Key Components


#### TestCase Class

Represents a test case with user messages, expectations, and the number of retries. It is used to evaluate the effectiveness of a prompt.

- `userMessages`: A list of messages simulating a conversation leading up to the response.
- `expectations`: A list of expectations to evaluate the response.
- `retries`: The number of retries allowed for the test case.


#### GeneticApi Interface

Defines the operations for mutating and recombining prompts.

- `mutate`: Mutates a given prompt based on a directive (e.g., Rephrase, Randomize).
- `recombine`: Recombines two prompts to produce a new prompt.


#### Main Methods


##### runGeneticGenerations

Runs multiple generations of genetic optimization to improve prompts based on test cases.

Parameters:
- `prompts`: Initial list of prompts to optimize.
- `testCases`: List of `TestCase` instances to evaluate prompts.
- `actorFactory`: Factory function to create actors based on prompts.
- `resultMapper`: Function to map actor responses to strings for evaluation.
- `selectionSize`: Number of top prompts to select for the next generation.
- `populationSize`: Size of the prompt population for each generation.
- `generations`: Number of generations to run.

Returns:
- A list of optimized prompts.


##### regenerate

Generates a new set of prompts based on progenitors through mutation or recombination.

Parameters:
- `progenetors`: List of parent prompts.
- `desiredCount`: Desired number of prompts in the new generation.

Returns:
- A list of new prompts.


### Usage Example

```kotlin
val apiClient = OpenAIClient("your_api_key")
val actorOptimization = ActorOptimization(api = apiClient)

val initialPrompts = listOf("How can I help you today?")
val testCases = listOf(
    ActorOptimization.TestCase(
        userMessages = listOf(ApiModel.ChatMessage(role = ApiModel.Role.user, content = "I need assistance with my order.")),
        expectations = listOf(/* Define your expectations here */)
    )
)

val optimizedPrompts = actorOptimization.runGeneticGenerations(
    prompts = initialPrompts,
    testCases = testCases,
    actorFactory = { prompt -> /* Your actor factory logic here */ },
    resultMapper = { response -> /* Your result mapping logic here */ }
)

println("Optimized Prompts: $optimizedPrompts")
```


### Customization

You can customize the mutation rate and types by adjusting the `mutationRate` and `mutatonTypes` parameters during the instantiation of the `ActorOptimization` class.


### Conclusion

The `ActorOptimization` class provides a powerful tool for optimizing conversational prompts using genetic algorithms and OpenAI's GPT models. By defining test cases and expectations, users can iteratively improve prompts to achieve desired responses.

# record\ImageActorInterceptor.kt


## User Documentation for ImageActorInterceptor

The `ImageActorInterceptor` class is a specialized component designed to intercept and potentially modify the behavior of an `ImageActor` instance. This class is part of the `com.simiacryptus.skyenet.core.actors.record` package and is intended for advanced users who need to customize or extend the functionality of image generation or processing within a system that utilizes the Skyenet framework.


### Overview

`ImageActorInterceptor` acts as a wrapper around an existing `ImageActor` instance, allowing users to intercept and modify the inputs and outputs of the `ImageActor`'s methods. This is particularly useful for debugging, logging, or applying custom transformations to the data processed by the `ImageActor`.


### Key Features

- **Function Interception**: Allows interception of the `response` and `render` methods of the `ImageActor`.
- **Custom Processing**: Enables custom processing of inputs and outputs without modifying the original `ImageActor` code.
- **Compatibility**: Seamlessly integrates with existing `ImageActor` instances, preserving their properties and behaviors.


### Usage


#### Initialization

To use the `ImageActorInterceptor`, you must first have an instance of `ImageActor` that you wish to intercept. You then create an instance of `ImageActorInterceptor` by passing the `ImageActor` instance and a `FunctionWrapper` instance to its constructor.

```kotlin
val originalImageActor = ImageActor(...)
val functionInterceptor = FunctionWrapper(...)
val imageActorInterceptor = ImageActorInterceptor(originalImageActor, functionInterceptor)
```


#### Interception and Custom Processing

The `FunctionWrapper` provided during initialization is responsible for defining how the interception and potential modification of the `ImageActor`'s behavior occur. You must implement the `wrap` method of the `FunctionWrapper` to specify the custom processing logic.

```kotlin
val functionInterceptor = object : FunctionWrapper {
    override fun <T, R> wrap(input: T, function: (T) -> R): R {
        // Implement custom processing logic here
        return function(input)
    }
}
```


#### Methods


##### `response`

Intercepts the `response` method calls of the `ImageActor`, allowing for custom processing of chat messages and model interactions.

```kotlin
imageActorInterceptor.response(inputMessages, model, api)
```


##### `render`

Intercepts the `render` method calls of the `ImageActor`, enabling custom processing of text inputs for image rendering.

```kotlin
val image = imageActorInterceptor.render(text, api)
```


### Conclusion

The `ImageActorInterceptor` provides a powerful mechanism for customizing the behavior of `ImageActor` instances in the Skyenet framework. By leveraging function interception, developers can implement custom logic for processing inputs and outputs, enhancing the flexibility and capabilities of their image generation and processing systems.

# record\ParsedActorInterceptor.kt


## ParsedActorInterceptor Documentation

The `ParsedActorInterceptor` class is designed to act as a middleware that intercepts and modifies the behavior of a `ParsedActor` in a flexible manner. It allows for the interception and manipulation of responses generated by the `ParsedActor` through a custom `FunctionWrapper`. This class is part of the `com.simiacryptus.skyenet.core.actors.record` package.


### Features

- **Interception and Modification**: Allows for the interception of the response generation process, enabling the modification or enhancement of responses.
- **Flexible Parsing**: Utilizes a custom parser to process and transform the response text into a desired object format.
- **Lazy Evaluation**: Employs lazy evaluation for response objects to optimize performance and resource utilization.
- **Seamless Integration**: Designed to seamlessly integrate with existing `ParsedActor` instances without requiring significant modifications.


### Usage


#### Initialization

To initialize a `ParsedActorInterceptor`, you need to provide an instance of `ParsedActor` and a `FunctionWrapper`. The `FunctionWrapper` is responsible for defining how the responses are intercepted and processed.

```kotlin
val parsedActorInterceptor = ParsedActorInterceptor(
    inner = parsedActorInstance,
    functionInterceptor = customFunctionWrapper
)
```


#### Responding to Inputs

The `ParsedActorInterceptor` overrides the `respond` method to intercept the response generation process. It utilizes the provided `FunctionWrapper` to modify or enhance the response based on custom logic.

```kotlin
val response = parsedActorInterceptor.respond(
    input = listOf("Your input here"),
    api = apiInstance
)
```


#### Custom Response Processing

The interceptor uses a lazy evaluation strategy for processing response objects. The actual parsing and processing of the response text into the desired object format are deferred until the `obj` property is accessed. This approach optimizes performance by avoiding unnecessary computations.


### Key Methods

- **respond**: Intercepts the response generation process, allowing for custom processing of inputs and modification of the generated response.
- **response**: A method designed to wrap the response generation process, enabling the interception and modification of responses for a given set of input messages and a specified model.


### Example

```kotlin
// Initialize your ParsedActor instance
val parsedActor = ...

// Define your custom function wrapper for intercepting responses
val functionWrapper = FunctionWrapper { ... }

// Initialize the ParsedActorInterceptor with the ParsedActor and FunctionWrapper
val interceptor = ParsedActorInterceptor(
    inner = parsedActor,
    functionInterceptor = functionWrapper
)

// Use the interceptor to respond to inputs
val customResponse = interceptor.respond(
    input = listOf("Hello, world!"),
    api = apiInstance
)

// Access the modified response text and object
println(customResponse.text)
println(customResponse.obj)
```


### Conclusion

The `ParsedActorInterceptor` provides a powerful mechanism for intercepting and modifying the behavior of `ParsedActor` instances. By leveraging custom function wrappers, developers can implement sophisticated logic to enhance and tailor the responses generated by their actors, enabling more dynamic and context-aware applications.

# record\TextToSpeechActorInterceptor.kt


## TextToSpeechActorInterceptor Documentation

The `TextToSpeechActorInterceptor` class is a part of the `com.simiacryptus.skyenet.core.actors.record` package, designed to intercept and modify the behavior of a `TextToSpeechActor` instance. This class allows for additional processing or modification of inputs and outputs to the `TextToSpeechActor` through a provided `FunctionWrapper`. It is particularly useful for debugging, logging, or applying custom transformations to the text-to-speech process.


### Features

- **Interception and Modification**: Allows for the interception and potential modification of both the input to and output from the `TextToSpeechActor` methods.
- **Function Wrapping**: Utilizes a `FunctionWrapper` to apply custom logic around the invocation of the `TextToSpeechActor`'s methods.
- **Compatibility**: Seamlessly integrates with existing `TextToSpeechActor` instances without requiring modifications to their implementation.


### Usage


#### Initialization

To use the `TextToSpeechActorInterceptor`, you must first instantiate it with an existing `TextToSpeechActor` and a `FunctionWrapper` that defines the interception logic.

```kotlin
val originalActor = TextToSpeechActor(name, audioModel, "alloy", 1.0)
val functionInterceptor = FunctionWrapper(/* custom interception logic here */)
val interceptor = TextToSpeechActorInterceptor(originalActor, functionInterceptor)
```


#### Interception

Once the interceptor is initialized, it can be used in place of the original `TextToSpeechActor`. Calls to `response` and `render` will be intercepted according to the logic defined in the `FunctionWrapper`.


##### Response Interception

The `response` method allows for the interception of chat message processing. The custom logic can modify the input messages or the processing behavior before delegating to the original actor's `response` method.

```kotlin
interceptor.response(inputMessages, model, api)
```


##### Render Interception

The `render` method enables the interception of text rendering to audio. The interception logic can modify the input text or the rendering behavior before calling the original actor's `render` method.

```kotlin
val audioBytes = interceptor.render(text, api)
```


### Customization

The behavior of the `TextToSpeechActorInterceptor` is largely determined by the `FunctionWrapper` provided during initialization. By implementing custom logic within this wrapper, users can achieve a wide range of effects, from simple logging to complex input/output transformations.


### Conclusion

The `TextToSpeechActorInterceptor` provides a powerful and flexible mechanism for augmenting the behavior of `TextToSpeechActor` instances. By leveraging function wrapping, it offers a seamless way to integrate custom logic into the text-to-speech process, making it an invaluable tool for developers looking to extend or customize the functionality of their text-to-speech applications.

# record\SimpleActorInterceptor.kt


## SimpleActorInterceptor Documentation

The `SimpleActorInterceptor` class is a part of the `com.simiacryptus.skyenet.core.actors.record` package, designed to act as a wrapper around instances of `SimpleActor`. This class allows for the interception and potential modification of function calls, specifically the `response` function of the `SimpleActor` class. It is particularly useful for scenarios where additional processing or logging is required for the inputs and outputs of the `response` function.


### Features

- **Function Interception**: Enables the interception of the `response` function calls to `SimpleActor`, allowing for pre-processing or post-processing of inputs and outputs.
- **Custom Processing**: Through the use of a `FunctionWrapper`, custom logic can be applied to the inputs and outputs of the `response` function.
- **Seamless Integration**: Inherits from `SimpleActor`, ensuring compatibility and ease of integration with existing systems that utilize `SimpleActor` instances.


### Usage

To use the `SimpleActorInterceptor`, you must first have an instance of `SimpleActor` that you wish to wrap. You will also need to define or have a `FunctionWrapper` that specifies the custom logic to be applied during the interception.


#### Example

Below is a simple example demonstrating how to create and use a `SimpleActorInterceptor`:

```kotlin
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.actors.record.SimpleActorInterceptor
import com.simiacryptus.skyenet.core.util.FunctionWrapper

// Create an instance of SimpleActor
val simpleActor = SimpleActor(
    prompt = "Your prompt here",
    name = "ActorName",
    model = yourModelInstance, // Replace with your OpenAIModel instance
    temperature = 0.5
)

// Define your custom function logic
val functionInterceptor = FunctionWrapper { input, model, function ->
    // Custom pre-processing logic here
    println("Before calling response: $input")
    
    // Call the original function
    val result = function(input, model)
    
    // Custom post-processing logic here
    println("After calling response")
    
    // Return the result
    result
}

// Create an instance of SimpleActorInterceptor
val interceptor = SimpleActorInterceptor(simpleActor, functionInterceptor)

// Now you can use the interceptor as you would use a SimpleActor
// The interceptor will apply your custom logic before and after calling the response function
```


### Parameters

- `inner`: The `SimpleActor` instance that is being wrapped.
- `functionInterceptor`: An instance of `FunctionWrapper` that defines the custom logic to be applied during the interception.


### Methods

- `response(vararg input: com.simiacryptus.jopenai.ApiModel.ChatMessage, model: OpenAIModel, api: API)`: Overrides the `response` method of `SimpleActor` to apply the custom logic defined in `functionInterceptor` before and after the original `response` function is called.


### Conclusion

The `SimpleActorInterceptor` class provides a powerful way to add custom processing logic to the inputs and outputs of the `response` function of `SimpleActor` instances. By leveraging the `FunctionWrapper`, developers can easily implement custom pre-processing and post-processing logic to enhance the functionality of their `SimpleActor` instances.

# test\ImageActorTestBase.kt


## User Documentation for ImageActorTestBase


### Overview

The `ImageActorTestBase` class is part of the testing framework for the `ImageActor` component within the Skyenet project. This abstract class extends `ActorTestBase`, specifically tailored for testing actors that process a list of strings as input and produce an `ImageResponse`. It provides a foundational setup for creating and testing instances of `ImageActor` based on different prompts.


### Key Features

- **Abstract Testing Framework**: Designed to facilitate the testing of `ImageActor` instances, ensuring they function as expected when given various prompts.
- **Prompt-Based Actor Factory**: Implements a method to instantiate `ImageActor` objects using specific prompts, allowing for flexible testing scenarios.


### Getting Started

To utilize `ImageActorTestBase` in your testing suite, follow these steps:

1. **Extend `ImageActorTestBase`**: Since `ImageActorTestBase` is an abstract class, you'll need to create a concrete subclass that implements any abstract methods (if any exist beyond those implemented in `ActorTestBase`).

2. **Implement Required Methods**: Ensure all abstract methods inherited from `ActorTestBase` are implemented. In the case of `ImageActorTestBase`, the `actorFactory` method is already implemented, but you may need to provide implementations for other abstract methods depending on your specific testing needs.

3. **Define Test Cases**: Create test cases within your subclass that utilize the `actorFactory` method to generate `ImageActor` instances with various prompts. These instances can then be used to verify the actor's response to different inputs.

4. **Run Tests**: Execute your tests to validate the behavior of `ImageActor` instances. This can help identify any issues or unexpected behavior in the actor's implementation.


### Example Usage

Below is a hypothetical example of how one might extend `ImageActorTestBase` to create a specific test suite for `ImageActor`:

```kotlin
package com.example.tests

import com.simiacryptus.skyenet.core.actors.test.ImageActorTestBase
import com.simiacryptus.skyenet.core.actors.ImageResponse
import org.junit.Assert
import org.junit.Test

class MyImageActorTests : ImageActorTestBase() {

    @Test
    fun testImageActorWithSamplePrompt() {
        val prompt = "A sample prompt"
        val imageActor = actorFactory(prompt)
        val response = imageActor.process(listOf(prompt))
        
        // Perform assertions on the response
        Assert.assertNotNull("The response should not be null", response)
        Assert.assertTrue("The response should be of type ImageResponse", response is ImageResponse)
        // Add more assertions as needed to validate the response
    }
}
```

In this example, `MyImageActorTests` extends `ImageActorTestBase` and defines a test case that checks whether `ImageActor` produces a non-null response of the correct type when given a sample prompt.


### Conclusion

`ImageActorTestBase` provides a structured approach to testing `ImageActor` instances within the Skyenet project. By extending this class and implementing specific test cases, developers can ensure their image processing actors behave as expected across a variety of scenarios.

# test\CodingActorTestBase.kt


## CodingActorTestBase Documentation

The `CodingActorTestBase` class is an abstract base class designed for testing coding actors in the Skyenet framework. It extends the functionality of `ActorTestBase` by specifically catering to actors that generate or interpret code, providing a streamlined way to create, test, and evaluate coding actors.


### Overview

Coding actors are specialized actors within the Skyenet framework that deal with generating or interpreting code based on given prompts. The `CodingActorTestBase` class facilitates the testing of these actors by setting up a common testing infrastructure that includes creating actors, sending prompts, and evaluating the generated code.


### Key Components


#### Properties

- `abstract val interpreterClass: KClass<out Interpreter>`: This abstract property must be implemented by subclasses to specify the class of the interpreter that the coding actor should use. The interpreter class must extend the `Interpreter` interface.


#### Methods

- `actorFactory(prompt: String): CodingActor`: This method overrides the `actorFactory` method from `ActorTestBase`. It is responsible for creating an instance of `CodingActor` with the specified interpreter class and details (prompt). This allows for the dynamic creation of coding actors based on the test requirements.

- `getPrompt(actor: BaseActor<CodingActor.CodeRequest, CodeResult>): String`: This method overrides the `getPrompt` method from `ActorTestBase`. It retrieves the prompt (details) from the given coding actor. This prompt is what the actor uses to generate or interpret code.

- `resultMapper(result: CodeResult): String`: This method overrides the `resultMapper` method from `ActorTestBase`. It maps the `CodeResult` (the output of the coding actor) to a string representation of the code. This is useful for evaluating the actor's output or for further processing.


### Usage

To use the `CodingActorTestBase`, you need to create a subclass that implements the `interpreterClass` property. This subclass will specify the interpreter that the coding actors should use during testing. Once the subclass is defined, you can create instances of your coding actors by providing prompts, and then use the provided methods to test and evaluate their code generation capabilities.


#### Example

```kotlin
class MyCodingActorTest : CodingActorTestBase() {
    override val interpreterClass = MyInterpreter::class // Specify your interpreter class here

    fun testMyCodingActor() {
        val prompt = "Generate a greeting message"
        val actor = actorFactory(prompt)
        val result = actor.generateCode(CodeRequest(prompt))
        println(resultMapper(result))
    }
}
```

In this example, `MyCodingActorTest` extends `CodingActorTestBase` and specifies `MyInterpreter` as the interpreter class. It includes a test method that creates a coding actor with a given prompt, generates code based on that prompt, and then prints the generated code.


### Conclusion

The `CodingActorTestBase` class provides a foundational framework for testing coding actors within the Skyenet framework. By abstracting common tasks such as actor creation, prompt handling, and result mapping, it simplifies the process of developing and testing coding actors.

# SimpleActor.kt


## SimpleActor Class Documentation

The `SimpleActor` class is part of the `com.simiacryptus.skyenet.core.actors` package and extends the functionality of the `BaseActor` class. It is designed to facilitate easy interaction with OpenAI's GPT models through the `com.simiacryptus.jopenai` API. This class simplifies the process of sending prompts to the model and receiving responses.


### Constructor Parameters

- `prompt`: A `String` representing the initial prompt or context to be sent to the model.
- `name`: An optional `String` parameter that specifies the name of the actor. It defaults to `null` if not provided.
- `model`: Specifies the model to be used for generating responses. It defaults to `ChatModels.GPT35Turbo`.
- `temperature`: A `Double` value that controls the randomness of the model's responses. Lower values make the model more deterministic. It defaults to `0.3`.


### Methods


#### respond

```kotlin
override fun respond(input: List<String>, api: API, vararg messages: ApiModel.ChatMessage): String
```

Generates a response based on the provided input and messages.

- `input`: A list of `String` representing the user's input or questions.
- `api`: An instance of `API` used to communicate with the OpenAI API.
- `messages`: Vararg parameter of `ApiModel.ChatMessage` representing additional context or messages to be considered by the model.

**Returns**: A `String` representing the model's response.

**Throws**: `RuntimeException` if no response is received from the model.


#### chatMessages

```kotlin
override fun chatMessages(questions: List<String>): Array<ApiModel.ChatMessage>
```

Converts a list of questions into an array of `ApiModel.ChatMessage`, including the initial prompt as part of the system's role.

- `questions`: A list of `String` representing the questions or inputs from the user.

**Returns**: An array of `ApiModel.ChatMessage` ready to be sent to the model.


#### withModel

```kotlin
override fun withModel(model: ChatModels): SimpleActor
```

Creates a new instance of `SimpleActor` with the specified model while retaining the other properties.

- `model`: The `ChatModels` instance specifying the new model to be used.

**Returns**: A new instance of `SimpleActor` configured with the specified model.


### Usage Example

```kotlin
val simpleActor = SimpleActor(
    prompt = "Hello, how can I assist you today?",
    name = "Assistant",
    model = ChatModels.GPT35Turbo,
    temperature = 0.3
)

val api = API("your_api_key") // Initialize your API instance with your API key
val questions = listOf("What is the weather like today?", "Can you tell me a joke?")
val response = simpleActor.respond(questions, api)
println(response)
```

This example demonstrates how to create an instance of `SimpleActor`, initialize it with a prompt, and use it to generate responses to a list of questions.

# test\ActorTestBase.kt


## ActorTestBase Documentation

The `ActorTestBase` class is an abstract class designed to facilitate the testing of actors within a system. It provides a structured way to optimize and test actors through predefined test cases and optimization strategies. This class is part of the `com.simiacryptus.skyenet.core.actors.test` package.


### Overview

The `ActorTestBase` class serves as a foundation for testing different types of actors. An actor, in this context, refers to a component that takes input, performs some processing, and produces output. The class is designed to be extended by concrete test classes that specify the behavior of the actors being tested.


### Key Components


#### Properties

- `api`: An instance of `OpenAIClient` used for making API calls. It is initialized with a debug log level.
- `testCases`: An abstract property that should be overridden to provide a list of test cases for optimization.
- `actor`: An abstract property representing the actor being tested.
- `actorFactory`: An abstract function that takes a prompt string and returns a new instance of the actor.


#### Methods


##### `opt()`

This method runs the optimization process for the actor. It uses genetic algorithms to evolve the actor based on the provided test cases. The method parameters allow for customization of the optimization process, including specifying a different actor or test cases.

Parameters:
- `actor`: The actor to optimize. Defaults to the actor defined in the class.
- `testCases`: The test cases to use for optimization. Defaults to the test cases defined in the class.
- `actorFactory`: A factory function to create new actor instances. Defaults to the `actorFactory` method defined in the class.
- `resultMapper`: A function to map the actor's output to a string. Defaults to the `resultMapper` method defined in the class.


##### `testOptimize()`

A convenience method that calls the `opt()` method with default parameters. It starts the optimization process for the actor using the predefined test cases and actor factory.


##### `testRun()`

This method iterates over the test cases and tests the actor's response to each case. It constructs the input messages for the actor, calls the `answer()` method to get the actor's response, and logs the result.


##### `answer()`

Takes an array of `ApiModel.ChatMessage` objects as input and returns the actor's response. This method is responsible for converting the chat messages into the appropriate input format for the actor, calling the actor's `respond()` method, and returning the result.


#### Companion Object

Contains a logger instance for logging information related to the test execution.


### Usage

To use the `ActorTestBase` class, you need to extend it with a concrete class that specifies the actor and test cases. Here's a simplified example:

```kotlin
class MyActorTest : ActorTestBase<MyInputType, MyResultType>() {
    override val testCases = listOf(/* Define your test cases here */)
    override val actor = MyActor()

    override fun actorFactory(prompt: String): BaseActor<MyInputType, MyResultType> {
        // Implement the logic to create a new instance of your actor
    }

    override fun getPrompt(actor: BaseActor<MyInputType, MyResultType>): String {
        // Implement the logic to generate a prompt for your actor
    }

    override fun resultMapper(result: MyResultType): String {
        // Implement the logic to convert your actor's result to a string
    }
}
```

In this example, you would replace `MyInputType` and `MyResultType` with the actual types used by your actor. You would also implement the abstract methods to provide the necessary functionality for testing your actor.

# test\ParsedActorTestBase.kt


## User Documentation for ParsedActorTestBase

The `ParsedActorTestBase` class is an abstract class designed to facilitate the testing of actors that parse responses using a specified parser. This class is part of the `com.simiacryptus.skyenet.core.actors.test` package and extends the functionality provided by `ActorTestBase`. It is tailored for use with actors that take a list of strings as input and produce a `ParsedResponse` object containing a generic type `T`.


### Overview

The `ParsedActorTestBase` class is designed to streamline the process of testing parsed actors by providing a structured framework. It abstracts away common testing functionalities, allowing developers to focus on the specifics of their parsed actor implementations. The class requires specifying the type of parser to be used for parsing responses through the `parserClass` parameter.


### Key Components


#### Constructor Parameters

- `parserClass`: This parameter expects a class that implements the `Function<String, T>` interface. It defines the parser that will be used to parse the responses from the actor. The parser class should take a string as input and return an instance of type `T`.


#### Methods


##### actorFactory

```kotlin
override fun actorFactory(prompt: String): ParsedActor
```

- **Description**: Creates an instance of `ParsedActor` with the specified prompt and parser class. The parsing model is set to `ChatModels.GPT35Turbo` by default.
- **Parameters**:
  - `prompt`: A string representing the prompt to be used by the actor.
- **Returns**: An instance of `ParsedActor` configured with the provided prompt and parser.


##### getPrompt

```kotlin
override fun getPrompt(actor: BaseActor<List<String>,ParsedResponse<T>>): String
```

- **Description**: Retrieves the prompt associated with the given actor.
- **Parameters**:
  - `actor`: The actor whose prompt is to be retrieved.
- **Returns**: The prompt string used by the specified actor.


##### resultMapper

```kotlin
override fun resultMapper(result: ParsedResponse<T>): String
```

- **Description**: Maps the `ParsedResponse` object to a string representation. This method is used to extract the text content from the `ParsedResponse` object.
- **Parameters**:
  - `result`: The `ParsedResponse` object containing the parsed result.
- **Returns**: A string representation of the parsed result.


### Usage

To use the `ParsedActorTestBase` class, you need to extend it in your test class and specify the type of the parser class as well as the generic type `T` that your parser returns. You will also need to implement any abstract methods if required.


#### Example

```kotlin
class MyActorTest : ParsedActorTestBase<MyResponseType>(MyResponseParser::class.java) {
    // Implement additional test methods or override existing ones if necessary
}
```

In this example, `MyResponseType` is the type returned by the parser, and `MyResponseParser` is the class that implements the `Function<String, MyResponseType>` interface to parse the actor's responses.


### Conclusion

The `ParsedActorTestBase` class provides a structured and efficient way to test actors that require response parsing. By abstracting common functionalities and providing a clear framework, it simplifies the testing process and allows developers to focus on the specifics of their actor implementations.

# TextToSpeechActor.kt


## TextToSpeechActor Documentation

The `TextToSpeechActor` class is a part of the `com.simiacryptus.skyenet.core.actors` package, designed to convert text into speech using OpenAI's API. This class extends the `BaseActor` class, allowing it to process lists of strings and generate speech responses.


### Features

- **Customizable Voice and Speed**: Users can specify the voice and speed of the speech.
- **Support for Different Audio Models**: The class supports various audio models provided by OpenAI.
- **Lazy Loading of Speech Data**: The speech data is loaded lazily, meaning it's only processed when needed, optimizing resource usage.


### Constructor Parameters

- `name`: Optional. The name of the actor.
- `audioModel`: The audio model to use for text-to-speech conversion. Defaults to `AudioModels.TTS_HD`.
- `voice`: The voice to be used. Defaults to `"alloy"`.
- `speed`: The speed of the speech. Defaults to `1.0`.


### Methods


#### `chatMessages(questions: List<String>)`

Converts a list of questions into an array of `ChatMessage` objects, which are then used by the OpenAI API for processing.

- **Parameters**: `questions` - A list of strings representing the questions or text to be converted into speech.
- **Returns**: An array of `ChatMessage` objects.


#### `render(text: String, api: API)`

Converts the given text into speech data using the specified OpenAI API client.

- **Parameters**:
  - `text`: The text to be converted into speech.
  - `api`: The OpenAI API client to use for the conversion.
- **Returns**: A byte array containing the speech data.


#### `respond(input: List<String>, api: API, vararg messages: ChatMessage)`

Processes the input text and generates a `SpeechResponse` containing the speech data.

- **Parameters**:
  - `input`: A list of strings to be processed.
  - `api`: The OpenAI API client to use for processing.
  - `messages`: Additional chat messages to consider in the processing.
- **Returns**: A `SpeechResponseImpl` object containing the speech data.


#### `withModel(model: AudioModels)`

Creates a new instance of `TextToSpeechActor` with the specified audio model.

- **Parameters**: `model` - The audio model to use.
- **Returns**: A new instance of `TextToSpeechActor`.


### Interfaces


#### `SpeechResponse`

An interface representing the response from the text-to-speech conversion.

- **Properties**:
  - `mp3data`: A byte array containing the MP3 data of the converted speech.


### Usage Example

```kotlin
val ttsActor = TextToSpeechActor()
val apiClient = OpenAIClient("your_api_key_here")
val questions = listOf("How is the weather today?", "What is the time?")
val speechResponse = ttsActor.respond(questions, apiClient)

// Access the MP3 data
val mp3Data = speechResponse.mp3data
```

This example demonstrates how to create an instance of `TextToSpeechActor`, process a list of questions, and access the generated speech data. Remember to replace `"your_api_key_here"` with your actual OpenAI API key.


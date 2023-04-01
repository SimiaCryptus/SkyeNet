# SkyeNet - A Helpful Pup! 🐾⚡

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

<!-- TOC -->
* [SkyeNet - A Helpful Pup! 🐾⚡](#skyenet---a-helpful-pup-)
  * [🧪 The SkyeNet Experiment](#-the-skyenet-experiment)
  * [🏗️ The Building Blocks of SkyeNet](#-the-building-blocks-of-skyenet)
  * [🎨 Customizing Your SkyeNet Experience](#-customizing-your-skyenet-experience)
  * [🚀 Unleashing SkyeNet's Potential](#-unleashing-skyenets-potential)
  * [💡 Example Usage](#-example-usage)
    * [📦 To Import](#-to-import)
    * [🌟 To Use](#-to-use)
<!-- TOC -->

Greetings, intrepid explorer! You have stumbled upon the delightful realm of SkyeNet, a general-purpose AI assistant
that combines the linguistic prowess of OpenAI ChatGPT with the nimble GraalVM JavaScript engine. SkyeNet is a unique
proof-of-concept designed to bring an interactive, engaging AI experience to life. With the added capabilities of OpenAI
Whisper and Google Cloud Platform, SkyeNet lends its voice recognition and text-to-speech talents to users brave enough
to embark on this journey.

## 🧪 The SkyeNet Experiment

At its core, SkyeNet seeks to interpret natural language commands and execute them as JavaScript code. The assistant is
capable of understanding a wide variety of commands, making it a versatile and powerful tool for users seeking an
interactive AI companion. SkyeNet's components can be customized to meet the needs of different applications or to
create a unique and engaging user experience.

As you venture deeper into SkyeNet, you'll discover that the project's anthropomorphic component naming adds a touch of
humor to the experience. Drawing inspiration from the fabled Dr. Frankenstein, SkyeNet's various parts come together to
create a cohesive and lively AI assistant.

## 🏗️ The Building Blocks of SkyeNet

SkyeNet's components work together harmoniously, much like the body parts of a fantastical creature:

* Brain: The nucleus of SkyeNet's intellect, the brain interfaces with the OpenAI API, giving life to our endearing AI
  assistant.
* Body: Acting as the AI's vessel, the body connects the heart and brain, providing a framework for action and allowing
  SkyeNet to interact with its environment.
* Ears: These auditory organs capture sounds and transform them into actionable data, granting SkyeNet the gift of
  hearing.
* Mouth: Bestowed with the ability to speak, SkyeNet's mouth interfaces with the Google Text-to-Speech API, enabling it
  to
  communicate with users through speech.
* Head: The command center of the AI assistant, the head is responsible for capturing audio through the ears,
  interacting
  with the user via the face, and relaying commands to the body.
* Face: The face is the graphical user interface, allowing SkyeNet to communicate with users visually and provide input
  and output in a user-friendly manner.
* Heart: The heart is the interface to the GraalVM JavaScript engine that powers SkyeNet, pumping life into the body and
  allowing the AI assistant to perform its various functions.

## 🎨 Customizing Your SkyeNet Experience

SkyeNet is designed to be flexible and adaptable, with components that can be customized to better serve your needs or
to create a unique user experience. By modifying SkyeNet's various components, you can tailor the AI assistant to your
preferences, making it a more personal and engaging companion.

For example, you can customize the voice recognition and text-to-speech components by integrating different APIs or
altering the existing ones. Additionally, you can modify the user interface to create a more visually appealing
experience, or even experiment with new input and output methods to push the boundaries of the AI assistant's
capabilities.

Furthermore, SkyeNet's ability to interpret natural language commands and execute them as JavaScript code allows you to
extend its functionality and create custom commands specific to your needs. This level of customization opens up a world
of possibilities for users to explore, making SkyeNet a versatile and powerful tool that can be adapted to a wide
variety of situations and tasks.

## 🚀 Unleashing SkyeNet's Potential

SkyeNet's unique combination of natural language understanding, voice recognition, text-to-speech, and JavaScript
execution capabilities opens up endless possibilities for users to harness the power of AI in creative and innovative
ways. From automating mundane tasks to developing complex applications, SkyeNet's versatility makes it an invaluable
assistant for both novice and experienced developers alike.

Here are just a few ideas for how you can use SkyeNet to revolutionize your projects:

* Home automation: Use SkyeNet to create a voice-controlled home automation system that can interpret your commands and
  control your smart devices, making your home more comfortable and efficient.
* Virtual Assistant: Develop a customized virtual assistant that can help you manage your daily tasks, answer your
  questions, and provide you with relevant information on demand.
* Gaming: Integrate SkyeNet into your favorite games to create voice-activated in-game commands or to provide an
  interactive companion that enhances your gaming experience.
* Education: Use SkyeNet as an engaging teaching tool that can help students learn new concepts, practice
  problem-solving skills, and receive immediate feedback on their progress.
* Customer support: Implement SkyeNet as an AI-powered customer support agent that can understand and respond to user
  inquiries, helping to streamline your support process and improve customer satisfaction.

The possibilities are virtually endless, limited only by your imagination and ingenuity. As you embark on your SkyeNet
adventure, you'll find that the AI assistant is not only a versatile and powerful tool but also a delightful and
engaging companion that can help you push the boundaries of what's possible.

So, brave adventurer, are you ready to unleash the power of SkyeNet and embark on an extraordinary journey into the
world of AI? With its anthropomorphic components, humorous naming, and lively personality, SkyeNet is sure to capture
your heart and inspire your creativity. Don your lab coat, channel your inner Dr. Frankenstein, and prepare to awaken
the adorably powerful AI assistant that is SkyeNet!

## 💡 Example Usage

### 📦 To Import

https://mvnrepository.com/artifact/com.simiacryptus/joe-penai

Maven:

```xml

<dependency>
    <groupId>com.simiacryptus</groupId>
    <artifactId>skyenet</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation group: 'com.simiacryptus', name: 'skyenet', version: '1.0.0'
```

```kotlin
implementation("com.simiacryptus:skyenet:1.0.0")
```

### 🌟 To Use

```kotlin
// Define OpenAI client
val brain = ChatProxy(
    Brain::class.java,
    File(openAIKey).readText().trim()
).create()
val body = Body(
    brain,
    mapOf(
        // Define tools that can be used in commands
        "toolObj" to TestTools(googleSpeechKey)
    )
)
// Launch the user interface
val head = Head(brain, body = body)
val jFrame = head.start()
// Wait for the window to close
while (jFrame.isVisible) {
    Thread.sleep(100)
}

class TestTools(keyfile: String) {
    // Private details will not be exported
    private val mouth = Mouth(keyfile)

    // Export methods to be called from the script
    @Export
    fun speak(text: String) = mouth.speak(text)
}
```

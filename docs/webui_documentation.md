# Skyenet Project Documentation

## Overview

Skyenet is a comprehensive framework designed to facilitate the development, integration, and deployment of AI-driven
applications. It leverages OpenAI's API to provide a suite of tools and services that enable developers to create
sophisticated AI models, interpreters for various programming languages, and utilities for web scraping, audio
processing, and more. The project aims to streamline the process of building intelligent applications by offering a
modular, extensible architecture.

## Key Components

### Core Actors

Skyenet's architecture is built around the concept of "actors" - modular components that interact with OpenAI's API to
perform specific tasks. These actors can generate responses based on input, execute code, process images, and handle
natural language understanding tasks.

- **BaseActor**: Serves as the abstract base for all actors, defining common properties and methods.
- **CodingActor**: Specializes in translating natural language instructions into executable code.
- **ImageActor**: Focuses on generating images based on textual prompts.
- **TextToSpeechActor**: Converts text to speech, facilitating auditory interfaces.

### Actor System

The `ActorSystem` class manages the lifecycle and interactions of various actors within the system. It supports
session-based context, user-specific data storage, and dynamic behavior modification through interceptors.

### Platform Integration

Skyenet provides integration with cloud platforms and utilities for enhanced functionality:

- **AWS Platform**: Facilitates interactions with AWS services, including S3 for storage and KMS for encryption.
- **OutputInterceptor**: Captures and redirects standard output and error streams for logging or testing purposes.
- **Selenium**: Offers an abstraction for web scraping and automation tasks using Selenium WebDriver.

### Utility Classes

A collection of utility classes and interfaces support various functionalities across the project:

- **ClasspathRelationships**: Analyzes and manages relationships between classes within JAR files.
- **FunctionWrapper**: Allows for interception and recording of function calls.
- **LoggingInterceptor**: Captures logging events from specified loggers.
- **RuleTreeBuilder**: Generates Kotlin code expressions for matching and filtering strings.

## Development Guide

### Setting Up the Development Environment

1. **Clone the Repository**: Start by cloning the Skyenet project repository to your local machine.
2. **Install Dependencies**: Ensure you have Kotlin and Java SDKs installed. Additionally, install any required
   dependencies, such as the OpenAI SDK and Selenium WebDriver.
3. **Configure API Keys**: For components that interact with external services (e.g., OpenAI, AWS), configure the
   necessary API keys and access credentials.

### Extending Skyenet

Skyenet's modular architecture makes it easy to extend and customize:

- **Adding New Actors**: Implement the `BaseActor` interface to create new actors for specific tasks.
- **Custom Interpreters**: Extend the `Interpreter` interface to support additional programming languages or execution
  environments.
- **Utility Enhancements**: Contribute new utility classes or enhance existing ones to support broader functionalities.

### Testing

Skyenet includes a comprehensive suite of tests to ensure the reliability and correctness of its components:

- **Unit Tests**: Validate the functionality of individual classes and methods.
- **Integration Tests**: Test the interactions between different components and external services.
- **Performance Tests**: Assess the efficiency and scalability of the framework under various conditions.

## Deployment

Skyenet applications can be deployed on various platforms, including cloud services and on-premise servers. The project
documentation provides guidelines for deployment, including containerization with Docker and orchestration with
Kubernetes for scalable, distributed applications.

## Contributing

Contributions to Skyenet are welcome! Whether it's adding new features, fixing bugs, or improving documentation, your
contributions help make Skyenet better for everyone. Please refer to the project's contribution guidelines for more
information on how to contribute effectively.

## Conclusion

Skyenet aims to be a versatile and powerful framework for building AI-driven applications. By providing a rich set of
tools and services, it enables developers to harness the power of AI more efficiently and effectively. Whether you're
building web applications, processing multimedia, or developing custom AI models, Skyenet offers the building blocks you
need to bring your projects to life.

<!-- TOC -->

* [Skyenet Project Documentation](#skyenet-project-documentation)
    * [Overview](#overview)
    * [Key Components](#key-components)
        * [Core Actors](#core-actors)
        * [Actor System](#actor-system)
        * [Platform Integration](#platform-integration)
        * [Utility Classes](#utility-classes)
    * [Development Guide](#development-guide)
        * [Setting Up the Development Environment](#setting-up-the-development-environment)
        * [Extending Skyenet](#extending-skyenet)
        * [Testing](#testing)
    * [Deployment](#deployment)
    * [Contributing](#contributing)
    * [Conclusion](#conclusion)
* [kotlin\com\simiacryptus\skyenet\apps\coding\ShellToolAgent.kt](#kotlincomsimiacryptusskyenetappscodingshelltoolagentkt)
  * [ShellToolAgent Class Documentation](#shelltoolagent-class-documentation)
  * [Key Features:](#key-features)
  * [Core Components:](#core-components)
  * [Usage:](#usage)
  * [Key Methods:](#key-methods)
  * [Extensibility:](#extensibility)
  * [Error Handling:](#error-handling)
  * [Conclusion](#conclusion-1)
* [kotlin\com\simiacryptus\skyenet\apps\coding\CodingAgent.kt](#kotlincomsimiacryptusskyenetappscodingcodingagentkt)
    * [CodingAgent Class Documentation](#codingagent-class-documentation)
        * [Overview](#overview-1)
        * [Key Components](#key-components-1)
        * [Constructor Parameters](#constructor-parameters)
        * [Methods](#methods)
            * [start(userMessage: String)](#startusermessage-string)
            * [displayCode(task: SessionTask, codeRequest: CodingActor.CodeRequest)](#displaycodetask-sessiontask-coderequest-codingactorcoderequest)
            * [displayFeedback(task: SessionTask, request: CodingActor.CodeRequest, response: CodeResult)](#displayfeedbacktask-sessiontask-request-codingactorcoderequest-response-coderesult)
            * [execute(task: SessionTask, response: CodeResult, request: CodingActor.CodeRequest)](#executetask-sessiontask-response-coderesult-request-codingactorcoderequest)
        * [Usage Example](#usage-example)
        * [Conclusion](#conclusion-2)
* [kotlin\com\github\simiacryptus\aicoder\util\SimpleDiffUtil.kt](#kotlincomgithubsimiacryptusaicoderutilsimplediffutilkt)
    * [SimpleDiffUtil and SocketManagerBase Extensions Documentation](#simplediffutil-and-socketmanagerbase-extensions-documentation)
        * [Overview](#overview-2)
        * [SimpleDiffUtil](#simplediffutil)
            * [Functionality](#functionality)
            * [Methods](#methods-1)
            * [Internal Mechanics](#internal-mechanics)
        * [SocketManagerBase Extension Functions](#socketmanagerbase-extension-functions)
            * [Functionality](#functionality-1)
            * [Methods](#methods-2)
            * [Usage Scenarios](#usage-scenarios)
        * [Conclusion](#conclusion-3)
* [kotlin\com\simiacryptus\skyenet\apps\general\WebDevApp.kt](#kotlincomsimiacryptusskyenetappsgeneralwebdevappkt)
    * [Web Development Assistant Application Documentation](#web-development-assistant-application-documentation)
        * [Overview](#overview-3)
        * [Key Components](#key-components-2)
            * [WebDevApp Class](#webdevapp-class)
                * [Key Methods](#key-methods-1)
            * [WebDevAgent Class](#webdevagent-class)
                * [Key Methods](#key-methods-2)
            * [Actors](#actors)
        * [Usage](#usage-1)
        * [Example](#example)
        * [Conclusion](#conclusion-4)
* [kotlin\com\simiacryptus\skyenet\apps\coding\ToolAgent.kt](#kotlincomsimiacryptusskyenetappscodingtoolagentkt)
    * [ToolAgent Class Documentation](#toolagent-class-documentation)
        * [Overview](#overview-4)
        * [Key Components](#key-components-3)
            * [Constructor Parameters](#constructor-parameters-1)
            * [Methods](#methods-3)
                * [`displayFeedback`](#displayfeedback)
                * [`createToolButton`](#createtoolbutton)
                * [`openAPIParsedActor`](#openapiparsedactor)
                * [`servletActor`](#servletactor)
                * [`schemaActor`](#schemaactor)
                * [`displayCodeFeedback`](#displaycodefeedback)
                * [`buildTestPage`](#buildtestpage)
                * [`getInterpreterString`](#getinterpreterstring)
            * [Utility Methods](#utility-methods)
        * [Usage](#usage-2)
        * [Example](#example-1)
* [kotlin\com\simiacryptus\skyenet\AgentPatterns.kt](#kotlincomsimiacryptusskyenetagentpatternskt)
    * [AgentPatterns Module Documentation](#agentpatterns-module-documentation)
        * [Overview](#overview-5)
            * [1. retryable Function](#1-retryable-function)
                * [Parameters:](#parameters)
                * [Returns:](#returns)
                * [Usage Example:](#usage-example-1)
            * [2. iterate Function](#2-iterate-function)
                * [Parameters:](#parameters-1)
                * [Returns:](#returns-1)
                * [Usage Example:](#usage-example-2)
            * [3. iterate Function (Overloaded Version)](#3-iterate-function-overloaded-version)
                * [Parameters:](#parameters-2)
                * [Returns:](#returns-2)
                * [Usage Example:](#usage-example-3)
        * [Conclusion](#conclusion-5)
* [kotlin\com\simiacryptus\skyenet\interpreter\ProcessInterpreter.kt](#kotlincomsimiacryptusskyenetinterpreterprocessinterpreterkt)
    * [ProcessInterpreter Class Documentation](#processinterpreter-class-documentation)
        * [Constructor](#constructor)
            * [ProcessInterpreter](#processinterpreter)
        * [Properties](#properties)
            * [command](#command)
        * [Methods](#methods-4)
            * [getLanguage](#getlanguage)
            * [getSymbols](#getsymbols)
            * [validate](#validate)
            * [run](#run)
        * [Usage Example](#usage-example-4)
        * [Notes](#notes)
* [kotlin\com\simiacryptus\skyenet\webui\application\ApplicationInterface.kt](#kotlincomsimiacryptusskyenetwebuiapplicationapplicationinterfacekt)
    * [ApplicationInterface Documentation](#applicationinterface-documentation)
        * [Constructor](#constructor-1)
            * [ApplicationInterface(SocketManagerBase socketManager)](#applicationinterfacesocketmanagerbase-socketmanager)
        * [Methods](#methods-5)
            * [hrefLink](#hreflink)
            * [textInput](#textinput)
            * [newTask](#newtask)
        * [Companion Object Methods](#companion-object-methods)
            * [oneAtATime](#oneatatime)
        * [Usage Example](#usage-example-5)
* [kotlin\com\simiacryptus\skyenet\webui\application\ApplicationDirectory.kt](#kotlincomsimiacryptusskyenetwebuiapplicationapplicationdirectorykt)
    * [Developer Documentation for `ApplicationDirectory` Class](#developer-documentation-for-applicationdirectory-class)
        * [Overview](#overview-6)
        * [Key Components](#key-components-4)
            * [Properties](#properties-1)
            * [Inner Classes](#inner-classes)
            * [Methods](#methods-6)
                * [Abstract and Open Methods](#abstract-and-open-methods)
                * [Protected Methods](#protected-methods)
            * [Companion Object](#companion-object)
        * [Usage](#usage-3)
        * [Conclusion](#conclusion-6)
* [kotlin\com\simiacryptus\skyenet\webui\application\ApplicationServer.kt](#kotlincomsimiacryptusskyenetwebuiapplicationapplicationserverkt)
    * [Developer Documentation for ApplicationServer](#developer-documentation-for-applicationserver)
        * [Overview](#overview-7)
        * [Key Components](#key-components-5)
            * [Fields and Properties](#fields-and-properties)
            * [Servlets and Filters](#servlets-and-filters)
            * [Session Management](#session-management)
            * [Settings Management](#settings-management)
            * [Utility Methods](#utility-methods-1)
        * [Usage](#usage-4)
            * [Example](#example-2)
        * [Conclusion](#conclusion-7)
* [kotlin\com\simiacryptus\skyenet\webui\application\ApplicationSocketManager.kt](#kotlincomsimiacryptusskyenetwebuiapplicationapplicationsocketmanagerkt)
    * [ApplicationSocketManager Class Documentation](#applicationsocketmanager-class-documentation)
        * [Overview](#overview-8)
        * [Constructor](#constructor-2)
        * [Key Methods and Properties](#key-methods-and-properties)
            * [onRun](#onrun)
            * [userMessage](#usermessage)
            * [applicationInterface](#applicationinterface)
        * [Companion Object](#companion-object-1)
            * [spinner](#spinner)
        * [Usage](#usage-5)
        * [Conclusion](#conclusion-8)
* [kotlin\com\simiacryptus\skyenet\webui\chat\ChatServer.kt](#kotlincomsimiacryptusskyenetwebuichatchatserverkt)
    * [ChatServer Class Documentation](#chatserver-class-documentation)
        * [Overview](#overview-9)
        * [Key Components](#key-components-6)
            * [Properties](#properties-2)
            * [Inner Classes](#inner-classes-1)
                * [WebSocketHandler](#websockethandler)
                    * [Key Methods](#key-methods-3)
            * [Abstract Methods](#abstract-methods)
            * [Open Properties](#open-properties)
            * [Methods](#methods-7)
        * [Usage](#usage-6)
        * [Companion Object](#companion-object-2)
            * [Properties](#properties-3)
            * [Extension Functions](#extension-functions)
        * [Conclusion](#conclusion-9)
* [kotlin\com\simiacryptus\skyenet\webui\chat\ChatSocket.kt](#kotlincomsimiacryptusskyenetwebuichatchatsocketkt)
    * [ChatSocket Class Documentation](#chatsocket-class-documentation)
        * [Dependencies](#dependencies)
        * [Class Overview](#class-overview)
        * [Usage](#usage-7)
            * [Example](#example-3)
        * [Conclusion](#conclusion-10)
* [kotlin\com\simiacryptus\skyenet\webui\chat\ChatSocketManager.kt](#kotlincomsimiacryptusskyenetwebuichatchatsocketmanagerkt)
    * [ChatSocketManager Class Documentation](#chatsocketmanager-class-documentation)
        * [Constructor](#constructor-3)
            * [Parameters](#parameters-3)
        * [Methods](#methods-8)
            * [onRun](#onrun-1)
                * [Parameters](#parameters-4)
            * [renderResponse](#renderresponse)
                * [Parameters](#parameters-5)
                * [Returns](#returns-3)
            * [onResponse](#onresponse)
                * [Parameters](#parameters-6)
        * [Properties](#properties-4)
        * [Companion Object](#companion-object-3)
        * [Usage](#usage-8)
        * [Error Handling](#error-handling-1)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\ApiKeyServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletapikeyservletkt)
    * [API Key Servlet Documentation](#api-key-servlet-documentation)
        * [Overview](#overview-10)
        * [Data Model](#data-model)
            * [ApiKeyRecord](#apikeyrecord)
        * [Supported Operations](#supported-operations)
            * [Handling GET Requests](#handling-get-requests)
            * [Handling POST Requests](#handling-post-requests)
        * [Interacting with the Servlet](#interacting-with-the-servlet)
            * [Editing an API Key Record](#editing-an-api-key-record)
            * [Deleting an API Key Record](#deleting-an-api-key-record)
            * [Creating a New API Key Record](#creating-a-new-api-key-record)
            * [Inviting a User to Use an API Key](#inviting-a-user-to-use-an-api-key)
        * [Utility Methods](#utility-methods-2)
        * [Storage](#storage)
        * [Conclusion](#conclusion-11)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\AppInfoServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletappinfoservletkt)
    * [AppInfoServlet Class Documentation](#appinfoservlet-class-documentation)
        * [Generics](#generics)
        * [Constructor](#constructor-4)
        * [Methods](#methods-9)
            * [`doGet(HttpServletRequest req, HttpServletResponse resp)`](#dogethttpservletrequest-req-httpservletresponse-resp)
        * [Usage](#usage-9)
        * [Dependencies](#dependencies-1)
        * [Conclusion](#conclusion-12)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\CancelThreadsServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletcancelthreadsservletkt)
    * [CancelThreadsServlet Documentation](#cancelthreadsservlet-documentation)
        * [Class Overview](#class-overview-1)
        * [Constructor](#constructor-5)
            * [`CancelThreadsServlet(ApplicationServer server)`](#cancelthreadsservletapplicationserver-server)
        * [Methods](#methods-10)
            * [`doGet(HttpServletRequest req, HttpServletResponse resp)`](#dogethttpservletrequest-req-httpservletresponse-resp-1)
            * [`doPost(HttpServletRequest req, HttpServletResponse resp)`](#doposthttpservletrequest-req-httpservletresponse-resp)
        * [Usage](#usage-10)
        * [Security Considerations](#security-considerations)
        * [Error Handling](#error-handling-2)
        * [Conclusion](#conclusion-13)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\CorsFilter.kt](#kotlincomsimiacryptusskyenetwebuiservletcorsfilterkt)
    * [CorsFilter Class Documentation](#corsfilter-class-documentation)
        * [Features](#features)
        * [Usage](#usage-11)
            * [Configuration](#configuration)
            * [Methods](#methods-11)
            * [CORS Headers Added](#cors-headers-added)
        * [Example](#example-4)
        * [Logging](#logging)
        * [Conclusion](#conclusion-14)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\FileServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletfileservletkt)
    * [FileServlet Class Documentation](#fileservlet-class-documentation)
        * [Constructor](#constructor-6)
        * [Methods](#methods-12)
            * [Public Methods](#public-methods)
            * [Private Methods](#private-methods)
        * [Utility Methods](#utility-methods-3)
        * [Companion Object](#companion-object-4)
        * [Cache Configuration](#cache-configuration)
        * [Exception Handling](#exception-handling)
        * [Usage](#usage-12)
        * [Example](#example-5)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\DeleteSessionServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletdeletesessionservletkt)
    * [DeleteSessionServlet Documentation](#deletesessionservlet-documentation)
        * [Overview](#overview-11)
        * [Usage](#usage-13)
            * [Initialization](#initialization)
            * [Handling GET Requests](#handling-get-requests-1)
            * [Handling POST Requests](#handling-post-requests-1)
        * [Security Considerations](#security-considerations-1)
        * [Dependencies](#dependencies-2)
        * [Conclusion](#conclusion-15)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\OAuthBase.kt](#kotlincomsimiacryptusskyenetwebuiservletoauthbasekt)
    * [OAuthBase Class Documentation](#oauthbase-class-documentation)
        * [Overview](#overview-12)
        * [Usage](#usage-14)
            * [Parameters](#parameters-7)
            * [Methods](#methods-13)
                * [`configure`](#configure)
                    * [Parameters:](#parameters-8)
                    * [Returns:](#returns-4)
        * [Example](#example-6)
        * [Conclusion](#conclusion-16)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\LogoutServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletlogoutservletkt)
    * [LogoutServlet Documentation](#logoutservlet-documentation)
        * [Overview](#overview-13)
        * [Usage](#usage-15)
            * [Key Methods](#key-methods-4)
            * [Logout Process](#logout-process)
            * [Error Handling](#error-handling-3)
        * [Example Deployment Descriptor Configuration](#example-deployment-descriptor-configuration)
        * [Conclusion](#conclusion-17)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\OAuthGoogle.kt](#kotlincomsimiacryptusskyenetwebuiservletoauthgooglekt)
    * [OAuthGoogle Class Documentation](#oauthgoogle-class-documentation)
        * [Overview](#overview-14)
        * [Key Components](#key-components-7)
        * [Usage](#usage-16)
        * [Configuration Parameters](#configuration-parameters)
        * [Example](#example-7)
        * [Dependencies](#dependencies-3)
        * [Important Notes](#important-notes)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\NewSessionServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletnewsessionservletkt)
    * [NewSessionServlet Documentation](#newsessionservlet-documentation)
        * [Usage](#usage-17)
        * [Implementation Details](#implementation-details)
            * [Import Statements](#import-statements)
            * [Class Definition](#class-definition)
            * [doGet Method](#doget-method)
                * [Parameters](#parameters-9)
                * [Implementation](#implementation)
        * [Example Usage](#example-usage)
        * [Conclusion](#conclusion-18)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\SessionIdFilter.kt](#kotlincomsimiacryptusskyenetwebuiservletsessionidfilterkt)
    * [SessionIdFilter Class Documentation](#sessionidfilter-class-documentation)
        * [Package](#package)
        * [Imports](#imports)
        * [Constructor](#constructor-7)
        * [Methods](#methods-14)
            * [init](#init)
            * [doFilter](#dofilter)
            * [destroy](#destroy)
        * [Usage Example](#usage-example-6)
        * [Conclusion](#conclusion-19)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\ProxyHttpServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletproxyhttpservletkt)
    * [ProxyHttpServlet Developer Documentation](#proxyhttpservlet-developer-documentation)
        * [Overview](#overview-15)
        * [Setup](#setup)
        * [Key Components](#key-components-8)
            * [Fields](#fields)
            * [Methods](#methods-15)
                * [`service(HttpServletRequest req, HttpServletResponse resp)`](#servicehttpservletrequest-req-httpservletresponse-resp)
                * [`getProxyRequest(HttpServletRequest req)`](#getproxyrequesthttpservletrequest-req)
                * [`onResponse(...)`](#onresponse-1)
                * [`onRequest(HttpServletRequest req, ByteArray bytes)`](#onrequesthttpservletrequest-req-bytearray-bytes)
            * [Usage Example](#usage-example-7)
        * [Extending `ProxyHttpServlet`](#extending-proxyhttpservlet)
        * [Conclusion](#conclusion-20)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\SessionSettingsServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletsessionsettingsservletkt)
    * [SessionSettingsServlet Documentation](#sessionsettingsservlet-documentation)
        * [Overview](#overview-16)
        * [Dependencies](#dependencies-4)
        * [Constructor](#constructor-8)
        * [Fields](#fields-1)
        * [HTTP Methods](#http-methods)
            * [doGet(HttpServletRequest req, HttpServletResponse resp)](#dogethttpservletrequest-req-httpservletresponse-resp-2)
                * [Process Flow](#process-flow)
            * [doPost(HttpServletRequest req, HttpServletResponse resp)](#doposthttpservletrequest-req-httpservletresponse-resp-1)
                * [Process Flow](#process-flow-1)
        * [Usage Example](#usage-example-8)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\SessionListServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletsessionlistservletkt)
    * [SessionListServlet Class Documentation](#sessionlistservlet-class-documentation)
        * [Dependencies](#dependencies-5)
        * [Constructor Parameters](#constructor-parameters-2)
        * [Key Methods](#key-methods-5)
            * [doGet(HttpServletRequest req, HttpServletResponse resp)](#dogethttpservletrequest-req-httpservletresponse-resp-3)
                * [Parameters](#parameters-10)
                * [Functionality](#functionality-2)
        * [Usage Example](#usage-example-9)
        * [Notes](#notes-1)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\SessionShareServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletsessionshareservletkt)
    * [SessionShareServlet Documentation](#sessionshareservlet-documentation)
        * [Overview](#overview-17)
        * [Key Components](#key-components-9)
            * [Dependencies](#dependencies-6)
            * [Main Methods](#main-methods)
                * [doGet(HttpServletRequest req, HttpServletResponse resp)](#dogethttpservletrequest-req-httpservletresponse-resp-4)
            * [Helper Methods](#helper-methods)
            * [Usage](#usage-18)
            * [Error Handling](#error-handling-4)
            * [Security Considerations](#security-considerations-2)
        * [Conclusion](#conclusion-21)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\SessionThreadsServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletsessionthreadsservletkt)
    * [SessionThreadsServlet Documentation](#sessionthreadsservlet-documentation)
        * [Overview](#overview-18)
        * [Usage](#usage-19)
            * [Request Parameters](#request-parameters)
            * [Response](#response)
            * [Example Request](#example-request)
        * [Implementation Details](#implementation-details-1)
            * [Key Components](#key-components-10)
            * [HTML Response Generation](#html-response-generation)
            * [Error Handling](#error-handling-5)
        * [Security Considerations](#security-considerations-3)
        * [Conclusion](#conclusion-22)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\ToolServlet.kt](#kotlincomsimiacryptusskyenetwebuiservlettoolservletkt)
    * [ToolServlet Developer Documentation](#toolservlet-developer-documentation)
        * [Overview](#overview-19)
        * [Key Components](#key-components-11)
            * [Tool Data Class](#tool-data-class)
            * [Main Methods](#main-methods-1)
                * [doGet(HttpServletRequest?, HttpServletResponse?)](#dogethttpservletrequest-httpservletresponse)
                * [doPost(HttpServletRequest?, HttpServletResponse?)](#doposthttpservletrequest-httpservletresponse)
                * [service(HttpServletRequest?, HttpServletResponse?)](#servicehttpservletrequest-httpservletresponse)
            * [Utility Methods](#utility-methods-4)
                * [indexPage(): String](#indexpage-string)
                * [toolDetailsPage(tool: Tool): String](#tooldetailspagetool-tool-string)
                * [serveEditPage(HttpServletRequest, HttpServletResponse, Tool)](#serveeditpagehttpservletrequest-httpservletresponse-tool)
        * [Security](#security)
        * [Extensibility](#extensibility-1)
        * [Example Usage](#example-usage-1)
        * [Conclusion](#conclusion-23)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\UserSettingsServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletusersettingsservletkt)
    * [UserSettingsServlet Documentation](#usersettingsservlet-documentation)
        * [Overview](#overview-20)
        * [Functionality](#functionality-3)
            * [doGet(HttpServletRequest req, HttpServletResponse resp)](#dogethttpservletrequest-req-httpservletresponse-resp-5)
            * [doPost(HttpServletRequest req, HttpServletResponse resp)](#doposthttpservletrequest-req-httpservletresponse-resp-2)
        * [Security Considerations](#security-considerations-4)
        * [Usage](#usage-20)
        * [Conclusion](#conclusion-24)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\WelcomeServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletwelcomeservletkt)
    * [WelcomeServlet Class Documentation](#welcomeservlet-class-documentation)
        * [Constructor](#constructor-9)
        * [Methods](#methods-16)
            * [doGet(HttpServletRequest req, HttpServletResponse resp)](#dogethttpservletrequest-req-httpservletresponse-resp-6)
            * [doPost(HttpServletRequest req, HttpServletResponse resp)](#doposthttpservletrequest-req-httpservletresponse-resp-3)
            * [homepage(User user)](#homepageuser-user)
            * [appRow(ApplicationDirectory.ChildWebApp app, User user)](#approwapplicationdirectorychildwebapp-app-user-user)
        * [Fields](#fields-2)
        * [Usage](#usage-21)
        * [Security](#security-1)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\UserInfoServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletuserinfoservletkt)
    * [UserInfoServlet Documentation](#userinfoservlet-documentation)
        * [Package](#package-1)
        * [Dependencies](#dependencies-7)
        * [Class Overview](#class-overview-2)
            * [UserInfoServlet](#userinfoservlet)
                * [Methods](#methods-17)
        * [Usage](#usage-22)
        * [Example Response](#example-response)
        * [Security Considerations](#security-considerations-5)
        * [Conclusion](#conclusion-25)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\UsageServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletusageservletkt)
    * [UsageServlet Documentation](#usageservlet-documentation)
        * [Overview](#overview-21)
        * [Key Methods](#key-methods-6)
            * [doGet(HttpServletRequest req, HttpServletResponse resp)](#dogethttpservletrequest-req-httpservletresponse-resp-7)
            * [serve(HttpServletResponse resp, Map<OpenAIModel, ApiModel.Usage> usage)](#servehttpservletresponse-resp-mapopenaimodel-apimodelusage-usage)
        * [Usage Example](#usage-example-10)
        * [Security Considerations](#security-considerations-6)
        * [Dependencies](#dependencies-8)
* [kotlin\com\simiacryptus\skyenet\webui\servlet\ZipServlet.kt](#kotlincomsimiacryptusskyenetwebuiservletzipservletkt)
    * [ZipServlet Class Documentation](#zipservlet-class-documentation)
        * [Dependencies](#dependencies-9)
        * [Constructor](#constructor-10)
        * [Public Methods](#public-methods-1)
            * [`doGet(HttpServletRequest req, HttpServletResponse resp)`](#dogethttpservletrequest-req-httpservletresponse-resp-8)
        * [Private Methods](#private-methods-1)
            * [`write(basePath: File, file: File, zip: ZipOutputStream)`](#writebasepath-file-file-file-zip-zipoutputstream)
        * [Usage Example](#usage-example-11)
        * [Security Considerations](#security-considerations-7)
* [kotlin\com\simiacryptus\skyenet\webui\session\SocketManager.kt](#kotlincomsimiacryptusskyenetwebuisessionsocketmanagerkt)
    * [SocketManager Interface Documentation](#socketmanager-interface-documentation)
        * [Interface Overview](#interface-overview)
        * [Methods](#methods-18)
            * [`removeSocket(socket: ChatSocket)`](#removesocketsocket-chatsocket)
            * [`addSocket(socket: ChatSocket, session: Session)`](#addsocketsocket-chatsocket-session-session)
            * [`getReplay(): List<String>`](#getreplay-liststring)
            * [`onWebSocketText(socket: ChatSocket, message: String)`](#onwebsockettextsocket-chatsocket-message-string)
        * [Usage Example](#usage-example-12)
        * [Conclusion](#conclusion-26)
* [kotlin\com\simiacryptus\skyenet\webui\session\SocketManagerBase.kt](#kotlincomsimiacryptusskyenetwebuisessionsocketmanagerbasekt)
    * [SocketManagerBase Class Documentation](#socketmanagerbase-class-documentation)
        * [Overview](#overview-22)
        * [Key Components](#key-components-12)
            * [Fields](#fields-3)
            * [Constructor](#constructor-11)
            * [Methods](#methods-19)
                * [Public](#public)
                * [Protected](#protected)
                * [Private](#private)
            * [Inner Classes](#inner-classes-2)
        * [Utility Functions](#utility-functions)
        * [Usage](#usage-23)
        * [Example](#example-8)
* [kotlin\com\simiacryptus\skyenet\webui\test\ImageActorTestApp.kt](#kotlincomsimiacryptusskyenetwebuitestimageactortestappkt)
    * [ImageActorTestApp Developer Documentation](#imageactortestapp-developer-documentation)
        * [Overview](#overview-23)
        * [Key Components](#key-components-13)
            * [Constructor](#constructor-12)
            * [Settings Data Class](#settings-data-class)
            * [Overridden Methods](#overridden-methods)
                * [initSettings](#initsettings)
                * [userMessage](#usermessage-1)
            * [Companion Object](#companion-object-5)
        * [Usage](#usage-24)
        * [Error Handling](#error-handling-6)
        * [Conclusion](#conclusion-27)
* [kotlin\com\simiacryptus\skyenet\webui\test\CodingActorTestApp.kt](#kotlincomsimiacryptusskyenetwebuitestcodingactortestappkt)
    * [CodingActorTestApp Class Documentation](#codingactortestapp-class-documentation)
        * [Constructor](#constructor-13)
            * [Parameters:](#parameters-11)
        * [Methods](#methods-20)
            * [userMessage](#usermessage-2)
                * [Parameters:](#parameters-12)
        * [Usage](#usage-25)
        * [Companion Object](#companion-object-6)
        * [Example](#example-9)
        * [Note](#note)
* [kotlin\com\simiacryptus\skyenet\webui\test\ParsedActorTestApp.kt](#kotlincomsimiacryptusskyenetwebuitestparsedactortestappkt)
    * [ParsedActorTestApp Class Documentation](#parsedactortestapp-class-documentation)
        * [Overview](#overview-24)
        * [Constructor](#constructor-14)
            * [Parameters](#parameters-13)
        * [Methods](#methods-21)
            * [userMessage](#usermessage-3)
                * [Parameters](#parameters-14)
        * [Companion Object](#companion-object-7)
            * [Properties](#properties-5)
        * [Usage Example](#usage-example-13)
        * [Conclusion](#conclusion-28)
* [kotlin\com\simiacryptus\skyenet\webui\test\SimpleActorTestApp.kt](#kotlincomsimiacryptusskyenetwebuitestsimpleactortestappkt)
    * [SimpleActorTestApp Documentation](#simpleactortestapp-documentation)
        * [Overview](#overview-25)
        * [Key Components](#key-components-14)
            * [Constructor](#constructor-15)
            * [Settings Data Class](#settings-data-class-1)
            * [User Message Handling](#user-message-handling)
            * [Logging](#logging-1)
        * [Usage](#usage-26)
        * [Conclusion](#conclusion-29)
* [kotlin\com\simiacryptus\skyenet\webui\util\MarkdownUtil.kt](#kotlincomsimiacryptusskyenetwebuiutilmarkdownutilkt)
    * [MarkdownUtil Documentation](#markdownutil-documentation)
        * [Functions](#functions)
            * [renderMarkdown](#rendermarkdown)
                * [Parameters:](#parameters-15)
                * [Returns:](#returns-5)
                * [Usage Example:](#usage-example-14)
            * [defaultOptions](#defaultoptions)
                * [Returns:](#returns-6)
        * [Implementation Details](#implementation-details-2)
        * [Notes](#notes-2)
* [kotlin\com\simiacryptus\skyenet\webui\session\SessionTask.kt](#kotlincomsimiacryptusskyenetwebuisessionsessiontaskkt)
    * [SessionTask Class Documentation](#sessiontask-class-documentation)
        * [Overview](#overview-26)
        * [Properties](#properties-6)
        * [Methods](#methods-22)
            * [Abstract Methods](#abstract-methods-1)
            * [Public Methods](#public-methods-2)
            * [Companion Object](#companion-object-8)
        * [Usage](#usage-27)
            * [Example](#example-10)
* [kotlin\com\simiacryptus\skyenet\webui\util\EncryptFiles.kt](#kotlincomsimiacryptusskyenetwebuiutilencryptfileskt)
    * [EncryptFiles Utility Documentation](#encryptfiles-utility-documentation)
        * [Overview](#overview-27)
        * [Usage](#usage-28)
            * [Prerequisites](#prerequisites)
            * [Encrypting and Writing Data](#encrypting-and-writing-data)
            * [Extension Functions](#extension-functions-1)
        * [Example](#example-11)
        * [Conclusion](#conclusion-30)
* [kotlin\com\simiacryptus\skyenet\webui\util\OpenAPI.kt](#kotlincomsimiacryptusskyenetwebuiutilopenapikt)
    * [Skyenet WebUI Util - OpenAPI Data Classes Documentation](#skyenet-webui-util---openapi-data-classes-documentation)
        * [Overview](#overview-28)
            * [OpenAPI](#openapi)
            * [Info](#info)
            * [Contact](#contact)
            * [License](#license)
            * [PathItem](#pathitem)
            * [Operation](#operation)
            * [Response](#response-1)
            * [Components](#components)
            * [Schema](#schema)
            * [Parameter](#parameter)
            * [Example, RequestBody, Header, SecurityScheme, Link, Callback, MediaType](#example-requestbody-header-securityscheme-link-callback-mediatype)
* [kotlin\com\simiacryptus\skyenet\webui\util\TensorflowProjector.kt](#kotlincomsimiacryptusskyenetwebuiutiltensorflowprojectorkt)
    * [TensorflowProjector Class Documentation](#tensorflowprojector-class-documentation)
        * [Constructor](#constructor-16)
        * [Methods](#methods-23)
            * [`toVectorMap(vararg words: String): Map<String, DoubleArray>`](#tovectormapvararg-words-string-mapstring-doublearray)
            * [`writeTensorflowEmbeddingProjectorHtml(vararg words: String): String`](#writetensorflowembeddingprojectorhtmlvararg-words-string-string)
        * [Usage Example](#usage-example-15)
        * [Notes](#notes-3)
* [kotlin\com\simiacryptus\skyenet\webui\util\Selenium2S3.kt](#kotlincomsimiacryptusskyenetwebuiutilselenium2s3kt)
    * [Developer Documentation for Selenium2S3 Class](#developer-documentation-for-selenium2s3-class)
        * [Overview](#overview-29)
        * [Key Features](#key-features-1)
        * [Initialization](#initialization-1)
        * [Core Methods](#core-methods)
            * [save](#save)
            * [process](#process)
            * [getHtml, getJson, getMedia](#gethtml-getjson-getmedia)
            * [saveJS, saveHTML](#savejs-savehtml)
        * [Utility Methods](#utility-methods-5)
        * [Closing Resources](#closing-resources)
        * [Companion Object](#companion-object-9)
            * [chromeDriver](#chromedriver)
            * [setCookies](#setcookies)
        * [Usage Example](#usage-example-16)
* [resources\application\chat.js](#resourcesapplicationchatjs)
    * [WebSocket Communication Module](#websocket-communication-module)
        * [Functions](#functions-1)
            * [getSessionId()](#getsessionid)
            * [send(message)](#sendmessage)
            * [connect(sessionId, customReceiveFunction)](#connectsessionid-customreceivefunction)
            * [showDisconnectedOverlay(show)](#showdisconnectedoverlayshow)
        * [Usage Example](#usage-example-17)
* [resources\application\index.html](#resourcesapplicationindexhtml)
    * [WebSocket Client Web Application Documentation](#websocket-client-web-application-documentation)
        * [Overview](#overview-30)
        * [Dependencies](#dependencies-10)
            * [External Libraries and Stylesheets](#external-libraries-and-stylesheets)
        * [HTML Structure](#html-structure)
            * [Head Section](#head-section)
            * [Body Section](#body-section)
                * [Toolbar](#toolbar)
                * [Namebar](#namebar)
                * [Session](#session)
                * [Modal](#modal)
                * [Footer](#footer)
        * [Functionality](#functionality-4)
        * [Scripts](#scripts)
            * [Prism.js](#prismjs)
            * [Mermaid.js](#mermaidjs)
            * [Application Scripts](#application-scripts)
        * [Conclusion](#conclusion-31)
* [resources\application\main.js](#resourcesapplicationmainjs)
    * [Developer Documentation](#developer-documentation)
        * [Overview](#overview-31)
        * [Functions](#functions-2)
            * [showModal(endpoint, useSession = true)](#showmodalendpoint-usesession--true)
            * [closeModal()](#closemodal)
            * [async fetchData(endpoint, useSession = true)](#async-fetchdataendpoint-usesession--true)
            * [onWebSocketText(event)](#onwebsockettextevent)
            * [updateTabs()](#updatetabs)
            * [toggleVerbose()](#toggleverbose)
            * [refreshReplyForms()](#refreshreplyforms)
            * [refreshVerbose()](#refreshverbose)
        * [Event Listeners](#event-listeners)
            * [Theme Change](#theme-change)
            * [Modal Triggers](#modal-triggers)
            * [Form Submission](#form-submission)
            * [Input Field Auto-Resize](#input-field-auto-resize)
            * [Fetch User Information](#fetch-user-information)
            * [Privacy and Terms Links](#privacy-and-terms-links)
        * [Conclusion](#conclusion-32)
* [resources\shared\schemes\_alien_spaceship.scss](#resourcessharedschemes_alien_spaceshipscss)
    * [Alien Spaceship Theme - Developer Documentation](#alien-spaceship-theme---developer-documentation)
        * [Base Colors](#base-colors)
        * [Derived Colors](#derived-colors)
        * [Usage](#usage-29)
            * [Buttons](#buttons)
            * [Messages](#messages)
            * [Modals](#modals)
        * [Conclusion](#conclusion-33)
* [resources\shared\schemes\_forest.scss](#resourcessharedschemes_forestscss)
    * [Forest Canopy Theme - Developer Documentation](#forest-canopy-theme---developer-documentation)
        * [Importing the Theme](#importing-the-theme)
        * [Typography Variables](#typography-variables)
        * [Base Colors](#base-colors-1)
        * [Derived Colors](#derived-colors-1)
        * [Buttons](#buttons-1)
        * [Forms](#forms)
        * [Messages and Modals](#messages-and-modals)
        * [Customization](#customization)
        * [Conclusion](#conclusion-34)
* [resources\shared\schemes\_night.scss](#resourcessharedschemes_nightscss)
    * [Nighttime Theme Color Scheme Documentation](#nighttime-theme-color-scheme-documentation)
        * [Base Colors](#base-colors-2)
        * [Derived Colors](#derived-colors-2)
        * [Component Styling](#component-styling)
            * [Buttons](#buttons-2)
            * [Forms](#forms-1)
            * [Messages and Modals](#messages-and-modals-1)
        * [Utility Variables](#utility-variables)
        * [Usage](#usage-30)
* [resources\shared\schemes\_normal.scss](#resourcessharedschemes_normalscss)
    * [Developer Documentation: UI Theme Variables](#developer-documentation-ui-theme-variables)
        * [Typography Variables](#typography-variables-1)
        * [Base Colors](#base-colors-3)
        * [Derived Colors](#derived-colors-3)
        * [Buttons](#buttons-3)
        * [Forms](#forms-2)
        * [Messages and Modals](#messages-and-modals-2)
* [resources\shared\schemes\_pony.scss](#resourcessharedschemes_ponyscss)
    * [Developer Documentation: Theme Customization](#developer-documentation-theme-customization)
        * [Typography Variables](#typography-variables-2)
        * [Base Colors](#base-colors-4)
        * [Derived Colors](#derived-colors-4)
        * [Buttons](#buttons-4)
        * [Forms](#forms-3)
        * [Messages and Modals](#messages-and-modals-3)
        * [Usage](#usage-31)
* [resources\application\favicon.svg](#resourcesapplicationfaviconsvg)
  * [Developer Documentation: SVG Graphic Implementation](#developer-documentation-svg-graphic-implementation)
  * [Overview](#overview-32)
  * [SVG Graphic Description](#svg-graphic-description)
  * [File Information](#file-information)
  * [Implementation Guide](#implementation-guide)
  * [Conclusion](#conclusion-35)
* [resources\shared\_main.scss](#resourcesshared_mainscss)
    * [Developer Documentation: SCSS Mixins and Styles](#developer-documentation-scss-mixins-and-styles)
        * [Mixins](#mixins)
            * [1. `typography`](#1-typography)
            * [2. `flex-container`](#2-flex-container)
            * [3. `fixed-full`](#3-fixed-full)
            * [4. `link-hover-transition`](#4-link-hover-transition)
            * [5. `message-style`](#5-message-style)
        * [Key Styling Sections](#key-styling-sections)
            * [Body](#body)
            * [Messages Container](#messages-container)
            * [Input Fields](#input-fields)
            * [Disconnected Overlay](#disconnected-overlay)
            * [Buttons](#buttons-5)
        * [Keyframes](#keyframes)
* [resources\welcome\index.html](#resourceswelcomeindexhtml)
    * [Developer Documentation: HTML Page Auto-Redirect](#developer-documentation-html-page-auto-redirect)
        * [Overview](#overview-33)
        * [Implementation](#implementation-1)
            * [HTML Structure](#html-structure-1)
                * [Meta Refresh Tag](#meta-refresh-tag)
            * [Fallback Link](#fallback-link)
        * [Usage](#usage-32)
        * [Considerations](#considerations)
        * [Conclusion](#conclusion-36)
* [resources\welcome\main.js](#resourceswelcomemainjs)
    * [Developer Documentation](#developer-documentation-1)
        * [Overview](#overview-34)
        * [Functions](#functions-3)
            * [showModal(endpoint)](#showmodalendpoint)
            * [closeModal()](#closemodal-1)
            * [async fetchData(endpoint)](#async-fetchdataendpoint)
            * [updateTabs()](#updatetabs-1)
            * [Event Listeners Setup](#event-listeners-setup)
        * [Usage](#usage-33)
        * [Conclusion](#conclusion-37)
* [resources\welcome\main.scss](#resourceswelcomemainscss)
    * [Developer Documentation: App Type Styles](#developer-documentation-app-type-styles)
        * [Import Statements](#import-statements-1)
        * [`.app-type` Class Definition](#app-type-class-definition)
        * [Usage](#usage-34)
        * [Example](#example-12)
* [resources\welcome\favicon.svg](#resourceswelcomefaviconsvg)
    * [Developer Documentation for SVG Illustration](#developer-documentation-for-svg-illustration)
        * [Overview](#overview-35)
        * [Styles](#styles)
        * [Paths](#paths)
        * [Understanding Path Coordinates](#understanding-path-coordinates)
        * [Modifying the SVG](#modifying-the-svg)
        * [Reusability](#reusability)
        * [Conclusion](#conclusion-38)
* [resources\welcome\favicon.png](#resourceswelcomefaviconpng)
    * [Developer Documentation](#developer-documentation-2)
        * [Overview](#overview-36)
        * [System Requirements](#system-requirements)
        * [Setup Instructions](#setup-instructions)
        * [Key Functionalities](#key-functionalities)
        * [Contributing](#contributing-1)
        * [Support](#support)

<!-- TOC -->

# kotlin\com\simiacryptus\skyenet\apps\coding\ShellToolAgent.kt

#### ShellToolAgent Class Documentation

The `ShellToolAgent` class is an abstract class designed to facilitate the creation and execution of shell tools within
a web application. It extends the `CodingAgent` class, integrating with an interpreter to execute code, manage session
tasks, and interact with the user interface. This class is part of a larger framework aimed at enabling dynamic code
execution and tool generation within a web environment.

##### Key Features:

- **Dynamic Code Execution**: Executes shell commands and scripts, allowing for real-time interaction and feedback.
- **Tool Generation**: Dynamically generates tools based on code execution results, including OpenAPI documentation and
  test pages.
- **User Interaction**: Provides a web-based interface for users to input commands, view execution results, and interact
  with generated tools.

##### Core Components:

- **Interpreter Integration**: Utilizes a Kotlin interpreter to execute shell commands and scripts.
- **Session Management**: Manages user sessions, storing execution history and generated tools.
- **UI Interaction**: Generates HTML forms and buttons for user interaction, including execution feedback and tool
  interaction.

##### Usage:

The `ShellToolAgent` class is abstract and is intended to be extended by specific implementations that define the
interpreter and tool generation logic. Here's a simplified usage example:

```kotlin
class MyShellToolAgent : ShellToolAgent<KotlinInterpreter>(/* constructor parameters */) {
  override fun getInterpreterString(): String {
    // Return a string representation of the interpreter used
  }

  // Implement other abstract methods and any additional functionality
}
```

##### Key Methods:

- **displayFeedback**: Generates and displays feedback for code execution, including interactive buttons for further
  actions.
- **execute**: Executes the provided code within the context of the current session and user.
- **createToolButton**: Generates a button for exporting execution results as a tool, including OpenAPI documentation
  and test pages.

##### Extensibility:

The class is designed to be extensible, allowing developers to customize the execution environment, tool generation
logic, and user interface components. By overriding methods and providing custom implementations, developers can tailor
the functionality to meet specific requirements.

##### Error Handling:

The class includes error handling mechanisms to manage exceptions during code execution and tool generation. It provides
feedback to the user and logs errors for debugging purposes.

#### Conclusion

The `ShellToolAgent` class is a powerful component for creating dynamic web-based tools that execute shell commands and
scripts. Its integration with an interpreter, session management capabilities, and interactive user interface make it a
versatile foundation for developing custom tooling solutions within web applications.

# kotlin\com\simiacryptus\skyenet\apps\coding\CodingAgent.kt

## CodingAgent Class Documentation

The `CodingAgent` class is a part of the `com.simiacryptus.skyenet.apps.code` package, designed to facilitate code
generation and execution within a specific application context. It leverages the OpenAI API for generating code snippets
based on user input and provides mechanisms for executing and providing feedback on the generated code. This class is
built to be integrated within a larger system that supports user interactions, code storage, and execution environments.

### Overview

`CodingAgent` extends the `ActorSystem` class, specializing in handling coding-related tasks. It interacts with users
through a defined `ApplicationInterface`, receives code generation requests, processes them using OpenAI's GPT models,
and manages the execution of generated code. Additionally, it provides functionalities for user feedback and code
regeneration.

### Key Components

- **API Integration**: Utilizes the OpenAI API for generating code snippets.
- **User Interaction**: Interfaces with users through tasks and messages, allowing for code requests and feedback.
- **Code Execution**: Supports executing generated code and handling execution results or errors.
- **Feedback Mechanism**: Allows users to provide feedback on generated code, facilitating iterative improvement.

### Constructor Parameters

- `api`: Instance of `API` for interacting with OpenAI services.
- `dataStorage`: Storage interface for persisting data.
- `session`: Current user session information.
- `user`: The user object, may be null.
- `ui`: Interface for application-specific UI interactions.
- `interpreter`: The class of the interpreter to be used for code execution.
- `symbols`: A map of predefined symbols available for code generation.
- `temperature`: Controls the randomness of the code generation. Lower values make the output more deterministic.
- `details`: Optional details for further customizing the code generation process.
- `model`: Specifies the OpenAI model to be used for code generation.
- `actorMap`: A map of actor types to their corresponding `CodingActor` instances.

### Methods

#### start(userMessage: String)

Initiates the code generation process based on the user's message. It creates a new task, processes the user's request,
and displays the generated code or any encountered errors.

#### displayCode(task: SessionTask, codeRequest: CodingActor.CodeRequest)

Displays the generated code to the user. If the user's message indicates that code is provided (enclosed in triple
backticks), it directly processes this code; otherwise, it requests code generation.

#### displayFeedback(task: SessionTask, request: CodingActor.CodeRequest, response: CodeResult)

Displays options for the user to interact with the generated code, including buttons for executing, regenerating code,
or providing feedback.

#### execute(task: SessionTask, response: CodeResult, request: CodingActor.CodeRequest)

Executes the generated code and displays the execution result or any errors.

### Usage Example

```kotlin
val codingAgent = CodingAgent(
  api = openAIClient,
  dataStorage = storageInterface,
  session = currentSession,
  user = currentUser,
  ui = applicationInterface,
  interpreter = MyInterpreter::class,
  symbols = mapOf("exampleSymbol" to Any()),
  temperature = 0.1,
  details = "Example details",
  model = ChatModels.davinci
)

codingAgent.start("Generate a hello world program in Python")
```

This example creates an instance of `CodingAgent` configured with necessary dependencies and initiates a code generation
request with a simple user message.

### Conclusion

The `CodingAgent` class provides a comprehensive solution for integrating AI-powered code generation and execution into
applications. It abstracts the complexities of interacting with the OpenAI API, managing user interactions, and
executing code, making it easier to build intelligent coding assistants and automation tools.

# kotlin\com\github\simiacryptus\aicoder\util\SimpleDiffUtil.kt

## SimpleDiffUtil and SocketManagerBase Extensions Documentation

### Overview

The provided code consists of two main components: `SimpleDiffUtil` and extension functions for `SocketManagerBase` to
add functionality for applying diffs to code. `SimpleDiffUtil` is a utility object that provides functionality to apply
patch strings to source code strings, simulating a simple version of a diff patch application. The `SocketManagerBase`
extension functions enhance a web UI session by allowing diffs to be applied directly from the session interface.

### SimpleDiffUtil

#### Functionality

- **Patch Application**: Applies a patch string to a source string, returning the modified source string.

#### Methods

- `fun patch(source: String, patch: String): String`
    - Applies a patch to the given source string.
    - **Parameters**:
        - `source`: The original source code as a string.
        - `patch`: The patch string, following a simplified diff format.
    - **Returns**: The modified source code as a string after applying the patch.

#### Internal Mechanics

- **Deletion Handling**: Identifies lines to be deleted from the source based on the patch and removes them.
- **Addition Handling**: Identifies lines to be added to the source based on the patch and inserts them.
- **Context Line Handling**: Ensures that non-modified lines (context lines) are correctly maintained in the source.
- **Line Matching**: Uses a Levenshtein Distance algorithm to match lines with a certain tolerance, allowing for minor
  discrepancies.

### SocketManagerBase Extension Functions

#### Functionality

- **Add Apply Diff Links**: Enhances a web UI session by adding links to apply diffs to code directly from the session
  interface.

#### Methods

1. `fun SocketManagerBase.addApplyDiffLinks(code: String, response: String, fullPatch: MutableList<String> = mutableListOf(), handle: (String) -> Unit): String`
    - Adds links to apply diffs to a single piece of code.
    - **Parameters**:
        - `code`: The original code as a string.
        - `response`: The session response content, where diff blocks are identified and links are added.
        - `fullPatch`: A mutable list of strings to keep track of applied patches.
        - `handle`: A callback function that handles the updated code.
    - **Returns**: The modified session response content with diff application links.

2. `fun SocketManagerBase.addApplyDiffLinks(code: Map<String, String>, response: String, handle: (Map<String, String>) -> Unit): String`
    - Adds links to apply diffs to multiple pieces of code, each identified by a filename.
    - **Parameters**:
        - `code`: A map of filenames to their corresponding code as strings.
        - `response`: The session response content, where diff blocks are identified and links are added.
        - `handle`: A callback function that handles the updated code map.
    - **Returns**: The modified session response content with diff application links.

#### Usage Scenarios

- **Code Review Sessions**: Allows reviewers to suggest changes directly in the session interface, which can then be
  applied to the code.
- **Collaborative Editing**: Enables real-time collaborative editing and versioning of code within a web UI session.

### Conclusion

The `SimpleDiffUtil` and the `SocketManagerBase` extension functions provide a foundational framework for applying diffs
to code within a web UI session. This setup facilitates easier code reviews and collaborative editing by allowing direct
application of changes from the session interface.

# kotlin\com\simiacryptus\skyenet\apps\general\WebDevApp.kt

## Web Development Assistant Application Documentation

### Overview

The Web Development Assistant Application is designed to facilitate the development of web applications by automating
the generation of code and architecture planning through interaction with AI models. It leverages the OpenAI API to
generate HTML, CSS, and JavaScript code based on user inputs, and provides tools for code review and architecture
discussion.

### Key Components

#### WebDevApp Class

The `WebDevApp` class extends `ApplicationServer` and serves as the main entry point for the application. It initializes
the application with a name and a set of symbols, and defines the behavior for handling user messages.

##### Key Methods

- `userMessage(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API)`: Processes
  messages from users and initiates the generation of web development artifacts based on the user's input.

- `initSettings(session: Session)`: Initializes default settings for a session.

#### WebDevAgent Class

The `WebDevAgent` class extends `ActorSystem` and is responsible for managing the interaction with different actors to
generate and review code. It defines various actor types for handling specific tasks such as HTML coding, JavaScript
coding, CSS coding, architecture discussion, and code review.

##### Key Methods

- `start(userMessage: String)`: Initiates the process of generating web development artifacts based on the user's
  message. It orchestrates the interaction with different actors to produce the desired output.

- `draftResourceCode(...)`: Generates code for a specific resource (HTML, CSS, JavaScript) based on the user's input and
  the architecture plan.

#### Actors

Actors are specialized components that handle specific tasks in the code generation and review process.
The `WebDevAgent` class defines several actors for different purposes:

- `HtmlCodingActor`
- `JavascriptCodingActor`
- `CssCodingActor`
- `ArchitectureDiscussionActor`
- `CodeReviewer`

Each actor type is associated with a `SimpleActor` or `ParsedActor` instance, which defines the prompt and model to be
used for generating or reviewing code.

### Usage

To use the Web Development Assistant Application, instantiate the `WebDevApp` class and configure it with the necessary
settings. Then, interact with the application through user messages, which will be processed by the `WebDevApp`
and `WebDevAgent` classes to generate and review web development artifacts.

### Example

```kotlin
val webDevApp = WebDevApp(
  applicationName = "My Web Dev Assistant",
  symbols = mapOf("symbol1" to "value1", "symbol2" to "value2"),
  temperature = 0.1
)

webDevApp.start()
```

This example creates an instance of the `WebDevApp` with a custom name and symbols, and starts the application. Users
can then interact with the application to generate and review web development artifacts.

### Conclusion

The Web Development Assistant Application provides a powerful tool for automating the generation and review of web
development code. By leveraging AI models, it simplifies the process of web application development and helps developers
to quickly prototype and refine their applications.

# kotlin\com\simiacryptus\skyenet\apps\coding\ToolAgent.kt

## ToolAgent Class Documentation

The `ToolAgent` class is an abstract class designed to facilitate the creation and manipulation of tools within a web
application. It extends the `CodingAgent` class, incorporating additional functionalities specific to handling code
generation, servlet creation, and OpenAPI documentation generation based on user interactions within a session.

### Overview

`ToolAgent` is tailored for applications that require dynamic code generation and evaluation, particularly in the
context of web development and API documentation. It leverages the capabilities of the `CodingActor` class to process
and generate code snippets, servlet implementations, and OpenAPI specifications.

### Key Components

#### Constructor Parameters

- `api`: An instance of `API` used for interacting with OpenAI services.
- `dataStorage`: Interface for data storage operations.
- `session`: Represents the current user session.
- `user`: Optional user information.
- `ui`: Interface for application UI interactions.
- `interpreter`: The Kotlin class of the interpreter to be used.
- `symbols`: A map of predefined symbols for code generation.
- `temperature`: Controls the randomness of the code generation.
- `details`: Optional details for further customization.
- `model`: Specifies the OpenAI model to be used for code generation.
- `actorMap`: A map of `ActorTypes` to `CodingActor` instances for different code generation tasks.

#### Methods

##### `displayFeedback`

Displays feedback to the user based on the code generation task's result. It includes play, regenerate, and tool
creation buttons for user interaction.

##### `createToolButton`

Generates a button in the UI that, when clicked, initiates the process of exporting the generated code as a servlet and
creating OpenAPI documentation.

##### `openAPIParsedActor`

Returns a `ParsedActor` instance configured for parsing OpenAPI specifications.

##### `servletActor`

Returns a `CodingActor` instance configured for generating servlet implementations.

##### `schemaActor`

Returns a `CodingActor` instance configured for generating data schema definitions.

##### `displayCodeFeedback`

Displays the generated code and provides options for acceptance, regeneration, or revision based on user feedback.

##### `buildTestPage`

Generates a test page for the created servlet based on the OpenAPI specification.

##### `getInterpreterString`

An abstract method that subclasses must implement to return the interpreter string representation.

#### Utility Methods

- `execWrap`: A companion object method that ensures the execution context is correctly set up for code generation and
  evaluation tasks.

### Usage

While `ToolAgent` is abstract and cannot be instantiated directly, it serves as a base for creating specific tool agents
within an application. Subclasses must implement the `getInterpreterString` method and can override other methods as
needed to customize behavior.

Subclasses of `ToolAgent` are responsible for handling specific tool-related tasks, such as generating code snippets,
creating servlets, and generating OpenAPI documentation based on user interactions. They utilize the
provided `CodingActor` instances for code generation and leverage the application UI for displaying feedback and options
to the user.

### Example

To create a specific tool agent, extend the `ToolAgent` class and implement the required methods. For instance:

```kotlin
class MyToolAgent(
  api: API,
  dataStorage: StorageInterface,
  session: Session,
  user: User?,
  ui: ApplicationInterface,
  interpreter: KClass<MyInterpreter>,
  symbols: Map<String, Any>,
  temperature: Double = 0.1,
  details: String? = null,
  model: ChatModels
) : ToolAgent<MyInterpreter>(api, dataStorage, session, user, ui, interpreter, symbols, temperature, details, model) {
  override fun getInterpreterString(): String {
    return "MyInterpreter"
  }
}
```

This example demonstrates how to create a specific tool agent by extending `ToolAgent` and providing the necessary
constructor parameters and method implementations.

# kotlin\com\simiacryptus\skyenet\AgentPatterns.kt

## AgentPatterns Module Documentation

The `AgentPatterns` module in the `com.simiacryptus.skyenet` package provides utility functions designed to facilitate
interactive and iterative processes within a web UI context. These functions are particularly useful for applications
that require user feedback loops, such as chatbots or interactive design tools. This documentation outlines the key
functionalities provided by the module and how to utilize them in your projects.

### Overview

The module contains three primary functions:

1. `retryable`: Allows the execution of a process with the option to retry, updating the UI with each attempt.
2. `iterate`: Facilitates an iterative feedback loop between the user and the application, allowing for the refinement
   of responses based on user input.
3. `iterate` (overloaded version): An extension of the `iterate` function, tailored for use with `BaseActor` instances,
   enabling more complex interactions such as those involving AI models.

#### 1. retryable Function

The `retryable` function is designed to execute a given process and provide the user with the option to retry the
operation. It is useful in scenarios where an operation may not succeed on the first attempt or where the user may wish
to try different inputs.

##### Parameters:

- `ui`: An instance of `ApplicationInterface`, which provides methods for interacting with the web UI.
- `task`: An optional `SessionTask` instance representing the current task. If not provided, a new task is created.
- `process`: A lambda function representing the process to be executed and retried as needed. It should return
  a `String` that will be displayed in the UI.

##### Returns:

- A `String` representing the final result of the process after any retries.

##### Usage Example:

```kotlin
val result = AgentPatterns.retryable(ui) {
  // Your process logic here, returning a String to display in the UI
}
```

#### 2. iterate Function

The `iterate` function enables an iterative feedback loop, allowing the application to refine its responses based on
user input. It is particularly useful for applications that require user feedback to improve the accuracy or relevance
of the response.

##### Parameters:

- `ui`: An instance of `ApplicationInterface`.
- `userMessage`: The initial user message or query.
- `heading`: An optional heading for the feedback loop, displayed in the UI. Defaults to the rendered markdown
  of `userMessage`.
- `initialResponse`: A lambda function that takes the user message and returns an initial response of type `T`.
- `reviseResponse`: A lambda function that takes the user message, the current response, and the user's feedback,
  returning a revised response of type `T`.
- `outputFn`: An optional lambda function that takes a `SessionTask` and the response of type `T`, and outputs it to the
  UI. By default, it renders the response as markdown.

##### Returns:

- The final response of type `T` after the iterative feedback loop.

##### Usage Example:

```kotlin
val finalResponse = AgentPatterns.iterate<String>(
  ui = ui,
  userMessage = "Initial query",
  initialResponse = { query -> "Initial response" },
  reviseResponse = { query, currentResponse, userFeedback -> "Revised response based on $userFeedback" }
)
```

#### 3. iterate Function (Overloaded Version)

This version of the `iterate` function is specifically designed for use with `BaseActor` instances, facilitating more
complex interactions such as those involving AI models.

##### Parameters:

- Inherits all parameters from the base `iterate` function.
- `actor`: An instance of `BaseActor<I, T>`, where `I` is the input type and `T` is the response type.
- `toInput`: A lambda function that converts a `String` (typically the user message) into the input type `I` expected by
  the `actor`.
- `api`: An instance of `API`, used by the `actor` to interact with external services or models.

##### Returns:

- The final response of type `T` after the iterative feedback loop.

##### Usage Example:

```kotlin
val finalResponse = AgentPatterns.iterate(
  input = "Initial query",
  actor = myActorInstance,
  toInput = { query -> /* Convert query to actor's input type */ },
  api = myApiInstance,
  ui = ui
)
```

### Conclusion

The `AgentPatterns` module provides powerful utilities for creating interactive and iterative user experiences in web
applications. By leveraging these functions, developers can implement complex feedback loops, retry mechanisms, and
AI-driven interactions with ease.

# kotlin\com\simiacryptus\skyenet\interpreter\ProcessInterpreter.kt

## ProcessInterpreter Class Documentation

The `ProcessInterpreter` class is part of the `com.simiacryptus.skyenet.interpreter` package and extends the
functionality of the `Interpreter` interface. This class is designed to execute shell commands or scripts in various
programming languages by wrapping the code and running it in a separate process. It provides a flexible way to execute
code dynamically, supporting different languages and custom execution environments.

### Constructor

#### ProcessInterpreter

```kotlin
ProcessInterpreter(defs: Map< String, Any > = mapOf ())
```

Initializes a new instance of the `ProcessInterpreter` class.

- **Parameters:**
    - `defs`: A map containing configuration options for the interpreter, such as the command to run, the working
      directory, environment variables, and the language. Defaults to an empty map.

### Properties

#### command

```kotlin
val command: List<String>
```

A read-only property that returns the command to be executed as a list of strings. The command is determined based on
the `defs` map passed to the constructor. It supports both a single string (which will be split into parts) and a list
of strings. If no command is specified, it defaults to `bash`.

### Methods

#### getLanguage

```kotlin
 override fun getLanguage(): String
```

Returns the programming language of the code to be interpreted. The language is specified in the `defs` map. If not
specified, it defaults to `bash`.

#### getSymbols

```kotlin
override fun getSymbols(): Map<String, Any>
```

Returns the symbols (configuration options) provided to the interpreter. This includes all the definitions passed
through the `defs` map.

#### validate

```kotlin
override fun validate(code: String): Throwable?
```

Validates the given code. This implementation always considers the code valid and returns `null`.

- **Parameters:**
    - `code`: The code to validate.

- **Returns:** Always `null`, indicating the code is valid.

#### run

```kotlin
override fun run(code: String): Any?
```

Executes the given code by wrapping it and running it in a separate process. It supports custom working directories,
environment variables, and execution timeouts.

- **Parameters:**
    - `code`: The code to execute.

- **Returns:** The output of the executed code. In case of an error, it returns a formatted string containing both the
  error and the output. If the process times out, it throws a `RuntimeException`.

### Usage Example

```kotlin
val interpreter = ProcessInterpreter(
  mapOf(
    "command" to "python",
    "workingDir" to "/path/to/working/dir",
    "env" to mapOf("KEY" to "value"),
    "language" to "python"
  )
)

val code = """
print("Hello, World!")
""".trimIndent()

try {
  val result = interpreter.run(code)
  println(result)
} catch (e: Exception) {
  e.printStackTrace()
}
```

This example creates an instance of `ProcessInterpreter` configured to run Python code, sets a working directory,
environment variables, and specifies the language. It then executes a simple Python script and prints the result.

### Notes

- The `run` method writes the code to the process's output stream and reads the result from the input stream. It handles
  process timeouts and captures both standard output and error output.
- The class supports customization through the `defs` map, allowing for flexible execution environments and commands.

# kotlin\com\simiacryptus\skyenet\webui\application\ApplicationInterface.kt

## ApplicationInterface Documentation

The `ApplicationInterface` class serves as a bridge between the web UI components and the underlying socket management
and session handling logic. It provides methods to create interactive web elements such as links and text input forms,
and to manage tasks representing long-running operations.

### Constructor

#### ApplicationInterface(SocketManagerBase socketManager)

Initializes a new instance of the `ApplicationInterface` class.

- **Parameters:**
    - `socketManager`: An instance of `SocketManagerBase` that handles socket connections and interactions.

### Methods

#### hrefLink

```kotlin
open fun hrefLink(
  linkText: String,
  classname: String = "href-link",
  handler: Consumer<Unit>
): String
```

Generates HTML for a hyperlink that triggers a specified handler when clicked.

- **Parameters:**
    - `linkText`: The text to display for the link.
    - `classname`: The CSS class to apply to the link. Defaults to `"href-link"`.
    - `handler`: A `Consumer<Unit>` that is triggered when the link is clicked.
- **Returns:** A `String` containing the HTML for the hyperlink.

#### textInput

```kotlin
open fun textInput(
  handler: Consumer<String>
): String
```

Generates HTML for a text input form that triggers a specified handler when the form is submitted.

- **Parameters:**
    - `handler`: A `Consumer<String>` that is triggered with the text input value when the form is submitted.
- **Returns:** A `String` containing the HTML for the text input form.

#### newTask

```kotlin
open fun newTask(): SessionTask
```

Creates a new `SessionTask` instance that can be used to display the progress of a long-running operation. Currently,
the method does not support cancelable tasks and defaults to non-cancelable tasks.

- **Returns:** A `SessionTask` instance representing the new task.

### Companion Object Methods

#### oneAtATime

```kotlin
fun <T> oneAtATime(handler: Consumer<T>): Consumer<T>
```

Wraps a given handler to ensure that it is executed one at a time. If the handler is already in execution, subsequent
calls will be ignored until the current execution completes.

- **Parameters:**
    - `handler`: The `Consumer<T>` to be wrapped.
- **Returns:** A `Consumer<T>` that ensures the handler is executed one at a time.

### Usage Example

```kotlin
val socketManager = MySocketManager()
val appInterface = ApplicationInterface(socketManager)

val linkHtml = appInterface.hrefLink("Click me", "custom-class") {
  println("Link clicked")
}

val textInputHtml = appInterface.textInput { inputText ->
  println("Form submitted with text: $inputText")
}

val task = appInterface.newTask()
// Use the task for long-running operations
```

This documentation provides an overview of the `ApplicationInterface` class, its constructor, methods, and a usage
example to help developers understand how to use it in their projects.

# kotlin\com\simiacryptus\skyenet\webui\application\ApplicationDirectory.kt

## Developer Documentation for `ApplicationDirectory` Class

The `ApplicationDirectory` class serves as a foundational component for creating web applications with embedded Jetty
servers, integrating various services including chat servers, OAuth authentication, and dynamic web content serving.
This class is designed to be extended and customized for specific web application needs.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.application`
- **Imports**: Various, including Jetty server components, servlets, and utility classes for handling OAuth and API
  keys.
- **Abstract Class**: Yes

### Key Components

#### Properties

- `localName`: The hostname used when the server is not in "server mode" (default: `"localhost"`).
- `publicName`: The hostname used in "server mode" (default: `"localhost"`).
- `port`: The port on which the server will listen (default: `8081`).
- `domainName`: The resolved domain name, set during initialization.
- `childWebApps`: A list of `ChildWebApp` instances representing child web applications to be hosted.

#### Inner Classes

- `ChildWebApp`: A data class representing a child web application, including its path and an instance of `ChatServer`.

#### Methods

##### Abstract and Open Methods

- `authenticatedWebsite()`: Returns an instance of `OAuthBase` for handling OAuth authentication. Can be overridden.
- `setupPlatform()`: Configures platform-specific services, such as Selenium. Can be overridden.
- `init(isServer: Boolean)`: Initializes the application, setting up the domain name and interceptors. Can be
  overridden.
- `start(port: Int, vararg webAppContexts: WebAppContext)`: Starts the Jetty server with the specified port and web
  application contexts. Can be overridden.
- `httpConnectionFactory()`: Creates and returns an `HttpConnectionFactory` with custom configuration. Can be
  overridden.
- `newWebAppContext(path: String, server: ChatServer)`: Creates a `WebAppContext` for a given path and `ChatServer`. Can
  be overridden.
- `newWebAppContext(path: String, baseResource: Resource, resourceBase: String, indexServlet: Servlet?)`: Creates
  a `WebAppContext` with a specified base resource and optional index servlet. Can be overridden.
- `newWebAppContext(path: String, servlet: Servlet)`: Creates a `WebAppContext` for a given path and servlet. Can be
  overridden.

##### Protected Methods

- `_main(args: Array<String>)`: The main method to start the application. It sets up the platform, initializes the
  application, configures the server, and starts it.

#### Companion Object

- Contains a logger instance and a utility method `allResources(resourceName: String)` for loading resources.

### Usage

To use the `ApplicationDirectory` class, you should extend it in your application and implement the abstract properties
and methods as needed. Here's a simplified example:

```kotlin
class MyApplicationDirectory : ApplicationDirectory() {
  override val childWebApps = listOf(
    ChildWebApp("/chat", ChatServer())
  )

  override fun setupPlatform() {
    // Custom platform setup
  }

  override fun authenticatedWebsite(): OAuthBase? {
    // Return an OAuthBase instance for authentication
  }

  // Implement other abstract/open methods as needed
}
```

After extending `ApplicationDirectory`, you can instantiate your subclass and call its `_main` method with the necessary
arguments to start the application.

### Conclusion

The `ApplicationDirectory` class provides a robust framework for building web applications with embedded Jetty servers,
offering out-of-the-box support for chat functionalities, OAuth authentication, and more. By extending and customizing
this class, developers can rapidly develop and deploy web applications tailored to their specific requirements.

# kotlin\com\simiacryptus\skyenet\webui\application\ApplicationServer.kt

## Developer Documentation for ApplicationServer

The `ApplicationServer` class is an abstract base class designed to facilitate the creation and management of web
applications within the Skyenet framework. It extends the `ChatServer` class, inheriting its chat functionalities, and
provides a structured way to set up web applications with authentication, authorization, session management, and static
file serving.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.application`
- **Dependencies**: Requires various Skyenet core platform services and utilities, as well as external libraries such as
  Jetty for web serving and SLF4J for logging.

### Key Components

#### Fields and Properties

- `applicationName`: The name of the application.
- `path`: The base URL path for the application.
- `resourceBase`: The base directory for serving static resources.
- `root`: The root directory for application data storage.
- `description`: A brief description of the application.
- `singleInput`, `stickyInput`: Flags to control input behavior.
- `appInfo`: A lazy-initialized map containing application information.
- `dataStorage`: A storage interface instance for data persistence.

#### Servlets and Filters

The class lazily initializes several `ServletHolder` instances for handling different aspects of the application:

- `appInfoServlet`: Serves application information.
- `userInfo`: Manages user information.
- `usageServlet`: Tracks application usage.
- `fileZip`, `fileIndex`: Serve and manage files.
- `sessionSettingsServlet`, `sessionShareServlet`, `sessionThreadsServlet`, `deleteSessionServlet`, `cancelSessionServlet`:
  Manage sessions and their settings.

A filter is added to the web application context to enforce read access control based on user authentication and
authorization.

#### Session Management

- `newSession(user: User?, session: Session)`: Creates a new `SocketManager` instance for managing WebSocket connections
  for a session.
- `userMessage(...)`: An abstract method intended to be overridden to handle user messages.

#### Settings Management

- `settingsClass`: Specifies the class type for application settings.
- `initSettings(session: Session)`: Initializes settings for a session.
- `getSettings(...)`: Retrieves or initializes settings for a session.

#### Utility Methods

- `sessionsServlet(path: String)`: Creates a servlet for listing sessions.
- `configure(webAppContext: WebAppContext)`: Configures the web application context, adding filters and servlets.
- `getMimeType(filename: String)`: Utility method to determine the MIME type based on file extension.
- `HttpServletRequest.getCookie(name: String)`: Extension function to retrieve a cookie value by name.

### Usage

To create a web application using the `ApplicationServer` class, you must extend it and implement the abstract methods,
such as `userMessage(...)`. You may also override open properties and methods to customize the application's behavior.

#### Example

```kotlin
class MyApplicationServer : ApplicationServer("MyApp", "/myapp") {
  override fun userMessage(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API) {
    // Handle user messages here
  }
}
```

In this example, `MyApplicationServer` extends `ApplicationServer`, providing an implementation for handling user
messages. Additional customization and functionality can be added by overriding other open methods and properties.

### Conclusion

The `ApplicationServer` class provides a robust foundation for building web applications within the Skyenet framework,
offering out-of-the-box support for essential features like authentication, authorization, session management, and file
serving. By extending this class and implementing the required abstract methods, developers can efficiently create
feature-rich web applications.

# kotlin\com\simiacryptus\skyenet\webui\application\ApplicationSocketManager.kt

## ApplicationSocketManager Class Documentation

The `ApplicationSocketManager` class is an abstract class designed to manage WebSocket connections for a specific
application within a web-based platform. It extends the `SocketManagerBase` class, inheriting its basic WebSocket
management functionalities and adding application-specific behaviors.

### Overview

This class is part of the `com.simiacryptus.skyenet.webui.application` package and integrates various components of the
platform, including user sessions, data storage, and external API interactions. It is designed to handle messages
received from users through WebSocket connections and to facilitate interaction with the application's backend services.

### Constructor

The constructor of the `ApplicationSocketManager` class requires the following parameters:

- `session: Session`: The current user session.
- `owner: User?`: The owner of the session, which can be null.
- `dataStorage: StorageInterface?`: The interface for data storage operations, which can be null.
- `applicationClass: Class<*>`: The class of the application for which this socket manager is being instantiated.

### Key Methods and Properties

#### onRun

```kotlin
override fun onRun(userMessage: String, socket: ChatSocket)
```

This method is called when a message is received from a user through the WebSocket. It processes the user message and
interacts with the application's backend services as necessary.

- `userMessage: String`: The message received from the user.
- `socket: ChatSocket`: The WebSocket connection through which the message was received.

#### userMessage

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

- `session: Session`: The current user session.
- `user: User?`: The user who sent the message, which can be null.
- `userMessage: String`: The message received from the user.
- `socketManager: ApplicationSocketManager`: The instance of the socket manager handling the message.
- `api: API`: The API client for interacting with backend services.

#### applicationInterface

```kotlin
open val applicationInterface by lazy { ApplicationInterface(this) }
```

A lazily initialized property that provides an interface to the application-specific functionalities. It is intended to
be used for interactions between the WebSocket management and the application's backend services.

### Companion Object

The companion object of the `ApplicationSocketManager` class contains static properties and methods that can be accessed
without an instance of the class.

#### spinner

```kotlin
val spinner: String
```

A static property that provides HTML content for a loading spinner, which can be used in the user interface to indicate
ongoing operations.

### Usage

To use the `ApplicationSocketManager` class, one must extend it and implement the abstract `userMessage` method to
define the application-specific behavior for handling user messages. The subclass can then be instantiated with the
necessary parameters (session, owner, dataStorage, and applicationClass) and used to manage WebSocket connections for
the application.

### Conclusion

The `ApplicationSocketManager` class provides a structured way to manage WebSocket connections and facilitate
communication between the user interface and the application's backend services. By extending this class and
implementing its abstract methods, developers can integrate real-time features into their web applications efficiently.

# kotlin\com\simiacryptus\skyenet\webui\chat\ChatServer.kt

## ChatServer Class Documentation

The `ChatServer` class is an abstract class designed to facilitate the creation of a chat server using Jetty web
sockets. It provides a structured way to handle web socket connections, manage sessions, and integrate with a storage
interface for data persistence.

### Overview

The `ChatServer` class encapsulates the core functionalities required to set up a chat server, including session
management, web socket configuration, and servlet setup. It leverages Jetty's web socket and servlet capabilities to
create a robust chat server framework.

### Key Components

#### Properties

- `applicationName`: An abstract property that should be overridden to specify the name of the application.
- `dataStorage`: An open property that can be overridden to provide an implementation of `StorageInterface` for data
  storage purposes. It is nullable and defaults to `null`.
- `sessions`: A mutable map that tracks active sessions and their corresponding `SocketManager` instances.

#### Inner Classes

##### WebSocketHandler

An inner class that extends `JettyWebSocketServlet`. It is responsible for configuring the web socket factory and
creating web socket instances for incoming connections.

###### Key Methods

- `configure(factory: JettyWebSocketServletFactory)`: Configures the web socket factory with specific settings such as
  timeouts, buffer sizes, and message sizes. It also sets up the web socket creator to handle incoming connections.

#### Abstract Methods

- `newSession(user: User?, session: Session): SocketManager`: An abstract method that must be implemented to create a
  new `SocketManager` instance for a given session. It allows for custom session initialization logic.

#### Open Properties

- `baseResource`: An open property that provides access to the base resource for the web server. It can be overridden to
  customize the resource base.

#### Methods

- `configure(webAppContext: WebAppContext)`: Configures the `WebAppContext` with servlets for handling default requests,
  web socket connections, and session creation.

### Usage

To use the `ChatServer` class, one must extend it and provide implementations for the abstract properties and methods.
Here is a simplified example:

```kotlin
class MyChatServer(resourceBase: String) : ChatServer(resourceBase) {
  override val applicationName = "MyChatApp"

  override fun newSession(user: User?, session: Session): SocketManager {
    // Implement session initialization logic here
    return MySocketManager(session)
  }
}
```

Once extended, the server can be configured and started using Jetty's standard server setup procedures.

### Companion Object

#### Properties

- `log`: A logger instance for logging purposes.

#### Extension Functions

- `JettyServerUpgradeRequest.getCookie(name: String)`: An extension function for `JettyServerUpgradeRequest` to simplify
  cookie retrieval.

### Conclusion

The `ChatServer` class provides a foundational framework for building chat servers with Jetty. By extending this class
and implementing the required abstract methods, developers can create customized chat server applications tailored to
their specific needs.

# kotlin\com\simiacryptus\skyenet\webui\chat\ChatSocket.kt

## ChatSocket Class Documentation

The `ChatSocket` class is part of the `com.simiacryptus.skyenet.webui.chat` package and is designed to handle WebSocket
connections for a chat application. It extends the `WebSocketAdapter` class provided by the Eclipse Jetty WebSocket API,
enabling it to manage WebSocket events such as connection, message reception, and disconnection.

### Dependencies

- `SocketManager`: A custom class that manages WebSocket sessions and their interactions.
- `SocketManagerBase`: A base class for `SocketManager` that provides utility methods, such as retrieving a user from a
  session.
- `Session`: Part of the Eclipse Jetty WebSocket API, representing a WebSocket session.
- `WebSocketAdapter`: A convenience class from the Eclipse Jetty WebSocket API that can be extended to create WebSocket
  endpoints.

### Class Overview

- **Constructor**: The class constructor takes a `SocketManager` instance, which is used to manage the WebSocket
  sessions.

- **Properties**:
    - `user`: A read-only property that retrieves the user associated with the current WebSocket session.

- **Methods**:
    - `onWebSocketConnect(session: Session)`: Called when a WebSocket connection is established. It registers the new
      connection with the `SocketManager` and sends any messages that need to be replayed to the newly connected client.
    - `onWebSocketText(message: String)`: Invoked when a text message is received from the client. It forwards the
      message to the `SocketManager` for further processing.
    - `onWebSocketClose(statusCode: Int, reason: String?)`: Triggered when the WebSocket connection is closed. It
      removes the connection from the `SocketManager`.

### Usage

The `ChatSocket` class is intended to be used as part of a server-side application that utilizes WebSockets for
real-time communication, specifically in a chat application context. It requires an instance of `SocketManager` to
handle session management and message broadcasting.

#### Example

To use `ChatSocket` in a server application, you would typically set it up as an endpoint that clients can connect to.
Here's a simplified example of how it might be integrated into a server setup:

```kotlin
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory

fun main() {
  val server = Server(8080)
  val socketManager = SocketManager()

  val wsHandler = object : WebSocketHandler() {
    override fun configure(factory: WebSocketServletFactory) {
      factory.register(ChatSocket::class.java) { ChatSocket(socketManager) }
    }
  }

  server.handler = wsHandler
  server.start()
  server.join()
}
```

This example sets up a basic Jetty server that listens on port 8080. It configures a WebSocket handler to create
instances of `ChatSocket`, passing in a shared `SocketManager` instance to manage the chat sessions.

### Conclusion

The `ChatSocket` class facilitates the creation of WebSocket endpoints for real-time chat applications, handling
connection, message reception, and disconnection events. It leverages the `SocketManager` class to manage chat sessions
and broadcast messages to connected clients.

# kotlin\com\simiacryptus\skyenet\webui\chat\ChatSocketManager.kt

## ChatSocketManager Class Documentation

The `ChatSocketManager` class is part of the `com.simiacryptus.skyenet.webui.chat` package and is responsible for
managing chat interactions within a web UI, leveraging the OpenAI API for generating chat responses. This class
extends `SocketManagerBase`, integrating chat functionality with a session-based web application.

### Constructor

```kotlin
ChatSocketManager(
  session: Session,
  model: ChatModels,
  userInterfacePrompt: String,
  initialAssistantPrompt: String = "",
systemPrompt: String,
api: OpenAIClient,
temperature: Double = 0.3,
applicationClass: Class<out ApplicationServer>,
storage: StorageInterface?
)
```

#### Parameters

- `session`: The current user session.
- `model`: The OpenAI model to be used for generating chat responses.
- `userInterfacePrompt`: A prompt displayed to the user at the start of the chat session.
- `initialAssistantPrompt`: An optional initial message from the assistant.
- `systemPrompt`: A system-level prompt used to initialize the chat context.
- `api`: The OpenAI client used for making API requests.
- `temperature`: Controls randomness in the response generation. Lower values make responses more deterministic.
- `applicationClass`: The application server class that this socket manager is associated with.
- `storage`: An optional storage interface for persisting chat data.

### Methods

#### onRun

```kotlin
override fun onRun(userMessage: String, socket: ChatSocket)
```

Handles the reception of a user message, generates a response using the OpenAI API, and sends the response back to the
user.

##### Parameters

- `userMessage`: The message received from the user.
- `socket`: The chat socket through which the message was received.

#### renderResponse

```kotlin
open fun renderResponse(response: String): String
```

Formats the response from the OpenAI API for display in the web UI.

##### Parameters

- `response`: The raw response string from the OpenAI API.

##### Returns

- A `String` containing the formatted response.

#### onResponse

```kotlin
open fun onResponse(response: String, responseContents: String)
```

A hook method that can be overridden to perform additional actions after a response has been generated and sent.

##### Parameters

- `response`: The response generated by the OpenAI API.
- `responseContents`: The formatted response contents that were sent to the user.

### Properties

- `messages`: A lazy-initialized list of chat messages, including system and assistant prompts, used to maintain the
  chat context.

### Companion Object

Contains a logger for logging information and errors.

### Usage

To use `ChatSocketManager`, instantiate it with the required parameters, including the session, OpenAI model, and
prompts. The class handles the chat interaction flow, including sending initial prompts, receiving user messages,
generating responses using the OpenAI API, and sending those responses back to the user.

This class is designed to be extended, allowing developers to customize the response rendering and handling through
overriding the `renderResponse` and `onResponse` methods.

### Error Handling

Errors during chat, such as issues with the OpenAI API requests, are logged using the class's logger. Override
the `onResponse` method to implement custom error handling or additional logging as needed.

# kotlin\com\simiacryptus\skyenet\webui\servlet\ApiKeyServlet.kt

## API Key Servlet Documentation

The `ApiKeyServlet` class is a part of the web application that manages API key records. It allows users to create,
edit, delete, and invite others to use API keys. This documentation provides an overview of its functionality, including
the data model, supported operations, and how to interact with the servlet.

### Overview

The servlet extends `HttpServlet` and overrides the `doGet` and `doPost` methods to handle HTTP GET and POST requests,
respectively. It interacts with the application's authentication, user settings, and usage management services to
perform its operations.

### Data Model

#### ApiKeyRecord

The `ApiKeyRecord` data class represents an API key record with the following properties:

- `owner`: The email of the user who owns the API key.
- `apiKey`: The API key string.
- `mappedKey`: A mapped or alias key for the original API key.
- `budget`: A budget limit for the API key usage.
- `comment`: A description or comment about the API key.
- `welcomeMessage`: A welcome message for users invited to use the API key.

### Supported Operations

The servlet supports the following operations based on the `action` parameter in the request:

- **Edit**: Allows users to edit an existing API key record.
- **Delete**: Enables users to delete an API key record.
- **Create**: Facilitates the creation of a new API key record.
- **Invite**: Provides a mechanism to invite other users to use an API key.

#### Handling GET Requests

The `doGet` method handles HTTP GET requests and supports the following actions: `edit`, `delete`, `create`,
and `invite`. It uses the `action` and `apiKey` parameters from the request to determine the operation to perform.

#### Handling POST Requests

The `doPost` method handles HTTP POST requests for accepting invitations and updating API key records. It supports
the `acceptInvite` action and updates to existing records.

### Interacting with the Servlet

#### Editing an API Key Record

To edit an API key record, send a GET request with the `action` parameter set to `edit` and the `apiKey` parameter set
to the API key you want to edit.

#### Deleting an API Key Record

To delete an API key record, send a GET request with the `action` parameter set to `delete` and the `apiKey` parameter
set to the API key you want to delete.

#### Creating a New API Key Record

To create a new API key record, send a GET request with the `action` parameter set to `create`.

#### Inviting a User to Use an API Key

To invite a user to use an API key, send a GET request with the `action` parameter set to `invite` and the `apiKey`
parameter set to the API key for which you want to send an invitation.

### Utility Methods

The servlet includes private utility methods for generating HTML
pages (`indexPage`, `serveInviteConfirmationPage`, `serveEditPage`) and for saving API key records to a JSON
file (`saveRecords`).

### Storage

API key records are stored in a JSON file located in the `.skyenet/apiKeys` directory under the application's data
storage root. The `apiKeyRecords` companion object property lazily loads the records from this file when accessed.

### Conclusion

The `ApiKeyServlet` class provides a comprehensive solution for managing API key records within the web application. By
supporting operations such as creation, editing, deletion, and invitation, it enables users to effectively manage their
API keys and control access to their services.

# kotlin\com\simiacryptus\skyenet\webui\servlet\AppInfoServlet.kt

## AppInfoServlet Class Documentation

The `AppInfoServlet` class is a custom servlet that extends `HttpServlet` and is designed to serve JSON data about an
application's specific information. This class is part of the `com.simiacryptus.skyenet.webui.servlet` package and
utilizes the `JsonUtil` class for JSON serialization.

### Generics

- `T`: The type parameter `T` represents the type of information the servlet will provide. This allows for flexibility
  in the type of data the servlet can handle and return as JSON.

### Constructor

- `AppInfoServlet(T info)`: Constructs an instance of `AppInfoServlet` with the specified information to be returned by
  the servlet.

  | Parameter | Type | Description |
      |-----------|------|-------------|
  | info      | `T`  | The information of type `T` that this servlet will return when handling GET requests. |

### Methods

#### `doGet(HttpServletRequest req, HttpServletResponse resp)`

Overrides the `doGet` method from `HttpServlet` to handle HTTP GET requests.

- **Parameters:**
    - `req`: `HttpServletRequest` - The request sent by the client to the server.
    - `resp`: `HttpServletResponse` - The response that the servlet sends back to the client.

- **Functionality:** When a GET request is received, this method sets the response content type to `text/json`, sets the
  HTTP status code to `200 OK`, and writes the JSON representation of the `info` object to the response. The JSON
  serialization is performed using the `JsonUtil.objectMapper()` utility.

- **Usage Example:** This method is automatically called by the servlet container when a GET request is made to the
  servlet's URL. It is not meant to be called directly.

### Usage

To use the `AppInfoServlet`, you need to instantiate it with the specific type of information you want to serve as JSON.
For example, if you have a class `AppInfo` that contains application metadata, you can create an instance
of `AppInfoServlet` like so:

```java
AppInfo appInfo = new AppInfo("MyApp", "1.0");
AppInfoServlet<AppInfo> servlet = new AppInfoServlet<>(appInfo);
```

After instantiation, the servlet needs to be registered with a servlet container or web server to handle requests to a
specific path.

### Dependencies

- `com.simiacryptus.jopenai.util.JsonUtil`: Used for converting the `info` object into its JSON representation.
- `jakarta.servlet.http.HttpServlet`: The base class for HTTP servlets.
- `jakarta.servlet.http.HttpServletRequest`: Represents the request sent by the client to the server.
- `jakarta.servlet.http.HttpServletResponse`: Represents the response that the servlet sends back to the client.

### Conclusion

The `AppInfoServlet` class provides a simple and flexible way to serve application-specific information as JSON over
HTTP. By leveraging generic types, it can be used to serve various kinds of information, making it a versatile component
in web applications.

# kotlin\com\simiacryptus\skyenet\webui\servlet\CancelThreadsServlet.kt

## CancelThreadsServlet Documentation

The `CancelThreadsServlet` class is part of the web UI module for managing session-based operations within an
application server environment. It extends `HttpServlet` and provides mechanisms to cancel running threads associated
with a specific session through a web interface. This servlet is designed to handle both GET and POST requests, allowing
users to initiate the cancellation process and confirm cancellation, respectively.

### Class Overview

- **Package**: `com.simiacryptus.skyenet.webui.servlet`
- **Dependencies**:
    - `com.simiacryptus.jopenai.util.JsonUtil`
    - `com.simiacryptus.skyenet.core.platform.*`
    - `jakarta.servlet.http.*`
    - Other internal components related to application server and session management.

### Constructor

#### `CancelThreadsServlet(ApplicationServer server)`

Initializes a new instance of the `CancelThreadsServlet` with a reference to the `ApplicationServer`.

- **Parameters**:
    - `server`: An instance of `ApplicationServer` to which this servlet belongs.

### Methods

#### `doGet(HttpServletRequest req, HttpServletResponse resp)`

Handles the GET request by displaying an HTML form where the user can confirm the cancellation of a session.

- **Parameters**:
    - `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet returns to the client.

#### `doPost(HttpServletRequest req, HttpServletResponse resp)`

Handles the POST request by processing the cancellation confirmation. It validates the session ID and confirmation
input, checks user authorization, and then proceeds to shut down the thread pool associated with the session.

- **Parameters**:
    - `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet returns to the client.

### Usage

1. **Initiating Cancellation**: A user initiates the cancellation process by navigating to the servlet's URL with
   a `sessionId` parameter in the query string. The servlet responds with an HTML form asking the user to confirm the
   cancellation by typing 'confirm'.

2. **Confirming Cancellation**: The user submits the confirmation form. The servlet validates the input and checks if
   the user is authorized to cancel the session. If authorized, the servlet cancels the session's threads and redirects
   the user to the home page.

### Security Considerations

- **Authorization**: The servlet checks if the user is authorized to perform cancellation operations. It ensures that
  only users with the appropriate permissions can cancel sessions, including global sessions.
- **Input Validation**: The servlet validates the `sessionId` and confirmation input to prevent unauthorized or
  accidental operations.

### Error Handling

- The servlet responds with `HttpServletResponse.SC_BAD_REQUEST` if the required parameters are missing or invalid.
- Throws an exception if the user is not authorized to perform the operation, ensuring that unauthorized attempts are
  logged and halted.

### Conclusion

The `CancelThreadsServlet` provides a web interface for managing the cancellation of sessions within an application
server environment. It ensures that only authorized users can perform cancellations and offers a user-friendly way to
confirm such operations, enhancing the application's manageability and security.

# kotlin\com\simiacryptus\skyenet\webui\servlet\CorsFilter.kt

## CorsFilter Class Documentation

The `CorsFilter` class is a servlet filter designed to handle Cross-Origin Resource Sharing (CORS) for web applications.
It is part of the `com.simiacryptus.skyenet.webui.servlet` package. This filter allows or restricts web applications
running on one domain to request resources from another domain. By default, web browsers restrict cross-origin HTTP
requests initiated from scripts for security reasons. The `CorsFilter` class provides a way to relax this security
measure for specific domains or paths.

### Features

- **Cross-Origin Request Handling**: Automatically adds CORS headers to HTTP responses for requests not ending
  with `/ws`.
- **Flexible URL Pattern Matching**: Applies to all paths (`/*`) by default, but can be easily adjusted by changing
  the `urlPatterns` parameter in the `@WebFilter` annotation.
- **Asynchronous Support**: Supports asynchronous request processing with `asyncSupported = true`.

### Usage

To use the `CorsFilter`, ensure it is included in your web application's deployment descriptor or annotated
with `@WebFilter` as shown in the code. No additional configuration is required for basic usage. The filter applies to
all incoming requests but excludes paths ending with `/ws`.

#### Configuration

- **URL Patterns**: The filter is configured to intercept all paths (`/*`). This can be modified by changing
  the `urlPatterns` attribute in the `@WebFilter` annotation.
- **Asynchronous Processing**: Enabled by default (`asyncSupported = true`). If your application does not use
  asynchronous processing, this can be set to `false`.

#### Methods

- `init(filterConfig: FilterConfig?)`: Initializes the filter. No custom initialization is performed in the current
  implementation.
- `doFilter(request: ServletRequest?, response: ServletResponse, chain: FilterChain)`: Processes incoming requests and
  adds CORS headers to the response if the request URI does not end with `/ws`.
- `destroy()`: Cleans up any resources used by the filter. No custom cleanup is performed in the current implementation.

#### CORS Headers Added

- `Access-Control-Allow-Origin: *`: Allows all domains to request resources.
- `Access-Control-Allow-Methods: POST, GET, OPTIONS, DELETE, PUT`: Specifies the allowed methods for cross-origin
  requests.
- `Access-Control-Max-Age: 3600`: Indicates how long the results of a preflight request can be cached.
- `Access-Control-Allow-Headers: Content-Type, x-requested-with, authorization`: Specifies the headers allowed in the
  actual request.

### Example

No additional steps are required to integrate the `CorsFilter` into your application beyond including it in your
project. Ensure your web application is configured to load servlet filters.

### Logging

The `CorsFilter` utilizes SLF4J for logging. Any exceptions encountered during the filtering process are logged as
warnings, including the stack trace for debugging purposes.

### Conclusion

The `CorsFilter` class is a straightforward solution for enabling CORS in web applications. By adding this filter,
developers can easily configure which resources are accessible from different origins, enhancing the interoperability
and security of web applications.

# kotlin\com\simiacryptus\skyenet\webui\servlet\FileServlet.kt

## FileServlet Class Documentation

The `FileServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends `HttpServlet` to
provide a web interface for accessing and managing files within a session-based directory structure. It interacts with
a `StorageInterface` to resolve file paths and supports both small and large file transfers efficiently.

### Constructor

- `FileServlet(StorageInterface dataStorage)`: Initializes a new instance of the `FileServlet` class using the
  provided `dataStorage` to manage file paths and access.

### Methods

#### Public Methods

- `void doGet(HttpServletRequest req, HttpServletResponse resp)`: Handles the GET request by serving files or
  directories based on the request path. It supports file downloading, directory listing, and redirection for directory
  paths without a trailing slash.

#### Private Methods

- `void writeSmall(FileChannel channel, HttpServletResponse resp, File file, HttpServletRequest req)`: Serves small
  files (less than 1MB) by reading the file content into memory and writing it to the response output stream.
- `void writeLarge(FileChannel channel, HttpServletResponse resp, File file, HttpServletRequest req)`: Serves large
  files using a `MappedByteBuffer` for efficient memory-mapped file I/O.
- `String directoryHTML(HttpServletRequest req, Session session, String filePath, String folders, String files)`:
  Generates an HTML page for directory listing, including links to contained files and subdirectories.

### Utility Methods

- `static List<String> parsePath(String path)`: Parses the request path into segments, performing validation to prevent
  directory traversal attacks and invalid characters.
- `static String directoryHTML(...)`: Generates the HTML content for directory listings.

### Companion Object

The companion object contains shared resources and utility functions:

- `Logger log`: SLF4J Logger instance for logging.
- `Cache<File, FileChannel> channelCache`: A Guava cache for managing open `FileChannel` instances, with automatic
  closing and removal of stale entries.

### Cache Configuration

The `channelCache` is configured with a maximum size of 100 entries and an expiration policy of 10 seconds after the
last access. It uses a `RemovalListener` to log and close `FileChannel` instances when they are evicted from the cache.

### Exception Handling

The class includes checks and exception handling to ensure safe path resolution, preventing directory traversal attacks
and handling invalid path characters. It also includes error handling for I/O operations, logging errors encountered
during file reading or writing.

### Usage

To use the `FileServlet`, it must be registered with a servlet container (e.g., Tomcat, Jetty) and mapped to a URL
pattern. The servlet requires a `StorageInterface` implementation to manage file storage and session-based directory
resolution.

### Example

```java
StorageInterface storage = new MyStorageImplementation();
FileServlet servlet = new FileServlet(storage);
// Servlet registration and URL mapping code here
```

This class is designed to be flexible and efficient, supporting both small and large file transfers while providing a
user-friendly directory browsing experience.

# kotlin\com\simiacryptus\skyenet\webui\servlet\DeleteSessionServlet.kt

## DeleteSessionServlet Documentation

`DeleteSessionServlet` is a servlet class designed to handle the deletion of user sessions within a web application. It
extends `HttpServlet` and overrides the `doGet` and `doPost` methods to provide functionality for confirming and
processing the deletion of a session, respectively.

### Overview

The servlet is part of a larger web application framework and interacts with various components such
as `ApplicationServer`, `ApplicationServices`, and `AuthorizationInterface` to perform its duties. It is designed to
ensure that only authorized users can delete sessions, and it provides both a confirmation mechanism and a direct
deletion process.

### Usage

#### Initialization

The servlet is initialized with an instance of `ApplicationServer`, which is passed to the constructor:

```java
public DeleteSessionServlet(ApplicationServer server) {
    this.server = server;
}
```

This `server` instance is used to interact with the application's data storage and authentication systems.

#### Handling GET Requests

The `doGet` method is responsible for presenting the user with a confirmation form when they attempt to delete a
session. It checks if the request contains a `sessionId` parameter and, if so, displays an HTML form asking the user to
confirm the deletion by typing 'confirm'.

```java

@Override
public void doGet(HttpServletRequest req, HttpServletResponse resp) {
    // Implementation details...
}
```

If the `sessionId` parameter is missing, the method responds with a `400 Bad Request` status and a message indicating
that a session ID is required.

#### Handling POST Requests

The `doPost` method processes the form submission, verifying that the user has typed 'confirm' and that a session ID is
provided. It also checks if the user is authorized to delete the session, with additional checks for global sessions.

```java

@Override
public void doPost(HttpServletRequest req, HttpServletResponse resp) {
    // Implementation details...
}
```

If the checks pass, the session is deleted from the application's data storage, and the user is redirected to the
application's home page.

### Security Considerations

- **Authorization Checks**: The servlet performs thorough authorization checks to ensure that only users with the
  appropriate permissions can delete sessions. This includes a general authorization check and a specific check for the
  deletion of global sessions.
- **Confirmation Requirement**: By requiring users to type 'confirm' before a session can be deleted, the servlet adds
  an extra layer of protection against accidental or malicious deletions.

### Dependencies

- `ApplicationServer`: Used to access application-specific services and data storage.
- `ApplicationServices`: Provides access to the application's authentication and authorization systems.
- `AuthorizationInterface`: Defines the operations and permissions used in authorization checks.
- `Session`: Represents a user session within the application.

### Conclusion

`DeleteSessionServlet` is a critical component of the web application, providing secure and user-friendly functionality
for deleting sessions. Its integration with the application's authentication and authorization systems ensures that
session deletions are handled safely and responsibly.

# kotlin\com\simiacryptus\skyenet\webui\servlet\OAuthBase.kt

## OAuthBase Class Documentation

The `OAuthBase` class serves as an abstract base for implementing OAuth authentication within a web application using
the Jetty server. This class is part of the `com.simiacryptus.skyenet.webui.servlet` package.

### Overview

OAuth is an open standard for access delegation, commonly used as a way for Internet users to grant websites or
applications access to their information on other websites but without giving them the passwords. This class provides a
structured way to integrate OAuth authentication into your web applications by defining a common interface and
functionality for OAuth operations.

### Usage

To use the `OAuthBase` class, you need to extend it in your own class and implement the abstract method `configure`.
This method is designed to configure the Jetty `WebAppContext` for OAuth authentication, including setting up necessary
filters if required.

#### Parameters

- `redirectUri`: A `String` specifying the URI to redirect to after authentication. This is a constructor parameter for
  the `OAuthBase` class.

#### Methods

##### `configure`

```kotlin
abstract fun configure(context: WebAppContext, addFilter: Boolean = true): WebAppContext
```

This abstract method must be implemented by subclasses. It is intended to configure the provided `WebAppContext` for
OAuth authentication.

###### Parameters:

- `context`: The `WebAppContext` to be configured. This is the context of your web application where you want to
  integrate OAuth authentication.
- `addFilter`: A `Boolean` indicating whether to add the OAuth authentication filter to the `WebAppContext`. The default
  value is `true`.

###### Returns:

- `WebAppContext`: The configured `WebAppContext` instance.

### Example

Below is an example of how to extend the `OAuthBase` class and implement the `configure` method:

```kotlin
package com.example.auth

import com.simiacryptus.skyenet.webui.servlet.OAuthBase
import org.eclipse.jetty.webapp.WebAppContext

class MyOAuthImpl(redirectUri: String) : OAuthBase(redirectUri) {
  override fun configure(context: WebAppContext, addFilter: Boolean): WebAppContext {
    // Implement OAuth configuration and filter setup here
    if (addFilter) {
      // Add OAuth filter to the context
    }
    return context
  }
}
```

In this example, `MyOAuthImpl` extends `OAuthBase` and provides an implementation for the `configure` method. This
method should include the logic to configure the `WebAppContext` for OAuth, including setting up any necessary filters
based on the `addFilter` parameter.

### Conclusion

The `OAuthBase` class provides a foundational structure for integrating OAuth authentication into web applications
running on the Jetty server. By extending this class and implementing the `configure` method, developers can customize
the OAuth authentication process to meet their application's requirements.

# kotlin\com\simiacryptus\skyenet\webui\servlet\LogoutServlet.kt

## LogoutServlet Documentation

The `LogoutServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends the `HttpServlet`
class provided by the Jakarta Servlet API. This servlet is designed to handle logout requests within a web application,
ensuring that users can securely sign out of their sessions.

### Overview

When a GET request is made to the servlet's mapped URL, the servlet processes the request by attempting to log out the
current user based on the session or authentication cookie provided in the request. If the logout process is successful,
the user is redirected to the application's home page. If the logout process fails (e.g., due to an invalid or missing
cookie), the servlet responds with a `400 Bad Request` status code.

### Usage

To use the `LogoutServlet`, it must be properly mapped in the web application's deployment descriptor (`web.xml`) or
through annotations, specifying the URL pattern it should respond to. Once mapped, it will automatically handle GET
requests to that URL for logging out users.

#### Key Methods

- `doGet(HttpServletRequest req, HttpServletResponse resp)`: This method is overridden from the `HttpServlet` class and
  is called by the server (via the service method) to allow the servlet to handle a GET request. It performs the logout
  logic and response handling.

#### Logout Process

1. **Cookie Retrieval**: The servlet first attempts to retrieve the authentication cookie from the incoming request
   using the `getCookie()` method from `ApplicationServer`.
2. **User Identification**: Using the retrieved cookie, it then attempts to identify the user by
   calling `getUser(cookie)` on the `authenticationManager` from `ApplicationServices`.
3. **Logout and Redirection**:
    - If no user is associated with the provided cookie (i.e., `user` is `null`), the servlet sets the response status
      to `400 Bad Request`.
    - If a user is successfully identified, the servlet proceeds to log out the user by calling `logout(cookie, user)`on
      the `authenticationManager` and then redirects the user to the home page (`"/"`).

#### Error Handling

- If the logout process fails due to issues such as an invalid or missing cookie, the servlet responds with
  a `400 Bad Request` status, indicating that the request cannot be processed due to client error.

### Example Deployment Descriptor Configuration

```xml

<servlet>
    <servlet-name>logoutServlet</servlet-name>
    <servlet-class>com.simiacryptus.skyenet.webui.servlet.LogoutServlet</servlet-class>
</servlet>
<servlet-mapping>
<servlet-name>logoutServlet</servlet-name>
<url-pattern>/logout</url-pattern>
</servlet-mapping>
```

### Conclusion

The `LogoutServlet` provides a straightforward way to handle user logout requests in a web application, ensuring that
users can securely end their sessions. By leveraging the `ApplicationServices` for user authentication management, it
integrates seamlessly with the application's overall security framework.

# kotlin\com\simiacryptus\skyenet\webui\servlet\OAuthGoogle.kt

## OAuthGoogle Class Documentation

The `OAuthGoogle` class provides a comprehensive solution for integrating Google OAuth2 authentication into Java-based
web applications. It extends the functionality of the `OAuthBase` class and is designed to facilitate user
authentication and authorization through Google's OAuth2 services.

### Overview

The class is structured to handle the OAuth2 flow, including the login redirection to Google's authorization service,
callback handling after successful authentication, and user session management. It leverages the Google Client Library
for Java to simplify the integration process.

### Key Components

- **LoginServlet**: An inner class that handles the redirection of users to Google's OAuth2 authorization page.
- **CallbackServlet**: An inner class responsible for handling the callback from Google after the user has authorized
  the application. It processes the authorization code, exchanges it for an access token, retrieves user information,
  and manages user sessions.
- **configure**: A method to configure servlets and optional filters in the web application context for handling login
  and callback URLs.

### Usage

1. **Initialization**: Create an instance of the `OAuthGoogle` class by providing the redirect URI, application name,
   and a function to retrieve the Google client secrets input stream.

2. **Configuration**: Invoke the `configure` method on the `OAuthGoogle` instance within your web application's
   initialization code to set up the necessary servlets and filters.

3. **Authentication Flow**: Direct users to the `/login` or `/googleLogin` endpoint to initiate the OAuth2 flow. After
   successful authentication and authorization by Google, users will be redirected to the specified redirect URI with an
   authorization code, which is then processed by the `CallbackServlet`.

4. **Session Management**: Upon successful authentication, a session ID is generated, stored, and sent to the client as
   a secure HTTP cookie. This session ID can be used for managing user sessions within your application.

### Configuration Parameters

- **redirectUri**: The URI to which Google will redirect users after they have authorized your application. This URI
  must be registered in the Google Cloud Console.
- **applicationName**: The name of your application. This should match the name registered in the Google Cloud Console.
- **key**: A function that returns an `InputStream` of the Google client secrets JSON file.

### Example

```java
WebAppContext context = new WebAppContext();
OAuthGoogle googleAuth = new OAuthGoogle(
        "http://yourdomain.com/oauth2callback",
        "Your Application Name",
        () -> getClass().getResourceAsStream("/path/to/client_secrets.json")
);
googleAuth.

configure(context, true);
```

### Dependencies

- Google Client Library for Java
- Jetty Server (for servlet and filter handling)
- SLF4J (for logging)

### Important Notes

- Ensure that the Google client secrets JSON file is securely stored and accessible by your application.
- The redirect URI provided during the `OAuthGoogle` class initialization must exactly match one of the URIs registered
  in the Google Cloud Console for your application.
- The session management implementation in the `CallbackServlet` is a basic example. Depending on your application's
  requirements, you may need to implement more sophisticated session handling mechanisms.

This documentation provides a high-level overview of the `OAuthGoogle` class and its usage. For more detailed
information, refer to the source code and the Google OAuth2 documentation.

# kotlin\com\simiacryptus\skyenet\webui\servlet\NewSessionServlet.kt

## NewSessionServlet Documentation

The `NewSessionServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends
the `HttpServlet` class provided by the Jakarta Servlet API. This servlet is designed to handle HTTP GET requests by
generating a new global session ID, setting the response content type to plain text, and returning the newly generated
session ID to the client.

### Usage

This servlet is intended to be used in web applications that require session management. When a client sends a GET
request to the endpoint where this servlet is mapped, it will receive a unique session ID in plain text format. This
session ID can then be used for tracking the session or for other purposes as required by the application.

### Implementation Details

#### Import Statements

The servlet imports the following classes:

- `com.simiacryptus.skyenet.core.platform.StorageInterface`: Used to generate a new global session ID.
- `jakarta.servlet.http.HttpServlet`: The base class for HTTP servlets.
- `jakarta.servlet.http.HttpServletRequest`: Represents the client's request.
- `jakarta.servlet.http.HttpServletResponse`: Represents the response that the servlet sends back to the client.

#### Class Definition

`NewSessionServlet` extends `HttpServlet` and overrides the `doGet` method to handle GET requests.

#### doGet Method

The `doGet` method has the following signature:

```kotlin
override fun doGet(req: HttpServletRequest, resp: HttpServletResponse)
```

##### Parameters

- `req: HttpServletRequest`: The request sent by the client. This parameter is not used in the current implementation
  but is available for future enhancements.
- `resp: HttpServletResponse`: The response that the servlet will send back to the client.

##### Implementation

1. **Generate Session ID**: A new global session ID is generated by calling `StorageInterface.newGlobalID()`.
2. **Set Content Type**: The content type of the response is set to "text/plain" using `resp.contentType`.
3. **Set Status Code**: The HTTP status code of the response is set to `HttpServletResponse.SC_OK` (200) to indicate a
   successful request.
4. **Return Session ID**: The generated session ID is converted to a string and written to the response
   using `resp.writer.write(sessionId.toString())`.

### Example Usage

This servlet does not require any specific configuration for its basic functionality. Once deployed and mapped to a URL
pattern in a web application, it can be accessed by sending a GET request to the corresponding URL. The response will
contain the generated session ID as plain text.

For example, if the servlet is mapped to `/newSession`, a GET request to `http://yourdomain.com/newSession` will return
a new session ID.

### Conclusion

The `NewSessionServlet` provides a simple and efficient way to generate and return new session IDs for web applications.
Its implementation can be extended or modified to suit specific requirements, such as adding authentication or logging
for session creation.

# kotlin\com\simiacryptus\skyenet\webui\servlet\SessionIdFilter.kt

## SessionIdFilter Class Documentation

The `SessionIdFilter` class is a custom implementation of the `Filter` interface used in Java Servlet applications. It
is designed to intercept HTTP requests and perform session validation, particularly focusing on secure pages that
require user authentication. If a user is not authenticated, the filter redirects them to a login page.

### Package

```java
package com.simiacryptus.skyenet.webui.servlet;
```

### Imports

The class uses several imports from the `jakarta.servlet` package, the `java.net` package for URL encoding, and specific
project imports for application services and utilities.

### Constructor

```java
SessionIdFilter((HttpServletRequest) ->
        Boolean isSecure, String
loginRedirect)
```

- `isSecure`: A lambda function that takes an `HttpServletRequest` object as input and returns a `Boolean` indicating
  whether the request targets a secure page that requires authentication.
- `loginRedirect`: A `String` specifying the URL to redirect unauthenticated users to, typically a login page.

### Methods

#### init

```java

@Override
void init(FilterConfig filterConfig)
```

- Initializes the filter. This implementation does not perform any initialization logic.

#### doFilter

```java

@Override
void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
```

- Core method of the filter. It intercepts HTTP requests and checks if the request is for a secure page and whether the
  user is authenticated.
- If the request is for a secure page (`isSecure` returns `true`) and the user is not authenticated (no valid session ID
  cookie or no user associated with the session ID), the method redirects the user to the login page specified in
  the `loginRedirect` parameter, appending the original requested URL as a query parameter for potential redirection
  after successful authentication.
- If the user is authenticated or the page is not secure, the filter chain proceeds normally
  with `chain.doFilter(request, response)`.

#### destroy

```java

@Override
void destroy()
```

- Cleans up any resources used by the filter. This implementation does not perform any cleanup logic.

### Usage Example

To use `SessionIdFilter` in a web application, you need to configure it in the `web.xml` file or programmatically in the
application's initialization code. You must provide the `isSecure` lambda function and the `loginRedirect` URL according
to your application's requirements.

```java
FilterRegistration.Dynamic sessionFilter = servletContext.addFilter("SessionIdFilter", new SessionIdFilter(
        request -> request.getRequestURI().startsWith("/secure"),
        "/login.jsp"
));
sessionFilter.

addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true,"/*");
```

In this example, all requests starting with `/secure` are considered to require authentication, and unauthenticated
users are redirected to `/login.jsp`.

### Conclusion

The `SessionIdFilter` class provides a flexible way to enforce authentication on secure pages within a Java Servlet
application. By redirecting unauthenticated users to a login page and allowing for custom logic to determine which pages
are secure, it helps maintain application security and user session management.

# kotlin\com\simiacryptus\skyenet\webui\servlet\ProxyHttpServlet.kt

## ProxyHttpServlet Developer Documentation

The `ProxyHttpServlet` class is designed to act as a reverse proxy, specifically tailored to support interactions with
the OpenAI API. It extends `HttpServlet` and provides a mechanism to forward requests from clients to the OpenAI API,
potentially modifying requests and responses along the way. This documentation outlines the key functionalities, setup,
and usage of the `ProxyHttpServlet`.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.servlet`
- **Dependencies**: Jakarta Servlet API, Apache HttpComponents, Jetty Server, and others for handling HTTP requests and
  JSON manipulation.
- **Key Features**:
    - Asynchronous HTTP client for forwarding requests.
    - Request and response modification capabilities.
    - API key management and budget checks.
    - Detailed logging of proxied requests and responses.

### Setup

To use `ProxyHttpServlet`, ensure you have the necessary dependencies in your project's build configuration. The servlet
is designed to be deployed on a servlet container or application server that supports the Jakarta Servlet API, such as
Jetty or Tomcat.

### Key Components

#### Fields

- `targetUrl`: The base URL of the OpenAI API. Defaults to `"https://api.openai.com/v1/"`.
- `asyncClient`: An instance of `CloseableHttpAsyncClient` used for executing asynchronous HTTP requests to the target
  URL.

#### Methods

##### `service(HttpServletRequest req, HttpServletResponse resp)`

Overrides the `service` method from `HttpServlet` to handle incoming requests. It performs several key operations:

- Starts an asynchronous context for the request.
- Validates the API key and checks the budget.
- Forwards the request to the OpenAI API using the asynchronous HTTP client.
- Modifies the response based on the application's logic before sending it back to the client.

##### `getProxyRequest(HttpServletRequest req)`

Constructs a `SimpleHttpRequest` to be sent to the OpenAI API. It filters and copies headers from the original request
and sets the request body.

##### `onResponse(...)`

A hook for modifying the response before it is sent back to the client. By default, it logs the request and response but
can be overridden for custom behavior.

##### `onRequest(HttpServletRequest req, ByteArray bytes)`

A hook for modifying the request before it is forwarded to the OpenAI API. By default, it returns the request body bytes
unchanged but can be overridden for custom behavior.

#### Usage Example

The `main` method and the `test` function within the `companion object` provide a basic example of setting up a Jetty
server with the `ProxyHttpServlet` and a simple test servlet. This setup is intended for demonstration purposes and
should be adapted for production environments.

### Extending `ProxyHttpServlet`

To customize the behavior of the proxy, you can extend `ProxyHttpServlet` and override the `onRequest` and `onResponse`
methods. This allows you to modify requests and responses according to your application's specific needs.

### Conclusion

The `ProxyHttpServlet` offers a flexible and powerful way to interact with the OpenAI API, providing mechanisms for
request and response manipulation, API key management, and detailed logging. By extending and customizing the servlet,
developers can integrate sophisticated proxy logic into their applications.

# kotlin\com\simiacryptus\skyenet\webui\servlet\SessionSettingsServlet.kt

## SessionSettingsServlet Documentation

The `SessionSettingsServlet` class is part of the web UI module for managing session-specific settings within an
application. It extends `HttpServlet` and provides mechanisms to both display and update settings associated with a user
session.

### Overview

This servlet is designed to interact with an `ApplicationServer` instance to retrieve and store settings. It supports
two HTTP methods:

- `GET`: To fetch and display the current settings for a given session.
- `POST`: To update the settings for a given session.

### Dependencies

- `com.simiacryptus.jopenai.util.JsonUtil`: For JSON serialization and deserialization.
- `com.simiacryptus.skyenet.core.platform`: For session management and authentication.
- `com.simiacryptus.skyenet.webui.application.ApplicationServer`: To interact with application-level services and data
  storage.
- `jakarta.servlet.http.HttpServlet`: Base class for handling HTTP requests.

### Constructor

```java
public SessionSettingsServlet(ApplicationServer server)
```

- `server`: An instance of `ApplicationServer` to interact with application services.

### Fields

- `settingsClass`: The class type for the settings. Currently set to `Map.class.java`, but it's designed to be easily
  changed to match the actual settings class used by the server.

### HTTP Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

Handles the HTTP GET request to fetch and display the current settings.

- `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
- `resp`: The `HttpServletResponse` object that contains the response the servlet returns to the client.

##### Process Flow

1. Sets the response content type to "text/html".
2. Checks if the request contains a "sessionId" parameter.
    - If not, responds with a 400 Bad Request status and a message indicating that a session ID is required.
    - If a session ID is provided:
        1. Retrieves the session and user information.
        2. Fetches the settings associated with the session and user.
        3. Serializes the settings to JSON and embeds them in an HTML form for display and editing.

#### doPost(HttpServletRequest req, HttpServletResponse resp)

Handles the HTTP POST request to update the settings for a given session.

- `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
- `resp`: The `HttpServletResponse` object that contains the response the servlet sends to the client.

##### Process Flow

1. Sets the response content type to "text/html".
2. Checks if the request contains a "sessionId" parameter.
    - If not, responds with a 400 Bad Request status and a message indicating that a session ID is required.
    - If a session ID is provided:
        1. Retrieves the session information.
        2. Deserializes the updated settings from the request.
        3. Stores the updated settings in the data storage associated with the session and user.
        4. Redirects the user to a confirmation or summary page.

### Usage Example

This servlet is typically mapped to a URL pattern within a web application's deployment descriptor (web.xml) or through
annotations, allowing it to handle requests to view or update session settings.

```xml

<servlet>
    <servlet-name>sessionSettingsServlet</servlet-name>
    <servlet-class>com.simiacryptus.skyenet.webui.servlet.SessionSettingsServlet</servlet-class>
</servlet>
<servlet-mapping>
<servlet-name>sessionSettingsServlet</servlet-name>
<url-pattern>/settings</url-pattern>
</servlet-mapping>
```

This setup enables users to navigate to `/settings` to view or modify their session settings, provided they include a
valid session ID in their request.

# kotlin\com\simiacryptus\skyenet\webui\servlet\SessionListServlet.kt

## SessionListServlet Class Documentation

The `SessionListServlet` class is a part of the web UI module designed to handle HTTP GET requests to list user sessions
in a web application. It extends `HttpServlet` from the Jakarta Servlet API, enabling it to respond to HTTP requests.
This class is specifically tailored for applications that require session management and user authentication.

### Dependencies

- Jakarta Servlet API
- Application-specific classes such as `StorageInterface`, `ApplicationServer`, and `authenticationManager`
- Java Standard Library classes like `SimpleDateFormat`

### Constructor Parameters

- `dataStorage`: An instance of `StorageInterface` used to interact with the application's data storage system.
- `prefix`: A `String` value representing the URL prefix for session links.
- `applicationServer`: An instance of `ApplicationServer` used to access application-specific settings and descriptions.

### Key Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

This method overrides `doGet` from `HttpServlet` to handle GET requests. It generates an HTML page listing all sessions
associated with the authenticated user.

##### Parameters

- `req`: The `HttpServletRequest` object that contains the request the client has made to the servlet.
- `resp`: The `HttpServletResponse` object that contains the response the servlet sends to the client.

##### Functionality

1. **Content Type and Status**: Sets the response content type to "text/html" and the HTTP status code to `SC_OK` (200).

2. **User Authentication**: Retrieves the current user based on a cookie from the request, using
   the `authenticationManager`.

3. **Session Retrieval**: Fetches a list of sessions associated with the authenticated user from `dataStorage`.

4. **HTML Generation**: Dynamically generates an HTML page that lists the user's sessions, including session names and
   creation times. The page includes a link for each session that, when clicked, redirects the user to a detailed view
   of that session.

5. **Styling**: The HTML includes basic CSS for styling the table and its contents.

### Usage Example

To use `SessionListServlet`, an instance of it must be created and registered with a servlet container. This is
typically done within the application's server setup code. Here's a simplified example:

```java
import com.simiacryptus.skyenet.core.platform.StorageInterface;
import com.simiacryptus.skyenet.webui.application.ApplicationServer;

// Assuming storage and appServer are already initialized
StorageInterface storage = ...;
ApplicationServer appServer = ...;
String prefix = "/sessions";

SessionListServlet sessionListServlet = new SessionListServlet(storage, prefix, appServer);

// Register the servlet with a servlet container (e.g., Jetty, Tomcat)
// This step is specific to the servlet container being used
```

### Notes

- The `SessionListServlet` class is designed to be flexible and can be adapted to different storage backends and
  application servers by implementing the `StorageInterface` and `ApplicationServer` interfaces accordingly.
- The class assumes that user authentication and session management logic are handled elsewhere in the application,
  specifically by the `authenticationManager` and `dataStorage` components.
- The generated HTML is intended for simple administrative or debugging purposes and may need to be customized or
  extended for production use.

# kotlin\com\simiacryptus\skyenet\webui\servlet\SessionShareServlet.kt

## SessionShareServlet Documentation

The `SessionShareServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and
extends `jakarta.servlet.http.HttpServlet`. It is designed to facilitate the sharing of user sessions within the Skyenet
web application framework. This servlet handles HTTP GET requests to generate and share session URLs, allowing users to
share their application state with others.

### Overview

When a GET request is received, the servlet performs several key operations:

1. **Authentication and Authorization**: It authenticates the user making the request and checks if they are authorized
   to share sessions.
2. **Session Validation**: Validates the session URL provided in the request parameters.
3. **Session Sharing**: If the session can be shared, it either retrieves an existing share ID or generates a new one,
   then saves the session state to a specified location accessible via the generated/shared URL.

### Key Components

#### Dependencies

- **ApplicationServices**: Provides access to various application-level services like authentication, authorization, and
  cloud storage interfaces.
- **StorageInterface**: Used for session ID parsing and JSON data storage/retrieval.
- **Selenium2S3**: A specialized component for saving web session states, potentially including image loading based on
  request parameters.

#### Main Methods

##### doGet(HttpServletRequest req, HttpServletResponse resp)

Handles the HTTP GET request. It performs the following operations:

1. **User Authentication**: Retrieves the user based on the session cookie.
2. **URL and Host Validation**: Checks if the request contains a valid URL and if the host is accepted for sharing.
3. **Session Sharing Process**:
    - Checks for an existing share ID and reuses it if valid.
    - If no valid share ID exists and the user is authorized, generates a new share ID and saves the session state.

Parameters:

- `HttpServletRequest req`: The HTTP request object.
- `HttpServletResponse resp`: The HTTP response object.

#### Helper Methods

- **url(String appName, String shareId)**: Constructs the share URL based on the application name and share ID.
- **acceptHost(User user, String host)**: Validates the host against allowed values or checks if the user has admin
  authorization.
- **validateUrl(String previousShare)**: Checks if a previously shared URL is still processing or validates its HTTP
  status.

#### Usage

To use this servlet within your web application, ensure it is properly mapped in your web application's deployment
descriptor (`web.xml`) or through annotations. When making a GET request to the servlet's mapped URL, include the
session URL as a parameter. The servlet will respond with either the sharing URL or an error message, depending on the
request's validity and the user's authorization.

#### Error Handling

The servlet responds with appropriate HTTP status codes and messages in case of errors, such as:

- **400 Bad Request**: If the URL parameter is missing or invalid.
- **403 Forbidden**: If the user is not authorized to share sessions.

#### Security Considerations

Ensure that user authentication and authorization checks are robust to prevent unauthorized access and sharing of
sensitive session data.

### Conclusion

The `SessionShareServlet` provides a crucial functionality for session sharing within the Skyenet web application
framework, leveraging the platform's authentication, authorization, and storage services. Proper implementation and
usage of this servlet enhance collaboration and usability by allowing users to share their application states securely.

# kotlin\com\simiacryptus\skyenet\webui\servlet\SessionThreadsServlet.kt

## SessionThreadsServlet Documentation

The `SessionThreadsServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends
the `HttpServlet` class from the Jakarta Servlet API. This servlet is designed to provide a web interface for monitoring
thread pool statistics and stack traces for a specific session within the Skyenet web application. It is particularly
useful for debugging and performance monitoring.

### Overview

When a GET request is made to this servlet with a `sessionId` parameter, it responds with an HTML page displaying
detailed information about the thread pool associated with the given session. This includes general pool statistics such
as the number of active threads, pool size, queue size, completed task count, and total task count. Additionally, it
provides a list of all active threads in the pool, including their names and current stack traces.

### Usage

To use this servlet, it must be deployed as part of a Skyenet web application within a servlet container (e.g., Tomcat,
Jetty). The servlet is mapped to a specific URL pattern within the web application's deployment descriptor (`web.xml`)or
through annotations.

#### Request Parameters

- **sessionId**: Required. The unique identifier of the session for which thread pool information is requested.

#### Response

- The response is an HTML document containing two main sections:
    - **Pool Stats**: Displays various statistics about the thread pool associated with the provided session ID.
    - **Thread Stacks**: Lists all active threads in the pool, including their names and stack traces.

#### Example Request

```
GET /path/to/servlet?sessionId=12345
```

This request would return an HTML page with thread pool statistics and stack traces for the session with ID `12345`.

### Implementation Details

#### Key Components

- **ApplicationServer**: The `server` instance variable of type `ApplicationServer` is used to access application-wide
  services and configurations.
- **Session**: Represents the session for which information is being requested. It is instantiated using the `sessionId`
  parameter from the request.
- **AuthenticationManager**: Used to authenticate the user making the request based on cookies.
- **ClientManager**: Responsible for managing thread pools associated with sessions. It is used to retrieve the thread
  pool for the specified session and user.

#### HTML Response Generation

The servlet generates the response HTML dynamically within the `doGet` method. It uses Kotlin's multi-line string
feature to embed HTML code directly within the Kotlin source code. CSS styles are included within the `<head>` section
of the HTML to style the output.

#### Error Handling

If the `sessionId` parameter is not provided in the request, the servlet responds with HTTP status code 400 (Bad
Request) and a simple error message indicating that the session ID is required.

### Security Considerations

- **Authentication**: The servlet uses the application's authentication manager to verify the identity of the user
  making the request. Ensure that appropriate authentication checks are in place to prevent unauthorized access to
  sensitive information.
- **Input Validation**: The servlet should validate the `sessionId` parameter to prevent injection attacks or attempts
  to access unauthorized information.

### Conclusion

The `SessionThreadsServlet` provides a valuable tool for monitoring and debugging thread pools associated with specific
sessions in the Skyenet web application. By offering insights into thread activity and pool statistics, it aids
developers and administrators in ensuring the application's performance and stability.

# kotlin\com\simiacryptus\skyenet\webui\servlet\ToolServlet.kt

## ToolServlet Developer Documentation

### Overview

`ToolServlet` is an abstract class extending `HttpServlet` designed to manage and serve custom tools within a web
application. It allows for the dynamic addition, editing, deletion, and execution of tools, which are defined by
user-provided Kotlin code and associated metadata. This class is part of a larger application framework aimed at
providing a flexible environment for tool management and execution.

### Key Components

#### Tool Data Class

- **Purpose**: Represents a tool with its path, API description, interpreter string, and servlet code.
- **Fields**:
    - `path`: The URL path associated with the tool.
    - `openApiDescription`: Metadata describing the tool's API in OpenAPI format.
    - `interpreterString`: A string that specifies how the tool's code should be interpreted.
    - `servletCode`: The Kotlin code that implements the tool's functionality.

#### Main Methods

##### doGet(HttpServletRequest?, HttpServletResponse?)

- **Description**: Handles GET requests. It serves different pages based on the query parameters, such as the tool index
  page, tool details page, edit form, and performs actions like tool deletion.
- **Parameters**:
    - `req`: The HttpServletRequest object.
    - `resp`: The HttpServletResponse object.

##### doPost(HttpServletRequest?, HttpServletResponse?)

- **Description**: Handles POST requests for editing existing tools or importing tools from a JSON file.
- **Parameters**:
    - `req`: The HttpServletRequest object.
    - `resp`: The HttpServletResponse object.

##### service(HttpServletRequest?, HttpServletResponse?)

- **Description**: Overrides the default `service` method to provide custom routing logic. It finds the requested tool
  and, if authorized, uses a dynamically constructed servlet to handle the request.
- **Parameters**:
    - `req`: The HttpServletRequest object.
    - `resp`: The HttpServletResponse object.

#### Utility Methods

##### indexPage(): String

- **Description**: Generates the HTML content for the index page listing all available tools.

##### toolDetailsPage(tool: Tool): String

- **Description**: Generates the HTML content for the details page of a specific tool.

##### serveEditPage(HttpServletRequest, HttpServletResponse, Tool)

- **Description**: Serves the edit page for a specific tool, allowing users to modify its properties.

### Security

The servlet checks for user authentication and authorization before allowing access to tool management functionalities.
It ensures that only users with admin rights can add, edit, or delete tools.

### Extensibility

Developers can extend `ToolServlet` to implement additional functionalities specific to their application's
requirements. The dynamic nature of tool management allows for a high degree of customization and extensibility.

### Example Usage

To use `ToolServlet`, one must define a concrete implementation that provides specific functionalities for tool
management. This involves creating a subclass that might override some methods or add new ones, depending on the
application's needs.

```kotlin
class MyToolServlet(app: ApplicationDirectory) : ToolServlet(app) {
  // Implement additional methods or override existing ones
}
```

This class can then be registered with the web application's servlet container to handle requests for the tools' URLs.

### Conclusion

`ToolServlet` provides a robust framework for managing and executing custom tools within a web application. Its flexible
design and security features make it suitable for applications requiring dynamic tool management and execution
capabilities.

# kotlin\com\simiacryptus\skyenet\webui\servlet\UserSettingsServlet.kt

## UserSettingsServlet Documentation

The `UserSettingsServlet` class is a part of the web UI module designed to handle user settings within a web
application. It extends `HttpServlet` and overrides the `doGet` and `doPost` methods to provide functionality for
retrieving and updating user settings, respectively. This servlet is designed to work within an application that
utilizes a cookie-based authentication system and a settings management system.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.servlet`
- **Dependencies**:
    - `com.simiacryptus.jopenai.util.JsonUtil` for JSON serialization and deserialization.
    - `com.simiacryptus.skyenet.core.platform.*` for accessing application services like authentication and user
      settings management.
    - `jakarta.servlet.http.*` for servlet functionalities.

### Functionality

#### doGet(HttpServletRequest req, HttpServletResponse resp)

This method is invoked when an HTTP GET request is made to the servlet's URL. It is responsible for displaying the
current user settings in a web form.

- **Parameters**:
    - `HttpServletRequest req`: The request object containing the request data.
    - `HttpServletResponse resp`: The response object used to send data back to the client.

- **Process**:
    1. Sets the response content type to `text/html`.
    2. Retrieves the user information based on the cookie provided in the request.
    3. If the user is not authenticated, it sets the response status to `SC_BAD_REQUEST`.
    4. For authenticated users, it retrieves the user settings, masks sensitive information, and displays them in a form
       within an HTML page.

- **Sensitive Information Handling**:
    - API keys are masked with a predefined mask value for security.
    - API base URLs are defaulted to `https://api.openai.com/v1` if not specified.

#### doPost(HttpServletRequest req, HttpServletResponse resp)

This method is invoked when an HTTP POST request is made to the servlet's URL, typically when the user submits the
settings form.

- **Parameters**:
    - `HttpServletRequest req`: The request object containing the form data.
    - `HttpServletResponse resp`: The response object used to send data back to the client.

- **Process**:
    1. Retrieves the user information based on the cookie provided in the request.
    2. If the user is not authenticated, it sets the response status to `SC_BAD_REQUEST`.
    3. For authenticated users, it parses the updated settings from the request, reconciles them with the previous
       settings (especially for masked fields like API keys), and updates the user settings in the system.
    4. Redirects the user to the root URL upon successful update.

- **Sensitive Information Handling**:
    - Handles the masking and unmasking of API keys to ensure that actual values are preserved unless explicitly changed
      by the user.

### Security Considerations

- The servlet relies on cookie-based authentication. Ensure that cookies are secured and handled properly to prevent
  unauthorized access.
- Sensitive information like API keys is masked in the user interface to prevent exposure. Care should be taken to
  ensure that this masking is securely implemented.

### Usage

This servlet is intended to be mapped to a specific URL pattern within a web application. Users can navigate to this URL
to view and update their settings. The actual URL mapping and integration depend on the web application's configuration.

### Conclusion

The `UserSettingsServlet` provides a secure and user-friendly interface for managing user settings within a web
application. It ensures that sensitive information is handled carefully while offering users the ability to update their
settings as needed.

# kotlin\com\simiacryptus\skyenet\webui\servlet\WelcomeServlet.kt

## WelcomeServlet Class Documentation

The `WelcomeServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends the `HttpServlet`
class to serve as the entry point for handling HTTP GET and POST requests for the SkyeNet web application. This servlet
is responsible for rendering the homepage, user information, user settings, and serving static resources based on the
request URI.

### Constructor

- `WelcomeServlet(ApplicationDirectory parent)`: Initializes a new instance of the `WelcomeServlet` class with a
  reference to the `ApplicationDirectory` that contains it. This reference is used to access other servlets and
  resources within the application.

### Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

Handles HTTP GET requests by determining the request URI and responding with the appropriate content. The method
supports rendering the homepage, redirecting to the user information page, and serving static resources.

- **Parameters:**
    - `HttpServletRequest req`: The request object containing the client's request information.
    - `HttpServletResponse resp`: The response object used to send data back to the client.

#### doPost(HttpServletRequest req, HttpServletResponse resp)

Handles HTTP POST requests, specifically for updating user settings. If the request URI starts with `/userSettings`, it
forwards the request to the `userSettingsServlet`. Otherwise, it responds with a 404 error.

- **Parameters:**
    - `HttpServletRequest req`: The request object containing the client's request information.
    - `HttpServletResponse resp`: The response object used to send data back to the client.

#### homepage(User user)

Generates the HTML content for the homepage, including the navigation bar, application list, and footer. It dynamically
renders markdown content for the welcome message and post-application list message.

- **Parameters:**
    - `User user`: The current user object, which may be `null` if the user is not authenticated.

- **Returns:** A `String` containing the HTML content for the homepage.

#### appRow(ApplicationDirectory.ChildWebApp app, User user)

Generates the HTML content for a row in the application list table. It checks if the user is authorized to access the
application and displays links for listing sessions and creating new sessions.

- **Parameters:**
    - `ApplicationDirectory.ChildWebApp app`: The application to generate a row for.
    - `User user`: The current user object, which may be `null` if the user is not authenticated.

- **Returns:** A `String` containing the HTML content for an application row in the list.

### Fields

- `protected open val welcomeMarkdown`: A markdown string that is rendered on the homepage above the application list.
- `protected open val postAppMarkdown`: A markdown string that is rendered on the homepage below the application list.

### Usage

To use the `WelcomeServlet`, it must be registered with a servlet container (e.g., Tomcat, Jetty) in the web
application's deployment descriptor (`web.xml`) or programmatically through a `ServletContext`. Once registered, it will
respond to HTTP GET and POST requests at its designated URL pattern.

### Security

The servlet uses the `ApplicationServices.authorizationManager` to check if the current user is authorized to perform
specific operations, such as reading or writing to an application. It ensures that sensitive actions and information are
protected and only accessible to authorized users.

# kotlin\com\simiacryptus\skyenet\webui\servlet\UserInfoServlet.kt

## UserInfoServlet Documentation

The `UserInfoServlet` class is a part of the web UI module designed to handle HTTP GET requests to retrieve user
information in JSON format. This servlet extends `HttpServlet` from the Jakarta Servlet API, enabling it to respond to
HTTP requests within a web application.

### Package

```plaintext
com.simiacryptus.skyenet.webui.servlet
```

### Dependencies

- `com.simiacryptus.jopenai.util.JsonUtil`: Utilized for converting user objects to JSON strings.
- `com.simiacryptus.skyenet.core.platform.ApplicationServices`: Provides access to application-wide services, including
  authentication management.
- `com.simiacryptus.skyenet.core.platform.User`: Represents the user entity whose information is to be retrieved.
- `com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie`: A helper method to extract cookies
  from the request, used for authentication purposes.
- `jakarta.servlet.http.HttpServlet`: The base class for HTTP servlets.
- `jakarta.servlet.http.HttpServletRequest`: Represents the client's request.
- `jakarta.servlet.http.HttpServletResponse`: Represents the response that the servlet sends to the client.

### Class Overview

#### UserInfoServlet

A servlet class designed to handle HTTP GET requests by providing user information in JSON format. It checks the user's
authentication status using a cookie and returns the user's details if authenticated.

##### Methods

- `doGet(HttpServletRequest req, HttpServletResponse resp)`: Handles the GET request by retrieving the user's
  information based on the authentication cookie and responding with the user details in JSON format.

### Usage

1. **Deployment**: This servlet needs to be deployed as part of a Java web application running in a servlet container (
   e.g., Tomcat, Jetty).
2. **Configuration**: Ensure that the servlet is mapped to a URL pattern in the web application's deployment
   descriptor (`web.xml`) or through annotations.
3. **Request Handling**: When a GET request is made to the servlet's mapped URL, the servlet attempts to authenticate
   the user based on a cookie.
4. **Response**:
    - If the user is not authenticated or the cookie is invalid, the servlet responds with an empty JSON object (`{}`).
    - If the user is authenticated, the servlet responds with the user's information in JSON format.

### Example Response

For an authenticated user, the response might look like:

```json
{
  "id": "12345",
  "username": "johndoe",
  "email": "johndoe@example.com"
}
```

For an unauthenticated request, the response will be:

```json
{}
```

### Security Considerations

- Ensure that sensitive user information is protected and not exposed unnecessarily.
- Validate and sanitize the cookie to prevent security vulnerabilities such as injection attacks.
- Implement proper error handling to avoid leaking information through error messages.

### Conclusion

The `UserInfoServlet` provides a straightforward way to retrieve authenticated user information in a web application. By
leveraging cookies for authentication, it ensures that user details are only provided to authenticated sessions,
enhancing the application's security.

# kotlin\com\simiacryptus\skyenet\webui\servlet\UsageServlet.kt

## UsageServlet Documentation

The `UsageServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends the `HttpServlet`
class to provide a web interface for displaying usage statistics related to OpenAI models. This servlet is designed to
present users with a summary of their usage, including the number of prompt and completion tokens used, as well as the
associated costs.

### Overview

When a GET request is made to this servlet, it determines whether the request includes a `sessionId` parameter. If
present, it fetches the session-specific usage summary; otherwise, it attempts to retrieve the user-specific usage
summary based on the user's cookie. The usage information is then displayed in an HTML table format.

### Key Methods

#### doGet(HttpServletRequest req, HttpServletResponse resp)

This method is overridden from the `HttpServlet` class and is called by the server to allow the servlet to handle a GET
request.

- **Parameters:**
    - `HttpServletRequest req`: The request object containing client request data.
    - `HttpServletResponse resp`: The response object used for sending data back to the client.

- **Functionality:**
    - Sets the content type of the response to "text/html".
    - Checks if the request contains a `sessionId` parameter:
        - If yes, retrieves the session usage summary and calls the `serve` method.
        - If no, retrieves the user information from the cookie and fetches the user usage summary, then calls
          the `serve` method.
    - If the user information is not found, sets the response status to `SC_BAD_REQUEST`.

#### serve(HttpServletResponse resp, Map<OpenAIModel, ApiModel.Usage> usage)

This private method generates and sends an HTML response containing a summary of usage statistics.

- **Parameters:**
    - `HttpServletResponse resp`: The response object used for sending data back to the client.
    - `Map<OpenAIModel, ApiModel.Usage> usage`: A map containing usage statistics for each OpenAI model.

- **Functionality:**
    - Calculates total prompt tokens, completion tokens, and cost from the usage map.
    - Constructs an HTML page displaying the usage statistics in a table format.
    - Writes the constructed HTML to the response writer.

### Usage Example

To use `UsageServlet`, it must be mapped to a URL pattern in the web application's deployment descriptor (web.xml) or
through annotations. Once mapped, it can be accessed by sending a GET request to the configured URL, optionally
including a `sessionId` parameter to retrieve session-specific usage statistics.

### Security Considerations

- Ensure that access to usage statistics is properly secured to prevent unauthorized access.
- Validate and sanitize the `sessionId` parameter to prevent injection attacks.

### Dependencies

- `com.simiacryptus.jopenai.models.OpenAIModel`
- `com.simiacryptus.skyenet.core.platform.ApplicationServices`
- `com.simiacryptus.skyenet.core.platform.Session`
- `com.simiacryptus.skyenet.webui.application.ApplicationServer`
- `jakarta.servlet.http.HttpServlet`
- `jakarta.servlet.http.HttpServletRequest`
- `jakarta.servlet.http.HttpServletResponse`

This documentation provides an overview of the `UsageServlet` class functionality and usage within a web application
context.

# kotlin\com\simiacryptus\skyenet\webui\servlet\ZipServlet.kt

## ZipServlet Class Documentation

The `ZipServlet` class is part of the `com.simiacryptus.skyenet.webui.servlet` package and extends the `HttpServlet`
class to provide functionality for dynamically creating and serving ZIP files based on files stored within a
session-specific directory. This servlet interacts with a storage interface to access session data and generates a ZIP
file containing the requested files, which is then sent back to the client.

### Dependencies

- `com.simiacryptus.skyenet.core.platform.*`: Utilizes classes for session management, authentication, and storage
  interaction.
- `jakarta.servlet.http.*`: For handling HTTP servlet requests and responses.
- `java.io.File`: For file manipulation.
- `java.util.zip.*`: For creating ZIP files.

### Constructor

```kotlin
ZipServlet(val dataStorage : StorageInterface)
```

- **Parameters:**
    - `dataStorage`: An implementation of `StorageInterface` to interact with the application's data storage.

### Public Methods

#### `doGet(HttpServletRequest req, HttpServletResponse resp)`

Handles the GET request by creating a ZIP file containing the requested files from the session directory and sending it
to the client.

- **Parameters:**
    - `req`: The `HttpServletRequest` object that contains the request the client made to the servlet.
    - `resp`: The `HttpServletResponse` object that contains the response the servlet returns to the client.

- **Process Flow:**
    1. Extracts the session ID and path from the request parameters.
    2. Validates the requested path.
    3. Determines the session directory based on the session ID and authenticated user.
    4. Creates a temporary ZIP file and writes the requested files into it.
    5. Sets the response content type to `application/zip` and sends the ZIP file content back to the client.

### Private Methods

#### `write(basePath: File, file: File, zip: ZipOutputStream)`

Recursively writes files and directories from the specified path into the ZIP output stream.

- **Parameters:**
    - `basePath`: The base directory from which the relative paths of files in the ZIP are calculated.
    - `file`: The current file or directory to add to the ZIP file.
    - `zip`: The `ZipOutputStream` to which the file data is written.

- **Functionality:**
    - If the `file` is a regular file, it is added to the ZIP file with its relative path from `basePath`.
    - If the `file` is a directory, the method is called recursively for each file within the directory, excluding
      hidden files (those starting with a dot).

### Usage Example

To use `ZipServlet`, it must be mapped to a URL pattern in your web application's deployment descriptor or through
annotations. Once mapped, it can be accessed by sending a GET request to the mapped URL with the required
parameters (`session` and optionally `path`).

```kotlin
val storageInterface: StorageInterface = // Obtain an implementation of StorageInterface
val zipServlet = ZipServlet(storageInterface)
```

Ensure that the servlet is properly initialized and configured within your web application to handle requests.

### Security Considerations

- Ensure that proper authentication and authorization checks are performed to prevent unauthorized access to sensitive
  files.
- Validate and sanitize input parameters (`session` and `path`) to avoid path traversal vulnerabilities.

This documentation provides an overview of the `ZipServlet` class's functionality and usage within a web application for
dynamically serving ZIP files based on session-specific data.

# kotlin\com\simiacryptus\skyenet\webui\session\SocketManager.kt

## SocketManager Interface Documentation

The `SocketManager` interface is a crucial component within the `com.simiacryptus.skyenet.webui.session` package,
designed to manage WebSocket connections for chat functionalities. It provides a structured way to handle chat sockets,
including adding and removing sockets, handling incoming text messages, and retrieving chat history.

### Interface Overview

```kotlin
package com.simiacryptus.skyenet.webui.session

import com.simiacryptus.skyenet.webui.chat.ChatSocket
import org.eclipse.jetty.websocket.api.Session

interface SocketManager {
  fun removeSocket(socket: ChatSocket)
  fun addSocket(socket: ChatSocket, session: Session)
  fun getReplay(): List<String>
  fun onWebSocketText(socket: ChatSocket, message: String)
}
```

### Methods

#### `removeSocket(socket: ChatSocket)`

Removes a specified `ChatSocket` from the manager. This method is typically called when a WebSocket connection is closed
or needs to be terminated for any reason.

- **Parameters:**
    - `socket: ChatSocket` - The chat socket instance to be removed.

#### `addSocket(socket: ChatSocket, session: Session)`

Adds a new `ChatSocket` to the manager, associating it with a specific `Session`. This method is usually called when a
new WebSocket connection is established.

- **Parameters:**
    - `socket: ChatSocket` - The chat socket instance to be added.
    - `session: Session` - The session associated with the WebSocket connection.

#### `getReplay(): List<String>`

Retrieves the chat history. This method returns a list of strings, each representing a message in the chat history. This
can be used to replay chat messages to a newly connected client.

- **Returns:** A `List<String>` containing the chat history.

#### `onWebSocketText(socket: ChatSocket, message: String)`

Handles incoming text messages from a WebSocket connection. This method is called whenever a text message is received
from a client.

- **Parameters:**
    - `socket: ChatSocket` - The chat socket through which the message was received.
    - `message: String` - The text message received from the client.

### Usage Example

Below is a hypothetical example of how the `SocketManager` interface might be implemented and used within a chat
application:

```kotlin
class ChatSocketManager : SocketManager {
  private val sockets = mutableListOf<ChatSocket>()
  private val chatHistory = mutableListOf<String>()

  override fun addSocket(socket: ChatSocket, session: Session) {
    sockets.add(socket)
    // Optionally, send chat history to the newly connected client
    socket.sendMessages(getReplay())
  }

  override fun removeSocket(socket: ChatSocket) {
    sockets.remove(socket)
  }

  override fun getReplay(): List<String> = chatHistory

  override fun onWebSocketText(socket: ChatSocket, message: String) {
    chatHistory.add(message)
    // Broadcast the message to all connected clients
    sockets.forEach { it.sendMessage(message) }
  }
}
```

This example demonstrates a basic implementation of the `SocketManager` interface, managing chat sockets, maintaining a
chat history, and broadcasting messages to all connected clients.

### Conclusion

The `SocketManager` interface plays a fundamental role in managing WebSocket connections for chat functionalities within
the application. By defining a clear contract for adding, removing, and handling chat sockets, it facilitates the
development of robust and scalable chat features.

# kotlin\com\simiacryptus\skyenet\webui\session\SocketManagerBase.kt

## SocketManagerBase Class Documentation

The `SocketManagerBase` class is an abstract class designed to manage WebSocket connections for a chat application,
handling message sending, receiving, and authorization. It serves as a foundation for building WebSocket managers that
require session management, user authentication, and message handling capabilities.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.session`
- **Imports**: Various, including platform services, chat utilities, and standard Java utilities.
- **Inheritance**: Implements the `SocketManager` interface.

### Key Components

#### Fields

- `session`: Represents the current user session.
- `dataStorage`: Interface for data storage operations, nullable.
- `owner`: The user who owns this session, nullable.
- `messageStates`: A map storing the state of messages.
- `applicationClass`: The class of the application for authorization purposes.
- `sockets`: A map linking `ChatSocket` instances to Jetty WebSocket sessions.
- `sendQueues`: A map managing message queues for each `ChatSocket`.
- `messageVersions`: Tracks version numbers for messages to manage updates.

#### Constructor

The constructor initializes the class with a session, optional data storage, an owner, a map of message states, and the
application class. It retrieves initial message states from the data storage if available.

#### Methods

##### Public

- `removeSocket(socket: ChatSocket)`: Removes a WebSocket from the manager.
- `addSocket(socket: ChatSocket, session: org.eclipse.jetty.websocket.api.Session)`: Adds a WebSocket to the manager
  after authorization checks.
- `send(out: String)`: Sends a message to all connected WebSockets.
- `getReplay()`: Retrieves a list of all messages with their current state and version.
- `newTask(cancelable: Boolean = false)`: Initializes a new task, optionally cancelable, and returns a `SessionTask`
  instance.
- `hrefLink(linkText: String, classname: String, handler: Consumer<Unit>)`: Generates HTML for a hyperlink that triggers
  a specified action.
- `textInput(handler: Consumer<String>)`: Generates HTML for a text input form that triggers a specified action.

##### Protected

- `onRun(userMessage: String, socket: ChatSocket)`: Abstract method to be implemented by subclasses, defining behavior
  for incoming messages.
- `canWrite(user: User?)`: Checks if the given user has write access.

##### Private

- `publish(out: String)`: Internal method to queue messages for sending.
- `setMessage(key: String, value: String)`: Updates or sets the value of a message, returning its new version.
- `onCmd(id: String, code: String)`: Handles special command messages.

#### Inner Classes

- `SessionTaskImpl`: Implementation of `SessionTask`, handling task-specific operations like sending messages and saving
  files.

### Utility Functions

- `randomID()`: Generates a random ID string.
- `divInitializer(operationID: String, cancelable: Boolean)`: Generates initial HTML for a task, including a cancel
  button if requested.
- `getUser(session: org.eclipse.jetty.websocket.api.Session)`: Retrieves the `User` associated with a WebSocket session.

### Usage

This class is designed to be extended by specific WebSocket manager implementations that require session and user
management, message handling, and authorization. Implementors will need to provide functionality for the
abstract `onRun` method to define how incoming messages are processed.

### Example

```kotlin
class MySocketManager(session: Session, dataStorage: StorageInterface?, owner: User?) :
  SocketManagerBase(session, dataStorage, owner) {
  override fun onRun(userMessage: String, socket: ChatSocket) {
    // Implementation for handling user messages
  }
}
```

This example demonstrates how to extend `SocketManagerBase` to create a custom WebSocket manager that processes user
messages according to application-specific logic.

# kotlin\com\simiacryptus\skyenet\webui\test\ImageActorTestApp.kt

## ImageActorTestApp Developer Documentation

The `ImageActorTestApp` class is an extension of the `ApplicationServer` designed to facilitate testing and interaction
with `ImageActor` instances within a web application context. This class allows users to send messages to
an `ImageActor` and receive responses, including text and images, through a web interface.

### Overview

- **Package**: `com.simiacryptus.skyenet.webui.test`
- **Imports**: Utilizes a variety of imports from `com.simiacryptus.jopenai`, `com.simiacryptus.skyenet.core`,
  and `com.simiacryptus.skyenet.webui` packages, among others.
- **Dependencies**: Requires an instance of `ImageActor`, `ApplicationInterface`, and `API` for its operations.

### Key Components

#### Constructor

The constructor initializes the `ImageActorTestApp` with a specific `ImageActor` instance and optional parameters for
application name and temperature. The application name defaults to "ImageActorTest_" followed by the simple name of
the `ImageActor` class. The temperature parameter influences the behavior of the `ImageActor` but is set to 0.3 by
default.

```kotlin
ImageActorTestApp(
  private val actor : ImageActor,
applicationName: String = "ImageActorTest_" + actor.javaClass.simpleName,
temperature: Double = 0.3,
)
```

#### Settings Data Class

Defines a data class `Settings` with a nullable `ImageActor` property. This class is used to manage application-specific
settings.

```kotlin
data class Settings(
  val actor: ImageActor? = null,
)
```

#### Overridden Methods

##### initSettings

Initializes settings for a session by returning an instance of the `Settings` data class with the `ImageActor` specified
at construction.

```kotlin
override fun <T : Any> initSettings(session: Session): T = Settings(actor = actor) as T
```

##### userMessage

Handles messages from users. It sets a budget for the API client, processes the user message, sends it to
the `ImageActor`, and then displays the response through the UI. It also handles any errors that occur during this
process.

```kotlin
override fun userMessage(
  session: Session,
  user: User?,
  userMessage: String,
  ui: ApplicationInterface,
  api: API
)
```

#### Companion Object

Contains a logger for the class, used to log warnings and errors.

```kotlin
companion object {
  private val log = LoggerFactory.getLogger(ImageActorTestApp::class.java)
}
```

### Usage

To use `ImageActorTestApp`, an instance of `ImageActor` must be provided. This instance is then used to interact with
users through a web interface, where users can send messages and receive responses that include both text and images.

### Error Handling

Errors during message processing are caught and logged using the class's logger. Additionally, error messages are
displayed to the user through the UI.

### Conclusion

The `ImageActorTestApp` class provides a structured way to test and interact with `ImageActor` instances within a web
application, facilitating the development and debugging of image-based conversational agents.

# kotlin\com\simiacryptus\skyenet\webui\test\CodingActorTestApp.kt

## CodingActorTestApp Class Documentation

The `CodingActorTestApp` class extends the `ApplicationServer` to create a specialized server application designed to
test `CodingActor` instances. This application allows users to send messages, which are then processed by
the `CodingActor` to generate code responses. These responses can be executed if the user has the necessary permissions.

### Constructor

```kotlin
CodingActorTestApp(
  private val actor : CodingActor,
applicationName: String = "CodingActorTest_" + actor.name,
temperature: Double = 0.3,
)
```

#### Parameters:

- `actor`: The `CodingActor` instance that will be used to generate code responses.
- `applicationName`: The name of the application. Defaults to "CodingActorTest_" followed by the name of the actor.
- `temperature`: A parameter influencing the randomness of the generated code. Lower values make the code more
  deterministic.

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

Handles messages sent by users. It processes the user message through the `CodingActor`, checks if the user has
execution permissions, and displays the generated code along with an optional execution link.

##### Parameters:

- `session`: The current user session.
- `user`: The user sending the message. Can be `null`.
- `userMessage`: The message sent by the user.
- `ui`: The application interface used to interact with the user interface.
- `api`: The API instance used for making external API calls.

### Usage

1. **Initialization**: Create an instance of `CodingActorTestApp` by providing a `CodingActor` instance and optionally
   specifying the application name and temperature.

2. **User Interaction**: The application listens for messages from users. When a message is received, it is processed as
   follows:
    - The message is echoed back to the user.
    - The `CodingActor` generates a code response based on the user message.
    - The application checks if the user has permission to execute the generated code.
    - If execution is permitted, a link is provided to execute the code, and the results are displayed.

3. **Error Handling**: If an error occurs during message processing, it is logged, and an error message is displayed to
   the user.

### Companion Object

- `log`: A logger instance for logging error messages.

### Example

```kotlin
val codingActor = CodingActor(/* initialization parameters */)
val testApp = CodingActorTestApp(codingActor)
testApp.start()
```

This example creates a new instance of `CodingActorTestApp` with a specified `CodingActor` and starts the application
server to listen for user messages.

### Note

This class requires external dependencies such as `ApplicationServer`, `CodingActor`, and `API` to be properly set up in
your project environment. Ensure that all necessary permissions and configurations are in place for the application to
function correctly.

# kotlin\com\simiacryptus\skyenet\webui\test\ParsedActorTestApp.kt

## ParsedActorTestApp Class Documentation

The `ParsedActorTestApp` class is a specialized application server designed for testing `ParsedActor` instances within
the Skyenet framework. It extends the `ApplicationServer` class, providing a web interface to interact with and test the
responses of a given `ParsedActor`.

### Overview

This class is designed to facilitate the testing of `ParsedActor` instances by allowing users to send messages through a
web interface and view the responses generated by the `ParsedActor`. It integrates with the Skyenet core platform and
utilizes the JOpenAI library for processing natural language inputs.

### Constructor

```kotlin
ParsedActorTestApp < T : Any > (
    private
val actor: ParsedActor<T>,
applicationName: String = "ParsedActorTest_" + actor.resultClass.simpleName,
temperature: Double = 0.3,
)
```

#### Parameters

- `actor`: The `ParsedActor` instance to be tested.
- `applicationName`: (Optional) The name of the application, defaulting to "ParsedActorTest_" followed by the simple
  name of the result class of the actor.
- `temperature`: (Optional) A parameter influencing the randomness of the response generation, defaulting to 0.3.

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

Handles messages sent by users through the web interface. It processes the user's message using the `ParsedActor`,
generates a response, and displays it back to the user.

##### Parameters

- `session`: The current session object.
- `user`: The user object, which can be `null`.
- `userMessage`: The message string sent by the user.
- `ui`: The application interface for interacting with the UI.
- `api`: The API client used for processing the message.

### Companion Object

#### Properties

- `log`: A logger instance for logging warnings and errors.

### Usage Example

To use `ParsedActorTestApp`, you first need to instantiate a `ParsedActor` with the desired configuration. Then, create
an instance of `ParsedActorTestApp` with the `ParsedActor` as a parameter. Finally, start the application server to
begin testing.

```kotlin
val myActor = ParsedActor<MyResultType>(...)
val testApp = ParsedActorTestApp(myActor)
testApp.start()
```

This will start a web server where users can send messages to be processed by the `ParsedActor`, and view the generated
responses.

### Conclusion

The `ParsedActorTestApp` class provides a convenient way to test and interact with `ParsedActor` instances, making it
easier for developers to debug and refine their natural language processing applications within the Skyenet framework.

# kotlin\com\simiacryptus\skyenet\webui\test\SimpleActorTestApp.kt

## SimpleActorTestApp Documentation

The `SimpleActorTestApp` class is a part of the `com.simiacryptus.skyenet.webui.test` package, designed to integrate
a `SimpleActor` into a web-based application server environment. This class extends the `ApplicationServer` to provide a
specialized application that interacts with users through a web interface, leveraging the capabilities of
a `SimpleActor` for processing user messages.

### Overview

The `SimpleActorTestApp` class is designed to facilitate testing and interaction with instances of `SimpleActor`. It
provides a web UI for users to send messages to the `SimpleActor`, and receive responses. This setup is useful for
debugging, testing, or demonstrating the capabilities of `SimpleActor` implementations.

### Key Components

#### Constructor

The constructor initializes the application with a specific `SimpleActor` instance, an optional application name, and a
temperature parameter for the actor's response generation process.

```kotlin
SimpleActorTestApp(
  private val actor : SimpleActor,
applicationName: String = "SimpleActorTest_" + actor.javaClass.simpleName,
temperature: Double = 0.3
)
```

- `actor`: The `SimpleActor` instance to be used for answering user messages.
- `applicationName`: An optional name for the application, defaulting to "SimpleActorTest_" followed by the simple name
  of the actor's class.
- `temperature`: A parameter influencing the randomness of the actor's responses, with a default value of 0.3.

#### Settings Data Class

The `Settings` data class encapsulates the configuration settings for the application, currently only including
the `SimpleActor` instance.

```kotlin
data class Settings(
  val actor: SimpleActor? = null,
)
```

#### User Message Handling

The `userMessage` method is overridden to process messages from users. It sets a budget for the API client, sends the
user's message to the `SimpleActor`, and returns the actor's response.

```kotlin
override fun userMessage(
  session: Session,
  user: User?,
  userMessage: String,
  ui: ApplicationInterface,
  api: API
)
```

- `session`: The current user session.
- `user`: The user sending the message, which can be `null`.
- `userMessage`: The message from the user.
- `ui`: The application interface for interacting with the UI.
- `api`: The API client for external communications.

#### Logging

A companion object provides a logger for the class, facilitating logging throughout the application lifecycle.

```kotlin
companion object {
  private val log = LoggerFactory.getLogger(SimpleActorTestApp::class.java)
}
```

### Usage

To use `SimpleActorTestApp`, instantiate it with a `SimpleActor` and optionally specify an application name and
temperature. Deploy the application on a server capable of running Kotlin applications, and navigate to the specified
path (`/simpleActorTest` by default) to interact with the `SimpleActor` through the web UI.

### Conclusion

The `SimpleActorTestApp` class provides a convenient way to test and demonstrate the capabilities of `SimpleActor`
instances within a web application context. By extending the `ApplicationServer`, it integrates seamlessly with web UI
components, allowing for interactive user engagement.

# kotlin\com\simiacryptus\skyenet\webui\util\MarkdownUtil.kt

## MarkdownUtil Documentation

The `MarkdownUtil` object in the `com.simiacryptus.skyenet.webui.util` package provides utility functions for rendering
Markdown content into HTML. It leverages the Flexmark Java library to parse and render Markdown. This utility is
designed to enhance the display of Markdown content by optionally incorporating interactive tabs for different content
views, such as separating visual diagrams from their source code or providing a side-by-side view of Markdown and its
HTML rendering.

### Functions

#### renderMarkdown

```kotlin
fun renderMarkdown(markdown: String, options: MutableDataSet = defaultOptions(), tabs: Boolean = true): String
```

Renders the given Markdown string into HTML. It allows customization of the parsing and rendering process
through `options` and can optionally wrap the output in interactive tabs for enhanced viewing.

##### Parameters:

- `markdown`: The Markdown content to be rendered as a `String`. If this parameter is blank, the function returns an
  empty string.
- `options`: A `MutableDataSet` specifying options for the Markdown parser and renderer. Defaults to `defaultOptions()`
  if not provided.
- `tabs`: A `Boolean` indicating whether to wrap the rendered HTML and the original Markdown in interactive tabs.
  Defaults to `true`.

##### Returns:

A `String` containing the rendered HTML. If `tabs` is `true`, the HTML includes additional markup for interactive tabs.

##### Usage Example:

```kotlin
val markdownContent = "# Hello World\nThis is a sample Markdown."
val htmlContent = MarkdownUtil.renderMarkdown(markdownContent)
println(htmlContent)
```

#### defaultOptions

```kotlin
private fun defaultOptions(): MutableDataSet
```

Generates a default set of options for the Markdown parser and renderer. This includes enabling table support via
the `TablesExtension`.

##### Returns:

A `MutableDataSet` with the default configuration for parsing and rendering Markdown.

### Implementation Details

- The `renderMarkdown` function first checks if the provided Markdown string is blank, returning an empty string if
  true.
- It then creates a parser and renderer using the provided or default options.
- The Markdown content is parsed into a document, which is then rendered into HTML.
- If the `tabs` parameter is true, the function replaces specific HTML patterns (e.g., code blocks for mermaid diagrams)
  with tabbed interfaces. This allows users to switch between different views (e.g., diagram and source code).
- Additionally, if `tabs` is true, the entire content is wrapped in another layer of tabs, separating the rendered HTML,
  the original Markdown, and an option to hide the content.
- The function uses regular expressions to safely insert the Markdown content into the HTML template, escaping `<`
  and `>` characters to prevent HTML injection.

### Notes

- The `@Language("HTML")` annotation is used to inform the IDE about the language of the string literals for better
  syntax highlighting and error checking.
- The `RegexOption.DOT_MATCHES_ALL` option allows the dot (`.`) in regular expressions to match newline characters,
  enabling the pattern to match multi-line code blocks.

# kotlin\com\simiacryptus\skyenet\webui\session\SessionTask.kt

## SessionTask Class Documentation

The `SessionTask` class is an abstract class designed to manage and display task outputs in a web UI environment. It
provides a structured way to append messages, errors, images, and other HTML elements to the task output, with support
for real-time updates through a spinner animation. This class is part of the `com.simiacryptus.skyenet.webui.session`
package.

### Overview

`SessionTask` serves as a base class for tasks that require output to be displayed to the user in a web interface. It
manages a buffer of messages and provides methods to append different types of content to this buffer. The class also
handles the display of a loading spinner to indicate that a task is in progress.

### Properties

- `buffer`: A mutable list of `StringBuilder` objects that holds the HTML content to be displayed.
- `spinner`: A string representing the HTML for a loading spinner animation.

### Methods

#### Abstract Methods

- `send(html: String)`: Sends the current HTML content to be displayed. Implementations should define how this content
  is displayed in the UI.
- `saveFile(relativePath: String, data: ByteArray)`: Saves the given data to a file and returns the URL of the file.
  Implementations should handle the file saving process and URL generation.

#### Public Methods

- `add(message: String, showSpinner: Boolean, tag: String, className: String)`: Adds a message to the task output with
  customizable HTML tag and CSS class.
- `hideable(ui: ApplicationInterface, message: String, showSpinner: Boolean, tag: String, className: String)`: Adds a
  hideable message to the task output, which can be dismissed by the user.
- `echo(message: String, showSpinner: Boolean, tag: String)`: Echos a user message to the task output, using a specific
  HTML tag.
- `header(message: String, showSpinner: Boolean, tag: String, classname: String)`: Adds a header to the task output.
- `verbose(message: String, showSpinner: Boolean, tag: String)`: Adds a verbose message to the task output, intended for
  detailed or debug information.
- `error(ui: ApplicationInterface, e: Throwable, showSpinner: Boolean, tag: String)`: Displays an error message in the
  task output, with support for rendering exceptions in a user-friendly format.
- `complete(message: String, tag: String, className: String)`: Displays a final message in the task output and hides the
  spinner.
- `image(image: BufferedImage)`: Displays an image in the task output by saving the image to a file and embedding it
  using an `<img>` tag.

#### Companion Object

The companion object contains a logger instance for the class and a constant for the spinner HTML. It also includes an
extension function `toPng()` for `BufferedImage` objects to convert them to PNG format as a byte array.

### Usage

To use the `SessionTask` class, one must extend it and implement the abstract methods `send(html: String)`
and `saveFile(relativePath: String, data: ByteArray)`. Once implemented, the
various `add`, `echo`, `header`, `verbose`, `error`, `complete`, and `image` methods can be used to append content to
the task output.

#### Example

```kotlin
class MySessionTask : SessionTask() {
  override fun send(html: String) {
    // Implementation to send HTML to the UI
  }

  override fun saveFile(relativePath: String, data: ByteArray): String {
    // Implementation to save a file and return its URL
  }
}
```

In this example, `MySessionTask` extends `SessionTask` and provides implementations for the abstract methods. This setup
allows for the appending of messages, errors, images, and other content to the task output in a structured and
interactive manner.

# kotlin\com\simiacryptus\skyenet\webui\util\EncryptFiles.kt

## EncryptFiles Utility Documentation

The `EncryptFiles` utility is a part of the `com.simiacryptus.skyenet.webui.util` package, designed to encrypt files
using AWS Key Management Service (KMS) and write the encrypted data to a specified location. This utility leverages
the `ApplicationServices` for encryption, making it a convenient tool for securing sensitive information.

### Overview

The utility consists of a Kotlin object named `EncryptFiles` that contains a `main` function. This function demonstrates
how to encrypt a string (in this case, an empty string for demonstration purposes) using a specified AWS KMS key and
then write the encrypted data to a file. Additionally, two extension functions, `write` and `encrypt`, are provided to
facilitate writing byte data to a file and encrypting strings, respectively.

### Usage

#### Prerequisites

- AWS Account: Ensure you have an AWS account and have access to the AWS Key Management Service (KMS).
- AWS KMS Key: You need the ARN (Amazon Resource Name) of the KMS key that will be used for encryption.
- Kotlin Environment: The utility is written in Kotlin, so ensure your development environment supports Kotlin projects.

#### Encrypting and Writing Data

1. **Main Function**: The `main` function in the `EncryptFiles` object is the entry point. It demonstrates encrypting a
   string and writing the encrypted data to a file. Replace the empty string `""` with the content you wish to encrypt
   and specify the correct KMS key ARN in place
   of `"arn:aws:kms:us-east-1:470240306861:key/a1340b89-64e6-480c-a44c-e7bc0c70dcb1"`.

2. **Writing to a File**: The `write` extension function on `String` takes a file path as its argument and writes the
   calling string to the specified path. Ensure the path is correctly set to where you want the encrypted file to be
   saved.

3. **Encrypting Data**: The `encrypt` extension function on `String` takes a KMS key ARN as its argument and returns the
   encrypted version of the calling string. It utilizes the `ApplicationServices.cloud!!.encrypt` method for encryption,
   which must be properly configured to interact with AWS KMS.

#### Extension Functions

- **write(outpath: String)**: Writes the calling string to the specified output path. It converts the string to a byte
  array before writing.

- **encrypt(keyId: String)**: Encrypts the calling string using the specified AWS KMS key. The string is first encoded
  to a byte array, which is then encrypted. If encryption fails, a `RuntimeException` is thrown.

### Example

```kotlin
fun main() {
  // Example content to encrypt
  val content = "Hello, World!"

  // Encrypt the content using a specified KMS key
  val encryptedContent = content.encrypt("arn:aws:kms:your-region:your-account-id:key/your-key-id")

  // Write the encrypted content to a file
  encryptedContent.write("/path/to/your/encrypted_file.kms")
}
```

Replace `"Hello, World!"` with the actual content you wish to encrypt, and adjust the KMS key ARN and file path
accordingly.

### Conclusion

The `EncryptFiles` utility provides a straightforward way to encrypt data using AWS KMS and write the encrypted data to
a file. It is essential to handle encryption keys and sensitive data securely and ensure that AWS KMS permissions are
correctly configured for your application.

# kotlin\com\simiacryptus\skyenet\webui\util\OpenAPI.kt

## Skyenet WebUI Util - OpenAPI Data Classes Documentation

This documentation provides an overview of the data classes used in the Skyenet WebUI Util package for representing
OpenAPI specifications. These classes are designed to model the structure of OpenAPI documents, allowing for easy
serialization and deserialization of API specifications.

### Overview

The OpenAPI specification is a standard format for describing RESTful APIs. The classes documented here represent the
various components of an OpenAPI document, including information about the API itself, its paths, operations, and
reusable components.

#### OpenAPI

The root document of an OpenAPI specification.

- **openapi**: The OpenAPI specification version.
- **info**: Metadata about the API.
- **paths**: The available paths and operations for the API.
- **components**: Reusable components for the API specification.

#### Info

Metadata about the API.

- **title**: The title of the application.
- **version**: The version of the OpenAPI document.
- **description**: A short description of the application.
- **termsOfService**: A URL to the Terms of Service for the API.
- **contact**: Contact information for the API.
- **license**: License information for the API.

#### Contact

Contact information.

- **name**: The identifying name of the contact person/organization.
- **url**: The URL pointing to the contact information.
- **email**: The email address of the contact person/organization.

#### License

License information for the API.

- **name**: The name of the license.
- **url**: The URL to the license text.

#### PathItem

Defines an API endpoint and its operations.

- **get, put, post, delete, options, head, patch**: The operations available on this path.

#### Operation

An API operation (endpoint).

- **summary**: A short summary of what the operation does.
- **description**: A verbose explanation of the operation behavior.
- **responses**: The list of possible responses from the operation.
- **parameters**: A list of parameters that are applicable for this operation.
- **operationId**: Unique string used to identify the operation.
- **requestBody**: The request body applicable for the operation.
- **security**: A declaration of which security mechanisms can be used for this operation.
- **tags**: A list of tags for API documentation control.
- **callbacks**: Possible callbacks for the operation.
- **deprecated**: Declares this operation to be deprecated.

#### Response

Describes a single response from an API Operation.

- **description**: A short description of the response.
- **content**: A map containing descriptions of potential response payloads.

#### Components

Holds a set of reusable objects for different aspects of the OpenAPI spec.

- **schemas, responses, parameters, examples, requestBodies, headers, securitySchemes, links, callbacks**: Maps of their
  respective types, allowing for reuse across the API.

#### Schema

Defines a schema for a parameter or response body.

- **type**: The type of the schema.
- **properties**: A map containing the properties of the schema.
- **items**: A schema or reference to a schema for items in an array.
- **`$ref`**: A reference to a defined schema.
- **format**: The extending format for the previously mentioned type.
- **description**: A short description of the schema.

#### Parameter

Describes a single operation parameter.

- **name**: The name of the parameter.
- **in**: The location of the parameter.
- **description**: A brief description of the parameter.
- **required**: Determines whether this parameter is mandatory.
- **schema**: The schema defining the type used for the parameter.
- **content**: A map containing the representations for the parameter.
- **example**: An example of the parameter's potential value.

#### Example, RequestBody, Header, SecurityScheme, Link, Callback, MediaType

These classes represent various components that can be used within the OpenAPI specification, such as examples of
request bodies, headers, security schemes, links between operations, callbacks for webhooks, and media types for request
and response content.

Each of these classes has fields appropriate to their use within the API specification, providing a comprehensive
toolkit for defining and documenting APIs in a structured and reusable manner.

# kotlin\com\simiacryptus\skyenet\webui\util\TensorflowProjector.kt

## TensorflowProjector Class Documentation

The `TensorflowProjector` class is designed to facilitate the generation of embeddings using OpenAI's API and to
visualize these embeddings using TensorFlow's Embedding Projector. This class provides a seamless integration between
the storage of embedding vectors, metadata, and the generation of a web-based visualization.

### Constructor

The constructor of the `TensorflowProjector` class requires several parameters for initialization:

- `api`: An instance of `API` from the `com.simiacryptus.jopenai` package, used for generating embeddings.
- `dataStorage`: An implementation of `StorageInterface` for storing the generated files.
- `sessionID`: A `Session` object representing the current session.
- `host`: A `String` representing the host address.
- `session`: An instance of `ApplicationInterface` representing the current application session.
- `userId`: An optional `User` object representing the current user.

### Methods

#### `toVectorMap(vararg words: String): Map<String, DoubleArray>`

This private method generates a map of word embeddings. Each word in the input is mapped to its corresponding embedding
vector.

- **Parameters**: `words` - A variable number of `String` objects representing the words to be embedded.
- **Returns**: A `Map<String, DoubleArray>` where each key is a word and its value is the embedding vector.

#### `writeTensorflowEmbeddingProjectorHtml(vararg words: String): String`

Generates the necessary files for visualizing embeddings in TensorFlow's Embedding Projector and returns HTML code to
embed the projector in a web page.

- **Parameters**: `words` - A variable number of `String` objects representing the words to be visualized.
- **Returns**: A `String` containing HTML code. This code includes links to the generated files (vectors, metadata, and
  projector configuration) and an iframe embedding the TensorFlow Projector.

### Usage Example

```kotlin
val apiClient = OpenAIClient("YourAPIKey")
val storage = YourStorageImplementation()
val sessionID = Session("YourSessionID")
val host = "http://yourhost.com"
val session = YourApplicationInterface()
val userId = User("YourUserID")

val projector = TensorflowProjector(apiClient, storage, sessionID, host, session, userId)
val words = arrayOf("word1", "word2", "word3")
val html = projector.writeTensorflowEmbeddingProjectorHtml(*words)

// Output the HTML to your web page
println(html)
```

### Notes

- Ensure that the `API` instance (`apiClient` in the example) is properly authenticated with your OpenAI API key.
- The `StorageInterface` (`storage` in the example) should be implemented to handle file storage according to your
  application's requirements.
- The generated HTML code can be embedded in any web page to display the TensorFlow Embedding Projector with your
  embeddings.

This class simplifies the process of generating and visualizing embeddings, making it accessible to integrate advanced
natural language processing features into your applications.

# kotlin\com\simiacryptus\skyenet\webui\util\Selenium2S3.kt

## Developer Documentation for Selenium2S3 Class

The `Selenium2S3` class is a comprehensive utility designed for web scraping, content processing, and saving the fetched
data to an S3-compatible storage system. It leverages Selenium for web interaction, Apache HttpClient for asynchronous
HTTP requests, and Jsoup for HTML parsing.

### Overview

This class is part of the `com.simiacryptus.skyenet.webui.util` package and extends the `Selenium` interface. It is
designed to navigate web pages, process content, and save the processed data to cloud storage. The class supports
handling HTML, JSON, and media content types, and it provides mechanisms for link replacement and content editing.

### Key Features

- **Web Navigation and Content Fetching**: Uses Selenium WebDriver for navigating web pages and fetching content.
- **Asynchronous HTTP Requests**: Utilizes Apache HttpClient for asynchronous fetching of web resources.
- **Content Processing**: Offers methods for processing HTML and JSON content, including link replacement and content
  editing.
- **Cloud Storage Integration**: Supports saving processed content to an S3-compatible cloud storage system.
- **Customizable WebDriver**: Allows customization of the Selenium WebDriver, including headless operation and image
  loading preferences.

### Initialization

The class constructor accepts a `ThreadPoolExecutor` for managing asynchronous tasks and an optional array of cookies
for session management.

```kotlin
val selenium2S3 = Selenium2S3(Executors.newCachedThreadPool() as ThreadPoolExecutor, cookies)
```

### Core Methods

#### save

The `save` method is the primary entry point for processing and saving web content. It navigates to a specified URL,
processes the page content, and saves the processed content to cloud storage.

```kotlin
selenium2S3.save(URL("http://example.com"), "filename.html", "saveRoot")
```

#### process

The `process` method handles the processing of individual links found on the web page. It determines the content type
and delegates to the appropriate method for further processing.

#### getHtml, getJson, getMedia

These methods are responsible for fetching and processing HTML, JSON, and media content, respectively. They perform
asynchronous HTTP requests and process the responses.

#### saveJS, saveHTML

These methods handle the final processing and saving of JavaScript and HTML content to cloud storage.

### Utility Methods

- **get**: Prepares an HTTP GET request with cookies.
- **currentPageLinks**: Extracts links from the current page using Selenium WebDriver or Jsoup.
- **toAbsolute**, **toRelative**: Methods for converting links to absolute or relative forms.
- **toArchivePath**: Transforms links to a format suitable for archiving.
- **validate**: Validates the content type of fetched resources.
- **mimeType**: Determines the MIME type based on file extension.
- **editPage**: Performs custom edits on an HTML document.

### Closing Resources

The `close` method ensures proper shutdown of the WebDriver and HttpClient, releasing all associated resources.

### Companion Object

The companion object includes utility methods for initializing the Chrome WebDriver and managing cookies.

#### chromeDriver

A static method for configuring and initializing a Chrome WebDriver instance.

#### setCookies

A static method for setting cookies in the WebDriver.

### Usage Example

```kotlin
val cookies = arrayOf(/* Your cookies here */)
val executor = Executors.newCachedThreadPool() as ThreadPoolExecutor
val selenium2S3 = Selenium2S3(executor, cookies)
try {
  selenium2S3.save(URL("http://example.com"), null, "mySaveRoot")
} finally {
  selenium2S3.close()
}
```

This documentation provides an overview of the `Selenium2S3` class and its capabilities. For detailed usage and
customization, refer to the method descriptions and parameters within the class source code.

# resources\application\chat.js

## WebSocket Communication Module

This module provides a simple interface for establishing WebSocket connections, sending messages, and managing session
IDs for real-time web applications. It includes functions for connecting to a WebSocket server, sending messages through
the WebSocket, and handling session IDs for maintaining the state of the connection.

### Functions

#### getSessionId()

Retrieves or generates a new session ID for the WebSocket connection.

- If the current URL contains a hash (representing a session ID), it extracts and returns this ID.
- If no session ID is present in the URL, it requests a new session ID from the server at the endpoint `'newSession'`.
  Upon receiving a new session ID, it updates the URL hash with this ID and initiates a WebSocket connection using this
  ID.

**Returns:** The session ID as a string if it exists in the URL hash; otherwise, it initiates the process to fetch and
set a new session ID but does not return a value.

#### send(message)

Sends a message through the established WebSocket connection.

- **Parameters:**
    - `message`: The message to be sent through the WebSocket. This should be a string.

- **Throws:** An error if the WebSocket is not in the `OPEN` state (readyState !== 1), indicating that the connection is
  not ready to send messages.

#### connect(sessionId, customReceiveFunction)

Establishes a WebSocket connection using the provided session ID and sets up event listeners for the connection.

- **Parameters:**
    - `sessionId`: The session ID to be used for the WebSocket connection. This should be a string obtained
      from `getSessionId()`.
    - `customReceiveFunction` (optional): A custom function to handle incoming WebSocket messages. If not provided, the
      default handler `onWebSocketText` will be used.

- **Behavior:** This function constructs the WebSocket URL based on the current window location and the provided session
  ID, then initiates the WebSocket connection. It also sets up event listeners for `open`, `message`, `close`,
  and `error` events on the WebSocket.

#### showDisconnectedOverlay(show)

Controls the visibility of a hypothetical "disconnected" overlay based on the WebSocket connection state.

- **Parameters:**
    - `show`: A boolean indicating whether to show (true) or hide (false) the disconnected overlay.

- **Behavior:** This function enables or disables elements with the class `ws-control` based on the `show` parameter.
  This can be used to disable user interface controls when the WebSocket connection is lost.

### Usage Example

```javascript
// Attempt to connect using an existing session ID or fetch a new one
const sessionId = getSessionId();
if (sessionId) {
    connect(sessionId);
}

// Custom function to handle incoming messages
function onCustomMessage(event) {
    console.log('Received message:', event.data);
}

// Send a message through the WebSocket
function sendMessage() {
    try {
        send('Hello, world!');
    } catch (error) {
        console.error(error.message);
    }
}

// Reconnect with a custom message handler
function reconnectWithCustomHandler() {
    const sessionId = getSessionId();
    if (sessionId) {
        connect(sessionId, onCustomMessage);
    }
}

// Example of using the showDisconnectedOverlay function
function handleConnectionChange(isConnected) {
    showDisconnectedOverlay(!isConnected);
}
```

This module simplifies the process of managing WebSocket connections and session IDs, making it easier to build
real-time interactive web applications.

# resources\application\index.html

## WebSocket Client Web Application Documentation

This document provides an overview and detailed explanation of the WebSocket Client Web Application's HTML structure,
including its dependencies, layout, and functionality. This application is designed to facilitate real-time
communication between clients and a server using WebSockets.

### Overview

The application's front-end is structured in HTML and makes extensive use of external libraries and stylesheets for
enhanced functionality and aesthetics. It includes a dynamic user interface for sending and receiving messages in
real-time, managing sessions, and customizing themes.

### Dependencies

The application relies on several external resources and libraries:

- **Prism.js**: Used for syntax highlighting in the application. It enhances the readability of code snippets shared
  within the chat.
- **Mermaid.js**: A JavaScript-based diagramming and charting tool that renders markdown-inspired text definitions to
  create and modify diagrams dynamically.
- **Main CSS**: The primary stylesheet for the application, defining the visual aspects of the UI.
- **Main JS & Chat JS**: JavaScript files containing the logic for WebSocket communication and UI interactions.

#### External Libraries and Stylesheets

- Prism.js related stylesheets and scripts for syntax highlighting and additional features like line numbers, match
  braces, and toolbar.
- Mermaid.js for diagramming, loaded as an ES module.
- Application-specific styles (`main.css`) and scripts (`main.js`, `chat.js`).

### HTML Structure

#### Head Section

Contains metadata, links to stylesheets for Prism.js and the application, and script tags for the application logic.

#### Body Section

##### Toolbar

A navigation bar with dropdown menus for navigating through the application, managing sessions, changing themes, and
accessing about sections like privacy policy and terms of service.

##### Namebar

Displays login information and user-specific options like settings, usage, and logout, which become visible upon login.

##### Session

The main interactive area where users can input and send messages. It includes a form with a textarea for message input
and a submit button.

##### Modal

A generic modal component for displaying additional information or forms in an overlay.

##### Footer

Contains attribution and links to external resources or the project's repository.

### Functionality

- **Real-Time Communication**: Utilizes WebSockets for real-time messaging between clients and the server.
- **Theme Customization**: Users can select different themes for the UI from the "Themes" dropdown.
- **Session Management**: Options for managing and sharing sessions, including settings, files, usage, and deletion.
- **User Interaction**: Login/logout functionality and user-specific settings accessible through the namebar.

### Scripts

#### Prism.js

Integrated for syntax highlighting and additional features to enhance the display of code snippets within messages.

#### Mermaid.js

Initialized in a script tag as an ES module, it allows for the rendering of diagrams based on textual descriptions.

#### Application Scripts

- **main.js**: Contains the core functionality for WebSocket communication and handling UI interactions.
- **chat.js**: Dedicated to handling chat-specific functionalities, including sending, receiving, and displaying
  messages.

### Conclusion

This WebSocket Client Web Application provides a robust platform for real-time communication, enhanced with features
like syntax highlighting and diagram rendering. Its modular structure and use of external libraries make it a flexible
and visually appealing solution for online chat applications.

# resources\application\main.js

## Developer Documentation

This documentation provides an overview and detailed explanation of the JavaScript functions and event listeners used in
a web application for managing modals, fetching data, handling WebSocket messages, and dynamically updating the user
interface.

### Overview

The application includes functionality for:

- Displaying and hiding modals
- Fetching data from an endpoint and displaying it within a modal
- Handling WebSocket messages and updating the UI accordingly
- Dynamically updating tabs, verbose states, and reply forms
- Managing user settings and themes
- Handling user input and form submissions

### Functions

#### showModal(endpoint, useSession = true)

Displays a modal by setting its display style to 'block'. It fetches data from the specified endpoint and displays it
within the modal. The `useSession` parameter indicates whether to append a session ID to the endpoint.

**Parameters:**

- `endpoint`: The URL endpoint from which to fetch data.
- `useSession`: A boolean indicating whether to use the session ID. Defaults to `true`.

#### closeModal()

Hides the modal by setting its display style to 'none'.

#### async fetchData(endpoint, useSession = true)

Fetches data from the specified endpoint asynchronously. If `useSession` is true, it appends the session ID to the
endpoint. The function updates the modal content with the fetched data or an error message if the fetch fails.

**Parameters:**

- `endpoint`: The URL endpoint from which to fetch data.
- `useSession`: A boolean indicating whether to use the session ID. Defaults to `true`.

#### onWebSocketText(event)

Handles WebSocket messages by parsing the message data and updating the UI accordingly. It supports message versioning,
input visibility, and sticky input behavior.

**Parameters:**

- `event`: The WebSocket message event.

#### updateTabs()

Attaches click event listeners to tab buttons to switch between tab contents within a tab container.

#### toggleVerbose()

Toggles the visibility of elements with the 'verbose' class based on the text of the 'verbose' button.

#### refreshReplyForms()

Attaches keydown event listeners to reply input fields to submit the form when the Enter key is pressed without the
Shift key.

#### refreshVerbose()

Refreshes the visibility of verbose elements based on the current state of the 'verbose' button.

### Event Listeners

The application attaches several event listeners on the DOMContentLoaded event to handle user interactions such as
clicking on UI elements, submitting forms, and changing themes.

#### Theme Change

Event listeners are attached to theme selection elements to change the application's theme by updating the stylesheet
link and saving the selected theme to localStorage.

#### Modal Triggers

Click event listeners are attached to various elements to display modals related to settings, usage, deletion,
cancellation, threads, and sharing.

#### Form Submission

A submit event listener is attached to the main input form to prevent the default form submission behavior and handle
the submission manually.

#### Input Field Auto-Resize

An input event listener is attached to a text input field to automatically adjust its height based on the content.

#### Fetch User Information

Fetches user information on page load and updates the UI based on whether the user is logged in.

#### Privacy and Terms Links

Click event listeners are attached to the privacy policy and terms of service links to display the respective modals.

### Conclusion

This documentation covers the core functionality of the web application related to modals, data fetching, WebSocket
communication, and dynamic UI updates. Developers can use this guide to understand and extend the application's
features.

# resources\shared\schemes\_alien_spaceship.scss

## Alien Spaceship Theme - Developer Documentation

Welcome to the developer documentation for the Alien Spaceship theme. This theme is designed to bring a cosmic and
adventurous feel to your application with its carefully chosen color palette inspired by the vastness of space and the
mystery of alien worlds. Below, you will find detailed information on the theme's color scheme, including base colors,
derived colors, and specific use cases such as buttons, messages, and modals.

### Base Colors

The Alien Spaceship theme is built around a set of base colors that evoke the depth and mystery of space:

- **Deep Space Black** (`#0b0c10`): A deep black that captures the void of space.
- **Galactic Blue** (`#1f2833`): A dark blue reminiscent of the galaxy's vast expanse.
- **Nebula Pink** (`#c5c6c7`): A soft pink inspired by distant nebulae.
- **Asteroid Gray** (`#4e4e50`): A muted gray that mimics the rocky surface of asteroids.
- **Alien Green** (`#66fcf1`): A bright green for highlights, inspired by classic alien aesthetics.
- **Laser Red** (`#fc4445`): A striking red for important elements, inspired by laser beams.
- **Space Dust** (`#ccc`): A light grey used for background contrasts, reminiscent of cosmic dust.
- **Comet Tail** (`#45a29e`): A medium teal for borders and lines, inspired by comet tails.

### Derived Colors

Derived colors are variations and combinations of the base colors, tailored for specific UI elements:

- **Backgrounds, Text, and Borders**:
    - Secondary Background Color: Deep Space Black
    - Secondary Text Color: Space Dust
    - Border Color: Comet Tail

- **Application List**:
    - Header Background: Galactic Blue
    - Header Text: Space Dust
    - Link Color: Laser Red
    - Row Even Background: Nebula Pink
    - Row Hover Background: Asteroid Gray

- **Buttons**:
    - Background Color: Laser Red
    - Text Color: Deep Space Black
    - Hover Background Color: Darkened Laser Red

- **Messages and Modals**:
    - Info Background Color: Lightened Nebula Pink
    - Success Background Color: Lightened Alien Green
    - Error Background Color: Lightened Laser Red
    - Modal Content Background Color: Deep Space Black

### Usage

#### Buttons

To style buttons within the Alien Spaceship theme, use the following Sass variables:

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

#### Messages

For informational, success, and error messages, apply the corresponding background and text color variables:

```scss
.message-info {
  background-color: $message-info-bg-color;
  color: $message-info-text-color;
}

.message-success {
  background-color: $message-success-bg-color;
  color: $message-success-text-color;
}

.message-error {
  background-color: $message-error-bg-color;
  color: $message-error-text-color;
}
```

#### Modals

Modals should use the modal content background and text color variables for consistency:

```scss
.modal-content {
  background-color: $modal-content-bg-color;
  color: $modal-content-text-color;
  box-shadow: $modal-content-shadow;
}
```

### Conclusion

The Alien Spaceship theme offers a unique and engaging aesthetic for applications, drawing inspiration from the
mysteries of outer space and alien worlds. By adhering to the color scheme and usage guidelines provided in this
documentation, developers can ensure a cohesive and visually appealing user interface.

# resources\shared\schemes\_forest.scss

## Forest Canopy Theme - Developer Documentation

The Forest Canopy theme is a comprehensive styling framework designed to provide a rich, earthy aesthetic to web
applications. This document outlines the variables and their intended uses within the theme, facilitating easy
customization and application.

### Importing the Theme

Ensure you import the theme at the beginning of your stylesheet to access its variables and mixins:

```scss
@import 'night';
```

### Typography Variables

Typography variables define the font styles used throughout the theme.

- `$font-family-primary`: Primary font family, set to `fantasy`.
- `$font-family-secondary`: Secondary font family, set to `sans-serif`.
- `$font-size-base`: Base font size, set to `1em`.
- `$font-size-large`: Large font size, set to `1.5px`.
- `$font-size-small`: Small font size, set to `0.8px`.
- `$font-weight-normal`: Normal font weight, set to `100`.
- `$font-weight-bold`: Bold font weight, set to `500`.

### Base Colors

Base colors define the primary palette for the Forest Canopy theme.

- `$base-forest-floor`: Rich dark earth, used for main backgrounds.
- `$base-tree-bark`: Warm brown, used for headers and footers.
- `$base-canopy-leaf`: Deep green, used for accents and active elements.
- `$base-sunbeam`: Light khaki, used for highlights and special features.
- `$base-dew`: Pale green, used for subtle background contrasts.
- `$base-underbrush`: Dark olive green, used for secondary backgrounds and buttons.
- `$base-fern`: Muted medium green, used for borders and separators.
- `$base-acorn`: Nutty brown, used for text and icons.

### Derived Colors

Derived colors are calculated based on the base colors to maintain a cohesive theme.

- `$secondary-bg-color`: Matches `$base-forest-floor`.
- `$secondary-text-color`: Matches `$base-acorn`.
- `$applist-header-bg`: Matches `$base-tree-bark`.
- ... (and so on for each derived color variable, explaining its intended use and base color reference).

### Buttons

Button styling variables provide a consistent look and feel for interactive elements.

- `$button-bg-color`: Background color for buttons, set to `$base-canopy-leaf`.
- `$button-text-color`: Text color for buttons, set to `$base-forest-floor`.
- `$button-hover-bg-color`: Background color for buttons on hover, darkened by 10%.
- `$button-hover-text-color`: Text color for buttons on hover, lightened by 10%.
- `$button-border-radius`: Border radius for buttons, matches `$border-radius`.
- `$button-box-shadow`: Box shadow for buttons, matches `$box-shadow`.

### Forms

Form styling variables ensure inputs and form elements align with the theme.

- `$input-border-color`: Border color for inputs, set to `$base-fern`.
- `$input-border-focus-color`: Border color for inputs on focus, set to `$base-dew`.
- `$input-bg-color`: Background color for inputs, set to `$base-forest-floor`.
- `$input-text-color`: Text color for inputs, set to `$base-acorn`.
- `$input-padding`: Padding for inputs, set to `10px`.

### Messages and Modals

Variables for messages and modals provide feedback and interaction cues to users.

- `$message-info-bg-color`: Background color for info messages, lightened by 40% from `$base-canopy-leaf`.
- `$message-info-text-color`: Text color for info messages, set to `$base-tree-bark`.
- ... (and so on for each message and modal variable, explaining its intended use and base color reference).

### Customization

To customize the theme, you can override the default values of the variables before importing the theme or in your
custom stylesheets. This allows for a personalized look while maintaining the integrity of the theme's design.

### Conclusion

The Forest Canopy theme offers a rich set of variables for comprehensive styling of web applications. By adhering to
this documentation, developers can ensure consistent application of the theme and ease of customization.

# resources\shared\schemes\_night.scss

## Nighttime Theme Color Scheme Documentation

This documentation provides an overview of the color variables defined for the Nighttime theme. The theme is designed
with a dark aesthetic, suitable for applications that require a dark mode or wish to offer a less bright, eye-friendly
interface for nighttime usage. The color scheme is composed of base colors, derived colors, and specific component
styling for buttons, messages, and modals.

### Base Colors

The foundation of the Nighttime theme is built upon these base colors:

- `$base-night`: #141414 - A very dark gray, almost black, used primarily for backgrounds.
- `$base-midnight-blue`: #2c3e50 - A dark blue, suitable for headers and footers.
- `$base-twilight-blue`: #34495e - A slightly lighter shade of dark blue for accents.
- `$base-dark-grey`: #7f8c8d - A muted grey for readable text that isn't too harsh on the eyes.
- `$base-moonlit-aqua`: #1abc9c - A muted aqua for subtle highlights and accents.
- `$base-starlight-yellow`: #f1c40f - A soft yellow for important buttons or icons.
- `$base-cloudy-grey`: #bdc3c7 - A lighter grey for background contrasts.
- `$base-meteorite-border`: #95a5a6 - A medium grey for borders and lines.

### Derived Colors

Derived colors are calculated based on the base colors to maintain a cohesive theme across various UI components:

- Backgrounds, text, headers, links, and more have specific derived colors for consistency.
- Special UI elements like buttons, dropdowns, modals, and messages utilize these derived colors for background, text,
  and border styling.
- Interaction states such as hover and active are also defined with slight modifications to the base or derived colors
  to provide visual feedback.

### Component Styling

#### Buttons

- Background, text, and hover states for buttons are defined to ensure they stand out and provide necessary user
  feedback.

#### Forms

- Form elements retain the base theme colors for consistency but can be customized as needed.

#### Messages and Modals

- Information, success, and error messages have specific background and text color styling for clear communication.
- Modals utilize the dark theme colors for background and text, with added shadows for depth.

### Utility Variables

- `$border-radius`, `$box-shadow`, and `$transition-speed` are utility variables provided for consistent styling and
  animations across the theme.

### Usage

To use these variables in your project, ensure you import this stylesheet at the beginning of your Sass or SCSS files.
You can then reference these variables throughout your styles to maintain a consistent theme.

```scss
@import 'path/to/nighttime-theme.scss';

body {
  background-color: $primary-bg-color;
  color: $primary-text-color;
}
```

This documentation should serve as a reference for developers looking to implement the Nighttime theme in their
projects. The defined colors and variables offer a comprehensive palette for creating dark-themed UIs with a focus on
readability and user experience during nighttime usage.

# resources\shared\schemes\_normal.scss

## Developer Documentation: UI Theme Variables

This documentation outlines the variables defined for styling a web application's user interface (UI). These variables
are categorized into typography, base colors, derived colors, buttons, forms, and messages & modals. Utilizing these
variables ensures a consistent and easily maintainable theme across the application.

### Typography Variables

Typography variables define the fonts, sizes, and weights used throughout the application.

- `$font-family-primary`: Primary font stack. Default: `'Helvetica Neue', Helvetica, Arial, sans-serif`.
- `$font-family-secondary`: Secondary font stack. Default: `'Segoe UI', Tahoma, Geneva, Verdana, sans-serif`.
- `$font-size-base`: Base font size. Default: `1em`.
- `$font-size-large`: Large font size. Default: `1.5px`.
- `$font-size-small`: Small font size. Default: `0.8px`.
- `$font-weight-normal`: Normal font weight. Default: `400`.
- `$font-weight-bold`: Bold font weight. Default: `700`.

### Base Colors

Base colors define the core palette from which the application's color scheme is derived.

- `$base-white`: Pure white. Default: `#ffffff`.
- `$base-dark`: Dark color, typically used for text. Default: `#333333`.
- `$base-light-blue`: Light blue, used for subtle backgrounds. Default: `#dae8fc`.
- `$base-grey`: Grey, adjusted for readability. Default: `#7f8c8d`.
- `$base-dark-blue`: Dark blue, suitable for headers or footers. Default: `#2c3e50`.
- `$base-light-grey`: Light grey, good for backgrounds. Default: `#ecf0f1`.
- `$base-blue`: Vibrant blue, used for links and buttons. Default: `#3498db`.
- `$base-green`: Success color. Default: `#2ecc71`.
- `$base-red`: Error color. Default: `#e74c3c`.
- `$base-orange`: Warm, friendly color for buttons. Default: `#e67e22`.
- `$base-dark-grey`: For subtle text. Default: `#95a5a6`.

### Derived Colors

Derived colors are based on the base colors and are used for specific UI elements like backgrounds, text, borders, and
more.

- `$secondary-bg-color`: Secondary background color. Inherits from `$base-white`.
- `$secondary-text-color`: Secondary text color. Inherits from `$base-dark`.
- (Further derived colors follow a similar pattern, adjusting for specific UI elements like headers, links, buttons,
  etc.)

### Buttons

Button variables define the styling for buttons within the application.

- `$button-bg-color`: Background color for buttons. Inherits from `$base-blue`.
- `$button-text-color`: Text color for buttons. Inherits from `$base-white`.
- `$button-hover-bg-color`: Background color for buttons on hover. Darkened by 10% from `$button-bg-color`.
- `$button-hover-text-color`: Text color for buttons on hover. Inherits from `$button-text-color`.
- `$button-border-radius`: Border radius for buttons. Inherits from `$border-radius`.
- `$button-box-shadow`: Box shadow for buttons. Inherits from `$box-shadow`.

### Forms

Form variables define the styling for form elements like inputs.

- `$input-border-color`: Border color for inputs. Inherits from `$base-grey`.
- `$input-border-focus-color`: Border color for inputs on focus. Inherits from `$base-blue`.
- `$input-bg-color`: Background color for inputs. Inherits from `$base-white`.
- `$input-text-color`: Text color for inputs. Inherits from `$secondary-text-color`.
- `$input-padding`: Padding for inputs. Default: `10px`.

### Messages and Modals

Variables for messages and modals define the styling for informational, success, and error messages, as well as modal
dialogs.

- `$message-info-bg-color`: Background color for informational messages. Lightened by 50% from `$base-blue`.
- `$message-info-text-color`: Text color for informational messages. Inherits from `$base-dark-blue`.
- (Further message and modal variables adjust similarly for success, error messages, and modal content.)

This documentation provides a comprehensive overview of the UI theme variables used in the application. Developers can
refer to this guide to understand and utilize the predefined variables for consistent styling across the application.

# resources\shared\schemes\_pony.scss

## Developer Documentation: Theme Customization

This documentation provides an overview of the theme customization options available through the use of SCSS variables.
These variables allow for easy adjustments to typography, colors, buttons, forms, messages, and modals to maintain
consistency and facilitate rapid development.

### Typography Variables

Customize the typography across your application by adjusting these variables.

- `$font-family-primary`: Primary font family. Default is `cursive`.
- `$font-family-secondary`: Secondary font family. Default is `cursive`.
- `$font-size-base`: Base font size for standard text. Default is `1.2em`.
- `$font-size-large`: Font size for larger text elements. Default is `1.6px`.
- `$font-size-small`: Font size for smaller text elements. Default is `0.9px`.
- `$font-weight-normal`: Weight for normal text. Default is `400`.
- `$font-weight-bold`: Weight for bold text. Default is `700`.

### Base Colors

Define the core palette for your application with these base color variables.

- `$base-bubblegum`: Playful pink, ideal for primary elements. Default is `#ff77a9`.
- `$base-light-blue`: Vibrant blue, used for links and buttons. Default is `#3471FF`.
- `$base-candy-red`: Soft red, suitable for calls to action and highlights. Default is `#ff6b6b`.
- `$base-vanilla`: Soft off-white, for backgrounds to ensure legibility. Default is `#f3f3f3`.
- `$base-charcoal`: Dark grey, softer than black for better readability. Default is `#454545`.
- Additional pastel colors for backgrounds and highlights include pastel pink (`$base-pastel-pink`), pastel
  yellow (`$base-pastel-yellow`), and pastel red (`$base-pastel-red`).
- `$base-soft-grey`: Soft grey for less important elements. Default is `#ced4da`.

### Derived Colors

These variables utilize the base colors to maintain a cohesive theme across various UI components.

- `$secondary-bg-color`, `$secondary-text-color`: Colors for secondary backgrounds and text.
- `$applist-header-bg`, `$applist-header-text`: Background and text colors for app list headers.
- `$border-color`, `$border-radius`, `$box-shadow`: Defaults for borders and shadows.
- `$error-color`, `$success-color`: Colors for error messages and success indicators.
- `$link-color`, `$link-hover-color`: Default and hover colors for links.
- `$modal-overlay-color`: Color for modal overlays.
- More variables are available for specific UI elements like buttons, dropdowns, messages, and forms.

### Buttons

Customize button appearance with these variables.

- `$button-bg-color`: Background color for buttons.
- `$button-text-color`: Text color for buttons.
- `$button-hover-bg-color`, `$button-hover-text-color`: Background and text colors for buttons on hover.
- `$button-border-radius`, `$button-box-shadow`: Border radius and box shadow for buttons.

### Forms

Adjust the appearance of form elements.

- `$input-border-color`, `$input-border-focus-color`: Border colors for input fields, including focus state.
- `$input-bg-color`, `$input-text-color`, `$input-padding`: Background color, text color, and padding for input fields.

### Messages and Modals

Define the styling for informational messages and modal dialogs.

- `$message-info-bg-color`, `$message-info-text-color`: Background and text colors for informational messages.
- `$message-success-bg-color`, `$message-success-text-color`: Background and text colors for success messages.
- `$message-error-bg-color`, `$message-error-text-color`: Background and text colors for error messages.
- `$modal-content-bg-color`, `$modal-content-text-color`, `$modal-content-shadow`: Styling for modal content.

### Usage

To use these variables, simply assign them to the desired CSS properties within your SCSS files. For example:

```scss
body {
  font-family: $font-family-primary;
  background-color: $primary-bg-color;
  color: $primary-text-color;
}
```

This approach ensures that your application maintains a consistent look and feel, while also providing the flexibility
to adapt the theme as needed.

# resources\application\favicon.svg

#### Developer Documentation: SVG Graphic Implementation

##### Overview

This document provides an overview and implementation details for integrating the provided SVG graphic into a web
application or website. The SVG graphic is a complex illustration with multiple layers, colors, and styles, designed to
be scalable and adaptable across different platforms and devices.

##### SVG Graphic Description

The SVG graphic is a detailed illustration composed of various elements, including paths, shapes, and styled components.
It features a rich color palette and intricate design elements, making it suitable for high-resolution displays. The
graphic is designed with versatility in mind, allowing for easy customization and scaling.

##### File Information

- **File Name:** `custom_graphic.svg`
- **Dimensions:** 700x700 pixels
- **File Size:** Varies depending on the optimization level

##### Implementation Guide

1. **Adding the SVG to HTML:**
   Embed the SVG code directly into your HTML file to ensure the best performance and flexibility. This method allows
   for easy manipulation of the SVG properties using CSS and JavaScript.

   ```html
   <!-- Place this code where you want the SVG graphic to appear -->
   <div class="svg-container">
       <!-- SVG code goes here -->
   </div>
   ```

2. **Styling the SVG:**
   The SVG graphic comes with predefined classes for styling various elements within the graphic. You can override these
   styles or add new ones by targeting the specific classes in your CSS file.

   ```css
   /* Example of overriding the fill color of elements with class 'st0' */
   .st0 {
       fill: #newColor;
   }
   ```

3. **Interactivity and Animation:**
   Enhance the SVG graphic with interactivity and animation using JavaScript or CSS. For example, you can add hover
   effects, click events, or animate certain elements within the SVG.

   ```javascript
   // Example of adding a hover effect using JavaScript
   document.querySelector('.st0').addEventListener('mouseover', function() {
       this.style.fill = '#hoverColor';
   });
   ```

   ```css
   /* Example of adding a simple animation using CSS */
   .st0 {
       transition: fill 0.3s ease;
   }
   .st0:hover {
       fill: #hoverColor;
   }
   ```

4. **Optimization:**
   To ensure optimal loading times, especially for complex SVG graphics, consider using SVG optimization tools like
   SVGO. This can significantly reduce the file size without compromising the quality of the graphic.

5. **Accessibility:**
   Enhance the accessibility of the SVG graphic by adding descriptive titles and descriptions using the `<title>`
   and `<desc>` tags within the SVG code. This provides context for screen readers and helps meet accessibility
   standards.

   ```html
   <svg>
       <title>Descriptive Title of the SVG Graphic</title>
       <desc>Description of the SVG Graphic, highlighting key elements.</desc>
       <!-- SVG content -->
   </svg>
   ```

##### Conclusion

Integrating the provided SVG graphic into your project enhances the visual appeal of your application or website. By
following the implementation guide, you can ensure that the graphic is displayed correctly, optimized for performance,
and accessible to all users.

# resources\shared\_main.scss

## Developer Documentation: SCSS Mixins and Styles

This documentation provides an overview of the SCSS mixins and styles used in the project. It covers the purpose and
usage of each mixin, as well as the key styling sections that apply these mixins for UI components.

### Mixins

#### 1. `typography`

**Purpose:** Sets the font properties for elements.

**Parameters:**

- `$font-family`: Font family (default: `$font-family-primary`)
- `$font-size`: Font size (default: `$font-size-base`)
- `$font-weight`: Font weight (default: `$font-weight-normal`)

**Usage:**

```scss
@include typography($font-family-secondary, 16px, bold);
```

#### 2. `flex-container`

**Purpose:** Creates a flex container with an optional direction.

**Parameters:**

- `$direction`: Flex direction (default: `column`)

**Usage:**

```scss
@include flex-container(row);
```

#### 3. `fixed-full`

**Purpose:** Applies a fixed position to an element, covering the entire viewport.

**Parameters:** None

**Usage:**

```scss
@include fixed-full;
```

#### 4. `link-hover-transition`

**Purpose:** Adds a color transition effect to links on hover.

**Parameters:** None

**Usage:**

```scss
@include link-hover-transition;
```

#### 5. `message-style`

**Purpose:** Applies common styling to message elements.

**Parameters:** None

**Usage:**

```scss
@include message-style;
```

### Key Styling Sections

#### Body

Applies typography, color, and spacing to the body element.

```scss
body {
  @include typography($font-family-secondary);
  color: $primary-text-color;
  background-color: $primary-bg-color;
  margin: 0;
  padding: 30px 0 50px;
}
```

#### Messages Container

Styles the container for messages, including padding, background color, and shadow.

```scss

#
#messages {
  @include flex-container;
  padding: 10px;
  background-color: $secondary-bg-color;
  box-shadow: $box-shadow;
}
```

#### Input Fields

Styles for chat and reply input fields, including background color, text color, and border properties.

```scss
.chat-input,
.reply-input {
  background-color: $secondary-bg-color;
  color: $primary-text-color;
  border-radius: $border-radius;
  padding: 10px;
  margin-bottom: 10px;
  overflow: auto;
  resize: vertical;
  flex: 1;
  border: 1px solid $border-color;
  box-shadow: $box-shadow;
}
```

#### Disconnected Overlay

Styles for a modal overlay displayed when disconnected, including positioning, background color, and typography.

```scss

#
#disconnected-overlay {
  @include fixed-full;
  display: none;
  background-color: $modal-overlay-color;
  z-index: 50;
  @include flex-container;
  color: white;
  font-size: $font-size-large;

  p {
    @include typography($font-size: $font-size-large, $font-weight: $font-weight-bold);
    line-height: 1.5;
    margin-bottom: 20px;
    animation: bounce $transition-speed infinite alternate;
    position: relative;
    color: firebrick;
  }
}
```

#### Buttons

Styles for various buttons, including play, regenerate, cancel, and close buttons. Applies typography, border,
background, and interaction effects.

```scss
.play-button,
.regen-button,
.cancel-button,
.close-button {
  @include typography($font-size: 1.5rem, $font-weight: $font-weight-bold);
  border: 2px solid transparent;
  background: $primary-bg-color;
  cursor: pointer;
  transition: all $transition-speed;
  padding: 5px 10px;
  border-radius: $border-radius;
  text-decoration: unset;

  &:focus,
  &:hover {
    outline: none;
    background-color: darken($primary-bg-color, 5%);
    border-color: $link-color;
  }

  &:active {
    transform: scale(0.95);
  }
}
```

### Keyframes

Animations for elements, such as `bounce` for bouncing effects and `spin` for continuous rotation.

```scss
@keyframes bounce {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-10px);
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
```

This documentation provides a comprehensive overview of the SCSS mixins and styles used throughout the project,
facilitating easier understanding and modification by developers.

# resources\welcome\index.html

## Developer Documentation: HTML Page Auto-Redirect

This document provides an overview and implementation details for creating an HTML page that automatically redirects
users to a new page. This technique is useful for redirecting traffic from old URLs to new ones, maintaining user
experience during site maintenance, or simply guiding users through a multi-step process on a website.

### Overview

The provided HTML template employs a meta refresh tag for redirection and offers a fallback link if the automatic
redirection does not occur. This ensures compatibility across various browsers and situations where JavaScript is
disabled.

### Implementation

#### HTML Structure

The HTML document is structured with the basic required elements: `<!DOCTYPE html>`, `<html>`, `<head>`, and `<body>`
tags. The core functionality of redirection is implemented within the `<head>` section of the document.

##### Meta Refresh Tag

```html

<meta http-equiv="refresh" content="0;url=/index.html">
```

- `http-equiv="refresh"`: This attribute specifies that the HTTP-equiv meta tag is being used for automatic page
  refresh.
- `content="0;url=/index.html"`: The `content` attribute defines the time delay and target URL for the redirection. `0`
  indicates that the redirection should occur immediately. Replace `/index.html` with the desired target URL.

#### Fallback Link

```html
<p>If you are not redirected, <a href="/index.html">click here</a>.</p>
```

In the body of the document, a paragraph (`<p>`) provides a textual message explaining the action to users. It includes
an anchor (`<a>`) tag that serves as a manual fallback method for redirection. Users can click this link if the
automatic redirection fails for any reason.

### Usage

1. **Customize the URL**: Change the `url` parameter in the meta tag and the `href` attribute in the anchor tag to the
   desired destination URL.
2. **Adjust the Delay**: If a delay before redirection is desired, modify the number in the `content` attribute of the
   meta tag. For example, `content="5;url=/index.html"` will delay the redirection by 5 seconds.
3. **Deployment**: Save the HTML code in a file and upload it to your web server. Ensure that the target URL is
   accessible and correct.

### Considerations

- **SEO Implications**: Frequent use of meta refresh redirection can have negative implications for search engine
  optimization (SEO), as search engines may view it as an attempt to manipulate page rankings.
- **Accessibility**: Ensure that the fallback link is clearly visible and accessible for users who might have automatic
  redirection disabled or are using screen readers.
- **Browser Compatibility**: While the meta refresh tag is widely supported, testing across different browsers and
  devices is recommended to ensure consistent behavior.

### Conclusion

The provided HTML template offers a simple and effective method for redirecting users to a new page. By following the
implementation details and considering the usage notes, developers can seamlessly integrate this functionality into
their web projects.

# resources\welcome\main.js

## Developer Documentation

This documentation provides an overview and detailed explanation of the JavaScript functions used to enhance the
interactivity of a web application. The application includes features such as modal pop-ups, tab navigation, dynamic
content fetching, and theme switching.

### Overview

The script is structured around several key functionalities:

- **Modal Operations**: Displaying and hiding modal pop-ups.
- **Data Fetching**: Asynchronously fetching and displaying data within modals.
- **Tab Navigation**: Dynamically updating tabbed content based on user interaction.
- **User Information Handling**: Fetching and displaying user information upon page load.
- **Theme Switching**: Allowing users to switch between different theme styles.

### Functions

#### showModal(endpoint)

Displays a modal pop-up and fetches content to be displayed within the modal.

- **Parameters**:
    - `endpoint`: The API endpoint or URL from which to fetch the content to be displayed in the modal.

- **Behavior**: This function first calls `fetchData(endpoint)` to asynchronously fetch content from the specified
  endpoint. It then makes the modal visible by setting its display style to `block`.

#### closeModal()

Hides the modal pop-up.

- **Behavior**: Sets the display style of the modal to `none`, effectively hiding it from view.

#### async fetchData(endpoint)

Asynchronously fetches data from a specified endpoint and displays it within the modal content area.

- **Parameters**:
    - `endpoint`: The API endpoint or URL from which to fetch content.

- **Behavior**: This function attempts to fetch data from the specified endpoint. Upon success, it updates the modal's
  content area with the fetched data. In case of an error, it logs the error to the console.

#### updateTabs()

Sets up event listeners for tab navigation, allowing users to switch between tabs.

- **Behavior**: This function adds click event listeners to all elements with the class `.tab-button`. When a tab button
  is clicked, the function updates the active state of both the tabs and the corresponding tab content, making the
  clicked tab and its content visible while hiding others.

#### Event Listeners Setup

Upon the DOM content being fully loaded, the script sets up various event listeners for handling modal operations, user
information fetching, and theme switching.

- **Modal Operations**: Event listeners are added to close the modal either by clicking the close button or by clicking
  outside the modal area.
- **User Information Handling**: Fetches user information and updates the UI accordingly. It also sets up modals for
  user settings, usage, privacy, and terms of service.
- **Theme Switching**: Allows users to switch between predefined themes by clicking on theme buttons. The selected theme
  is saved to `localStorage` for persistence across sessions.

### Usage

To utilize this script, ensure it is included in the HTML file where these functionalities are required. The HTML
structure must include elements with the IDs and classes referenced by the script (
e.g., `#modal`, `.tab-button`, `#theme_style`).

### Conclusion

This script provides a comprehensive set of functionalities for enhancing user interaction within a web application
through modals, tab navigation, dynamic content fetching, and theme customization. By following the outlined
documentation, developers can effectively implement and customize these features according to their application's needs.

# resources\welcome\main.scss

### Developer Documentation: App Type Styles

This section of the stylesheet is dedicated to defining the appearance of elements classified under the `.app-type`
class. This class is designed to stylize text elements in a specific way, making them stand out within the application's
UI. Below is a detailed breakdown of the `.app-type` class and its properties.

#### Import Statements

Before defining the `.app-type` styles, the stylesheet imports two other files:

1. `@import '../shared/schemes/normal';` - This line imports a stylesheet from a relative path that likely contains
   color schemes or other styling constants used throughout the application. The 'normal' scheme could imply default or
   primary styling options.

2. `@import '../shared/main';` - This import statement brings in the main stylesheet, which could include global styles,
   variables, mixins, or other foundational styles that are necessary for the application's consistent look and feel.

#### `.app-type` Class Definition

The `.app-type` class is applied to elements requiring a specific typographic treatment. The properties defined within
this class are as follows:

- `text-transform: uppercase;` - This property transforms the text of the element to uppercase, making it stand out and
  providing a visual cue of its importance or category.

- `font-size: 0.6em;` - Sets the font size to 0.6 times the size of the font of its parent element. This relative sizing
  ensures that the `.app-type` elements maintain proportionality within different contexts.

- `font-weight: 600;` - Applies a font weight of 600, making the text semi-bold. This weight is heavier than normal but
  not as heavy as bold, striking a balance that enhances readability while drawing attention.

- `background-color: rgba(0, 200, 255, 0.25);` - The background color is set to a semi-transparent light blue, using the
  RGBA color model. The alpha value of 0.25 provides a subtle hint of color, ensuring the text remains the focal point.

- `border-radius: 5px;` - Applies rounded corners to the element with a radius of 5 pixels, softening its appearance and
  making it more visually appealing.

- `padding: 2px 5px;` - Adds padding inside the element, with 2 pixels on the top and bottom and 5 pixels on the left
  and right. This padding creates space around the text, improving legibility and aesthetic appeal.

- `color: white;` - Sets the text color to white, ensuring high contrast against the semi-transparent light blue
  background, which aids in readability and draws attention to the text.

#### Usage

To apply these styles, add the `class="app-type"` attribute to the desired HTML element. This class is particularly
suited for labels, tags, or any small pieces of text that need to be highlighted within the application's interface.

#### Example

```html
<span class="app-type">New</span>
```

This example demonstrates how to apply the `.app-type` class to a `<span>` element, which would render the text "NEW" in
uppercase, with the specified styling applied, making it stand out as a distinct element within the UI.

# resources\welcome\favicon.svg

## Developer Documentation for SVG Illustration

This SVG illustration is a complex graphic designed using XML-based markup. It features a variety of shapes, colors, and
styles to create a detailed image. The SVG is structured with various elements such as `<path>`, `<style>`, and color
classes to organize and style the illustration. Below is a breakdown of the key components of this SVG illustration to
aid developers in understanding, modifying, or reusing the code.

### Overview

The SVG starts with the declaration and a comment indicating the generator used for creating this SVG file. It specifies
the SVG version, the view box dimensions, and style settings for the entire illustration.

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Generator: Adobe Illustrator 28.1.0, SVG Export Plug-In . SVG Version: 6.00 Build 0)  -->
<svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" ...>
```

### Styles

The `<style>` tag defines CSS styles used within the SVG. Classes such as `.st0`, `.st1`, etc., are defined here,
specifying the fill colors for different parts of the illustration. For example:

```css
<
style type

=
"text/css"
>
.st0 {
    fill: #09090F;
}

.st1 {
    fill: #AFB1E0;
}

...

<
/
style >
```

### Paths

The illustration is composed of multiple `<path>` elements, each representing a part of the image. These paths use
the `d` attribute to define their shape and are styled using the classes defined in the `<style>` tag. For instance:

```xml

<path class="st0" d="M567.4,547.7c-0.4,0-0.6,0.1-1.3,0.6c-2.3,1.4-4.1,2.6-6,3.8..."/>
<path class="st1" d="M496.8,648.7c-4.3,2.8-8.8,5.6-14,8.9c-1.3-9.9-2.9-19.2-3.6-28.6..."/>
        ...
```

### Understanding Path Coordinates

Each `<path>` element's `d` attribute contains a series of commands and parameters that draw the shape. For
example, `M567.4,547.7` moves the pen to the starting point, and subsequent commands like `c` (cubic Bezier curve) draw
the path based on control points and endpoints.

### Modifying the SVG

To modify this SVG:

1. **Change Colors**: Update the `fill` property in the `<style>` section for the corresponding class.

2. **Alter Shapes**: Adjust the commands in the `d` attribute of `<path>` elements. This requires understanding of SVG
   path syntax, including commands like `M` (move to), `L` (line to), `C` (cubic Bezier curve), etc.

3. **Resize or Transform**: Use attributes like `width`, `height`, `viewBox`, and transform properties directly on
   the `<svg>` tag or individual elements.

4. **Add or Remove Elements**: Copy existing `<path>` elements and modify them, or remove them as needed. New shapes can
   be added by defining new `<path>` elements with their own `d` attribute.

### Reusability

This SVG's modular structure (using classes for styling) makes it easy to reuse or adapt parts of the illustration. For
instance, similar elements can share a class to apply consistent styling, which can then be changed universally by
modifying the class definition.

### Conclusion

This SVG illustration is a detailed graphic made up of styled paths. By understanding the structure and syntax of SVG,
developers can effectively modify, enhance, or repurpose this illustration for various applications.

# resources\welcome\favicon.png

## Developer Documentation

### Overview

This document provides a comprehensive guide for developers working with the given codebase. It includes details on the
system requirements, setup instructions, key functionalities, and guidelines for contributing to the project. The aim is
to ensure developers have the necessary information to effectively collaborate and contribute to the development
process.

### System Requirements

Before proceeding with the setup, ensure your development environment meets the following requirements:

- **Operating System**: Windows 10 or later, macOS Catalina or later, Linux (Ubuntu 20.04, Fedora 32, or similar)
- **Programming Language**: Ensure the specific version of the programming language required by the project is
  installed. For example, Python 3.8, Node.js 14.x, etc.
- **Dependencies**: Various projects may require different dependencies. Refer to the `requirements.txt`
  or `package.json` file in the project directory for a list of necessary dependencies.
- **Development Tools**: An IDE or text editor of your choice (e.g., Visual Studio Code, PyCharm, Atom).

### Setup Instructions

Follow these steps to set up the project environment on your local machine:

1. **Clone the Repository**: Clone the project repository to your local machine using Git commands or by downloading the
   ZIP file from the project's page.

   ```bash
   git clone https://github.com/your-project/repository.git
   ```

2. **Install Dependencies**: Navigate to the project directory and install the required dependencies.

   For Python projects:
   ```bash
   pip install -r requirements.txt
   ```

   For Node.js projects:
   ```bash
   npm install
   ```

3. **Environment Variables**: If the project requires environment variables, set them up according to the project's
   documentation. This may involve creating a `.env` file in the project root.

4. **Run the Application**: Start the application by running the appropriate command in the terminal. This command
   varies depending on the project's technology stack.

   For Python projects:
   ```bash
   python app.py
   ```

   For Node.js projects:
   ```bash
   npm start
   ```

### Key Functionalities

Describe the key functionalities of the project, including any APIs, libraries, or frameworks used. For example:

- **API Endpoints**: List and describe available API endpoints if the project is a web service or application.
- **Database Integration**: Detail the database system used (e.g., MySQL, MongoDB) and the structure of the database.
- **Authentication**: Explain the authentication mechanism if applicable (e.g., JWT, OAuth).

### Contributing

To contribute to the project, follow these guidelines:

1. **Fork the Repository**: Create a fork of the main repository on your GitHub account.
2. **Create a Feature Branch**: Work on new features or bug fixes in a separate branch created from the latest version
   of the main branch.

   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Commit Changes**: Make and commit your changes with clear, concise commit messages. Follow any commit message
   conventions specified by the project.

   ```bash
   git commit -m "Add a new feature"
   ```

4. **Push Changes**: Push your changes to your fork on GitHub.

   ```bash
   git push origin feature/your-feature-name
   ```

5. **Open a Pull Request**: Create a pull request from your feature branch to the main repository for review.

### Support

For support, please open an issue in the project's GitHub repository or contact the project maintainers directly via
email.

---

This documentation is intended to be a living document, updated as the project evolves. Contributors are encouraged to
suggest improvements or additions to ensure it remains a valuable resource for all developers involved in the project.


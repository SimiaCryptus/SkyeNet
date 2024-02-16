package com.simiacryptus.skyenet.webui.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

// OpenAPI root document
data class OpenApi(
  val openapi: String = "3.0.0",
  val info: Info? = null,
  val paths: Map<String, PathItem>? = emptyMap(),
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val components: Components? = null
) {
  fun merge(other: OpenApi) = OpenApi(
    openapi = openapi,
    info = info,
    paths = (paths ?: emptyMap()) + (other.paths ?: emptyMap()),
    components = components?.merge(other.components) ?: other.components
  )
}

// Metadata about the API
data class Info(
  val title: String? = null,
  val version: String? = null,
  val description: String? = null,
  val termsOfService: String? = null,
  val contact: Contact? = null,
  val license: License? = null
)

// Contact information
data class Contact(
  val name: String? = null,
  val url: String? = null,
  val email: String? = null
)

// License information
data class License(
  val name: String? = null,
  val url: String? = null
)

// Paths and operations
data class PathItem(
  val get: Operation? = null,
  val put: Operation? = null,
  val post: Operation? = null,
  val delete: Operation? = null,
  val options: Operation? = null,
  val head: Operation? = null,
  val patch: Operation? = null
)

// An API operation
data class Operation(
  val summary: String? = null,
  val description: String? = null,
  val responses: Map<String, Response>? = emptyMap()
)

// Operation response
data class Response(
  val description: String? = null,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val content: Map<String, MediaType>? = emptyMap()
)

// Components for reusable objects
data class Components(
  val schemas: Map<String, Schema>? = emptyMap(),
  val responses: Map<String, Response>? = emptyMap(),
  val parameters: Map<String, Parameter>? = emptyMap(),
  val examples: Map<String, Example>? = emptyMap(),
  val requestBodies: Map<String, RequestBody>? = emptyMap(),
  val headers: Map<String, Header>? = emptyMap(),
  val securitySchemes: Map<String, SecurityScheme>? = emptyMap(),
  val links: Map<String, Link>? = emptyMap(),
  val callbacks: Map<String, Callback>? = emptyMap()
) {
  fun merge(components: Components?) = if (null == components) this else Components(
    schemas = schemas?.plus(components.schemas ?: emptyMap()) ?: components.schemas,
    responses = responses?.plus(components.responses ?: emptyMap()) ?: components.responses,
    parameters = parameters?.plus(components.parameters ?: emptyMap()) ?: components.parameters,
    examples = examples?.plus(components.examples ?: emptyMap()) ?: components.examples,
    requestBodies = requestBodies?.plus(components.requestBodies ?: emptyMap()) ?: components.requestBodies,
    headers = headers?.plus(components.headers ?: emptyMap()) ?: components.headers,
    securitySchemes = securitySchemes?.plus(components.securitySchemes ?: emptyMap()) ?: components.securitySchemes,
    links = links?.plus(components.links ?: emptyMap()) ?: components.links,
    callbacks = callbacks?.plus(components.callbacks ?: emptyMap()) ?: components.callbacks
  )
}

// Simplified examples of component objects
data class Schema(val type: String? = null, val properties: Map<String, Schema>? = emptyMap(), val items: Schema? = null)
data class Parameter(val name: String? = null, val `in`: String? = null, val description: String? = null)
data class Example(val summary: String? = null, val description: String? = null)
data class RequestBody(val description: String? = null, val content: Map<String, MediaType>? = null)
data class Header(val description: String? = null)
data class SecurityScheme(val type: String? = null, val description: String? = null)
data class Link(val operationId: String? = null)
data class Callback(val expression: String? = null)
data class MediaType(val schema: Schema? = null)

// Function to serialize OpenApi object to JSON string
fun serializeOpenApiSpec(openApi: OpenApi): String {
  val mapper = jacksonObjectMapper()
  return mapper.writeValueAsString(openApi)
}

// Function to deserialize JSON string to OpenApi object
fun deserializeOpenApiSpec(json: String): OpenApi {
  val mapper = jacksonObjectMapper()
  return mapper.readValue(json)
}

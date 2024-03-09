package com.simiacryptus.skyenet.webui

import org.junit.jupiter.api.Test
import org.openapitools.codegen.OpenAPIGenerator
import org.openapitools.codegen.SpecValidationException
import java.io.File
import java.net.URI

class TestOpenAPITool {


    @Test
  fun test() {
      process(
        outDir = "C:/Users/andre/code/jo-penai/build/openapi",
        apiname = "openapi",
        suffix = ".yaml",
        url = "https://raw.githubusercontent.com/openai/openai-openapi/master/openapi.yaml",
        generator = "java",
        additionalProperties = mapOf(
          "asyncNative" to "true",
          "library" to "apache-httpclient",
          "serializationLibrary" to "jackson",
          "apiPackage" to "com.simiacryptus.api.java.openai",
          "modelPackage" to "com.simiacryptus.api.java.openai.model",
        )
      )
//      process(
//        outDir = "C:/Users/andre/code/jo-penai/build/openapi",
//        apiname = "openapi",
//        suffix = ".yaml",
//        url = "https://raw.githubusercontent.com/openai/openai-openapi/master/openapi.yaml",
//        generator = "kotlin",
//        additionalProperties = mapOf(
//          "apiPackage" to "com.simiacryptus.api.kotlin.openai",
//        )
//      )
      //https://docs.mistral.ai/redocusaurus/plugin-redoc-0.yaml
      process(
        outDir = "C:/Users/andre/code/jo-penai/build/openapi",
        apiname = "openapi",
        suffix = ".yaml",
        url = "https://docs.mistral.ai/redocusaurus/plugin-redoc-0.yaml",
        generator = "java",
        additionalProperties = mapOf(
          "asyncNative" to "true",
          "library" to "apache-httpclient",
          "serializationLibrary" to "jackson",
          "apiPackage" to "com.simiacryptus.api.java.mistral",
          "modelPackage" to "com.simiacryptus.api.java.mistral.model",
        )
      )
//      process(
//        outDir = "C:/Users/andre/code/jo-penai/build/openapi",
//        apiname = "openapi",
//        suffix = ".yaml",
//        url = "https://docs.mistral.ai/redocusaurus/plugin-redoc-0.yaml",
//        generator = "kotlin",
//        additionalProperties = mapOf(
//          "apiPackage" to "com.simiacryptus.api.kotlin.mistral",
//        )
//      )
      // file:///C:\Users\andre\code\jo-penai\ModelsLab AI API's.postman_collection.json
      process(
        outDir = "C:/Users/andre/code/jo-penai/build/openapi",
        apiname = "openapi",
        suffix = ".json",
        url = "file:///C:/Users/andre/code/jo-penai/ModelsLab_API.json",
        generator = "java",
        additionalProperties = mapOf(
          "asyncNative" to "true",
          "library" to "apache-httpclient",
          "serializationLibrary" to "jackson",
          "apiPackage" to "com.simiacryptus.api.java.modelslab",
          "modelPackage" to "com.simiacryptus.api.java.modelslab.model",
        )
      )
  }

  private fun process(
    outDir: String,
    apiname: String,
    suffix: String,
    url: String,
    generator: String,
    additionalProperties : Map<String, String> = mapOf(),
  ) {
    try {
      File(outDir).mkdirs()
      val inputFile = File.createTempFile(apiname, suffix).apply {
        val text = URI(url).toURL().readText()
        writeText(text)
        deleteOnExit()
      }.absolutePath
      OpenAPIGenerator.main(
        arrayOf(
          "generate",
          "--skip-validate-spec",
          "-i", inputFile,
          "-g", generator,
          "-o", outDir,
          if (additionalProperties.isEmpty()) null else (
              "--additional-properties=" + additionalProperties.entries.joinToString(",") { "${it.key}=${it.value}" })
        ).filterNotNull().toTypedArray()
      )
    } catch (e: SpecValidationException) {
      e.printStackTrace()
    }
  }
}
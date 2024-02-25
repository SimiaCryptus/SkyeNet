package com.simiacryptus.skyenet.webui

import org.openapitools.codegen.SpecValidationException
import java.io.File

class TestOpenAPITool {
  val tempFile = File.createTempFile("openapi", ".json").apply {
    writeText(
      """
      {
    "openapi" : "3.0.0",
    "info" : {
      "title" : "Gmail Labels API",
      "version" : "1.0.0",
      "description" : "API for fetching labels from a Gmail account."
    },
    "paths" : {
      "/tools/gmail/labels" : {
        "get" : {
          "summary" : "Get Gmail labels",
          "description" : "Retrieves a list of labels from the user's Gmail account.",
          "responses" : {
            "200" : {
              "description" : "A list of Gmail labels",
              "content" : {
                "application/json" : {
                  "schema" : {
                    "properties" : { }
                  }
                }
              }
            },
            "500" : {
              "description" : "Internal Server Error",
              "content" : {
                "text/plain" : {
                  "schema" : {
                    "type" : "string",
                    "properties" : { }
                  }
                }
              }
            }
          }
        }
      }
    },
    "components" : {
      "schemas" : {
        "LabelsResponse" : {
          "type" : "object",
          "properties" : {
            "labels" : {
              "type" : "array",
              "properties" : { }
            }
          }
        },
        "Label" : {
          "type" : "object",
          "properties" : {
            "id" : {
              "type" : "string",
              "properties" : { }
            },
            "name" : {
              "type" : "string",
              "properties" : { }
            }
          }
        }
      },
      "responses" : { },
      "parameters" : { },
      "examples" : { },
      "requestBodies" : { },
      "headers" : { },
      "securitySchemes" : { },
      "links" : { },
      "callbacks" : { }
    }
  }
    """.trimIndent()
    )
    deleteOnExit()
  }

//  @Test
  fun test() {
    //    val openApiTool = OpenAPITool()
    //    openApiTool.generate()


    try {
      val generator = "java"
      File("C:/Users/andre/Downloads/openapi/build/openapi-$generator").mkdirs()
      org.openapitools.codegen.OpenAPIGenerator.main(
        arrayOf(
          "generate",
          "--skip-validate-spec",
          "-i", "C:/Users/andre/Downloads/openapi.yaml",
          "-g", generator,
          "-o", "C:/Users/andre/Downloads/openapi/build/openapi-$generator",
        )
      )
    } catch (e: SpecValidationException) {

      e.printStackTrace()
    }

  }
}
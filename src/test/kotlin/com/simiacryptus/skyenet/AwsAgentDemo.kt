@file:Suppress("unused")

package com.simiacryptus.skyenet

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.rds.AmazonRDSClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.simiacryptus.skyenet.util.AgentDemoBase
import com.simiacryptus.skyenet.webui.SkyenetSimpleSessionServer
import com.simiacryptus.util.YamlDescriber
import org.junit.jupiter.api.Test
import java.awt.Desktop
import java.io.File
import java.net.URI

class AwsAgentDemo : AgentDemoBase() {

    class AwsClients(val defaultRegion: Regions) {

        fun s3() = AmazonS3ClientBuilder.standard().withRegion(defaultRegion).build()

        fun ec2() = AmazonEC2ClientBuilder.standard().withRegion(defaultRegion).build()

        fun rds() = AmazonRDSClientBuilder.standard().withRegion(defaultRegion).build()

        fun cloudwatch() = AmazonCloudWatchClientBuilder.standard().withRegion(defaultRegion).build()

        fun route53() = com.amazonaws.services.route53.AmazonRoute53ClientBuilder
            .standard().withRegion(defaultRegion).build()

        fun emr() = com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder
            .standard().withRegion(defaultRegion).build()

        fun lambda() = com.amazonaws.services.lambda.AWSLambdaClientBuilder
            .standard().withRegion(defaultRegion).build()

    }

    class HttpUtil {
        fun client() = org.apache.http.impl.client.HttpClients.createDefault()

    }

    override fun apiObjects() = mapOf(
        "aws" to AwsClients(Regions.US_EAST_1),
        "client" to HttpUtil(),
    )

    class AwsYamlDescriber : YamlDescriber() {
        override fun toYaml(
            rawType: Class<in Nothing>,
            stackMax: Int,
        ): String {
            val abbreviated = listOf(
                "com.amazonaws",
                "org.apache"
            )
            if (abbreviated.find { rawType.name.startsWith(it) } != null)
                return """
                |type: object
                |class: ${rawType.name}
                """.trimMargin()
            return super.toYaml(rawType, stackMax)
        }
    }

    @Test
    fun testWebAgent() {
        val port = 8080
        val agentDemoBase = this
        val server = object : SkyenetSimpleSessionServer(
            oauthConfig = File(File(System.getProperty("user.home")), "client_secret_google_oauth.json").absolutePath,
            yamlDescriber = AwsYamlDescriber(),
        ) {
            override fun apiObjects(): Map<String, Any> {
                return agentDemoBase.apiObjects()
            }

            override fun toString(e: Throwable): String {
                return e.message ?: e.toString()
            }

            //            override val model = "gpt-3.5-turbo"
            override val model = "gpt-4-0314"

            override fun heart(apiObjects: Map<String, Any>): Heart {
                return agentDemoBase.heart(apiObjects)
            }
        }.start(port)
        Desktop.getDesktop().browse(URI("http://localhost:$port/"))
        server.join()
    }

}


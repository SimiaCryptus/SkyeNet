package com.simiacryptus.skyenet.core.util

import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

open class AwsUtil {
    open val kmsClient: KmsClient by lazy {
        KmsClient.builder()
                .region(Region.US_EAST_1) // Specify the region or use the default region provider chain
                .build()
    }

    fun encryptFile(inputFilePath: String, outputFilePath: String) {
        val filePath = Paths.get(inputFilePath)
        val fileBytes = Files.readAllBytes(filePath)
        encryptData(fileBytes, outputFilePath)
    }

    fun encryptData(fileBytes: ByteArray, outputFilePath: String) {
        val encryptRequest = EncryptRequest.builder()
                .keyId("arn:aws:kms:us-east-1:470240306861:key/a1340b89-64e6-480c-a44c-e7bc0c70dcb1")
                .plaintext(SdkBytes.fromByteArray(fileBytes))
                .build()
        val result = kmsClient.encrypt(encryptRequest)
        val cipherTextBlob = result.ciphertextBlob()
        val encryptedData = Base64.getEncoder().encodeToString(cipherTextBlob.asByteArray())
        val outputPath = Paths.get(outputFilePath)
        Files.write(outputPath, encryptedData.toByteArray())
    }

    fun decryptResource(resourceFile: String, classLoader: ClassLoader = javaClass.classLoader!!): String {
        val encryptedData = classLoader.getResourceAsStream(resourceFile)?.readAllBytes()
        if (null == encryptedData) {
            throw RuntimeException("Unable to load resource: $resourceFile")
        }
        val decodedData = Base64.getDecoder().decode(encryptedData)
        val decryptRequest = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(decodedData))
                .build()
        val decryptResult = kmsClient.decrypt(decryptRequest)
        val decryptedData = decryptResult.plaintext().asByteArray()
        return String(decryptedData, StandardCharsets.UTF_8)
    }

  companion object : AwsUtil() {
    val log = LoggerFactory.getLogger(AwsUtil::class.java)
  }
}

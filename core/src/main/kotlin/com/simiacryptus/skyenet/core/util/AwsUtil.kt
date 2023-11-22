package com.simiacryptus.skyenet.core.util

import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.kms.model.DecryptRequest
import com.amazonaws.services.kms.model.EncryptRequest
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object AwsUtil {

    fun encryptFile(inputFilePath: String, outputFilePath: String) {
        val filePath = Paths.get(inputFilePath)
        val fileBytes = Files.readAllBytes(filePath)
        val kmsClient = AWSKMSClientBuilder.standard().build()
        val encryptRequest =
            EncryptRequest().withKeyId("arn:aws:kms:us-east-1:470240306861:key/a1340b89-64e6-480c-a44c-e7bc0c70dcb1")
                .withPlaintext(ByteBuffer.wrap(fileBytes))
        val result = kmsClient.encrypt(encryptRequest)
        val cipherTextBlob = result.ciphertextBlob
        val encryptedData = Base64.getEncoder().encodeToString(cipherTextBlob.array())
        val outputPath = Paths.get(outputFilePath)
        Files.write(outputPath, encryptedData.toByteArray())
    }

    fun decryptResource(resourceFile: String, classLoader: ClassLoader = javaClass.classLoader!!): String {
        val encryptedData = classLoader.getResourceAsStream(resourceFile)?.readAllBytes()
        if (null == encryptedData) {
            throw RuntimeException("Unable to load resource: $resourceFile")
        }
        val decodedData = Base64.getDecoder().decode(encryptedData)
        val kmsClient = AWSKMSClientBuilder.defaultClient()
        val decryptRequest = DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(decodedData))
        val decryptResult = kmsClient.decrypt(decryptRequest)
        val decryptedData = decryptResult.plaintext.array()
        return String(decryptedData, StandardCharsets.UTF_8)
    }
}
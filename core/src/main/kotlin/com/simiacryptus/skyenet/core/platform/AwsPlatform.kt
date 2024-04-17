package com.simiacryptus.skyenet.core.platform

import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.nio.charset.StandardCharsets
import java.util.*

open class AwsPlatform(
    private val bucket: String = System.getProperty("share_bucket", "share.simiacrypt.us"),
    override val shareBase: String = System.getProperty("share_base", "https://share.simiacrypt.us"),
    private val region: Region? = Region.US_EAST_1
) : CloudPlatformInterface {
    protected open val kmsClient: KmsClient by lazy {
        KmsClient.builder().region(Region.US_EAST_1)
            //.credentialsProvider(ProfileCredentialsProvider.create("data"))
            .build()
    }

    protected open val s3Client: S3Client by lazy {
        S3Client.builder()
            .region(region)
            .build()
    }

    override fun upload(
        path: String,
        contentType: String,
        bytes: ByteArray
    ): String {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket).key(path.replace("/{2,}".toRegex(), "/").removePrefix("/"))
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes)
        )
        return "$shareBase/$path"
    }

    override fun upload(
        path: String,
        contentType: String,
        request: String
    ): String {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket).key(path.replace("/{2,}".toRegex(), "/").removePrefix("/"))
                .contentType(contentType)
                .build(),
            RequestBody.fromString(request)
        )
        return "$shareBase/$path"
    }


    override fun encrypt(fileBytes: ByteArray, keyId: String): String? =
        Base64.getEncoder().encodeToString(
            kmsClient.encrypt(
                EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(SdkBytes.fromByteArray(fileBytes))
                    .build()
            ).ciphertextBlob().asByteArray()
        )

    override fun decrypt(encryptedData: ByteArray): String = String(
        kmsClient.decrypt(
            DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(Base64.getDecoder().decode(encryptedData)))
                .build()
        ).plaintext().asByteArray(), StandardCharsets.UTF_8
    )

    companion object {
        val log = LoggerFactory.getLogger(AwsPlatform::class.java)
        fun get() = try {
            AwsPlatform()
        } catch (e: Throwable) {
            log.info("Error initializing AWS platform", e)
            null
        }
    }
}
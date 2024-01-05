package com.simiacryptus.skyenet.core.platform

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

open class S3Uploader(
  private val bucket: String = "share.simiacrypt.us",
  override val shareBase: String = "https://share.simiacrypt.us"
) : UploaderInterface {
  override fun upload(
    path: String,
    contentType: String,
    bytes: ByteArray
  ) : String {
    S3Client.builder()
      .region(Region.US_EAST_1)
      .build().putObject(
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
  ) : String {
    S3Client.builder()
      .region(Region.US_EAST_1)
      .build().putObject(
        PutObjectRequest.builder()
          .bucket(bucket).key(path.replace("/{2,}".toRegex(), "/").removePrefix("/"))
          .contentType(contentType)
          .build(),
        RequestBody.fromString(request)
      )
    return "$shareBase/$path"
  }

}
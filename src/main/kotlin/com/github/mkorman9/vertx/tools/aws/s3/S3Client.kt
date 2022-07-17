package com.github.mkorman9.vertx.tools.aws.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.io.ByteArrayInputStream
import java.io.File

class S3Client private constructor() {
    companion object {
        fun create(): S3Client {
            return S3Client()
        }

        private const val READ_BUFFER_SIZE = 1024
    }

    private val client: AmazonS3 = AmazonS3ClientBuilder
        .standard()
        .build()

    fun getObject(vertx: Vertx, bucketName: String, key: String): Future<Buffer> {
        return vertx.executeBlocking { call ->
            try {
                val obj = client.getObject(bucketName, key)
                val contentStream = obj.objectContent
                val output = Buffer.buffer()

                val buffer = ByteArray(READ_BUFFER_SIZE)
                while (true) {
                    val bytesRead = contentStream.read(buffer)
                    if (bytesRead == -1) {
                        break
                    }

                    output.appendBytes(buffer)
                }

                call.complete(output)
            } catch (e: Exception) {
                call.fail(e)
            }
        }
    }

    fun putObject(vertx: Vertx, bucketName: String, key: String, content: Buffer): Future<Void> {
        return vertx.executeBlocking { call ->
            try {
                client.putObject(
                    bucketName,
                    key,
                    ByteArrayInputStream(content.bytes),
                    ObjectMetadata()
                )

                call.complete()
            } catch (e: Exception) {
                call.fail(e)
            }
        }
    }

    fun putFile(vertx: Vertx, bucketName: String, key: String, filePath: String): Future<Void> {
        return vertx.executeBlocking { call ->
            try {
                client.putObject(
                    bucketName,
                    key,
                    File(filePath)
                )

                call.complete()
            } catch (e: Exception) {
                call.fail(e)
            }
        }
    }
}

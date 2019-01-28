package uk.me.krupa.s3web.service

import com.amazonaws.AmazonServiceException
import com.amazonaws.SdkClientException
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.*
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.reactivex.Maybe
import io.reactivex.Single
import mu.KotlinLogging
import software.amazon.awssdk.http.HttpStatusCode
import java.io.ByteArrayInputStream
import javax.inject.Singleton

private val logger = KotlinLogging.logger {  }

@Singleton
@Requires(property = "backend.mode", value = "s3", defaultValue = "s3")
class V1SyncS3Backend(
        @Property(name = "aws.s3.region") val region: String,
        @Property(name = "aws.s3.bucket") val bucketName: String,
        @Property(name = "aws.access.key.id") val accessKeyId: String,
        @Property(name = "aws.secret.access.key") val secretAccessKey: String,
        @Property(name = "aws.s3.kms.key.id") val kmsKeyId: String? = null,
        @Property(name = "aws.s3.endpoint") val endpoint: String? = null

): Backend {

    val s3 = AmazonS3ClientBuilder.standard()
            .let {
                endpoint?.let { endpoint ->
                    it.withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(endpoint, region))
                } ?: it
            }
            .enablePathStyleAccess()
            .withCredentials(
                    AWSStaticCredentialsProvider(
                            BasicAWSCredentials(accessKeyId, secretAccessKey)
                    )
            )
            .build()

    override fun deleteObject(path: String): Single<Boolean> {
        return try {
            s3.deleteObject(DeleteObjectRequest(bucketName, path))
            Single.just(true)
        } catch (e: SdkClientException) {
            Single.error(e)
        } catch (e: AmazonServiceException) {
            Single.error(e)
        }

    }

    override fun uploadObject(path: String, data: ByteArray): Single<String> {
        logger.info("PUT to S3: {}", path)

        ByteArrayInputStream(data).use { stream ->
            val request = PutObjectRequest(bucketName, path, stream, ObjectMetadata())
                    .let {
                        kmsKeyId?.let { key ->
                            it.sseAwsKeyManagementParams = SSEAwsKeyManagementParams(key)
                        }
                        it
                    }

            return try {
                s3.putObject(request)
                Single.just(path)
            } catch (e: SdkClientException) {
                Single.error(e)
            } catch (e: AmazonServiceException) {
                Single.error(e)
            }
        }
    }

    override fun getObject(path: String): Maybe<ByteArray> {
        val result = s3.getObject(bucketName, path)
        return try {
            result.objectContent.use {
                Maybe.just(it.readBytes())
            }
        } catch (e: SdkClientException) {
            Maybe.error(e)
        } catch (e: AmazonServiceException) {
            if (e.statusCode == HttpStatusCode.NOT_FOUND) {
                Maybe.empty<ByteArray>()
            } else {
                Maybe.error(e)
            }
        }
    }

    override fun listFiles(path: String): Maybe<List<String>> {
        val prefix = path

        val paths = mutableListOf<String>()
        var response: ObjectListing? = null

        return try {
            do {
                response = response?.let {
                    s3.listNextBatchOfObjects(response)
                } ?: s3.listObjects(bucketName, prefix)

                response?.let {
                    paths.addAll(it.commonPrefixes)
                    paths.addAll(it.objectSummaries.map { it.key })
                }
                paths.addAll(response?.commonPrefixes ?: emptyList())
            } while (response?.isTruncated == true)

            Maybe.just(
                    paths.map { path ->
                        if (path.contains('/')) {
                            path.substring(0, path.indexOf('/') + 1)
                        } else {
                            path
                        }
                    }
            )

        } catch (e: SdkClientException) {
            Maybe.error(e)
        } catch (e: AmazonServiceException) {
            Maybe.error(e)
        }
    }
}
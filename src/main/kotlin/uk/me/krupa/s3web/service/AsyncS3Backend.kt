package uk.me.krupa.s3web.service

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.net.URI
import javax.inject.Singleton

private val logger = KotlinLogging.logger {  }

@Singleton
@Requires(property = "backend.mode", value = "s3", defaultValue = "s3")
class AsyncS3Backend(
        @Property(name = "aws.s3.region") val region: String,
        @Property(name = "aws.s3.bucket") val bucketName: String,
        @Property(name = "aws.access.key.id") val accessKeyId: String,
        @Property(name = "aws.secret.access.key") val secretAccessKey: String,
        @Property(name = "aws.s3.kms.key.id") val kmsKeyId: String? = null,
        @Property(name = "aws.s3.endpoint") val endpoint: URI? = null

): Backend {

    val s3 = S3AsyncClient.builder()
            .let {
                endpoint?.let { endpoint -> it.endpointOverride(endpoint) } ?: it
            }
            .serviceConfiguration {
                // TODO: https://github.com/aws/aws-sdk-java-v2/issues/953
                it.checksumValidationEnabled(false)
                it.pathStyleAccessEnabled(true)
            }
            .endpointOverride(endpoint)
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
            .build()

    override fun deleteObject(path: String): Single<Boolean> {
        return DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build()
                .let { s3.deleteObject(it) }
                .let { Single.fromFuture(it) }
                .map { true }
    }

    override fun uploadObject(path: String, data: ByteArray): Single<String> {
        logger.info("PUT to S3: {}", path)
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .contentLength(data.size.toLong())
                .key(path)
                .let { config ->
                    kmsKeyId?.run {
                        config.serverSideEncryption(ServerSideEncryption.AWS_KMS).ssekmsKeyId(this)
                    } ?: config
                }
                .build()
                .let { s3.putObject(it, AsyncRequestBody.fromBytes(data)) }
                .let { Single.fromFuture(it) }
                .doOnSuccess { logger.info("PUT to S3: {} - Done", path) }
                .map { path }
    }

    override fun getObject(path: String): Maybe<ByteArray> {
        return GetObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build()
                .let { s3.getObject(it, AsyncResponseTransformer.toBytes()) }
                .let { Maybe.fromFuture(it) }
                .map { it.asByteArray() }
    }

    private fun doListFiles(prefix: String, marker: String? = null): Flowable<ListObjectsResponse> {
        return ListObjectsRequest.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .let { if (marker != null) it.marker(marker) else it }
                .build()
                .let { s3.listObjects(it) }
                .let { Flowable.fromFuture(it) }
                .flatMap { resp ->
                    if (resp.isTruncated) {
                        Flowable.just(resp).concatWith(Flowable.defer {
                            doListFiles(prefix, resp.contents().last().key())
                        })
                    } else {
                        Flowable.just(resp)
                    }
                }
    }

    override fun listFiles(path: String): Maybe<List<String>> {
        val prefix = path.removePrefix("/")
        return doListFiles(prefix, null)
                .map {
                    it.commonPrefixes().map { it.prefix() } + it.contents().map { it.key() }
                }.map {
                    it.map { path ->
                        path.removePrefix(prefix)
                    }.map {
                        path -> if (path.contains('/')) { path.substring(0, path.indexOf('/') + 1) } else { path }
                    }
                }.reduce { a, b -> a + b }
                .map { HashSet(it).toList() }
    }
}
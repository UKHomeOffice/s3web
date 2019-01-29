package uk.me.krupa.s3web.service

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
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
@Requires(property = "backend.mode", value = "s3-async", defaultValue = "s3")
class AsyncS3Backend(
        @Property(name = "aws.s3.region") val region: String,
        @Property(name = "aws.s3.bucket") val bucketName: String,
        @Property(name = "aws.access.key.id") val accessKeyId: String,
        @Property(name = "aws.secret.access.key") val secretAccessKey: String,
        @Property(name = "aws.s3.kms.key.default") val useDefaultKmsKey: Boolean = false,
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
        val uploadPath = path.removePrefix("/")
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .contentLength(data.size.toLong())
                .key(uploadPath)
                .let { config ->
                    when {
                        kmsKeyId != null -> config.serverSideEncryption(ServerSideEncryption.AWS_KMS).ssekmsKeyId(kmsKeyId)
                        useDefaultKmsKey -> config.serverSideEncryption(ServerSideEncryption.AWS_KMS)
                        else -> config
                    }
                }
                .build()
                .let { s3.putObject(it, AsyncRequestBody.fromBytes(data)) }
                .let { Single.fromFuture(it) }
                .doOnSuccess { logger.info("PUT to S3: {} - Done", path) }
                .map {
                    path
                }
    }

    override fun getObject(path: String): Maybe<ByteArray> {
        val uploadPath = path.removePrefix("/")
        return GetObjectRequest.builder()
                .bucket(bucketName)
                .key(uploadPath)
                .build()
                .let { s3.getObject(it, AsyncResponseTransformer.toBytes()) }
                .let { Maybe.fromFuture(it) }
                .map { it.asByteArray() }
    }

    override fun listFiles(path: String): Maybe<List<String>> {
        val prefix = path.removePrefix("/")

        val behaviour: BehaviorProcessor<String> = BehaviorProcessor.createDefault("")

        return behaviour.concatMap { marker ->
            ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .let {
                        if (marker == "") {
                            it
                        } else {
                            it.marker(marker)
                        }
                    }
                    .build()
                    .let { Flowable.fromFuture(s3.listObjects(it)) }
        }.doOnNext { response ->
            if (response.isTruncated) {
                behaviour.onNext(response.contents().last().key())
            } else {
                behaviour.onComplete()
            }
        }.take(5).concatMapIterable {
            it.contents()
        }.map {
            it.key()
        }.map {
            it.removePrefix(prefix)
        }.map { path ->
            if (path.contains('/')) {
                path.substring(0, path.indexOf('/') + 1)
            } else {
                path
            }
        }.toList().toMaybe().map { it.toSet().toList() }
    }
}
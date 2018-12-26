package uk.me.krupa.s3web.service

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.reactivex.Maybe
import io.reactivex.Single
import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.ServerSideEncryption
import java.net.URI
import javax.inject.Singleton

private val logger = KotlinLogging.logger {  }

@Singleton
@Requires(property = "backend.mode", value = "s3", defaultValue = "s3")
class S3Backend(
        @Property(name = "aws.s3.region") val region: String,
        @Property(name = "aws.s3.buckety") val bucketName: String,
        @Property(name = "aws.access.key.id") val accessKeyId: String,
        @Property(name = "aws.secret.access.key") val secretAccessKey: String,
        @Property(name = "aws.s3.kms.key.id") val kmsKeyId: String? = null,
        @Property(name = "aws.s3.endpoint") val endpoint: URI? = null

): Backend {

    val s3 = S3AsyncClient.builder()
            .let {
                endpoint?.let { endpoint -> it.endpointOverride(endpoint) } ?: it
            }
            .endpointOverride(endpoint)
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
            .build()

    override fun deleteObject(path: String): Single<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun uploadObject(path: String, data: ByteArray): Single<String> {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .contentLength(data.size.toLong())
                .key(path)
                .let {
                    kmsKeyId?.run { it.serverSideEncryption(ServerSideEncryption.AWS_KMS).ssekmsKeyId(kmsKeyId) } ?: it
                }
                .build()
                .let { s3.putObject(it, AsyncRequestBody.fromBytes(data)) }
                .let { Single.fromFuture(it) }
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

    override fun listFiles(path: String): Maybe<List<String>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
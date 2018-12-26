package uk.me.krupa.s3web.service

import com.github.javafaker.Faker
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldBe
import io.reactivex.Single
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.Charset
import java.time.Duration
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

object S3BackendSpec: Spek({
    describe("An S3 adapter") {
        val localStack = KGenericContainer("localstack/localstack:0.8.8")
                .withExposedPorts(4572)
                .withEnv("SERVICES", "s3")
                .withLogConsumer(Slf4jLogConsumer(LoggerFactory.getLogger("LocalStack")))
                .waitingFor(LogMessageWaitStrategy().withRegEx("(?s).*Ready.*").withStartupTimeout(Duration.ofSeconds(30)))
        var subjectUnderTest: S3Backend? = null

        beforeGroup {
            localStack.start()
            subjectUnderTest = S3Backend(
                    Region.EU_WEST_2.id(),
                    "mine",
                    "accessKey",
                    "secretKey",
                    endpoint = URI.create("http://${localStack.containerIpAddress}:${localStack.getMappedPort(4572)}")
            )
            CreateBucketRequest.builder()
                    .bucket(subjectUnderTest!!.bucketName)
                    .build()
                    .let { subjectUnderTest!!.s3!!.createBucket(it) }
                    .let { Single.fromFuture(it) }
                    .blockingGet()
        }

        afterGroup {
            localStack.stop()
        }

        describe("a valid file") {
            val filename = Faker().file().fileName()
            val data = UUID.randomUUID().toString().toByteArray(Charset.forName("UTF-8"))

            it("can be uploaded") {
                val result = subjectUnderTest?.uploadObject(filename, data)?.blockingGet()
                result shouldBe filename
            }

            it("can be downloaded") {
                val result = subjectUnderTest?.getObject(filename)?.blockingGet()
                result shouldBe data
            }
        }
    }
})
package uk.me.krupa.s3web.service

import com.github.javafaker.Faker
import io.kotlintest.shouldBe
import mu.KotlinLogging
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.net.URI
import java.nio.charset.Charset
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

private val logger = KotlinLogging.logger {  }

object BackendSpec: Spek({

    val localStack = KGenericContainer("localstack/localstack:0.8.8")
            .withExposedPorts(4572)
            .withEnv("SERVICES", "s3")
            .withLogConsumer(Slf4jLogConsumer(LoggerFactory.getLogger("LocalStack")))
            .waitingFor(LogMessageWaitStrategy().withRegEx("(?s).*Ready.*").withStartupTimeout(Duration.ofSeconds(30)))

    var asyncS3Backend: AsyncS3Backend? = null
    val stubBackend = StubBackend()

    describe("Available S3 adapters") {
        beforeGroup {
            logger.info { "Starting LocalStack" }
            localStack.start()
            logger.info { "Assigning s3Backend" }
            val uri = URI.create("http://${localStack.containerIpAddress}:${localStack.getMappedPort(4572)}")
            asyncS3Backend = AsyncS3Backend(
                    Region.EU_WEST_2.id(),
                    "mine",
                    "accessKey",
                    "secretKey",
                    endpoint = uri
            )
            CreateBucketRequest.builder()
                    .bucket(asyncS3Backend!!.bucketName)
                    .build()
                    .let { asyncS3Backend!!.s3!!.createBucket(it) }
                    .get(10, TimeUnit.SECONDS)
        }

        afterGroup {
            logger.info { "Terminating LocalStack" }
            localStack.stop()
        }

        mapOf(
                "In-memory" to { stubBackend },
                "Asynchronous S3" to { asyncS3Backend }
        ).forEach { key, factory ->
            describe(key) {
                describe("a valid file") {
                    val filename = Faker().file().fileName()
                    val data = UUID.randomUUID().toString().toByteArray(Charset.forName("UTF-8"))

                    it("can be uploaded") {
                        val result = factory()?.uploadObject(filename, data)?.blockingGet()
                        result shouldBe filename
                    }

                    it("can be downloaded") {
                        val result = factory()?.getObject(filename)?.blockingGet()
                        result shouldBe data
                    }
                }
            }
        }
    }
})
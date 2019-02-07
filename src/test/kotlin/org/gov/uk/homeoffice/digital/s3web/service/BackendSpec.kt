package org.gov.uk.homeoffice.digital.s3web.service

import com.github.javafaker.Faker
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.nio.charset.Charset
import java.util.*

object BackendSpec: Spek({

    val stubBackend = StubBackend()

    describe("Available S3 adapters") {
        mapOf(
                "In-memory" to { stubBackend }
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

                    it ("can be deleted") {
                        val result = factory()?.deleteObject(filename)?.blockingGet()
                        result shouldBe true
                    }
                }

                describe("1020 files") {
                    val data = UUID.randomUUID().toString().toByteArray(Charset.forName("UTF-8"))
                    it("can all be uploaded") {
                        (1..1020).forEach {
                            val dir = if (it < 1010) "0" else "1"
                            val filename = "/$dir/$it.txt"
                            factory()?.uploadObject(filename, data)?.blockingGet() shouldBe filename
                        }
                    }

                    it("Only lists 2 entries in the root directory") {
                        val listing = factory()?.listFiles("/")?.blockingGet()
                        listing shouldNotBe null
                        listing?.let {
                            it shouldContainExactlyInAnyOrder  listOf("0/", "1/")
                        }
                    }

                    it ("Lists 11 files in 1/") {
                        val listing = factory()?.listFiles("/1/")?.blockingGet()
                        listing shouldNotBe null
                        listing?.let {
                            it shouldContainExactlyInAnyOrder  (1010..1020).map { "$it.txt" }
                        }
                    }

                    it ("Lists 1009 files in 1/") {
                        val listing = factory()?.listFiles("/0/")?.blockingGet()
                        listing shouldNotBe null
                        listing?.let {
                            it.size shouldBe 1009
                            it shouldContainExactlyInAnyOrder  (1..1009).map { "$it.txt" }
                        }
                    }
                }
            }
        }
    }
})
package uk.me.krupa.s3web.service

import com.github.javafaker.Faker
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldBe
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.*

const val PREFIX = "/dir/mine.tar"

object TarballExtractorSpec: Spek({
    describe("A Tarball Extractor") {
        val backend = StubBackend()
        val subjectUnderTest = TarballExtractor(backend)
        val faker = Faker()
        val expectedContents = (1..20).map { faker.file().fileName().toString() to UUID.randomUUID().toString() }.toMap()

        describe("a valid tarball") {
            val underlying = ByteArrayOutputStream()
            TarArchiveOutputStream(underlying).use {
                expectedContents.forEach { key, value ->
                    val entry = TarArchiveEntry(key)
                    val data = value.toByteArray(Charset.forName("UTF-8"))
                    entry.size = data.size.toLong()
                    it.putArchiveEntry(entry)
                    it.write(data)
                    it.closeArchiveEntry()
                }
            }

            TarArchiveInputStream(ByteArrayInputStream(underlying.toByteArray())).use {
                subjectUnderTest.uploadTar(PREFIX, it).blockingLast()
            }

            describe("the list of uploaded files") {
                it("should contain all tar entries") {
                    backend.storage.keys shouldContainExactlyInAnyOrder expectedContents.keys.map { "$PREFIX/$it" }
                }

                it("should all have the correct contents") {
                    expectedContents.forEach { key, expectedValue ->
                        val value = String(backend.storage["$PREFIX/$key"] ?: ByteArray(0), Charset.forName("UTF-8"))
                        value shouldBe expectedValue
                    }
                }
            }
        }
    }
})
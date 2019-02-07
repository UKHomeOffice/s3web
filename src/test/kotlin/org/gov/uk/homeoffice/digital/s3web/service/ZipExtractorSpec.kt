package org.gov.uk.homeoffice.digital.s3web.service

import com.github.javafaker.Faker
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val ZIP_PREFIX = "/dir/mine.zip"

object ZipExtractorSpec: Spek({
    describe("A ZIP Extractor") {
        val backend = StubBackend()
        val subjectUnderTest = ZipExtractor(backend)
        val faker = Faker()
        val expectedContents = (1..6000).map { faker.file().fileName().toString() to UUID.randomUUID().toString() }.toMap()

        describe("a valid zip") {
            val underlying = ByteArrayOutputStream()
            ZipOutputStream(underlying).use {
                expectedContents.forEach { key, value ->
                    val entry = ZipEntry(key)
                    val data = value.toByteArray(Charset.forName("UTF-8"))
                    entry.size = data.size.toLong()
                    it.putNextEntry(entry)
                    it.write(data)
                    it.closeEntry()
                }
            }

            ZipInputStream(ByteArrayInputStream(underlying.toByteArray())).use {
                subjectUnderTest.uploadZip(ZIP_PREFIX, it).blockingLast()
            }

            describe("the list of uploaded files") {
                it("should contain all ZIP entries") {
                    backend.storage.keys shouldContainExactlyInAnyOrder expectedContents.keys.map { "$ZIP_PREFIX/$it" }
                }

                it("should all have the correct contents") {
                    expectedContents.forEach { key, expectedValue ->
                        val value = String(backend.storage["$ZIP_PREFIX/$key"] ?: ByteArray(0), Charset.forName("UTF-8"))
                        value shouldBe expectedValue
                    }
                }
            }
        }
    }
})
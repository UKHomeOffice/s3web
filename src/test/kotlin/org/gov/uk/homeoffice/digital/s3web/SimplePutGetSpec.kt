package org.gov.uk.homeoffice.digital.s3web

import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.haveSubstring
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.http.*
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.apache.commons.compress.utils.IOUtils
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.nio.file.Paths
import java.util.*

object SimplePutGetSpec: Spek({
    describe("Simple PUT and GET specifications") {
        val embeddedServer : EmbeddedServer = ApplicationContext.run(EmbeddedServer::class.java, mapOf("backend.mode" to "stub"))
        val client : HttpClient = HttpClient.create(embeddedServer.url)

        listOf(
                listOf("/index.html", "text/html"),
                listOf("/in/a/directory/index.gif", "image/gif"),
                listOf("/test.txt", "text/plain"),
                listOf("/test.hfrmp", "application/octet-stream")
        ).forEach { (path, contentType) ->
            describe(path) {

                val content = UUID.randomUUID().toString()
                it("can be uploaded") {
                    val request = HttpRequest.PUT<String>(
                            path,
                            content)
                    request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                    val rsp = client.toBlocking().exchange(request, Argument.VOID, Argument.VOID)
                    rsp.status shouldBe HttpStatus.CREATED
                    rsp.body() shouldBe null
                }

                it("can be downloaded with content type $contentType") {
                    val rsp = client.toBlocking().exchange(path, String::class.java)
                    rsp.header("Content-Type") shouldBe contentType
                    rsp.body() shouldBe content
                }

                it ("is listed when requesting a list of files in the parent directory") {
                    val pathSpec = Paths.get(path)
                    val rsp = client.toBlocking().exchange(pathSpec.parent.toString(), List::class.java)

                    rsp.header(HttpHeaders.CONTENT_TYPE) shouldBe "application/json"
                    rsp.body() shouldNotBe null
                    val prefix = """http://${embeddedServer.host}:${embeddedServer.port}"""
                    rsp.body()?.map { it.toString() }?.let {
                        it shouldContain prefix + pathSpec.toString()
                    }
                }

                it("can be deleted") {
                    val rsp = client.toBlocking().exchange<Any,String>(HttpRequest.DELETE(path))
                    rsp.status shouldBe HttpStatus.NO_CONTENT
                    rsp.body() shouldBe null
                }


            }
        }

        listOf(
                listOf("Uncompressed tar file", "test.tar"),
                listOf("Compressed tar file", "test.tar.gz"),
                listOf("ZIP file", "test.zip")
        ).forEach {(description, filename) ->
            describe(description) {
                val data = javaClass.classLoader.getResourceAsStream(filename).use {
                    IOUtils.toByteArray(it)
                }

                it("can be uploaded") {
                    val request = HttpRequest.PUT<ByteBuf>("/$filename", Unpooled.copiedBuffer(data))
                    request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                    val rsp: HttpResponse<Any> = client.toBlocking().exchange(request)
                    rsp.status shouldBe HttpStatus.CREATED
                }

                it("uploaded correct data") {
                    val rsp = client.toBlocking().retrieve("/$filename/site/index.html", String::class.java)
                    rsp shouldNotBe null
                    rsp.should(haveSubstring("<h5>Project Documentation</h5>"))
                }
            }
        }

        afterGroup {
            client.close()
            embeddedServer.close()
        }
    }
})
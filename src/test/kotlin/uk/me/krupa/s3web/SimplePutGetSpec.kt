package uk.me.krupa.s3web

import io.kotlintest.shouldBe
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.*

object SimplePutGetSpec: Spek({
    describe("Simple PUT and GET specifications") {
        val embeddedServer : EmbeddedServer = ApplicationContext.run(EmbeddedServer::class.java)
        val client : HttpClient = HttpClient.create(embeddedServer.url)

        listOf(
                listOf("/index.html", "text/html"),
                listOf("/in/a/directory/index.gif", "image/gif"),
                listOf("/test.txt", "text/plain"),
                listOf("/test.hfrmp", "application/octet-stream")
        ).forEach { (path, contentType) ->
            describe(path) {

                afterEachTest {
                    client.toBlocking().exchange<Any,String>(HttpRequest.DELETE(path))
                }

                val content = UUID.randomUUID().toString()
                it("can be uploaded") {
                    val request = HttpRequest.PUT<String>(
                            path,
                            content)
                    request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                    val rsp = client.toBlocking().exchange(request, Argument.VOID, Argument.VOID)
                    rsp.status shouldBe HttpStatus.CREATED
                }

                it("can be downloaded with content type $contentType") {
                    val data = client.toBlocking().exchange(path, String::class.java)
                    data.header("Content-Type") shouldBe contentType
                    data.body() shouldBe content
                }
            }
        }


        afterGroup {
            client.close()
            embeddedServer.close()
        }
    }
})
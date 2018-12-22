package uk.me.krupa.s3web

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
import kotlin.test.assertEquals

object SimplePutGetSpec: Spek({
    describe("Simple PUT and GET specifications") {
        val embeddedServer : EmbeddedServer = ApplicationContext.run(EmbeddedServer::class.java)
        val client : HttpClient = HttpClient.create(embeddedServer.url)

        test("GET from root directory returns data") {
            val rsp = client.toBlocking().retrieve("/index.html", String::class.java)
            assertEquals("<html>test</html>", rsp)
        }

        listOf("/index.html", "/in/a/directory/index.html").forEach { path ->
            test("PUT to $path returns 201 status") {
                val request = HttpRequest.PUT<String>(
                        path,
                        "<html>test</html>")
                request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                val rsp = client.toBlocking().exchange(request, Argument.VOID, Argument.VOID)
                assertEquals(rsp.status, HttpStatus.ACCEPTED)
            }
        }


        afterGroup {
            client.close()
            embeddedServer.close()
        }
    }
})
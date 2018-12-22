package uk.me.krupa.s3web

import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import kotlin.test.assertEquals

object SimplePutGetSpec: Spek({
    describe("Simple PUT and GET specifications") {
        val embeddedServer : EmbeddedServer = ApplicationContext.run(EmbeddedServer::class.java)
        val client : HttpClient = HttpClient.create(embeddedServer.url)

        describe("PUT to new subdirectory returns 201 status") {
            val request = HttpRequest.PUT<String>("/dir/subdir/index.html", "<html>empty</html>")
            val rsp = client.toBlocking().exchange(request, Argument.VOID)
            assertEquals(rsp.status, HttpStatus.ACCEPTED)
        }

        afterGroup {
            client.close()
            embeddedServer.close()
        }
    }
})
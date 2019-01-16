package uk.me.krupa.s3web

import io.micronaut.context.ApplicationContext
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import org.hamcrest.CoreMatchers.equalTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.junit.Assert.assertThat

object HealthCheckSpec: Spek({
    describe("HealthCheckSpec Suite") {
        val embeddedServer : EmbeddedServer = ApplicationContext.run(EmbeddedServer::class.java)
        val client : HttpClient = HttpClient.create(embeddedServer.url)

        describe("/health returns UP status") {
            val rsp : Map<*, *>? = client.toBlocking().retrieve("/health", Map::class.java)
            assertThat(rsp?.get("status")?.toString() ?: "", equalTo("UP"))
        }

        afterGroup {
            client.close()
            embeddedServer.close()
        }
    }
})
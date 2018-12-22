package uk.me.krupa.s3web

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Put
import mu.KotlinLogging

private val logger = KotlinLogging.logger {  }

@Controller("/")
class SimpleUploadController {

    @Get("{path:.*}")
    fun getAny(path: String): String {
        logger.info { "GET from $path" }
        return "<html>test</html>"
    }

    @Put("{path:.*}", consumes = [MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN] )
    fun putAny(path: String): HttpStatus {
        logger.info { "PUT to $path" }
        return HttpStatus.ACCEPTED
    }
}
package uk.me.krupa.s3web

import io.micronaut.http.*
import io.micronaut.http.annotation.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.CompositeByteBuf
import io.reactivex.Maybe
import io.reactivex.Single
import mu.KotlinLogging
import uk.me.krupa.s3web.service.Backend
import javax.activation.MimetypesFileTypeMap
import javax.validation.constraints.Size

fun ByteBuf.directToArray(): ByteArray {
    val ary = ByteArray(this.readableBytes())
    this.readBytes(ary)
    return ary
}

@Controller("/")
class SimpleUploadController(private val backend: Backend) {

    val storage = mutableMapOf<String,ByteArray>()

    @Delete("{path:.*}")
    fun deleteAny(path: String): Single<HttpStatus> {
        logger.info { "DELETE $path" }
        return backend.deleteObject(path).map { HttpStatus.NO_CONTENT }.onErrorReturn { HttpStatus.NOT_ACCEPTABLE }
    }

    @Get("{path:.*}")
    fun getAny(path: String): Maybe<MutableHttpResponse<Any>> {
        logger.info { "GET from $path" }
        val mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(path)
        return backend.getObject(path).map { HttpResponse.ok<Any>(it).header(HttpHeaders.CONTENT_TYPE, mimeType) }.switchIfEmpty(Maybe.defer {
            backend.listFiles(path).map { HttpResponse.ok<Any>(it).header(HttpHeaders.CONTENT_TYPE, "application/json") }
                    .onErrorReturn { HttpResponse.badRequest() }
        })
    }

    @Put("{path:.*}", consumes = [MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN] )
    fun putAny(path: String, @Size(max = MAX_FILE_SIZE) @Body data: CompositeByteBuf): Single<HttpStatus> {
        logger.info { "PUT to $path" }
        val flat = data.map { it.directToArray() }.map { it.toList() }.flatten().toByteArray()
        return backend.uploadObject(path, flat).map { HttpStatus.CREATED }.onErrorReturn { HttpStatus.NOT_ACCEPTABLE }
    }

}

private val logger = KotlinLogging.logger {  }

const val MAX_FILE_SIZE = 10 * 1024 * 1024

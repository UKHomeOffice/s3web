package uk.me.krupa.s3web

import io.micronaut.context.annotation.Property
import io.micronaut.http.*
import io.micronaut.http.annotation.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.CompositeByteBuf
import io.reactivex.Maybe
import io.reactivex.Single
import mu.KotlinLogging
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import uk.me.krupa.s3web.service.Backend
import uk.me.krupa.s3web.service.TarballExtractor
import uk.me.krupa.s3web.service.ZipExtractor
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import javax.activation.FileTypeMap
import javax.validation.constraints.Size

fun ByteBuf.directToArray(): ByteArray {
    val ary = ByteArray(this.readableBytes())
    this.readBytes(ary)
    return ary
}

@Controller("/")
class SimpleUploadController(
        private val backend: Backend,
        private val tarballExtractor: TarballExtractor,
        val zipExtractor: ZipExtractor,
        @Property(name = "mimetype") val mimeTypes: Map<String,String>
) {

    val storage = mutableMapOf<String,ByteArray>()

    @Delete("{path:.*}")
    fun deleteAny(path: String): Single<HttpStatus> {
        logger.info { "DELETE $path" }
        return backend.deleteObject("/$path").map { HttpStatus.NO_CONTENT }.onErrorReturn { HttpStatus.NOT_ACCEPTABLE }
    }

    @Get("{path:.*}")
    fun getAny(path: String, request: HttpRequest<String>): Maybe<MutableHttpResponse<Any>> {
        logger.info { "GET from $path" }

        val host = request.headers[HttpHeaders.HOST] ?: "localhost:8080"

        val mimeType =
                mimeTypes.getOrElse(File(path).extension.toLowerCase()) { FileTypeMap.getDefaultFileTypeMap().getContentType(path) }

        return backend.getObject("/$path").map { HttpResponse.ok<Any>(it).header(HttpHeaders.CONTENT_TYPE, mimeType) }.switchIfEmpty(Maybe.defer {
            backend.listFiles("/$path")
                    .map {
                        it.map {
                            entry -> "http://$host/${path.removePrefix("/")}/${entry.removePrefix("/")}"
                        }
                    }
                    .map { HttpResponse.ok<Any>(it).header(HttpHeaders.CONTENT_TYPE, "application/json") }
                    .onErrorReturn { HttpResponse.badRequest() }
        })
    }

    @Put("{path:.*}", consumes = [MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN] )
    fun putAny(path: String, @Size(max = MAX_FILE_SIZE) @Body data: CompositeByteBuf): Single<MutableHttpResponse<MutableList<String>>>? {
        logger.info { "PUT to $path" }
        val flat = data.map { it.directToArray() }.map { it.toList() }.flatten().toByteArray()

        val response = when {
            path.endsWith(".tar") -> tarballExtractor.uploadTar(path, TarArchiveInputStream(ByteArrayInputStream(flat))).toList()
            path.endsWith(".tar.gz") -> tarballExtractor.uploadTar(path, TarArchiveInputStream(GZIPInputStream(ByteArrayInputStream(flat)))).toList()
            path.endsWith(".zip") -> zipExtractor.uploadZip(path, ZipInputStream(ByteArrayInputStream(flat))).toList()
            else -> backend.uploadObject("/$path", flat).map { listOf("/$path") }
        }.map { HttpResponse.created(it) }.onErrorReturn { HttpResponse.badRequest() }
        return response
    }

}

private val logger = KotlinLogging.logger {  }

const val MAX_FILE_SIZE = 100 * 1024 * 1024

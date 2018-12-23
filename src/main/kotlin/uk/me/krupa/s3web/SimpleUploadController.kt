package uk.me.krupa.s3web

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.CompositeByteBuf
import mu.KotlinLogging
import java.nio.charset.Charset
import javax.activation.MimeType
import javax.activation.MimetypesFileTypeMap
import javax.validation.constraints.Size

private val logger = KotlinLogging.logger {  }

const val MAX_FILE_SIZE = 10 * 1024 * 1024

@Controller("/")
class SimpleUploadController {

    val storage = mutableMapOf<String,ByteArray>()

    @Delete("{path:.*}")
    fun deleteAny(path: String): HttpResponse<Any> {
        logger.info { "DELETE $path" }
        return HttpResponse.noContent()
    }

    @Get("{path:.*}")
    fun getAny(path: String): HttpResponse<ByteArray> {
        logger.info { "GET from $path" }
        val mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(path)
        return HttpResponse.ok(storage[path] ?: byteArrayOf()).header("Content-Type", mimeType)
    }

    @Put("{path:.*}", consumes = [MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN] )
    fun putAny(path: String, @Size(max = MAX_FILE_SIZE) @Body data: CompositeByteBuf): HttpStatus {
        logger.info { "PUT to $path with data $data of size ${data.maxCapacity()}" }
        val space = ByteArray(MAX_FILE_SIZE)
        var index = 0
        data.forEach {
            it.forEachByte { space[index++] = it;true }
        }
        storage[path] = space.copyOf(index)
        return HttpStatus.CREATED
    }
}
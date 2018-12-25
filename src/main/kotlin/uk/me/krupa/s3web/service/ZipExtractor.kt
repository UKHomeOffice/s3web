package uk.me.krupa.s3web.service

import io.reactivex.Flowable
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Singleton

@Singleton
class ZipExtractor(val backend: Backend) {
    fun uploadZip(path: String, data: ZipInputStream): Flowable<String> {
        val entry: ZipEntry? = data.nextEntry

        return when {
            entry == null -> {
                data.close()
                Flowable.empty<String>()
            }
            entry.isDirectory -> Flowable.defer {
                uploadZip(path, data)
            }
            else -> {
                val ary = data.readBytes()
                val fullPath = Paths.get("/$path", entry.name).toAbsolutePath().toString()
                backend.uploadObject(fullPath, ary).toFlowable().concatWith(Flowable.defer {
                    uploadZip(path, data)
                })
            }
        }
    }
}
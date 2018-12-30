package uk.me.krupa.s3web.service

import io.reactivex.Flowable
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.nio.file.Paths
import javax.inject.Singleton

@Singleton
class TarballExtractor(val backend: Backend) {
    fun uploadTar(path: String, data: TarArchiveInputStream): Flowable<String> {
        val entry: TarArchiveEntry? = data.nextTarEntry

        return when {
            entry == null -> {
                data.close()
                Flowable.empty<String>()
            }
            entry.isFile -> {
                val ary = ByteArray(entry.size.toInt())
                data.read(ary, 0, ary.size)
                val fullPath = Paths.get("/$path", entry.name).toAbsolutePath().toString()
                backend.uploadObject(fullPath, ary).toFlowable().concatWith(Flowable.defer {
                    uploadTar(path, data)
                })
            }
            else -> Flowable.defer {
                uploadTar(path, data)
            }
        }
    }
}
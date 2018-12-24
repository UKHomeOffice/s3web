package uk.me.krupa.s3web.service

import io.reactivex.Observable
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.nio.file.Paths
import javax.inject.Singleton

@Singleton
class TarballExtractor(val backend: Backend) {
    fun uploadTar(path: String, data: TarArchiveInputStream): Observable<Boolean> {
        val entry: TarArchiveEntry? = data.nextTarEntry

        return when {
            entry == null -> {
                data.close()
                Observable.just(true)
            }
            entry.isFile -> {
                val ary = ByteArray(entry.size.toInt())
                data.read(ary, 0, ary.size)
                val fullPath = Paths.get(path, entry.name).toAbsolutePath().toString()
                backend.uploadObject(fullPath, ary).toObservable().compose {
                    uploadTar(path, data)
                }
            }
            else -> Observable.just(true).compose {
                uploadTar(path, data)
            }
        }
    }
}
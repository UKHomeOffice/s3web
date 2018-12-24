package uk.me.krupa.s3web.service

import io.reactivex.Observable
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Singleton

@Singleton
class ZipExtractor(val backend: Backend) {
    fun uploadZip(path: String, data: ZipInputStream): Observable<Boolean> {
        val entry: ZipEntry? = data.nextEntry

        return when {
            entry == null -> {
                data.close()
                Observable.just(true)
            }
            entry.isDirectory -> {
                uploadZip(path, data)
            }
            else -> Observable.just(true).compose {
                val ary = data.readBytes()
                val fullPath = Paths.get("/$path", entry.name).toAbsolutePath().toString()
                backend.uploadObject(fullPath, ary).toObservable().compose {
                    uploadZip(path, data)
                }
            }
        }
    }
}
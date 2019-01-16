package uk.me.krupa.s3web.service

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.nio.file.Paths
import javax.inject.Singleton

@Singleton
class TarballExtractor(val backend: Backend) {
    fun uploadTar(path: String, data: TarArchiveInputStream): Flowable<String> {
        return Flowable.range(0, Integer.MAX_VALUE)
                .concatMap {
                    val entry = data.nextTarEntry
                    if (entry != null) {
                        if (entry.isFile) {
                            var offset = 0
                            val ary = ByteArray(entry.size.toInt())
                            while (offset < entry.size.toInt()) {
                                offset += data.read(ary, offset, ary.size - offset)
                            }
                            val fullPath = Paths.get("/$path", entry.name).toAbsolutePath().toString()
                            backend.uploadObject(fullPath, ary).toObservable().map { fullPath }.toFlowable(BackpressureStrategy.BUFFER)
                        } else {
                            Flowable.just(path)
                        }
                    } else {
                        Flowable.just("")
                    }
                }
                .takeUntil { it == "" }
                .filter { it != "" }
                .doOnComplete { data.close() }
    }
}
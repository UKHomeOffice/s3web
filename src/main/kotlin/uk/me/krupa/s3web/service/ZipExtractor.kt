package uk.me.krupa.s3web.service

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import javax.inject.Singleton

@Singleton
class ZipExtractor(val backend: Backend) {
    fun uploadZip(path: String, data: ZipInputStream): Flowable<String> {
        return Flowable.range(0, Integer.MAX_VALUE)
                .concatMap {
                    val entry = data.nextEntry
                    if (entry != null) {
                        val ary = data.readBytes()
                        val fullPath = Paths.get("/$path", entry.name).toAbsolutePath().toString()
                        backend.uploadObject(fullPath, ary).toObservable().map { fullPath }.toFlowable(BackpressureStrategy.BUFFER)
                    } else {
                        Flowable.just("")
                    }
                }
                .takeUntil { it == "" }
                .filter { it != "" }
                .doOnComplete { data.close() }
    }
}
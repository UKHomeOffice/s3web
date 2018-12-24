package uk.me.krupa.s3web.service

import io.micronaut.context.annotation.Requires
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Singleton

@Singleton
@Requires(property = "s3.enabled", value = "false", defaultValue = "true")
class StubBackend: Backend {
    val storage = mutableMapOf<String,ByteArray>()

    override fun uploadObject(path: String, data: ByteArray): Single<Boolean> {
        storage[path] = data
        return Single.just(true)
    }

    override fun deleteObject(path: String): Single<Boolean> {
        return Single.just(true)
    }

    override fun getObject(path: String): Maybe<ByteArray> {
        if (storage.containsKey(path)) {
            return Maybe.just(storage[path] ?: ByteArray(0))
        } else {
            return Maybe.empty()
        }
    }

    override fun listFiles(path: String): Maybe<List<String>> {
        val moddedPath = if (path.endsWith("/") || path == "") { path } else {"$path/"}
        return Maybe.just(
                storage.keys.toList().filter { it.startsWith(moddedPath) }.map { it.removePrefix(moddedPath) }
        )
    }

}
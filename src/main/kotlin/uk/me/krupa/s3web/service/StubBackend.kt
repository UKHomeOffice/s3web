package uk.me.krupa.s3web.service

import io.micronaut.context.annotation.Requires
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

    override fun getObject(path: String): Single<ByteArray> {
        return Single.just(storage[path] ?: ByteArray(0))
    }

}
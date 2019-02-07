package org.gov.uk.homeoffice.digital.s3web.service

import io.micronaut.context.annotation.Requires
import io.reactivex.Maybe
import io.reactivex.Single
import mu.KotlinLogging
import javax.inject.Singleton

private val logger = KotlinLogging.logger {  }

@Singleton
@Requires(property = "backend.mode", value = "stub", defaultValue = "s3")
class StubBackend: Backend {
    val storage = mutableMapOf<String,ByteArray>()

    override fun uploadObject(path: String, data: ByteArray): Single<String> {
        logger.info { "Uploading $path of size ${data.size}" }
        storage[path] = data
        return Single.just(path)
    }

    override fun deleteObject(path: String): Single<Boolean> {
        logger.info { "Deleting $path" }
        return Single.just(true)
    }

    override fun getObject(path: String): Maybe<ByteArray> {
        logger.info { "Getting $path" }
        if (storage.containsKey(path)) {
            return Maybe.just(storage[path] ?: ByteArray(0))
        } else {
            return Maybe.empty()
        }
    }

    override fun listFiles(path: String): Maybe<List<String>> {
        logger.info { "Listing $path" }
        val moddedPath = if (path.endsWith("/") || path == "") { path } else {"$path/"}
        return Maybe.just(
                storage.keys.toList()
                        .filter { it.startsWith(moddedPath) }
                        .map { it.removePrefix(moddedPath) }
                        .map {
                            if (it.contains('/')) {
                                it.substring(0, it.indexOf('/') + 1)
                            } else {
                                it
                            }
                        }.toSet().toList().sorted()
        )
    }

}
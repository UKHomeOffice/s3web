package uk.me.krupa.s3web.service

import io.reactivex.Single

interface Backend {

    fun deleteObject(path: String): Single<Boolean>
    fun uploadObject(path: String, data: ByteArray): Single<Boolean>
    fun getObject(path: String): Single<ByteArray>

}
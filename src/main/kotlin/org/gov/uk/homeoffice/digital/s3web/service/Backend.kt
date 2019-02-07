package org.gov.uk.homeoffice.digital.s3web.service

import io.reactivex.Maybe
import io.reactivex.Single

interface Backend {

    fun deleteObject(path: String): Single<Boolean>
    fun uploadObject(path: String, data: ByteArray): Single<String>
    fun getObject(path: String): Maybe<ByteArray>
    fun listFiles(path: String): Maybe<List<String>>

}
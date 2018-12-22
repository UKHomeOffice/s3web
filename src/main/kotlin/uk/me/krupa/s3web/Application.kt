package uk.me.krupa.s3web

import io.micronaut.runtime.Micronaut

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("uk.me.krupa.s3web")
                .mainClass(Application.javaClass)
                .start()
    }
}
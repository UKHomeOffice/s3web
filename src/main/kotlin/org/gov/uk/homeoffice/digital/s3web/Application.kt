package org.gov.uk.homeoffice.digital.s3web

import io.micronaut.runtime.Micronaut

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("org.gov.uk.homeoffice.digital.s3web")
                .mainClass(Application.javaClass)
                .start()
    }
}
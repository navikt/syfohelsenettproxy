package no.nav.syfo.helsepersonell

import io.ktor.http.HttpStatusCode

class HelsepersonellException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    val feilmelding = Feilmelding(message = message)
}

data class Feilmelding(val status: HttpStatusCode = HttpStatusCode.InternalServerError, val message: String?)

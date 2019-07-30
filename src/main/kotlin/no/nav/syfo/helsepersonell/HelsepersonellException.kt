package no.nav.syfo.helsepersonell

import io.ktor.http.HttpStatusCode

class HelsepersonellException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    val feilmelding = Feilmelding(status = HttpStatusCode.InternalServerError, message = message)
}

data class Feilmelding(val status: HttpStatusCode, val message: String?)

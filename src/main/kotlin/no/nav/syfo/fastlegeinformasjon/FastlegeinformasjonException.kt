package no.nav.syfo.fastlegeinformasjon

import io.ktor.http.HttpStatusCode

class FastlegeinformasjonException(message: String?, cause: Throwable?) :
    RuntimeException(message, cause) {
    val feilmelding = Feilmelding(message = message)
}

data class Feilmelding(
    val status: HttpStatusCode = HttpStatusCode.InternalServerError,
    val message: String?
)

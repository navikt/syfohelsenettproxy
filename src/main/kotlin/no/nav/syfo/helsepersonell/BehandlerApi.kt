package no.nav.syfo.helsepersonell

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.syfo.logger

fun Route.registerBehandlerApi(helsepersonellService: HelsepersonellService) {
    get("/behandler") {
        val fnr =
            call.request.header("behandlerFnr")
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Mangler header `behandlerFnr` med fnr")
                    logger.warn("Mottatt kall som mangler header behandlerFnr")
                    return@get
                }

        when (val behandler = helsepersonellService.finnBehandler(fnr)) {
            null -> call.respond(HttpStatusCode.NotFound, "Fant ikke behandler")
            else -> {
                call.respond(behandler)
            }
        }
    }

    get("/behandlerMedHprNummer") {
        val hprNummer =
            call.request.header("hprNummer")
                ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Mangler header `hprNummer` med HPR-nummer"
                    )
                    logger.warn("Mottatt kall som mangler header hprNummer")
                    return@get
                }

        if (!hprNummer.all { Character.isDigit(it) }) {
            call.respond(HttpStatusCode.NotFound, "Fant ikke behandler for $hprNummer")
        } else {
            when (val behandler = helsepersonellService.finnBehandlerFraHprNummer(hprNummer)) {
                null -> call.respond(HttpStatusCode.NotFound, "Fant ikke behandler fra HPR-nummer")
                else -> {
                    call.respond(behandler)
                }
            }
        }
    }
}

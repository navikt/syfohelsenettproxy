package no.nav.syfo.helsepersonell

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.syfo.log

fun Route.registerPingApi(helsepersonellService: HelsepersonellService) {
    get("/ping") {
        val requestId =
            call.request.header("requestId")
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Mangler header `requestId`")
                    log.warn("Mottatt kall som mangler header requestId")
                    return@get
                }

        when (val pingResponse = helsepersonellService.ping(requestId)) {
            null -> call.respond(HttpStatusCode.InternalServerError, "Ping svarte ikkje")
            else -> {
                call.respond(pingResponse)
            }
        }
    }
}

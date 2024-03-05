package no.nav.syfo.sfs

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.syfo.plugins.BrukerPrincipal

fun Route.getPerson(sykmelderService: SykmelderService) {
    get("/hentperson") {
        val bruker = call.principal<BrukerPrincipal>()
        if (bruker == null) {
            call.respond(HttpStatusCode.Unauthorized, "No principal")
            return@get
        }
        val person = sykmelderService.getPerson(bruker.fnr)
        call.respond(person)
    }
}

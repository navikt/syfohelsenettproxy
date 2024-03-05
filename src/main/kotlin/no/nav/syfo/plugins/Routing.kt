package no.nav.syfo.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.registerBehandlerApi
import no.nav.syfo.helsepersonell.registerPingApi
import no.nav.syfo.sfs.SykmelderService
import no.nav.syfo.sfs.getPerson
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val helsepersonellService by inject<HelsepersonellService>()
    val sykmelderService by inject<SykmelderService>()
    routing {
        route("/api/v2") {
            authenticate("servicebrukerAADv2") {
                registerBehandlerApi(helsepersonellService)
                registerPingApi(helsepersonellService)
            }
        }
        route("/rest/v1") { authenticate("tokenx") { getPerson(sykmelderService) } }
    }
}

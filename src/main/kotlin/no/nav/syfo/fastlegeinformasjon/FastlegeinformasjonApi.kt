package no.nav.syfo.fastlegeinformasjon

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.syfo.logger
import no.nav.syfo.securelog

fun Route.registerFastlegeinformasjonApi(fastlegeinformasjonService: FastlegeinformasjonService) {
    get("/fastlegeinformasjon") {
        val kommunenr =
            call.request.header("kommunenr")
                ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Mangler header `kommunenr` med kommunenr",
                    )
                    logger.warn("Mottatt kall som mangler header kommunenr")
                    return@get
                }

        val fastlegeinformasjon = fastlegeinformasjonService.hentFastlegeinformasjon(kommunenr)
        securelog.info("fastlegeinformasjon is: ${fastlegeinformasjon.exportGPContractsResult}")

        call.respond(fastlegeinformasjon)
    }
}

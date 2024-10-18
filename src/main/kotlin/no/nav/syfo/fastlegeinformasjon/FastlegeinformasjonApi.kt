package no.nav.syfo.fastlegeinformasjon

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.syfo.logger

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

        /*
        val fastlegeinformasjonexport =
            fastlegeinformasjonService.hentFastlegeinformasjonExport(kommunenr)


        if (fastlegeinformasjonexport == null) {
            call.respond(
                HttpStatusCode.NotFound,
                "Fant ikke fastlegeinformasjonexport for kommunenr: $kommunenr",
            )
        } else {
            logger.info("Hentet fastlegeinformasjonexport for kommunenr: $kommunenr")
            // TODO zip
            call.respond(byteArrayOf(0x48, 101, 108, 108, 111))
            // call.respond(fastlegeinformasjonexport)
        }
        */
        call.respond(byteArrayOf(0x48, 101, 108, 108, 111))
    }
}

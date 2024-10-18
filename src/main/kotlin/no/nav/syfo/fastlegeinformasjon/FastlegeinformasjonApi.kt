package no.nav.syfo.fastlegeinformasjon

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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

        val fastlegeinformasjonexport =
            fastlegeinformasjonService.hentFastlegeinformasjonExport(kommunenr)

        logger.info("Hentet fastlegeinformasjonexport for kommunenr: $kommunenr")

        call.respond(fastlegeinformasjonexport)
    }
}

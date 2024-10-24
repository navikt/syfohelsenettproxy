package no.nav.syfo.fastlegeinformasjon

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.syfo.logger
import org.slf4j.MDC

fun Route.registerFastlegeinformasjonApi(fastlegeinformasjonService: FastlegeinformasjonService) {
    get("/fastlegeinformasjon") {
        val kommunenr = call.request.header("kommunenr")
        val callId = MDC.get("Nav-Callid")

        if (kommunenr == null) {
            logger.warn("Mottatt kall som mangler header kommunenr")
            call.respond(
                HttpStatusCode.BadRequest,
                "Mangler header `kommunenr` med kommunenr",
            )
        } else {
            logger.info("Mottatt kall til /fastlegeinformasjon for kommunenr: $kommunenr med Nav-Callid: $callId")
            val fastlegeinformasjonexport =
                fastlegeinformasjonService.hentFastlegeinformasjonExport(kommunenr)

            logger.info("Hentet fastlegeinformasjonexport for kommunenr: $kommunenr med Nav-Callid: $callId")
            logger.info(
                "Størrelse for kommunenr: $kommunenr er: (${fastlegeinformasjonexport.size / 1024} KB) med Nav-Callid: $callId",
            )

            call.respondBytes(fastlegeinformasjonexport)
        }
    }
}

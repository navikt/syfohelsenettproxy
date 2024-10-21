package no.nav.syfo.fastlegeinformasjon

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.syfo.logger

fun Route.registerFastlegeinformasjonApi(fastlegeinformasjonService: FastlegeinformasjonService) {
    get("/fastlegeinformasjon") {
        withContext(Dispatchers.IO.limitedParallelism(1)) {
            val kommunenr = call.request.header("kommunenr")

            if (kommunenr == null) {
                logger.warn("Mottatt kall som mangler header kommunenr")
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Mangler header `kommunenr` med kommunenr",
                )
            } else {
                val fastlegeinformasjonexport =
                    fastlegeinformasjonService.hentFastlegeinformasjonExport(kommunenr)

                logger.info("Hentet fastlegeinformasjonexport for kommunenr: $kommunenr")
                logger.info(
                    "Størrelse for kommunenr: $kommunenr er: (${fastlegeinformasjonexport.size / 1024} KB)",
                )

                call.respondBytes(fastlegeinformasjonexport)
            }
        }
    }
}

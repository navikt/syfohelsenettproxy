package no.nav.syfo.fastlegeinformasjon

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.response.*
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

        val fastlegeinformasjonexport =
            fastlegeinformasjonService.hentFastlegeinformasjonExport(kommunenr)

        logger.info("Hentet fastlegeinformasjonexport for kommunenr: $kommunenr")
        logger.info(
            "Størrelse for kommunenr: $kommunenr er: (${fastlegeinformasjonexport.size / 1024} KB)",
        )

        call.respondBytes(fastlegeinformasjonexport)

        /*
        call.respond(
            object : OutgoingContent.WriteChannelContent() {
                val chunks1MB = fastlegeinformasjonexport.toList().chunked(1024 * 1024);


                chunks1MB.forEach {

                }
            }

        )

         */

        /*

        call.respond(
            object : OutgoingContent.WriteChannelContent() {
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    logger.info("start")
                    while (startIndex < endIndex) {
                        var tempEndIndex = startIndex + chuckSize
                        if (tempEndIndex > endIndex) {
                            tempEndIndex = endIndex
                        }
                        logger.info("${tempEndIndex / (1024 * 1024)}")
                        channel.writeFully(fastlegeinformasjonexport.sliceArray(startIndex until tempEndIndex))
                        channel.flush()
                        startIndex = tempEndIndex
                    }
                    channel.flushAndClose()
                }
            },
        )

         */

        /*call.respond(
            ByteArrayContent(
                bytes = fastlegeinformasjonexport,
                contentType = ContentType.Application.OctetStream,
                status = HttpStatusCode.OK,
            ),
        )
                client.("/fastlegeinformasjon") {

         */

        /*
        var startIndex = 0
        val chuckSize = 1024 * 1024
        val endIndex = fastlegeinformasjonexport.size

        call.respondBytesWriter(contentType = ContentType.Application.OctetStream) {

            logger.info("start")
            while (startIndex < endIndex) {
                var tempEndIndex = startIndex + chuckSize
                if (tempEndIndex > endIndex) {
                    tempEndIndex = endIndex
                }
                logger.info("${tempEndIndex / (1024 * 1024)}")
                writeFully(fastlegeinformasjonexport.sliceArray(startIndex until tempEndIndex))
                flush()
                startIndex = tempEndIndex
            }
            flushAndClose()

        }

         */

        /*
        call.respondOutputStream(
            contentType = ContentType.Application.OctetStream,
            status = HttpStatusCode.OK,
        ) {
            ByteArrayOutputStream().write(fastlegeinformasjonexport)
        }

         */

        /*
        call.respondOutputStream{
            withContext(Dispatchers.IO.limitedParallelism(1)) {
                ByteArrayOutputStream().writeBytes(fastlegeinformasjonexport)
            }
        }
        */

        /*
        call.respondBytesWriter {
            writeByteArray(fastlegeinformasjonexport)
           // ByteArrayOutputStream().write(fastlegeinformasjonexport)
        }

        */
    }
}

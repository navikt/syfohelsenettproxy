package no.nav.syfo

import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("no.nav.syfo.syfohelsenettproxy")
    embeddedServer(Netty, 8080) {
        routing {
            get("/isAlive") {
                call.respondText("I'm alive! :)")
            }
            get("/isReady") {
                call.respondText("I'm ready! :)")
            }
        }
    }.start(wait = true)
}

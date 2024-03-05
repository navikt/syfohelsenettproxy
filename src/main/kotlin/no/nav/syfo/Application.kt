package no.nav.syfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.hotspot.DefaultExports
import javax.xml.datatype.DatatypeFactory
import no.nav.syfo.plugins.configureAuth
import no.nav.syfo.plugins.configureContentNegotiation
import no.nav.syfo.plugins.configureModules
import no.nav.syfo.plugins.configureNaisThings
import no.nav.syfo.plugins.configureRouting
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

val logger: Logger = LoggerFactory.getLogger("no.nav.syfo.syfohelsenettproxy")

val securelog = LoggerFactory.getLogger("securelog")
val objectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

fun main() {
    DefaultExports.initialize()
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    configureModules()
    configureContentNegotiation()
    configureAuth()
    configureNaisThings()
    configureRouting()
}

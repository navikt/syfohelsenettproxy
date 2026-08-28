package no.nav.syfo

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
import no.nav.syfo.plugins.configureSwagger
import org.apache.cxf.common.logging.LogUtils
import org.apache.cxf.common.logging.Slf4jLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

val logger: Logger = LoggerFactory.getLogger("no.nav.syfo.syfohelsenettproxy")

val securelog = LoggerFactory.getLogger("securelog")
var objectMapper: JsonMapper = jacksonMapperBuilder().build()

fun main() {
    LogUtils.setLoggerClass(Slf4jLogger::class.java)
    DefaultExports.initialize()
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    configureModules()
    configureContentNegotiation()
    configureAuth()
    configureNaisThings()
    configureRouting()
    configureSwagger()
}

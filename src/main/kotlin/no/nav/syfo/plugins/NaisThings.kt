package no.nav.syfo.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import java.util.UUID
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.application.metrics.monitorHttpRequests
import org.koin.ktor.ext.inject
import org.slf4j.event.Level

fun Application.configureNaisThings() {
    val applicationState by inject<ApplicationState>()

    install(CallLogging) {
        level = Level.TRACE
        mdc("Nav-Callid") { call ->
            call.request.queryParameters["Nav-Callid"] ?: UUID.randomUUID().toString()
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")

            no.nav.syfo.logger.error("Caught exception", cause)
            throw cause
        }
    }
    routing { registerNaisApi(applicationState) }
    intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
}

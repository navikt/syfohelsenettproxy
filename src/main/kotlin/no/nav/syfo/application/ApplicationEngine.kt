package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.*
import no.nav.syfo.Environment
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.application.metrics.monitorHttpRequests
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.registerBehandlerApi
import no.nav.syfo.helsepersonell.registerPingApi
import no.nav.syfo.logger
import org.slf4j.event.Level

fun createApplicationEngine(
    environment: Environment,
    applicationState: ApplicationState,
    helsepersonellService: HelsepersonellService,
    jwkProviderAadV2: JwkProvider
): ApplicationEngine =
    embeddedServer(Netty, environment.applicationPort) {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        setupAuth(environment = environment, jwkProviderAadV2 = jwkProviderAadV2)
        install(CallLogging) {
            level = Level.TRACE
            mdc("Nav-Callid") { call ->
                call.request.queryParameters["Nav-Callid"] ?: UUID.randomUUID().toString()
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")

                logger.error("Caught exception", cause)
                throw cause
            }
        }

        routing {
            registerNaisApi(applicationState)
            route("/api/v2") {
                authenticate("servicebrukerAADv2") {
                    registerBehandlerApi(helsepersonellService)
                    registerPingApi(helsepersonellService)
                }
            }
        }
        intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
    }

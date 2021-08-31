package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.Environment
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.application.metrics.monitorHttpRequests
import no.nav.syfo.helsepersonell.HelsepersonellException
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.registerBehandlerApi
import java.util.UUID

@KtorExperimentalAPI
fun createApplicationEngine(
    environment: Environment,
    applicationState: ApplicationState,
    helsepersonellService: HelsepersonellService,
    jwkProvider: JwkProvider,
    authorizedUsers: List<String>,
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
        setupAuth(
            environment = environment,
            jwkProvider = jwkProvider,
            authorizedUsers = authorizedUsers,
            jwkProviderAadV2 = jwkProviderAadV2
        )
        install(CallLogging) {
            mdc("Nav-Callid") { call ->
                call.request.queryParameters["Nav-Callid"] ?: UUID.randomUUID().toString()
            }
        }
        install(StatusPages) {
            exception<HelsepersonellException> { e ->
                call.respond(HttpStatusCode.InternalServerError, e.feilmelding)
            }
        }

        routing {
            registerNaisApi(applicationState)
            route("/api") {
                authenticate("servicebrukerAADv1") {
                    registerBehandlerApi(helsepersonellService)
                }
            }
            route("/api/v2") {
                authenticate("servicebrukerAADv2") {
                    registerBehandlerApi(helsepersonellService)
                }
            }
        }
        intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
    }

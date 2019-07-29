package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.helsepersonellV1
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

val log = LoggerFactory.getLogger("no.nav.syfo.syfohelsenettproxy")
const val NAV_CALLID = "Nav-CallId"

data class ApplicationState(var running: Boolean = true, var ready: Boolean = true)

fun main() {
    val environment = Environment()
    val credentials: VaultCredentials = objectMapper.readValue(Paths.get(environment.vaultPath).toFile())
    val applicationState = ApplicationState()

    val authorizedUsers = emptyList<String>()

    val helsepersonellV1 = helsepersonellV1(
        environment.helsepersonellv1EndpointURL,
        credentials.serviceuserUsername,
        credentials.serviceuserPassword,
        environment.securityTokenServiceUrl
    )

    val helsepersonellService = HelsepersonellService(helsepersonellV1)

    val applicationServer = embeddedServer(Netty, 8080) {

        setupAuth(environment, authorizedUsers)
        setupContentNegotiation()

        routing {
            registerCallIdInterceptor()
            registerNaisApi(readynessCheck = { applicationState.ready }, livenessCheck = { applicationState.running })
            authenticate {
                registerBehandlerApi(helsepersonellService)
            }
        }
        applicationState.ready = true
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        // Kubernetes polls every 5 seconds for liveness, mark as not ready and wait 5 seconds for a new readyness check
        applicationState.ready = false
        Thread.sleep(10000)
        applicationServer.stop(10, 10, TimeUnit.SECONDS)
    })
}

private fun Routing.registerCallIdInterceptor() {
    intercept(ApplicationCallPipeline.Setup) {
        call.request.header(NAV_CALLID)
            ?.let { MDC.put(NAV_CALLID, it) }
            ?: run {
                log.warn("Kall mangler Call Id")
                call.respond(BadRequest, "Mangler header `NAV_CALLID`")
                return@intercept
            }
    }
}

fun Application.setupContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}

fun Application.setupAuth(
    environment: Environment,
    authorizedUsers: List<String>
) {
    install(Authentication) {
        jwt {
            verifier(
                JwkProviderBuilder(URL(environment.jwkKeysUrl))
                    .cached(10, 24, TimeUnit.HOURS)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build(), environment.jwtIssuer
            )
            realm = "syfohelsenettproxy"
            validate { credentials ->
                val appid: String = credentials.payload.getClaim("appid").asString()
                log.info("authorization attempt for $appid")
                if (appid in authorizedUsers && credentials.payload.audience.contains(environment.clientId)) {
                    log.info("authorization ok")
                    return@validate JWTPrincipal(credentials.payload)
                }
                log.info("authorization failed")
                return@validate null
            }
        }
    }
}

fun Route.registerBehandlerApi(helsepersonellService: HelsepersonellService) {
    get("/behandler") {
        val fnr = call.request.header("behandlerFnr") ?: run {
            call.respond(BadRequest, "Mangler header `behandlerFnr` med fnr")
            return@get
        }

        when (val behandler = helsepersonellService.finnBehandler(fnr)) {
            null -> call.respond(NotFound, "Fant ikke behandler")
            else -> {
                call.respond(behandler)
            }
        }
    }
}

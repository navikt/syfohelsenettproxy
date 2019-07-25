package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.helsepersonellV1
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

val log = LoggerFactory.getLogger("no.nav.syfo.syfohelsenettproxy")

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

fun main() {

    val env = Environment()
    val credentials =
        objectMapper.readValue<VaultCredentials>(Paths.get("/var/run/secrets/nais.io/vault/credentials.json").toFile())
    val authorizedUsers = emptyList<String>()

    val helsepersonellV1 = helsepersonellV1(
        env.helsepersonellv1EndpointURL,
        credentials.serviceuserUsername,
        credentials.serviceuserPassword,
        env.securityTokenServiceUrl
    )

    val helsepersonellService = HelsepersonellService(helsepersonellV1)

    embeddedServer(Netty, 8080) {

        setupAuth(env, authorizedUsers)
        setupContentNegotiation()

        routing {

            get("/isAlive") {
                call.respondText("I'm alive! :)")
            }
            get("/isReady") {
                call.respondText("I'm ready! :)")
            }
            authenticate {
                setupBehandlerApi(helsepersonellService)
            }
        }
    }.start(wait = true)
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
    env: Environment,
    authorizedUsers: List<String>
) {
    install(Authentication) {
        jwt {
            verifier(
                JwkProviderBuilder(URL(env.jwkKeysUrl))
                    .cached(10, 24, TimeUnit.HOURS)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build(), env.jwtIssuer
            )
            realm = "syfohelsenettproxy"
            validate { credentials ->
                val appid: String = credentials.payload.getClaim("appid").asString()
                log.info("authorization attempt for $appid")
                if (appid in authorizedUsers && credentials.payload.audience.contains(env.clientId)) {
                    log.info("authorization ok")
                    return@validate JWTPrincipal(credentials.payload)
                }
                log.info("authorization failed")
                return@validate null
            }
        }
    }
}

fun Route.setupBehandlerApi(helsepersonellService: HelsepersonellService) {
    get("/behandler") {
//        withTraceInterceptor {
            val fnr = call.request.header("behandlerFnr") ?: throw RuntimeException()

            when (val behandler = helsepersonellService.finnBehandler(fnr)) {
                null -> call.respond(NotFound, "Fant ikke behandler")
                else -> {
                    call.respond(behandler) }
            }
//        }
    }
}

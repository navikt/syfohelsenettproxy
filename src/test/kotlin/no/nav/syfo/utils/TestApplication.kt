package no.nav.syfo.utils

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.testing.*
import java.nio.file.Paths
import java.util.UUID
import no.nav.syfo.Environment
import no.nav.syfo.helsepersonell.HelsepersonellException
import no.nav.syfo.plugins.harTilgang
import no.nav.syfo.plugins.unauthorized

fun ApplicationTestBuilder.setUpTestApplication() {
    application {
        install(CallLogging) {
            mdc("Nav-Callid") { call ->
                call.request.queryParameters["Nav-Callid"] ?: UUID.randomUUID().toString()
            }
        }
        install(StatusPages) {
            exception<HelsepersonellException> { call, e ->
                call.respond(HttpStatusCode.InternalServerError, e.feilmelding)
            }
        }
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }
}

fun ApplicationTestBuilder.setUpAuth(): Environment {
    val env =
        Environment(
            helsepersonellv1EndpointURL = "http://url",
            clientIdV2 = "helsenett-clientId-v2",
            jwkKeysUrlV2 = "url",
            jwtIssuerV2 = "https://sts.issuer.net/myidV2",
            tokenXWellKnownUrl = "http://url",
            clientIdTokenX = "tokenx-clientId",
            fastlegeinformasjonv2EndpointURL = "http://url",
        )

    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    val jwkProvider = JwkProviderBuilder(uri).build()

    application {
        install(Authentication) {
            jwt(name = "servicebrukerAADv2") {
                verifier(jwkProvider, env.jwtIssuerV2)
                validate { credentials ->
                    when {
                        harTilgang(credentials, env.clientIdV2) -> JWTPrincipal(credentials.payload)
                        else -> unauthorized(credentials)
                    }
                }
                challenge { _, _ ->
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        "servicebrukerAADv2 token validation failed"
                    )
                }
            }
        }
    }

    return env
}

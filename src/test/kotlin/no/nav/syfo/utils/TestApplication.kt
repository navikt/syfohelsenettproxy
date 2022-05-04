package no.nav.syfo.utils

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.testing.TestApplicationEngine
import no.nav.syfo.Environment
import no.nav.syfo.application.setupAuth
import no.nav.syfo.helsepersonell.HelsepersonellException
import java.nio.file.Paths
import java.util.UUID

fun TestApplicationEngine.setUpTestApplication() {
    start(true)
    application.install(CallLogging) {
        mdc("Nav-Callid") { call ->
            call.request.queryParameters["Nav-Callid"] ?: UUID.randomUUID().toString()
        }
    }
    application.install(StatusPages) {
        exception<HelsepersonellException> { call, e ->
            call.respond(HttpStatusCode.InternalServerError, e.feilmelding)
        }
    }
    application.install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}

fun TestApplicationEngine.setUpAuth(): Environment {
    val env = Environment(
        securityTokenServiceUrl = "url",
        helsepersonellv1EndpointURL = "http://url",
        clientIdV2 = "helsenett-clientId-v2",
        jwkKeysUrlV2 = "url",
        jwtIssuerV2 = "https://sts.issuer.net/myidV2"
    )

    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    val jwkProvider = JwkProviderBuilder(uri).build()

    application.setupAuth(env, jwkProvider)
    return env
}

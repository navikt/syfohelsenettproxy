package no.nav.syfo.utils

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
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
        exception<HelsepersonellException> { e ->
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

val testAuthorizedUsers = listOf("smmottak", "smregler")

fun TestApplicationEngine.setUpAuth(jwkKeysUrl: String = "url", audience: List<String> = testAuthorizedUsers): Environment {
    val env = Environment(
        securityTokenServiceUrl = "url",
        helsepersonellv1EndpointURL = "http://url",
        aadAccessTokenUrl = "url",
        aadDiscoveryUrl = "url",
        jwkKeysUrl = jwkKeysUrl,
        jwtIssuer = "https://sts.issuer.net/myid",
        clientId = "helsenett-clientId",
        syfosmmottakClientId = "smmottak",
        syfosminfotrygdClientId = "sminfotrygd",
        syfosmreglerClientId = "smregler",
        syfosmpapirreglerClientId = "papirregler",
        syfosmpapirmottakClientId = "papirmottak",
        padm2ClientId = "padm",
        smregistreringBackendClientId = "smregistrering",
        clientIdV2 = "helsenett-clientId-v2",
        jwkKeysUrlV2 = "https://keys.url",
        jwtIssuerV2 = "https://sts.issuer.net/myidV2"
    )

    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    val jwkProvider = JwkProviderBuilder(uri).build()

    application.setupAuth(env, jwkProvider, audience, jwkProvider)
    return env
}

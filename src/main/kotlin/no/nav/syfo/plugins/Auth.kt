package no.nav.syfo.plugins

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.Environment
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application.configureAuth")

fun Application.configureAuth() {
    val tokenXConfig by inject<AuthConfiguration>(named("TokenXAuthConfig"))
    val aadConfig by inject<AuthConfiguration>(named("AadAuthConfig"))

    install(Authentication) {
        jwt(name = "tokenx") {
            verifier(tokenXConfig.jwkProvider, tokenXConfig.issuer)
            validate { credentials ->
                when {
                    hasClientIdAudience(
                        credentials,
                        tokenXConfig.clientId,
                    ) && erNiva4(credentials) -> {
                        val principal = JWTPrincipal(credentials.payload)
                        BrukerPrincipal(
                            fnr = finnFnrFraToken(principal),
                            principal = principal,
                        )
                    }
                    else -> unauthorized(credentials)
                }
            }
        }
        jwt(name = "servicebrukerAADv2") {
            verifier(aadConfig.jwkProvider, aadConfig.issuer)
            validate { credentials ->
                when {
                    harTilgang(credentials, aadConfig.clientId) -> JWTPrincipal(credentials.payload)
                    else -> unauthorized(credentials)
                }
            }
        }
    }
}

fun finnFnrFraToken(principal: JWTPrincipal): String {
    return if (
        principal.payload.getClaim("pid") != null &&
            !principal.payload.getClaim("pid").asString().isNullOrEmpty()
    ) {
        logger.debug("Bruker fnr fra pid-claim")
        principal.payload.getClaim("pid").asString()
    } else {
        logger.debug("Bruker fnr fra subject")
        principal.payload.subject
    }
}

data class BrukerPrincipal(
    val fnr: String,
    val principal: JWTPrincipal,
) : Principal

fun erNiva4(credentials: JWTCredential): Boolean {
    return "Level4" == credentials.payload.getClaim("acr").asString()
}

fun hasClientIdAudience(credentials: JWTCredential, clientId: String): Boolean {
    return credentials.payload.audience.contains(clientId)
}

class AuthConfiguration(
    val jwkProvider: JwkProvider,
    val issuer: String,
    val clientId: String,
)

fun getAadAuthConfig(env: Environment): AuthConfiguration {
    val jwkProviderAadV2 =
        JwkProviderBuilder(URL(env.jwkKeysUrlV2))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    return AuthConfiguration(
        jwkProvider = jwkProviderAadV2,
        issuer = env.jwtIssuerV2,
        clientId = env.clientIdV2,
    )
}

fun getTokenXAuthConfig(env: Environment): AuthConfiguration {
    val wellKnown = getWellKnownTokenX(env.tokenXWellKnownUrl)
    val jwkProviderTokenX =
        JwkProviderBuilder(URL(wellKnown.jwks_uri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
    val tokenXIssuer = wellKnown.issuer

    return AuthConfiguration(
        jwkProvider = jwkProviderTokenX,
        issuer = tokenXIssuer,
        clientId = env.clientIdTokenX,
    )
}

fun harTilgang(credentials: JWTCredential, clientId: String): Boolean {
    val appid: String = credentials.payload.getClaim("azp").asString()
    no.nav.syfo.logger.debug("authorization attempt for $appid")
    return credentials.payload.audience.contains(clientId)
}

fun unauthorized(credentials: JWTCredential): Principal? {
    no.nav.syfo.logger.warn(
        "Auth: Unexpected audience for jwt {}, {}",
        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
        StructuredArguments.keyValue("audience", credentials.payload.audience)
    )
    return null
}

fun getWellKnownTokenX(wellKnownUrl: String) = runBlocking {
    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }
    HttpClient(Apache, config).get(wellKnownUrl).body<WellKnownTokenX>()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class WellKnownTokenX(
    val token_endpoint: String,
    val jwks_uri: String,
    val issuer: String,
)

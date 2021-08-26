package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.helsepersonell.HelsepersonellRedis
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.helsepersonellV1
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.xml.datatype.DatatypeFactory

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfohelsenettproxy")

@KtorExperimentalAPI
fun main() {
    val environment = Environment()
    val vaultSecrets = VaultSecrets(
        serviceuserPassword = getFileAsString("/secrets/serviceuser/password"),
        serviceuserUsername = getFileAsString("/secrets/serviceuser/username"),
        pale2ClientId = getFileAsString("/secrets/azuread/pale-2/client_id"),
        pale2ReglerClientId = getFileAsString("/secrets/azuread/pale-2-regler/client_id"),
        redisPassword = getEnvVar("REDIS_PASSWORD")
    )
    val jwkProvider = JwkProviderBuilder(URL(environment.jwkKeysUrl))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    DefaultExports.initialize()
    val applicationState = ApplicationState()

    val jedisPool = JedisPool(JedisPoolConfig(), environment.redisHost, environment.redisPort)

    val authorizedUsers = listOf(
        environment.syfosmmottakClientId,
        environment.syfosminfotrygdClientId,
        environment.syfosmreglerClientId,
        environment.syfosmpapirreglerClientId,
        environment.syfosmpapirmottakClientId,
        vaultSecrets.pale2ClientId,
        vaultSecrets.pale2ReglerClientId,
        environment.padm2ReglerClientId,
        environment.padm2ClientId,
        environment.smregistreringBackendClientId
    )

    val helsepersonellV1 = helsepersonellV1(
        environment.helsepersonellv1EndpointURL,
        vaultSecrets.serviceuserUsername,
        vaultSecrets.serviceuserPassword,
        environment.securityTokenServiceUrl
    )

    val helsepersonellService = HelsepersonellService(helsepersonellV1, HelsepersonellRedis(jedisPool, vaultSecrets.redisPassword))

    val applicationEngine = createApplicationEngine(
        environment = environment,
        applicationState = applicationState,
        helsepersonellService = helsepersonellService,
        jwkProvider = jwkProvider,
        authorizedUsers = authorizedUsers
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
}

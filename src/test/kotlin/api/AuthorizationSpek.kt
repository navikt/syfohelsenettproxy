package api

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import java.net.ServerSocket
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import no.nav.syfo.Environment
import no.nav.syfo.setupAuth
import no.nav.syfo.setupContentNegotiation
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import utils.fakeJWTApi
import utils.genereateJWT

object AuthorizationSpek : Spek({

    val randomPort = ServerSocket(0).use { it.localPort }

    val environment = Environment(
        securityTokenServiceUrl = "https://sts.url",
        helsepersonellv1EndpointURL = "https://helsepersonell.url",
        aadAccessTokenUrl = "https://aadAccesstoken.url",
        aadDiscoveryUrl = "https://aadDicovery.url",
        jwkKeysUrl = "http://localhost:$randomPort/fake.jwt",
        jwtIssuer = "https://sts.issuer.net/myid",
        clientId = "helsenett-clientId",
        vaultPath = "src/test/resources/credentials.json",
        syfosminfotrygdClientId = "syfosminfotrygdClientId",
        syfosmmottakClientId = "syfosmmottakClientId",
        syfosmreglerClientId = "syfosmreglerClientId",
        syfosmpapirreglerClientId = "syfosmpapirreglerClientId",
        syfosmpapirmottakClientId = "syfosmpapirmottakClientId",
        padm2ReglerClientId = "padm2reglerClientId",
        padm2ClientId = "padm2ClientId",
        smregistreringBackendClientId = "smregistreringBackendClientId"
    )

    describe("Authorization") {
        val fakeApi = fakeJWTApi(randomPort)
        val engine = TestApplicationEngine()
        engine.start(wait = false)
        engine.application.apply {
            setupAuth(environment, listOf("consumerClientId"))
            setupContentNegotiation()
            routing {
                authenticate { get("/testApi") { call.respond(HttpStatusCode.OK) } }
            }
        }

        afterGroup {
            fakeApi.stop(0L, 0L, TimeUnit.SECONDS)
            engine.stop(0L, 0L, TimeUnit.SECONDS)
        }

        it("Uten token gir 401") {
            with(engine.handleRequest(HttpMethod.Get, "/testApi")) {
                response.status()?.shouldEqual(HttpStatusCode.Unauthorized)
            }
        }

        it("Feil audience gir 401") {
            with(engine.handleRequest(HttpMethod.Get, "/testApi") {
                addHeader("Authorization", "Bearer ${genereateJWT(audience = "another audience")}")
            }) {
                response.status()?.shouldEqual(HttpStatusCode.Unauthorized)
            }
        }

        it("ConsumerId mangler i authorized users git 401") {
            with(engine.handleRequest(HttpMethod.Get, "/testApi") {
                addHeader("Authorization", "Bearer ${genereateJWT(consumerClientId = "my random app")}")
            }) {
                response.status()?.shouldEqual(HttpStatusCode.Unauthorized)
            }
        }

        it("Utgått token gir 401") {
            with(engine.handleRequest(HttpMethod.Get, "/testApi") {
                addHeader(
                    "Authorization",
                    "Bearer ${genereateJWT(expiry = LocalDateTime.now().minusMinutes(5))}"
                )
            }) {
                response.status()?.shouldEqual(HttpStatusCode.Unauthorized)
            }
        }

        it("Med gyldig token gir 200 OK") {
            with(engine.handleRequest(HttpMethod.Get, "/testApi") {
                addHeader("Authorization", "Bearer ${genereateJWT()}")
            }) {
                response.status()?.shouldEqual(HttpStatusCode.OK)
            }
        }
    }
})

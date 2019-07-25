package api

import fakeJWTApi
import genereateJWT
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import no.nav.syfo.Environment
import no.nav.syfo.setupAuth
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object AuthorizationSpek : Spek({

    val randomPort = ServerSocket(0).use { it.localPort }

    val env = Environment(
            securityTokenServiceUrl = "https://sts.url",
            helsepersonellv1EndpointURL = "https://helsepersonell.url",
            aadAccessTokenUrl = "https://aadAccesstoken.url",
            aadDiscoveryUrl = "https://aadDicovery.url",
            jwkKeysUrl = "http://localhost:$randomPort/fake.jwt",
            jwtIssuer = "https://sts.issuer.net/myid",
            clientId = "helsenett-clientId"
    )

    val fakeApi = fakeJWTApi(randomPort)

    describe("Authorization") {
        afterGroup {
            fakeApi.stop(0L, 0L, TimeUnit.SECONDS)
        }

        with(TestApplicationEngine()) {
            start()
            application.setupAuth(env, listOf("consumerClientId"))
            application.routing { authenticate { get("/testApi") { call.respond(HttpStatusCode.OK) } } }

            it("Uten token gir 401") {
                with(handleRequest(HttpMethod.Get, "/testApi")) {
                    response.status()?.shouldEqual(HttpStatusCode.Unauthorized)
                }
            }

            it("Feil audience gir 401") {
                with(handleRequest(HttpMethod.Get, "/testApi") {
                    addHeader("Authorization", "Bearer ${genereateJWT(audience = "another audience")}")
                }) {
                    response.status()?.shouldEqual(HttpStatusCode.Unauthorized)
                }
            }

            it("ConsumerId mangler i authorized users git 401") {
                with(handleRequest(HttpMethod.Get, "/testApi") {
                    addHeader("Authorization", "Bearer ${genereateJWT(consumerClientId = "my random app")}")
                }) {
                    response.status()?.shouldEqual(HttpStatusCode.Unauthorized)
                }
            }

            it("Utgått token gir 401") {
                with(handleRequest(HttpMethod.Get, "/testApi") {
                    addHeader(
                            "Authorization",
                            "Bearer ${genereateJWT(expiry = LocalDateTime.now().minusMinutes(5))}"
                    )
                }) {
                    response.status()?.shouldEqual(HttpStatusCode.Unauthorized)
                }
            }

            it("Med gyldig token gir 200 OK") {
                with(handleRequest(HttpMethod.Get, "/testApi") {
                    addHeader("Authorization", "Bearer ${genereateJWT()}")
                }) {
                    response.status()?.shouldEqual(HttpStatusCode.OK)
                }
            }
        }
    }
})

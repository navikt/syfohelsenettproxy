package no.nav.syfo.application

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import no.nav.syfo.utils.fakeJWTApi
import no.nav.syfo.utils.genereateJWT
import no.nav.syfo.utils.setUpAuth
import no.nav.syfo.utils.setUpTestApplication
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object AuthorizationSpek : Spek({
    val randomPort = ServerSocket(0).use { it.localPort }
    val fakeApi = fakeJWTApi(randomPort)

    afterGroup {
        fakeApi.stop(TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(0))
    }

    describe("Authorization") {
        with(TestApplicationEngine()) {
            setUpTestApplication()
            setUpAuth()
            application.routing {
                authenticate("servicebrukerAADv2") { get("/testApi") { call.respond(HttpStatusCode.OK) } }
            }

            it("Uten token gir 401") {
                with(handleRequest(HttpMethod.Get, "/testApi")) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.Unauthorized)
                }
            }

            it("Feil audience gir 401") {
                with(
                    handleRequest(HttpMethod.Get, "/testApi") {
                        addHeader("Authorization", "Bearer ${genereateJWT(audience = "another audience")}")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.Unauthorized)
                }
            }

            it("Utgått token gir 401") {
                with(
                    handleRequest(HttpMethod.Get, "/testApi") {
                        addHeader(
                            "Authorization",
                            "Bearer ${genereateJWT(expiry = LocalDateTime.now().minusMinutes(5))}"
                        )
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.Unauthorized)
                }
            }

            it("Med gyldig token gir 200 OK") {
                with(
                    handleRequest(HttpMethod.Get, "/testApi") {
                        addHeader("Authorization", "Bearer ${genereateJWT()}")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.OK)
                }
            }
        }
    }
})

package no.nav.syfo.application

import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.*
import java.time.LocalDateTime
import no.nav.syfo.utils.genereateJWT
import no.nav.syfo.utils.setUpAuth
import no.nav.syfo.utils.setUpTestApplication
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class AuthorizationTest {
    @Test
    internal fun `Uten token gir 401`() {
        testApplication {
            setUpTestApplication()
            setUpAuth()
            routing {
                authenticate("servicebrukerAADv2") {
                    get("/testApi") { call.respond(HttpStatusCode.OK) }
                }
            }
            val response = client.get("/testApi")

            response.status.shouldBeEqualTo(HttpStatusCode.Unauthorized)
            response.bodyAsText().shouldBeEqualTo("servicebrukerAADv2 token validation failed")
        }
    }

    @Test
    internal fun `Feil audience gir 401`() {
        testApplication {
            setUpTestApplication()
            setUpAuth()
            routing {
                authenticate("servicebrukerAADv2") {
                    get("/testApi") { call.respond(HttpStatusCode.OK) }
                }
            }
            val response =
                client.get("/testApi") {
                    header(Authorization, "Bearer ${genereateJWT(audience = "another audience")}")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.Unauthorized)
            response.bodyAsText().shouldBeEqualTo("servicebrukerAADv2 token validation failed")
        }
    }

    @Test
    internal fun `Utgaatt token gir 401`() {
        testApplication {
            setUpTestApplication()
            setUpAuth()
            routing {
                authenticate("servicebrukerAADv2") {
                    get("/testApi") { call.respond(HttpStatusCode.OK) }
                }
            }

            val response =
                client.get("/testApi") {
                    header(
                        Authorization,
                        "Bearer ${genereateJWT(expiry = LocalDateTime.now().minusMinutes(5))}",
                    )
                }

            response.status.shouldBeEqualTo(HttpStatusCode.Unauthorized)
            response.bodyAsText().shouldBeEqualTo("servicebrukerAADv2 token validation failed")
        }
    }

    @Test
    internal fun `Med gyldig token gir 200 OK`() {
        testApplication {
            setUpTestApplication()
            setUpAuth()
            routing {
                authenticate("servicebrukerAADv2") {
                    get("/testApi") { call.respond(HttpStatusCode.OK) }
                }
            }
            val response =
                client.get("/testApi") { header(Authorization, "Bearer ${genereateJWT()}") }
            response.status.shouldBeEqualTo(HttpStatusCode.OK)
        }
    }
}

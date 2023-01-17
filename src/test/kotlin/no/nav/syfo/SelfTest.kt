package no.nav.syfo

import io.kotest.core.spec.style.FunSpec
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.registerNaisApi
import org.junit.jupiter.api.Assertions.assertEquals

class SelfTest : FunSpec({

    context("Successfull liveness and readyness tests") {
        with(TestApplicationEngine()) {
            start()
            val applicationState = ApplicationState()
            applicationState.ready = true
            applicationState.alive = true
            application.routing { registerNaisApi(applicationState) }

            test("Returns ok on is_alive") {
                with(handleRequest(HttpMethod.Get, "/internal/is_alive")) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals("I'm alive! :)", response.content)
                }
            }
            test("Returns ok in is_ready") {
                with(handleRequest(HttpMethod.Get, "/internal/is_ready")) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals("I'm ready! :)", response.content)
                }
            }
        }
    }
    context("Unsuccessful liveness and readyness") {
        with(TestApplicationEngine()) {
            start()
            val applicationState = ApplicationState()
            applicationState.ready = false
            applicationState.alive = false
            application.routing { registerNaisApi(applicationState) }

            test("Returns internal server error when liveness check fails") {
                with(handleRequest(HttpMethod.Get, "/internal/is_alive")) {
                    assertEquals(HttpStatusCode.InternalServerError, response.status())
                    assertEquals("I'm dead x_x", response.content)
                }
            }

            test("Returns internal server error when readyness check fails") {
                with(handleRequest(HttpMethod.Get, "/internal/is_ready")) {
                    assertEquals(HttpStatusCode.InternalServerError, response.status())
                    assertEquals("Please wait! I'm not ready :(", response.content)
                }
            }
        }
    }
})

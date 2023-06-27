package no.nav.syfo.helsepersonell

import io.kotest.core.spec.style.FunSpec
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.utils.setUpTestApplication
import no.nhn.schemas.reg.hprv2.IHPR2Service
import org.amshove.kluent.shouldBeEqualTo

class PingApiTest :
    FunSpec({
        val wsMock = mockk<IHPR2Service>()
        val redis = mockk<HelsepersonellRedis>()
        val helsePersonService = HelsepersonellService(wsMock, redis)

        beforeTest {
            every { wsMock.ping("1") } returns "pong"
            every { wsMock.ping("2") } returns null
            every { redis.getFromHpr(any()) } returns null
            every { redis.getFromFnr(any()) } returns null
            every { redis.save(any(), any()) } returns Unit
        }

        context("Ping with response") {
            with(TestApplicationEngine()) {
                setUpTestApplication()
                application.routing { registerPingApi(helsePersonService) }
                test("Gets pong in response") {
                    with(handleRequest(HttpMethod.Get, "/ping") { addHeader("requestId", "1") }) {
                        response.status()?.shouldBeEqualTo(HttpStatusCode.OK)
                        val pingResponse = response.content!!

                        pingResponse.shouldBeEqualTo("pong")
                    }
                }

                test("Gets null in response") {
                    with(handleRequest(HttpMethod.Get, "/ping") { addHeader("requestId", "2") }) {
                        response.status()?.shouldBeEqualTo(HttpStatusCode.InternalServerError)
                        val error: String = response.content!!

                        error.shouldBeEqualTo("Ping svarte ikkje")
                    }
                }

                test("Should return BadRequest when requestId header is missing") {
                    with(handleRequest(HttpMethod.Get, "/ping") {}) {
                        response.status()?.shouldBeEqualTo(HttpStatusCode.BadRequest)
                        val error: String = response.content!!
                        error shouldBeEqualTo "Mangler header `requestId`"
                    }
                }
            }
        }
    })

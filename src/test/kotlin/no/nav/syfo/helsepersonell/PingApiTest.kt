package no.nav.syfo.helsepersonell

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.util.*
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.helsepersonell.redis.HelsepersonellRedis
import no.nav.syfo.utils.setUpTestApplication
import no.nhn.schemas.reg.hprv2.IHPR2Service
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PingApiTest {
    val wsMock = mockk<IHPR2Service>()
    val redis = mockk<HelsepersonellRedis>()
    val helsePersonService = HelsepersonellService(wsMock, redis)

    @BeforeAll
    internal fun setup() {
        every { wsMock.ping("1") } returns "pong"
        every { wsMock.ping("2") } returns null
        every { redis.getFromHpr(any()) } returns null
        every { redis.getFromFnr(any()) } returns null
        every { redis.save(any(), any()) } returns Unit
    }

    @InternalAPI
    @Test
    internal fun `Gets pong in responsee`() {
        with(TestApplicationEngine()) {
            setUpTestApplication()
            application.routing { registerPingApi(helsePersonService) }
            with(handleRequest(HttpMethod.Get, "/ping") { addHeader("requestId", "1") }) {
                response.status()?.shouldBeEqualTo(HttpStatusCode.OK)
                val pingResponse = response.content!!

                pingResponse.shouldBeEqualTo("pong")
            }
        }
    }

    @InternalAPI
    @Test
    internal fun `Gets null in response`() {
        with(TestApplicationEngine()) {
            setUpTestApplication()
            application.routing { registerPingApi(helsePersonService) }
            with(handleRequest(HttpMethod.Get, "/ping") { addHeader("requestId", "2") }) {
                response.status()?.shouldBeEqualTo(HttpStatusCode.InternalServerError)
                val error: String = response.content!!

                error.shouldBeEqualTo("Ping svarte ikkje")
            }
        }
    }

    @InternalAPI
    @Test
    internal fun `Should return BadRequest when requestId header is missing`() {
        with(TestApplicationEngine()) {
            setUpTestApplication()
            application.routing { registerPingApi(helsePersonService) }
            with(handleRequest(HttpMethod.Get, "/ping") {}) {
                response.status()?.shouldBeEqualTo(HttpStatusCode.BadRequest)
                val error: String = response.content!!
                error shouldBeEqualTo "Mangler header `requestId`"
            }
        }
    }
}

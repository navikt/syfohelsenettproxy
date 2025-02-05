package no.nav.syfo.helsepersonell

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.helsepersonell.valkey.HelsepersonellValkey
import no.nav.syfo.utils.setUpTestApplication
import no.nhn.schemas.reg.hprv2.IHPR2Service
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PingApiTest {
    val wsMock = mockk<IHPR2Service>()
    val valkey = mockk<HelsepersonellValkey>()
    val helsePersonService = HelsepersonellService(wsMock, valkey)

    @BeforeAll
    internal fun setup() {
        every { wsMock.ping("1") } returns "pong"
        every { wsMock.ping("2") } returns null
        every { valkey.getFromHpr(any()) } returns null
        every { valkey.getFromFnr(any()) } returns null
        every { valkey.save(any(), any()) } returns Unit
    }

    @Test
    internal fun `Gets pong in responsee`() {
        testApplication {
            setUpTestApplication()
            routing { registerPingApi(helsePersonService) }

            val response = client.get("/ping") { header("requestId", "1") }

            response.status.shouldBeEqualTo(HttpStatusCode.OK)
            val pingResponse = response.bodyAsText()

            pingResponse.shouldBeEqualTo("pong")
        }
    }

    @Test
    internal fun `Gets null in response`() {
        testApplication {
            setUpTestApplication()
            routing { registerPingApi(helsePersonService) }
            val response = client.get("/ping") { header("requestId", "2") }

            response.status.shouldBeEqualTo(HttpStatusCode.InternalServerError)
            val error: String = response.bodyAsText()

            error.shouldBeEqualTo("Ping svarte ikkje")
        }
    }

    @Test
    internal fun `Should return BadRequest when requestId header is missing`() {
        testApplication {
            setUpTestApplication()
            routing { registerPingApi(helsePersonService) }

            val response = client.get("/ping")

            response.status.shouldBeEqualTo(HttpStatusCode.BadRequest)
            val error: String = response.bodyAsText()
            error shouldBeEqualTo "Mangler header `requestId`"
        }
    }
}

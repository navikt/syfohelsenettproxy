package no.nav.syfo.fastlegeinformasjon

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.utils.setUpTestApplication
import no.nhn.schemas.reg.flr.IFlrExportOperations
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FastlegeinformasjonApiTest {
    private val wsMock = mockk<IFlrExportOperations>()
    private val fastlegeinformasjonService = FastlegeinformasjonService(wsMock)

    @BeforeEach
    fun beforeEach() {
        every { wsMock.exportGPContracts(any()) } returns byteArrayOf(0x48, 101, 108, 108, 111)
    }

    @Test
    internal fun `ExportGPContracts returnerer faar bytearray`() {
        with(TestApplicationEngine()) {
            setUpTestApplication()
            application.routing { registerFastlegeinformasjonApi(fastlegeinformasjonService) }
            with(
                handleRequest(HttpMethod.Get, "/fastlegeinformasjon") {
                    addHeader("kommunenr", "0301")
                    addHeader("Nav-CallId", "callId")
                },
            ) {
                response.status()?.shouldBeEqualTo(HttpStatusCode.OK)
                val byteArray = response.content!!

                byteArray.shouldBeEqualTo("Hello")
            }
        }
    }

    @Test
    internal fun `ExportGPContracts returnerer badrequest ved mangledene header kommunenr`() {
        with(TestApplicationEngine()) {
            setUpTestApplication()
            application.routing { registerFastlegeinformasjonApi(fastlegeinformasjonService) }
            with(
                handleRequest(HttpMethod.Get, "/fastlegeinformasjon") {
                    addHeader("Nav-CallId", "callId")
                },
            ) {
                response.status()?.shouldBeEqualTo(HttpStatusCode.BadRequest)
                val response = response.content!!

                response.shouldBeEqualTo("Mangler header `kommunenr` med kommunenr")
            }
        }
    }
}

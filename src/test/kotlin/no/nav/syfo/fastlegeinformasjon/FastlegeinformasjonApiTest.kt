package no.nav.syfo.fastlegeinformasjon

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.utils.setUpTestApplication
import no.nhn.schemas.reg.flr.IFlrExportOperations
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
        testApplication {
            setUpTestApplication()
            routing { registerFastlegeinformasjonApi(fastlegeinformasjonService) }

            val response =
                client.get("/fastlegeinformasjon") {
                    header("kommunenr", "0301")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.OK)
            val byteArray = response.bodyAsText()

            byteArray.shouldBeEqualTo("Hello")
        }
    }

    @Test
    internal fun `ExportGPContracts returnerer badrequest ved mangledene header kommunenr`() {
        testApplication {
            setUpTestApplication()
            routing { registerFastlegeinformasjonApi(fastlegeinformasjonService) }

            val response = client.get("/fastlegeinformasjon") { header("Nav-CallId", "callId") }

            response.status.shouldBeEqualTo(HttpStatusCode.BadRequest)
            val responsebody = response.bodyAsText()

            responsebody.shouldBeEqualTo("Mangler header `kommunenr` med kommunenr")
        }
    }

    @Disabled("Some oom issue with ktor client test")
    @Test
    internal fun `ExportGPContracts returnerer ok fil over 300 MB`() {
        val byteArray = ByteArray(400000000)

        every { wsMock.exportGPContracts(any()) } returns byteArray

        testApplication {
            setUpTestApplication()
            routing { registerFastlegeinformasjonApi(fastlegeinformasjonService) }

            val response =
                client.get("/fastlegeinformasjon") {
                    header("kommunenr", "0301")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.OK)
        }
    }
}

package api

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.objectMapper
import no.nav.syfo.setupBehandlerApi
import no.nav.syfo.setupContentNegotiation
import no.nhn.schemas.reg.common.no.Kode
import no.nhn.schemas.reg.hprv2.ArrayOfGodkjenning
import no.nhn.schemas.reg.hprv2.Godkjenning
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.Person
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object HelsepersonellSpek : Spek({

    describe("Helsepersonell") {

        val wsMock = mockk<IHPR2Service>()
        val helsePersonService = HelsepersonellService(wsMock)

        every { wsMock.hentPersonMedPersonnummer("fnr", any()) } returns
                Person().apply {
                    godkjenninger = ArrayOfGodkjenning().apply {
                        godkjenning.add(Godkjenning().apply {
                            autorisasjon = Kode().apply {
                                isAktiv = true
                                oid = 7704
                                verdi = "1"
                            }.apply {
                                helsepersonellkategori = Kode().apply {
                                    isAktiv = true
                                    verdi = null
                                    oid = 10
                                }
                            }
                        })
                    }

                    with(TestApplicationEngine()) {
                        start()
                        application.setupContentNegotiation()
                        application.routing {
                            setupBehandlerApi(helsePersonService)
                        }

                        it("Finner behandlingskode på behandler") {
                            with(handleRequest(HttpMethod.Get, "/behandler") {
                                addHeader("behandlerFnr", "fnr")
                            }) {
                                response.status()?.shouldEqual(HttpStatusCode.OK)
                                val behandler: Behandler =
                                        objectMapper.readValue(response.content!!, Behandler::class.java)
                                behandler.godkjenninger.size.shouldEqual(1)
                                behandler.godkjenninger[0].helsepersonellkategori.shouldNotBeNull()
                                behandler.godkjenninger[0].helsepersonellkategori?.aktiv.shouldEqual(true)
                                behandler.godkjenninger[0].helsepersonellkategori?.oid.shouldEqual(10)
                                behandler.godkjenninger[0].helsepersonellkategori?.verdi.shouldBeNull()

                                behandler.godkjenninger[0].autorisasjon.shouldNotBeNull()
                                behandler.godkjenninger[0].autorisasjon?.aktiv.shouldEqual(true)
                                behandler.godkjenninger[0].autorisasjon?.oid.shouldEqual(7704)
                                behandler.godkjenninger[0].autorisasjon?.verdi.shouldEqual("1")
                            }
                        }
                    }
                }
    }
})

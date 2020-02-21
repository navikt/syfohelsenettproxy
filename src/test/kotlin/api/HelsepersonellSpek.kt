package api

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import no.nav.syfo.callLogging
import no.nav.syfo.enforceCallId
import no.nav.syfo.errorHandling
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.helsepersonell.Feilmelding
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.objectMapper
import no.nav.syfo.registerBehandlerApi
import no.nav.syfo.setupContentNegotiation
import no.nhn.schemas.reg.common.no.Kode
import no.nhn.schemas.reg.hprv2.ArrayOfGodkjenning
import no.nhn.schemas.reg.hprv2.Godkjenning
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.Person
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBeNull
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object HelsepersonellSpek : Spek({

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
        }
    every { wsMock.hentPerson(1234, any()) } returns
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
            fornavn = "Fornavn"
            etternavn = "Etternavn"
            nin = "12345678910"
        }

    val engine = TestApplicationEngine()
    engine.start(wait = false)
    engine.application.apply {
        callLogging()
        errorHandling()
        setupContentNegotiation()
        routing {
            enforceCallId()
            registerBehandlerApi(helsePersonService)
        }
    }

    afterGroup {
        engine.stop(0L, 0L, TimeUnit.SECONDS)
    }

    describe("Helsepersonell gitt fnr") {

        it("Finner behandlingskode på behandler") {
            with(engine.handleRequest(HttpMethod.Get, "/behandler") {
                addHeader("behandlerFnr", "fnr")
                addHeader("Nav-CallId", "callId")
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

        it("Enforces callid when interceptor is installed") {
            with(engine.handleRequest(HttpMethod.Get, "/behandler") {
                addHeader("behandlerFnr", "fnr")
            }) {
                response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }

        it("Sender feilmelding videre til konsumenten") {
            every { wsMock.hentPersonMedPersonnummer(any(), any()) } throws (IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage("fault"))
            with(engine.handleRequest(HttpMethod.Get, "/behandler") {
                addHeader("behandlerFnr", "fnr")
                addHeader("Nav-CallId", "callId")
            }) {
                response.status()?.shouldEqual(HttpStatusCode.InternalServerError)
                val feil: Feilmelding =
                    objectMapper.readValue(response.content!!, Feilmelding::class.java)

                feil.status.shouldEqual(HttpStatusCode.InternalServerError)
                feil.message.shouldEqual("fault")
            }
        }

        it("Should return 404 when personnr ikke funnet") {
            every { wsMock.hentPersonMedPersonnummer(any(), any()) } throws (IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage("ArgumentException: Personnummer ikke funnet"))
            with(engine.handleRequest(HttpMethod.Get, "/behandler") {
                addHeader("behandlerFnr", "fnr")
                addHeader("Nav-CallId", "callId")
            }) {
                response.status()?.shouldEqual(HttpStatusCode.NotFound)
                val feil: String = response.content!!
                feil shouldEqual "Fant ikke behandler"
            }
        }
    }

    describe("Helsepersonell gitt hpr-nummer") {

        it("Henter riktig info for behandler") {
            with(engine.handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                addHeader("hprNummer", "1234")
                addHeader("Nav-CallId", "callId")
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
                behandler.fornavn.shouldEqual("Fornavn")
                behandler.mellomnavn.shouldBeNull()
                behandler.etternavn.shouldEqual("Etternavn")
                behandler.fnr.shouldEqual("12345678910")
            }
        }

        it("Enforces callid when interceptor is installed") {
            with(engine.handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                addHeader("hprNummer", "fnr")
            }) {
                response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }

        it("Sender feilmelding videre til konsumenten") {
            every { wsMock.hentPerson(any(), any()) } throws (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("fault"))
            with(engine.handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                addHeader("hprNummer", "1234")
                addHeader("Nav-CallId", "callId")
            }) {
                response.status()?.shouldEqual(HttpStatusCode.InternalServerError)
                val feil: Feilmelding =
                    objectMapper.readValue(response.content!!, Feilmelding::class.java)

                feil.status.shouldEqual(HttpStatusCode.InternalServerError)
                feil.message.shouldEqual("fault")
            }
        }

        it("Return 404 when HPR-nr is not found") {
            every { wsMock.hentPerson(any(), any()) } throws (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("ArgumentException: HPR-nummer ikke funnet"))
            with(engine.handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                addHeader("hprNummer", "1234")
                addHeader("Nav-CallId", "callId")
            }) {
                response.status()?.shouldEqual(HttpStatusCode.NotFound)
                response.content shouldEqual "Fant ikke behandler fra HPR-nummer"
            }
        }

        it("Return 404 when HPR-nr ikke er oppgitt") {
            every { wsMock.hentPerson(any(), any()) } throws (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("ArgumentException: HPR-nummer må oppgis"))
            with(engine.handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                addHeader("hprNummer", "1234")
                addHeader("Nav-CallId", "callId")
            }) {
                response.status()?.shouldEqual(HttpStatusCode.NotFound)
                response.content shouldEqual "Fant ikke behandler fra HPR-nummer"
            }
        }
    }
})

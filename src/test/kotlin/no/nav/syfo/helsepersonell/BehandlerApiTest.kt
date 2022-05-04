package no.nav.syfo.helsepersonell

import io.kotest.core.spec.style.FunSpec
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.objectMapper
import no.nav.syfo.utils.setUpTestApplication
import no.nhn.schemas.reg.common.no.Kode
import no.nhn.schemas.reg.hprv2.ArrayOfGodkjenning
import no.nhn.schemas.reg.hprv2.Godkjenning
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.Person
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull

class BehandlerApiTest : FunSpec({
    val wsMock = mockk<IHPR2Service>()
    val redis = mockk<HelsepersonellRedis>()
    val helsePersonService = HelsepersonellService(wsMock, redis)

    beforeTest {
        every { wsMock.hentPersonMedPersonnummer("fnr", any()) } returns
            Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(
                        Godkjenning().apply {
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
                        }
                    )
                }
            }
        every { wsMock.hentPerson(1234, any()) } returns
            Person().apply {
                godkjenninger = ArrayOfGodkjenning().apply {
                    godkjenning.add(
                        Godkjenning().apply {
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
                        }
                    )
                }
                fornavn = "Fornavn"
                etternavn = "Etternavn"
                nin = "12345678910"
            }

        every { redis.getFromHpr(any()) } returns null
        every { redis.getFromFnr(any()) } returns null
        every { redis.save(any(), any()) } returns Unit
    }

    context("Helsepersonell gitt fnr") {
        with(TestApplicationEngine()) {
            setUpTestApplication()
            application.routing {
                registerBehandlerApi(helsePersonService)
            }
            test("Finner behandlingskode på behandler") {
                with(
                    handleRequest(HttpMethod.Get, "/behandler") {
                        addHeader("behandlerFnr", "fnr")
                        addHeader("Nav-CallId", "callId")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.OK)
                    val behandler: Behandler =
                        objectMapper.readValue(response.content!!, Behandler::class.java)
                    behandler.godkjenninger.size.shouldBeEqualTo(1)
                    behandler.godkjenninger[0].helsepersonellkategori.shouldNotBeNull()
                    behandler.godkjenninger[0].helsepersonellkategori?.aktiv.shouldBeEqualTo(true)
                    behandler.godkjenninger[0].helsepersonellkategori?.oid.shouldBeEqualTo(10)
                    behandler.godkjenninger[0].helsepersonellkategori?.verdi.shouldBeNull()

                    behandler.godkjenninger[0].autorisasjon.shouldNotBeNull()
                    behandler.godkjenninger[0].autorisasjon?.aktiv.shouldBeEqualTo(true)
                    behandler.godkjenninger[0].autorisasjon?.oid.shouldBeEqualTo(7704)
                    behandler.godkjenninger[0].autorisasjon?.verdi.shouldBeEqualTo("1")
                }
            }

            test("Sender feilmelding videre til konsumenten") {
                every {
                    wsMock.hentPersonMedPersonnummer(
                        any(),
                        any()
                    )
                } throws (IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage("fault"))
                with(
                    handleRequest(HttpMethod.Get, "/behandler") {
                        addHeader("behandlerFnr", "fnr")
                        addHeader("Nav-CallId", "callId")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.InternalServerError)
                    val feil: Feilmelding =
                        objectMapper.readValue(response.content!!, Feilmelding::class.java)

                    feil.status.shouldBeEqualTo(HttpStatusCode.InternalServerError)
                    feil.message.shouldBeEqualTo("fault")
                }
            }

            test("Should return 404 when personnr ikke funnet") {
                every {
                    wsMock.hentPersonMedPersonnummer(
                        any(),
                        any()
                    )
                } throws (IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage("ArgumentException: Personnummer ikke funnet"))
                with(
                    handleRequest(HttpMethod.Get, "/behandler") {
                        addHeader("behandlerFnr", "fnr")
                        addHeader("Nav-CallId", "callId")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.NotFound)
                    val feil: String = response.content!!
                    feil shouldBeEqualTo "Fant ikke behandler"
                }
            }
        }
    }

    context("Helsepersonell gitt hpr-nummer") {
        with(TestApplicationEngine()) {
            setUpTestApplication()
            application.routing {
                registerBehandlerApi(helsePersonService)
            }
            test("Henter riktig info for behandler") {
                with(
                    handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                        addHeader("hprNummer", "1234")
                        addHeader("Nav-CallId", "callId")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.OK)
                    val behandler: Behandler =
                        objectMapper.readValue(response.content!!, Behandler::class.java)
                    behandler.godkjenninger.size.shouldBeEqualTo(1)
                    behandler.godkjenninger[0].helsepersonellkategori.shouldNotBeNull()
                    behandler.godkjenninger[0].helsepersonellkategori?.aktiv.shouldBeEqualTo(true)
                    behandler.godkjenninger[0].helsepersonellkategori?.oid.shouldBeEqualTo(10)
                    behandler.godkjenninger[0].helsepersonellkategori?.verdi.shouldBeNull()

                    behandler.godkjenninger[0].autorisasjon.shouldNotBeNull()
                    behandler.godkjenninger[0].autorisasjon?.aktiv.shouldBeEqualTo(true)
                    behandler.godkjenninger[0].autorisasjon?.oid.shouldBeEqualTo(7704)
                    behandler.godkjenninger[0].autorisasjon?.verdi.shouldBeEqualTo("1")
                    behandler.fornavn.shouldBeEqualTo("Fornavn")
                    behandler.mellomnavn.shouldBeNull()
                    behandler.etternavn.shouldBeEqualTo("Etternavn")
                    behandler.fnr.shouldBeEqualTo("12345678910")
                }
            }

            test("Sender feilmelding videre til konsumenten") {
                every {
                    wsMock.hentPerson(
                        any(),
                        any()
                    )
                } throws (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("fault"))
                with(
                    handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                        addHeader("hprNummer", "1234")
                        addHeader("Nav-CallId", "callId")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.InternalServerError)
                    val feil: Feilmelding =
                        objectMapper.readValue(response.content!!, Feilmelding::class.java)

                    feil.status.shouldBeEqualTo(HttpStatusCode.InternalServerError)
                    feil.message.shouldBeEqualTo("fault")
                }
            }

            test("Return 404 when HPR-nr is not found") {
                every {
                    wsMock.hentPerson(
                        any(),
                        any()
                    )
                } throws (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("ArgumentException: HPR-nummer ikke funnet"))
                with(
                    handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                        addHeader("hprNummer", "1234")
                        addHeader("Nav-CallId", "callId")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.NotFound)
                    response.content shouldBeEqualTo "Fant ikke behandler fra HPR-nummer"
                }
            }

            test("Return 404 when HPR-nr ikke er oppgitt") {
                every {
                    wsMock.hentPerson(
                        any(),
                        any()
                    )
                } throws (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("ArgumentException: HPR-nummer må oppgis"))
                with(
                    handleRequest(HttpMethod.Get, "/behandlerMedHprNummer") {
                        addHeader("hprNummer", "1234")
                        addHeader("Nav-CallId", "callId")
                    }
                ) {
                    response.status()?.shouldBeEqualTo(HttpStatusCode.NotFound)
                    response.content shouldBeEqualTo "Fant ikke behandler fra HPR-nummer"
                }
            }
        }
    }
})

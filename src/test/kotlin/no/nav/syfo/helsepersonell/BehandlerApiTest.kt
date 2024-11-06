package no.nav.syfo.helsepersonell

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.helsepersonell.redis.HelsepersonellRedis
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BehandlerApiTest {
    private val wsMock = mockk<IHPR2Service>()
    private val redis = mockk<HelsepersonellRedis>()
    private val helsePersonService = HelsepersonellService(wsMock, redis)

    @BeforeEach
    fun beforeEach() {
        every { wsMock.hentPersonMedPersonnummer("fnr", any()) } returns
            Person().apply {
                godkjenninger =
                    ArrayOfGodkjenning().apply {
                        godkjenning.add(
                            Godkjenning().apply {
                                autorisasjon =
                                    Kode()
                                        .apply {
                                            isAktiv = true
                                            oid = 7704
                                            verdi = "1"
                                        }
                                        .apply {
                                            helsepersonellkategori =
                                                Kode().apply {
                                                    isAktiv = true
                                                    verdi = null
                                                    oid = 10
                                                }
                                        }
                            },
                        )
                    }
            }
        every { wsMock.hentPerson(1234, any()) } returns
            Person().apply {
                godkjenninger =
                    ArrayOfGodkjenning().apply {
                        godkjenning.add(
                            Godkjenning().apply {
                                autorisasjon =
                                    Kode()
                                        .apply {
                                            isAktiv = true
                                            oid = 7704
                                            verdi = "1"
                                        }
                                        .apply {
                                            helsepersonellkategori =
                                                Kode().apply {
                                                    isAktiv = true
                                                    verdi = null
                                                    oid = 10
                                                }
                                        }
                            },
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

    @Test
    internal fun `Helsepersonell gitt fnr finner behandlingskode paa behandler`() {
        testApplication {
            setUpTestApplication()
            routing { registerBehandlerApi(helsePersonService) }

            val response =
                client.get("/behandler") {
                    header("behandlerFnr", "fnr")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.OK)
            val behandler: Behandler =
                objectMapper.readValue(response.bodyAsText(), Behandler::class.java)
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

    @Test
    internal fun `Helsepersonell gitt fnr sender feilmelding videre til konsumenten`() {
        testApplication {
            setUpTestApplication()
            routing { registerBehandlerApi(helsePersonService) }
            every { wsMock.hentPersonMedPersonnummer(any(), any()) } throws
                (IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage(
                    "fault",
                ))

            val response =
                client.get("/behandler") {
                    header("behandlerFnr", "fnr")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.InternalServerError)
            val feil: Feilmelding =
                objectMapper.readValue(response.bodyAsText(), Feilmelding::class.java)

            feil.status.shouldBeEqualTo(HttpStatusCode.InternalServerError)
            feil.message.shouldBeEqualTo("fault")
        }
    }

    @Test
    internal fun `Helsepersonell gitt fnr should return 404 when personnr ikke funnet`() {
        testApplication {
            setUpTestApplication()
            routing { registerBehandlerApi(helsePersonService) }
            every { wsMock.hentPersonMedPersonnummer(any(), any()) } throws
                (IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage(
                    "ArgumentException: Personnummer ikke funnet",
                ))

            val response =
                client.get("/behandler") {
                    header("behandlerFnr", "fnr")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.NotFound)
            val feil: String = response.bodyAsText()
            feil shouldBeEqualTo "Fant ikke behandler"
        }
    }

    @Test
    internal fun `Helsepersonell gitt hpr-nummer henter riktig info for behandler`() {
        testApplication {
            setUpTestApplication()
            routing { registerBehandlerApi(helsePersonService) }
            val response =
                client.get("/behandlerMedHprNummer") {
                    header("hprNummer", "1234")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.OK)
            val behandler: Behandler =
                objectMapper.readValue(response.bodyAsText(), Behandler::class.java)
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

    @Test
    internal fun `Helsepersonell gitt hpr-nummer sender feilmelding videre til konsumenten`() {
        testApplication {
            setUpTestApplication()
            routing { registerBehandlerApi(helsePersonService) }
            every { wsMock.hentPerson(any(), any()) } throws
                (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("fault"))

            val response =
                client.get("/behandlerMedHprNummer") {
                    header("hprNummer", "1234")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.InternalServerError)
            val feil: Feilmelding =
                objectMapper.readValue(response.bodyAsText(), Feilmelding::class.java)

            feil.status.shouldBeEqualTo(HttpStatusCode.InternalServerError)
            feil.message.shouldBeEqualTo("fault")
        }
    }

    @Test
    internal fun `Helsepersonell gitt hpr-nummer return 404 when HPR-nr is not found`() {
        testApplication {
            setUpTestApplication()
            routing { registerBehandlerApi(helsePersonService) }
            every { wsMock.hentPerson(any(), any()) } throws
                (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage(
                    "ArgumentException: HPR-nummer ikke funnet",
                ))
            val response =
                client.get("/behandlerMedHprNummer") {
                    header("hprNummer", "1234")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.NotFound)
            response.bodyAsText() shouldBeEqualTo "Fant ikke behandler fra HPR-nummer"
        }
    }

    @Test
    internal fun `Helsepersonell gitt hpr-nummer return 404 when HPR-nr ikke er oppgitt`() {
        testApplication {
            setUpTestApplication()
            routing { registerBehandlerApi(helsePersonService) }
            every { wsMock.hentPerson(any(), any()) } throws
                (IHPR2ServiceHentPersonGenericFaultFaultFaultMessage(
                    "ArgumentException: HPR-nummer må oppgis",
                ))
            val response =
                client.get("/behandlerMedHprNummer") {
                    header("hprNummer", "1234")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.NotFound)
            response.bodyAsText() shouldBeEqualTo "Fant ikke behandler fra HPR-nummer"
        }
    }

    @Test
    internal fun `Helsepersonell gitt hpr-nummer return 400 when HPR-nr er tom string`() {
        testApplication {
            setUpTestApplication()
            routing { registerBehandlerApi(helsePersonService) }

            val response =
                client.get("/behandlerMedHprNummer") {
                    header("hprNummer", "")
                    header("Nav-CallId", "callId")
                }

            response.status.shouldBeEqualTo(HttpStatusCode.BadRequest)
            response.bodyAsText() shouldBeEqualTo "`hprNummer` er ein tom string"
        }
    }
}

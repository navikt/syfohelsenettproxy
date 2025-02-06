package no.nav.syfo.helsepersonell

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate.now
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.GregorianCalendar
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.ws.soap.SOAPFaultException
import no.nav.syfo.datatypeFactory
import no.nav.syfo.helsepersonell.valkey.HelsepersonellValkey
import no.nav.syfo.helsepersonell.valkey.JedisBehandlerModel
import no.nhn.schemas.reg.common.no.Kode
import no.nhn.schemas.reg.common.no.Periode
import no.nhn.schemas.reg.hprv2.ArrayOfGodkjenning
import no.nhn.schemas.reg.hprv2.ArrayOfTilleggskompetanse
import no.nhn.schemas.reg.hprv2.Godkjenning
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.Person
import no.nhn.schemas.reg.hprv2.Tilleggskompetanse
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HelsepersonellServiceTest {
    private val mock = mockk<IHPR2Service>()
    private val helsepersonellValkey = mockk<HelsepersonellValkey>(relaxed = true)

    @BeforeEach
    internal fun setup() {
        clearAllMocks()
        every { mock.hentPersonMedPersonnummer("fnr", any()) } returns getPerson()
        every { mock.hentPerson(any(), any()) } returns getPerson()
    }

    @Test
    internal fun `Should not save when fnr is empty`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromFnr(any()) } returns null

        val behandler = service.finnBehandler("fnr")
        val idag = now()
        val slot = slot<XMLGregorianCalendar>()
        verify { mock.hentPersonMedPersonnummer("fnr", capture(slot)) }
        verify(exactly = 1) { helsepersonellValkey.save(behandler!!, any()) }
        verify(exactly = 1) { helsepersonellValkey.getFromFnr("fnr") }

        val cal = slot.captured
        cal.toGregorianCalendar().toZonedDateTime().toLocalDate().shouldBeEqualTo(idag)
    }

    @Test
    internal fun `Henter behandler fra valkey`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromFnr("fnr") } returns
            JedisBehandlerModel(
                behandler = getBehandler(),
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
            )
        val behandler = service.finnBehandler("fnr")
        verify(exactly = 0) { mock.hentPersonMedPersonnummer(any(), any()) }
        verify(exactly = 0) { helsepersonellValkey.save(behandler!!, any()) }
        verify(exactly = 1) { helsepersonellValkey.getFromFnr("fnr") }
    }

    @Test
    internal fun `Henter behandler og lagrer i valkey`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromHpr("1000001") } returns
            JedisBehandlerModel(
                behandler = getBehandler(),
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
            )
        val behandler = service.finnBehandlerFraHprNummer("1000001")
        verify(exactly = 0) { mock.hentPersonMedPersonnummer(any(), any()) }
        verify(exactly = 0) { mock.hentPerson(any(), any()) }
        verify(exactly = 0) { helsepersonellValkey.save(behandler!!, any()) }
        verify(exactly = 0) { helsepersonellValkey.getFromFnr("fnr") }
        verify(exactly = 1) { helsepersonellValkey.getFromHpr("1000001") }
    }

    @Test
    internal fun `Henter behandler fra valkey med HPR`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromHpr("1000001") } returns null
        val behandler = service.finnBehandlerFraHprNummer("1000001")
        verify(exactly = 1) { mock.hentPerson(any(), any()) }
        verify(exactly = 1) { helsepersonellValkey.save(behandler!!, any()) }
        verify(exactly = 0) { helsepersonellValkey.getFromFnr("fnr") }
        verify(exactly = 1) { helsepersonellValkey.getFromHpr("1000001") }
    }

    @Test
    internal fun `Henter paa nytt fra WS om valkey timestamp er gammelt`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromHpr("1000001") } returns
            JedisBehandlerModel(
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(61),
                behandler = getBehandler(),
            )
        val behandler = service.finnBehandlerFraHprNummer("1000001")
        verify(exactly = 1) { mock.hentPerson(any(), any()) }
        verify(exactly = 1) { helsepersonellValkey.save(behandler!!, any()) }
        verify(exactly = 0) { helsepersonellValkey.getFromFnr("fnr") }
        verify(exactly = 1) { helsepersonellValkey.getFromHpr("1000001") }
    }

    @Test
    internal fun `Henter ikke paa nytt valkey timestamp er nytt`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromHpr("1000001") } returns
            JedisBehandlerModel(
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(59),
                behandler = getBehandler(),
            )
        val behandler = service.finnBehandlerFraHprNummer("1000001")
        verify(exactly = 0) { mock.hentPerson(any(), any()) }
        verify(exactly = 0) { helsepersonellValkey.save(behandler!!, any()) }
        verify(exactly = 0) { helsepersonellValkey.getFromFnr("fnr") }
        verify(exactly = 1) { helsepersonellValkey.getFromHpr("1000001") }
    }

    @Test
    internal fun `Skal bruke valkey om det feiler mot helsenett for hpr`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)

        every { helsepersonellValkey.getFromHpr("1000001") } returns
            JedisBehandlerModel(
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(120),
                behandler = getBehandler(),
            )
        every { mock.hentPerson(any(), any()) } throws
            IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("MESSAGE")
        val behandler = service.finnBehandlerFraHprNummer("1000001")
        behandler shouldBeEqualTo getBehandler()
        verify(exactly = 1) { mock.hentPerson(any(), any()) }
        verify(exactly = 0) { helsepersonellValkey.save(behandler!!, any()) }
        verify(exactly = 0) { helsepersonellValkey.getFromFnr("fnr") }
        verify(exactly = 1) { helsepersonellValkey.getFromHpr("1000001") }
    }

    @Test
    internal fun `Skal bruke valkey om det feiler mot helsenett for fnr`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromFnr("fnr") } returns
            JedisBehandlerModel(
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(120),
                behandler = getBehandler(),
            )
        every { mock.hentPersonMedPersonnummer(any(), any()) } throws
            IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage("MESSAGE")
        val behandler = service.finnBehandler("fnr")
        behandler shouldBeEqualTo getBehandler()
        verify(exactly = 1) { mock.hentPersonMedPersonnummer(any(), any()) }
        verify(exactly = 0) { helsepersonellValkey.save(behandler!!) }
        verify(exactly = 1) { helsepersonellValkey.getFromFnr("fnr") }
        verify(exactly = 0) { helsepersonellValkey.getFromHpr("1000001") }
    }

    @Test
    internal fun `Skal bruke valkey ved SOAPFaultException for FNR`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromFnr("fnr") } returns
            JedisBehandlerModel(
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(120),
                behandler = getBehandler(),
            )
        every { mock.hentPersonMedPersonnummer(any(), any()) } throws
            SOAPFaultException(mockk(relaxed = true))
        val behandler = service.finnBehandler("fnr")
        behandler shouldBeEqualTo getBehandler()
        verify(exactly = 1) { mock.hentPersonMedPersonnummer(any(), any()) }
        verify(exactly = 0) { helsepersonellValkey.save(behandler!!) }
        verify(exactly = 1) { helsepersonellValkey.getFromFnr("fnr") }
        verify(exactly = 0) { helsepersonellValkey.getFromHpr("1000001") }
    }

    @Test
    internal fun `Skal bruke valkey ved SOAPFaultException for HPR`() {
        val service = HelsepersonellService(mock, helsepersonellValkey)
        every { helsepersonellValkey.getFromHpr("1000001") } returns
            JedisBehandlerModel(
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(120),
                behandler = getBehandler(),
            )
        every { mock.hentPerson(any(), any()) } throws SOAPFaultException(mockk(relaxed = true))
        val behandler = service.finnBehandlerFraHprNummer("1000001")
        behandler shouldBeEqualTo getBehandler()
        verify(exactly = 1) { mock.hentPerson(any(), any()) }
        verify(exactly = 0) { helsepersonellValkey.save(behandler!!) }
        verify(exactly = 0) { helsepersonellValkey.getFromFnr("fnr") }
        verify(exactly = 1) { helsepersonellValkey.getFromHpr("1000001") }
    }
}

private fun getBehandler() = Behandler(emptyList(), "fnr", 1000001, "fornavn", null, "etternavn")

private fun getPerson(): Person {
    return Person().apply {
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
                        tilleggskompetanser =
                            ArrayOfTilleggskompetanse().apply {
                                tilleggskompetanse.add(
                                    Tilleggskompetanse().apply {
                                        avsluttetStatus =
                                            Kode().apply {
                                                isAktiv = false
                                                verdi = null
                                                oid = 10
                                            }
                                        eTag = ""
                                        gyldig =
                                            Periode().apply {
                                                fra =
                                                    datatypeFactory.newXMLGregorianCalendar(
                                                        GregorianCalendar(),
                                                    )
                                                til =
                                                    datatypeFactory.newXMLGregorianCalendar(
                                                        GregorianCalendar(),
                                                    )
                                            }
                                        id = 20
                                        type =
                                            Kode().apply {
                                                isAktiv = true
                                                verdi = "1"
                                                oid = 7702
                                            }
                                    },
                                )
                                tilleggskompetanse.add(
                                    Tilleggskompetanse().apply {
                                        avsluttetStatus = null
                                        eTag = ""
                                        gyldig =
                                            Periode().apply {
                                                fra = null
                                                til = null
                                            }
                                        id = 20
                                        type =
                                            Kode().apply {
                                                isAktiv = true
                                                verdi = "1"
                                                oid = 7702
                                            }
                                    },
                                )
                            }
                    },
                )
            }
    }
}

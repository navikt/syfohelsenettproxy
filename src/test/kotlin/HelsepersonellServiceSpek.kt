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
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.helsepersonell.HelsepersonellRedis
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.redis.JedisBehandlerModel
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
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class HelsepersonellServiceSpek : Spek({

    val mock = mockk<IHPR2Service>()
    val helsepersonellRedis = mockk<HelsepersonellRedis>(relaxed = true)

    beforeEachTest {
        clearAllMocks()
        every { mock.hentPersonMedPersonnummer("fnr", any()) } returns
                getPerson()
        every { mock.hentPerson(any(), any()) } returns getPerson()
    }

    describe("HelsepersonellService") {
        val service = HelsepersonellService(mock, helsepersonellRedis)

        it("Kaller WS med korrekte argumenter") {

            every { helsepersonellRedis.getFromFnr(any()) } returns null

            val behandler = service.finnBehandler("fnr")
            val idag = now()
            val slot = slot<XMLGregorianCalendar>()
            verify { mock.hentPersonMedPersonnummer("fnr", capture(slot)) }
            verify(exactly = 1) { helsepersonellRedis.save(behandler!!, any()) }
            verify(exactly = 1) { helsepersonellRedis.getFromFnr("fnr") }

            val cal = slot.captured
            cal.toGregorianCalendar().toZonedDateTime().toLocalDate().shouldEqual(idag)
        }

        it("Henter behandler fra redis") {
            every { helsepersonellRedis.getFromFnr("fnr") } returns JedisBehandlerModel(behandler = getBehandler(), timestamp = OffsetDateTime.now(ZoneOffset.UTC))
            val behandler = service.finnBehandler("fnr")
            verify(exactly = 0) { mock.hentPersonMedPersonnummer(any(), any()) }
            verify(exactly = 0) { helsepersonellRedis.save(behandler!!, any()) }
            verify(exactly = 1) { helsepersonellRedis.getFromFnr("fnr") }
        }
        it("Henter behandler og lagrer i redis") {
            every { helsepersonellRedis.getFromHpr("1000001") } returns JedisBehandlerModel(behandler = getBehandler(), timestamp = OffsetDateTime.now(ZoneOffset.UTC))
            val behandler = service.finnBehandlerFraHprNummer("1000001")
            verify(exactly = 0) { mock.hentPersonMedPersonnummer(any(), any()) }
            verify(exactly = 0) { mock.hentPerson(any(), any()) }
            verify(exactly = 0) { helsepersonellRedis.save(behandler!!, any()) }
            verify(exactly = 0) { helsepersonellRedis.getFromFnr("fnr") }
            verify(exactly = 1) { helsepersonellRedis.getFromHpr("1000001") }
        }

        it("Henter behandler fra redis med HPR") {
            every { helsepersonellRedis.getFromHpr("1000001") } returns null
            val behandler = service.finnBehandlerFraHprNummer("1000001")
            verify(exactly = 1) { mock.hentPerson(any(), any()) }
            verify(exactly = 1) { helsepersonellRedis.save(behandler!!, any()) }
            verify(exactly = 0) { helsepersonellRedis.getFromFnr("fnr") }
            verify(exactly = 1) { helsepersonellRedis.getFromHpr("1000001") }
        }

        it("Henter på nytt fra WS om redis timestamp er gammelt") {
            every { helsepersonellRedis.getFromHpr("1000001") } returns JedisBehandlerModel(timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(61), behandler = getBehandler())
            val behandler = service.finnBehandlerFraHprNummer("1000001")
            verify(exactly = 1) { mock.hentPerson(any(), any()) }
            verify(exactly = 1) { helsepersonellRedis.save(behandler!!, any()) }
            verify(exactly = 0) { helsepersonellRedis.getFromFnr("fnr") }
            verify(exactly = 1) { helsepersonellRedis.getFromHpr("1000001") }
        }
        it("Henter ikke på nytt redist timestamp er nytt") {
            every { helsepersonellRedis.getFromHpr("1000001") } returns JedisBehandlerModel(timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(59), behandler = getBehandler())
            val behandler = service.finnBehandlerFraHprNummer("1000001")
            verify(exactly = 0) { mock.hentPerson(any(), any()) }
            verify(exactly = 0) { helsepersonellRedis.save(behandler!!, any()) }
            verify(exactly = 0) { helsepersonellRedis.getFromFnr("fnr") }
            verify(exactly = 1) { helsepersonellRedis.getFromHpr("1000001") }
        }
        it("Skal bruke redis om det feiler mot helsenett for hpr") {
            every { helsepersonellRedis.getFromHpr("1000001") } returns JedisBehandlerModel(timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(120), behandler = getBehandler())
            every { mock.hentPerson(any(), any()) } throws IHPR2ServiceHentPersonGenericFaultFaultFaultMessage("MESSAGE")
            val behandler = service.finnBehandlerFraHprNummer("1000001")
            behandler shouldEqual getBehandler()
            verify(exactly = 1) { mock.hentPerson(any(), any()) }
            verify(exactly = 0) { helsepersonellRedis.save(behandler!!, any()) }
            verify(exactly = 0) { helsepersonellRedis.getFromFnr("fnr") }
            verify(exactly = 1) { helsepersonellRedis.getFromHpr("1000001") }
        }
        it("Skal bruke redis om det feiler mot helsenett for fnr") {
            every { helsepersonellRedis.getFromFnr("fnr") } returns JedisBehandlerModel(timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(120), behandler = getBehandler())
            every { mock.hentPersonMedPersonnummer(any(), any()) } throws IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage("MESSAGE")
            val behandler = service.finnBehandler("fnr")
            behandler shouldEqual getBehandler()
            verify(exactly = 1) { mock.hentPersonMedPersonnummer(any(), any()) }
            verify(exactly = 0) { helsepersonellRedis.save(behandler!!) }
            verify(exactly = 1) { helsepersonellRedis.getFromFnr("fnr") }
            verify(exactly = 0) { helsepersonellRedis.getFromHpr("1000001") }
        }
        it("SKal bruke redis ved SOAPFaultException for FNR") {
            every { helsepersonellRedis.getFromFnr("fnr") } returns JedisBehandlerModel(timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(120), behandler = getBehandler())
            every { mock.hentPersonMedPersonnummer(any(), any()) } throws SOAPFaultException(mockk(relaxed = true))
            val behandler = service.finnBehandler("fnr")
            behandler shouldEqual getBehandler()
            verify(exactly = 1) { mock.hentPersonMedPersonnummer(any(), any()) }
            verify(exactly = 0) { helsepersonellRedis.save(behandler!!) }
            verify(exactly = 1) { helsepersonellRedis.getFromFnr("fnr") }
            verify(exactly = 0) { helsepersonellRedis.getFromHpr("1000001") }
        }
        it("SKal bruke redis ved SOAPFaultException for HPR") {
            every { helsepersonellRedis.getFromHpr("1000001") } returns JedisBehandlerModel(timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(120), behandler = getBehandler())
            every { mock.hentPerson(any(), any()) } throws SOAPFaultException(mockk(relaxed = true))
            val behandler = service.finnBehandlerFraHprNummer("1000001")
            behandler shouldEqual getBehandler()
            verify(exactly = 1) { mock.hentPerson(any(), any()) }
            verify(exactly = 0) { helsepersonellRedis.save(behandler!!) }
            verify(exactly = 0) { helsepersonellRedis.getFromFnr("fnr") }
            verify(exactly = 1) { helsepersonellRedis.getFromHpr("1000001") }
        }
    }
})

private fun getBehandler() = Behandler(
    emptyList(), "fnr", 1000001, "fornavn", null, "etternavn"
)

private fun getPerson(): Person {
    return Person().apply {
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
                tilleggskompetanser = ArrayOfTilleggskompetanse().apply {
                    tilleggskompetanse.add(Tilleggskompetanse().apply {
                        avsluttetStatus = Kode().apply {
                            isAktiv = false
                            verdi = null
                            oid = 10
                        }
                        eTag = ""
                        gyldig = Periode().apply {
                            fra = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar())
                            til = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar())
                        }
                        id = 20
                        type = Kode().apply {
                            isAktiv = true
                            verdi = "1"
                            oid = 7702
                        }
                    })
                    tilleggskompetanse.add(Tilleggskompetanse().apply {
                        avsluttetStatus = null
                        eTag = ""
                        gyldig = Periode().apply {
                            fra = null
                            til = null
                        }
                        id = 20
                        type = Kode().apply {
                            isAktiv = true
                            verdi = "1"
                            oid = 7702
                        }
                    })
                }
            })
        }
    }
}

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.syfo.datatypeFactory
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nhn.schemas.reg.common.no.Kode
import no.nhn.schemas.reg.common.no.Periode
import no.nhn.schemas.reg.hprv2.ArrayOfGodkjenning
import no.nhn.schemas.reg.hprv2.ArrayOfTilleggskompetanse
import no.nhn.schemas.reg.hprv2.Godkjenning
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.Person
import no.nhn.schemas.reg.hprv2.Tilleggskompetanse
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate.now
import java.util.GregorianCalendar
import javax.xml.datatype.XMLGregorianCalendar

class HelsepersonellServiceSpek : Spek({

    val mock = mockk<IHPR2Service>()
    every { mock.hentPersonMedPersonnummer("fnr", any()) } returns
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
                    }
                })
            }
        }

    describe("HelsepersonellService") {
        it("Kaller WS med korrekte argumenter") {
            val service = HelsepersonellService(mock)
            service.finnBehandler("fnr")

            val idag = now()
            val slot = slot<XMLGregorianCalendar>()
            verify { mock.hentPersonMedPersonnummer("fnr", capture(slot)) }

            val cal = slot.captured
            cal.toGregorianCalendar().toZonedDateTime().toLocalDate().shouldEqual(idag)
        }
    }
})

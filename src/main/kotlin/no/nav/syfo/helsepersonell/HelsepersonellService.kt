package no.nav.syfo.helsepersonell

import no.nav.syfo.datatypeFactory
import no.nav.syfo.log
import no.nav.syfo.ws.createPort
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.Person
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.GregorianCalendar
import javax.xml.ws.soap.SOAPFaultException

class HelsepersonellService(private val helsepersonellV1: IHPR2Service) {

    private val PERSONNR_IKKE_FUNNET = "ArgumentException: Personnummer ikke funnet"
    private val HPR_NR_IKKE_FUNNET = "ArgumentException: HPR-nummer ikke funnet"

    fun finnBehandler(behandlersPersonnummer: String): Behandler? =
            try {
                helsepersonellV1.hentPersonMedPersonnummer(
                        behandlersPersonnummer, datatypeFactory.newXMLGregorianCalendar(GregorianCalendar())
                ).let { ws2Behandler(it) }
                        .also { log.info("Hentet behandler") }
            } catch (e: IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage) {
                when (e.message) {
                    PERSONNR_IKKE_FUNNET -> {
                        log.warn("Helsenett gir feilmelding: {}", e.message)
                        null
                    }
                    else -> {
                        log.error("Helsenett gir feilmelding: {}", e.message)
                        throw HelsepersonellException(message = e.message, cause = e.cause)
                    }
                }
            } catch (e: SOAPFaultException) {
                log.error("Helsenett gir feilmelding: {}", e.message)
                throw HelsepersonellException(message = e.message, cause = e.cause)
            }

    fun finnBehandlerFraHprNummer(hprNummer: String): Behandler? =
            try {
                helsepersonellV1.hentPerson(
                        Integer.valueOf(hprNummer), datatypeFactory.newXMLGregorianCalendar(GregorianCalendar())
                ).let { ws2Behandler(it) }
                        .also { log.info("Hentet behandler for HPR-nummer") }
            } catch (e: IHPR2ServiceHentPersonGenericFaultFaultFaultMessage) {
                when (e.message) {
                    HPR_NR_IKKE_FUNNET -> {
                        log.warn("Helsenett gir feilmelding (HPR-nummer): {}", e.message)
                        null
                    }
                    else -> {
                        log.error("Helsenett gir feilmelding (HPR-nummer): {}", e.message)
                        throw HelsepersonellException(message = e.message, cause = e.cause)
                    }
                }
            } catch (e: SOAPFaultException) {
                log.error("Helsenett gir feilmelding (HPR-nummer): {}", e.message)
                throw HelsepersonellException(message = e.message, cause = e.cause)
            }
}

fun ws2Behandler(person: Person): Behandler =
        Behandler(
                godkjenninger = person.godkjenninger.godkjenning.map { ws2Godkjenning(it) },
                fnr = person.nin,
                hprNummer = person.hprNummer,
                fornavn = person.fornavn,
                mellomnavn = person.mellomnavn,
                etternavn = person.etternavn
        )

fun ws2Godkjenning(godkjenning: no.nhn.schemas.reg.hprv2.Godkjenning): Godkjenning =
        Godkjenning(
                helsepersonellkategori = ws2Kode(godkjenning.helsepersonellkategori),
                autorisasjon = ws2Kode(godkjenning.autorisasjon),
                tillegskompetanse = when (godkjenning.tilleggskompetanser != null &&
                        godkjenning.tilleggskompetanser.tilleggskompetanse.isNotEmpty()) {
                    true -> godkjenning.tilleggskompetanser.tilleggskompetanse.map { ws2Tilleggskompetanse(it) }
                    else -> null
                }
        )

fun ws2Kode(kode: no.nhn.schemas.reg.common.no.Kode): Kode =
        Kode(
                aktiv = kode.isAktiv,
                oid = kode.oid,
                verdi = kode.verdi
        )

fun ws2Tilleggskompetanse(tillegskompetanse: no.nhn.schemas.reg.hprv2.Tilleggskompetanse): Tilleggskompetanse =
        Tilleggskompetanse(
                avsluttetStatus = tillegskompetanse.avsluttetStatus?.let { ws2Kode(tillegskompetanse.avsluttetStatus) },
                eTag = tillegskompetanse.eTag,
                gyldig = tillegskompetanse.gyldig?.let { ws2Periode(tillegskompetanse.gyldig) },
                id = tillegskompetanse.id,
                type = tillegskompetanse.type?.let { ws2Kode(tillegskompetanse.type) }
        )

fun ws2Periode(periode: no.nhn.schemas.reg.common.no.Periode): Periode =
        Periode(
                fra = periode.fra?.toGregorianCalendar()?.toZonedDateTime()?.withZoneSameInstant(ZoneOffset.UTC)?.toLocalDateTime(),
                til = periode.til?.toGregorianCalendar()?.toZonedDateTime()?.withZoneSameInstant(ZoneOffset.UTC)?.toLocalDateTime()
        )

data class Behandler(
    val godkjenninger: List<Godkjenning>,
    val fnr: String?,
    val hprNummer: Int?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
)

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null,
    val tillegskompetanse: List<Tilleggskompetanse>? = null
)

data class Kode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?
)

data class Tilleggskompetanse(
    val avsluttetStatus: Kode?,
    val eTag: String?,
    val gyldig: Periode?,
    val id: Int?,
    val type: Kode?
)

data class Periode(
    val fra: LocalDateTime?,
    val til: LocalDateTime?
)

fun helsepersonellV1(
    endpointUrl: String,
    serviceuserUsername: String,
    serviceuserPassword: String,
    securityTokenServiceUrl: String
) = createPort<IHPR2Service>(endpointUrl) {
    proxy {

        // TODO: Contact someone about this hacky workaround
        // talk to HDIR about HPR about they claim to send a ISO-8859-1 but its really UTF-8 payload
        val interceptor = object : AbstractSoapInterceptor(Phase.RECEIVE) {
            override fun handleMessage(message: SoapMessage?) {
                if (message != null)
                    message[Message.ENCODING] = "utf-8"
            }
        }
        inInterceptors.add(interceptor)
        inFaultInterceptors.add(interceptor)
    }

    port { withSTS(serviceuserUsername, serviceuserPassword, securityTokenServiceUrl) }
}

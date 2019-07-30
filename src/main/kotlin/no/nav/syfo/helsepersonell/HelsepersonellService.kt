package no.nav.syfo.helsepersonell

import no.nav.syfo.NAV_CALLID
import no.nav.syfo.log
import no.nav.syfo.ws.createPort
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.Person
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.headers.Header
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.xml.security.stax.ext.XMLSecurityConstants.datatypeFactory
import org.slf4j.MDC
import java.time.LocalDate
import java.util.UUID
import javax.xml.namespace.QName

class HelsepersonellService(private val helsepersonellV1: IHPR2Service) {
    fun finnBehandler(behandlersPersonnummer: String, paTidspunkt: LocalDate? = LocalDate.now()): Behandler? =
            helsepersonellV1.hentPersonMedPersonnummer(
                    behandlersPersonnummer, datatypeFactory.newXMLGregorianCalendar(paTidspunkt.toString())
            ).let { ws2Behandler(it) }
}

fun ws2Behandler(person: Person): Behandler =
    Behandler(godkjenninger = person.godkjenninger.godkjenning.map { ws2Godkjenning(it) })

fun ws2Godkjenning(godkjenning: no.nhn.schemas.reg.hprv2.Godkjenning): Godkjenning =
    Godkjenning(
        helsepersonellkategori = ws2Kode(godkjenning.helsepersonellkategori),
        autorisasjon = ws2Kode(godkjenning.autorisasjon)
    )

fun ws2Kode(kode: no.nhn.schemas.reg.common.no.Kode): Kode =
    Kode(
        aktiv = kode.isAktiv,
        oid = kode.oid,
        verdi = kode.verdi
    )

data class Behandler(
    val godkjenninger: List<Godkjenning>
)

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null
)

data class Kode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?
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
        outInterceptors.add(CallIdOutInterceptor())
        inFaultInterceptors.add(interceptor)
        features.add(WSAddressingFeature())
    }

    port { withSTS(serviceuserUsername, serviceuserPassword, securityTokenServiceUrl) }
}

class CallIdOutInterceptor : AbstractSoapInterceptor(Phase.WRITE) {
    override fun handleMessage(message: SoapMessage?) {
        val callId = MDC.get(NAV_CALLID) ?: run {
            UUID.randomUUID().toString()
                .also { log.info("Fant ikke callId på kall. Lager ny: $id") }
        }
        message?.headers?.add(Header(QName("callId"), callId))
        // Todo: Fjern denne etter vi ser at den virker
        log.info(message.toString())
    }
}

package no.nav.syfo.helsepersonell

import com.ctc.wstx.exc.WstxException
import no.nav.syfo.helpers.retry
import no.nav.syfo.ws.createPort
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.Person
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.xml.security.stax.ext.XMLSecurityConstants.datatypeFactory
import java.io.IOException
import java.time.LocalDate

class HelsepersonellService(private val helsepersonellV1: IHPR2Service) {
    suspend fun finnBehandler(behandlersPersonnummer: String, paTidspunkt: LocalDate? = LocalDate.now()): Behandler? =
        retry(
            callName = "hpr_hent_person_med_personnummer",
            retryIntervals = arrayOf(500L, 1000L, 3000L, 5000L, 10000L),
            legalExceptions = *arrayOf(IOException::class, WstxException::class)
        ) {
            helsepersonellV1.hentPersonMedPersonnummer(
                behandlersPersonnummer, datatypeFactory.newXMLGregorianCalendar(paTidspunkt.toString())
            ).let { ws2Behandler(it) }
        }
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
        inFaultInterceptors.add(interceptor)
        features.add(WSAddressingFeature())
    }

    port { withSTS(serviceuserUsername, serviceuserPassword, securityTokenServiceUrl) }
}

package no.nav.syfo.helsepersonell

import no.nav.syfo.datatypeFactory
import no.nav.syfo.helsepersonell.valkey.HelsepersonellValkey
import no.nav.syfo.helsepersonell.valkey.JedisBehandlerModel
import no.nav.syfo.logger
import no.nav.syfo.securelog
import no.nav.syfo.ws.createPort
import no.nhn.schemas.reg.hprv2.IHPR2Service
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.IHPR2ServicePingGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.IHPR2ServiceSøk2GenericFaultFaultFaultMessage
import no.nhn.schemas.reg.hprv2.PaginertResultatsett
import no.nhn.schemas.reg.hprv2.Person
import no.nhn.schemas.reg.hprv2.Søkeparametre
import org.apache.commons.lang3.StringUtils
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import javax.xml.ws.soap.SOAPFaultException


class HelsepersonellService(
    private val helsepersonellV1: IHPR2Service,
    private val helsepersonellValkey: HelsepersonellValkey
) {
    companion object {
        const val CACHE_TIME_HOURS = 1L
    }

    private val MAX_ANTALL_RESULTATER_PER_SIDE = 1000

    private val PERSONNR_IKKE_FUNNET = "ArgumentException: Personnummer ikke funnet"
    private val HPR_NR_IKKE_FUNNET = "ArgumentException: HPR-nummer ikke funnet"
    private val HPR_NR_IKKE_OPPGITT = "ArgumentException: HPR-nummer må oppgis"

    fun finnBehandler(behandlersPersonnummer: String): Behandler? {
        val fromValkey = helsepersonellValkey.getFromFnr(behandlersPersonnummer)
        if (fromValkey != null && shouldUseValkeyModel(fromValkey)) {
            logger.info("Returning behandler fnr found in valkey")
            return fromValkey.behandler
        }
        return try {
            helsepersonellV1
                .hentPersonMedPersonnummer(
                    behandlersPersonnummer,
                    datatypeFactory.newXMLGregorianCalendar(GregorianCalendar()),
                )
                .let { ws2Behandler(it) }
                .also {
                    logger.info("Hentet behandler for personnummer")
                    securelog.info("Hentet behandler for personnummer behandler objekt: $it")
                    helsepersonellValkey.save(it)
                }
        } catch (e: IHPR2ServiceHentPersonMedPersonnummerGenericFaultFaultFaultMessage) {
            return when (e.message) {
                PERSONNR_IKKE_FUNNET -> {
                    logger.warn("Helsenett gir feilmelding: {}", e.message)
                    null
                }
                else -> {
                    logger.error("Helsenett gir feilmelding: {}", e.message)
                    returnJedisOrThrow(fromValkey, e)
                }
            }
        } catch (e: SOAPFaultException) {
            logger.error("Helsenett gir feilmelding: {}", e.message)
            return returnJedisOrThrow(fromValkey, e)
        }
    }

    fun finnBehandlerFraHprNummer(hprNummer: String): Behandler? {
        val fromValkey = helsepersonellValkey.getFromHpr(hprNummer)
        if (fromValkey != null && shouldUseValkeyModel(fromValkey)) {
            logger.info("Returning behandler hpr found in valkey")
            return fromValkey.behandler
        }
        try {
            return helsepersonellV1
                .hentPerson(
                    Integer.valueOf(hprNummer),
                    datatypeFactory.newXMLGregorianCalendar(GregorianCalendar()),
                )
                .let { ws2Behandler(it) }
                .also {
                    logger.info("Hentet behandler for HPR-nummer")
                    securelog.info("Hentet behandler for HPR-nummer behandler objekt: $it")
                    helsepersonellValkey.save(it)
                }
        } catch (e: IHPR2ServiceHentPersonGenericFaultFaultFaultMessage) {
            return when {
                behandlerNotFound(e.message) -> {
                    logger.warn("Helsenett gir feilmelding (HPR-nummer): {}", e.message)
                    null
                }
                else -> {
                    logger.error("Helsenett gir feilmelding (HPR-nummer): {}", e.message)
                    returnJedisOrThrow(fromValkey, e)
                }
            }
        } catch (e: SOAPFaultException) {
            logger.error("Helsenett gir feilmelding (HPR-nummer): {}", e.message)
            return returnJedisOrThrow(fromValkey, e)
        }
    }

    fun soekBehandlere(soekeparametre: Soekeparametre): Behandlereresultat {
        var gjeldendeSide = 1
        return try {
            var soekeresultat = soekBehandlere(soekeparametre, gjeldendeSide)
            val behandlere = soekeresultat.personer.person.map { ws2Behandler(it) }
            while (soekeresultat.resultaterPerSide == MAX_ANTALL_RESULTATER_PER_SIDE) {
                gjeldendeSide++
                soekeresultat = soekBehandlere(soekeparametre, gjeldendeSide)
                behandlere + soekeresultat.personer.person.map { ws2Behandler(it) }
            }

            Behandlereresultat(behandlere = behandlere)
        } catch (e: IHPR2ServiceSøk2GenericFaultFaultFaultMessage) {
            logger.warn(
                "Helsenett gir feilmelding (Søk, gjeldende side: $gjeldendeSide): ${e.message}",
            )
            throw e
        } catch (e: SOAPFaultException) {
            logger.error(
                "Helsenett gir feilmelding (Søk, gjeldende side: $gjeldendeSide)): ${e.message}",
            )
            throw e
        }
    }

    private fun soekBehandlere(
        soekeparametre: Soekeparametre,
        gjeldendeSide: Int
    ): PaginertResultatsett {
        return helsepersonellV1.søk2(
            Søkeparametre().apply {
                // paginering
                resultaterPerSide = MAX_ANTALL_RESULTATER_PER_SIDE
                side = gjeldendeSide

                // søkekriterier
                if (soekeparametre.navn != null) {
                    navn = soekeparametre.navn
                }

                // evt hprNummer
                val hprNummerParameter = soekeparametre.hprNummer
                if (
                    !hprNummerParameter.isNullOrEmpty() && StringUtils.isNumeric(hprNummerParameter)
                ) {
                    hprNummer = hprNummerParameter.toInt()
                }
            },
        )
    }

    fun ping(requestId: String): String? {
        return try {
            helsepersonellV1.ping(requestId)
        } catch (e: IHPR2ServicePingGenericFaultFaultFaultMessage) {
            logger.error("Helsenett gir feilmelding {}", e.message)
            null
        } catch (e: SOAPFaultException) {
            logger.error("Helsenett gir feilmelding {}", e.message)
            null
        }
    }

    private fun returnJedisOrThrow(fromvalkey: JedisBehandlerModel?, e: Exception) =
        fromvalkey?.behandler.let {
            logger.info("Returning behandler found in valkey")
            it
        }
            ?: throw HelsepersonellException(message = e.message, cause = e.cause)

    private fun shouldUseValkeyModel(jedisBehandlerModel: JedisBehandlerModel): Boolean {
        return jedisBehandlerModel.timestamp.isAfter(
            OffsetDateTime.now(ZoneOffset.UTC).minusHours(CACHE_TIME_HOURS),
        )
    }

    private fun behandlerNotFound(message: String?): Boolean {
        return when (message) {
            HPR_NR_IKKE_FUNNET -> true
            HPR_NR_IKKE_OPPGITT -> true
            else -> false
        }
    }
}

fun ws2Behandler(person: Person): Behandler =
    Behandler(
        godkjenninger = person.godkjenninger.godkjenning.map { ws2Godkjenning(it) },
        fnr = person.nin,
        hprNummer = person.hprNummer,
        fornavn = person.fornavn,
        mellomnavn = person.mellomnavn,
        etternavn = person.etternavn,
    )

fun ws2Godkjenning(godkjenning: no.nhn.schemas.reg.hprv2.Godkjenning): Godkjenning =
    Godkjenning(
        helsepersonellkategori = ws2Kode(godkjenning.helsepersonellkategori),
        autorisasjon = ws2Kode(godkjenning.autorisasjon),
        tillegskompetanse =
            when (
                godkjenning.tilleggskompetanser != null &&
                    godkjenning.tilleggskompetanser.tilleggskompetanse.isNotEmpty()
            ) {
                true ->
                    godkjenning.tilleggskompetanser.tilleggskompetanse.map {
                        ws2Tilleggskompetanse(it)
                    }
                else -> null
            },
    )

fun ws2Kode(kode: no.nhn.schemas.reg.common.no.Kode): Kode =
    Kode(aktiv = kode.isAktiv, oid = kode.oid, verdi = kode.verdi)

fun ws2Tilleggskompetanse(
    tillegskompetanse: no.nhn.schemas.reg.hprv2.Tilleggskompetanse
): Tilleggskompetanse =
    Tilleggskompetanse(
        avsluttetStatus =
            tillegskompetanse.avsluttetStatus?.let { ws2Kode(tillegskompetanse.avsluttetStatus) },
        eTag = tillegskompetanse.eTag,
        gyldig = tillegskompetanse.gyldig?.let { ws2Periode(tillegskompetanse.gyldig) },
        id = tillegskompetanse.id,
        type = tillegskompetanse.type?.let { ws2Kode(tillegskompetanse.type) },
    )

fun ws2Periode(periode: no.nhn.schemas.reg.common.no.Periode): Periode =
    Periode(
        fra =
            periode.fra
                ?.toGregorianCalendar()
                ?.toZonedDateTime()
                ?.withZoneSameInstant(ZoneOffset.UTC)
                ?.toLocalDateTime(),
        til =
            periode.til
                ?.toGregorianCalendar()
                ?.toZonedDateTime()
                ?.withZoneSameInstant(ZoneOffset.UTC)
                ?.toLocalDateTime(),
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

data class Kode(val aktiv: Boolean, val oid: Int, val verdi: String?)

data class Tilleggskompetanse(
    val avsluttetStatus: Kode?,
    val eTag: String?,
    val gyldig: Periode?,
    val id: Int?,
    val type: Kode?
)

data class Periode(val fra: LocalDateTime?, val til: LocalDateTime?)

fun helsepersonellV1(
    endpointUrl: String,
    serviceuserUsername: String,
    serviceuserPassword: String
) =
    createPort<IHPR2Service>(endpointUrl) {
        proxy {
            // TODO: Contact someone about this hacky workaround
            // talk to HDIR about HPR about they claim to send a ISO-8859-1 but its really UTF-8
            // payload
            val inboundInterceptor =
                object : AbstractSoapInterceptor(Phase.RECEIVE) {
                    override fun handleMessage(message: SoapMessage?) {
                        if (message != null) {
                            message[Message.ENCODING] = Charsets.UTF_8.name()
                        }
                    }
                }
            inInterceptors.add(inboundInterceptor)
            inFaultInterceptors.add(inboundInterceptor)

            val outboundEncodingInteceptor =
                object : AbstractSoapInterceptor(Phase.SEND) {
                    override fun handleMessage(message: SoapMessage?) {
                        if (message != null) { message[Message.ENCODING] = Charsets.ISO_8859_1.name()
                        }
                    }
                }
            outInterceptors.add(outboundEncodingInteceptor)
            outFaultInterceptors.add(outboundEncodingInteceptor)
        }

        port { withBasicAuth(serviceuserUsername, serviceuserPassword) }
    }

package no.nav.syfo.helsepersonell.client

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.helsepersonell.Godkjenning
import no.nav.syfo.helsepersonell.Kode
import no.nav.syfo.helsepersonell.Periode
import no.nav.syfo.helsepersonell.Tilleggskompetanse

data class ByNINRequest(
    val nin: String,
    val atDate: LocalDate? = null,
    val includeHistory: Boolean? = null,
)

data class HelsepersonUtvidetDto(
    val person: PersonUtvidetDto? = null,
    val hprNummer: Int,
    val erstattetAvHprNummer: Int? = null,
    val godkjenninger: List<GodkjenningDto>? = null,
    val administrativeReaksjoner: List<AdministrativReaksjonDto>? = null,
    val sistOppdatert: LocalDateTime? = null,
    val gjelderForDato: LocalDate? = null,
)

data class PersonUtvidetDto(
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null,
    @JsonProperty("fødselsdato") val fodselsdato: LocalDate? = null,
    @JsonProperty("dødsdato") val dodsdato: LocalDate? = null,
    val manglerNin: Boolean = false,
    val nin: String? = null,
)

data class GodkjenningDto(
    val helsepersonellkategori: KodeDto? = null,
    val autorisasjon: KodeDto? = null,
    val periode: PeriodeDto? = null,
    val godkjentTurnusDato: LocalDate? = null,
    val rekvisisjonsretter: List<RekvisisjonsrettDto>? = null,
    val spesialistgodkjenninger: List<SpesialistgodkjenningDto>? = null,
    val tilleggskompetanser: List<TilleggskompetanseDto>? = null,
    @JsonProperty("vilkår") val vilkar: List<VilkarDto>? = null,
    val avsluttetStatus: KodeDto? = null,
    val administrativeReaksjoner: List<AdministrativReaksjonDto>? = null,
)

data class KodeDto(
    val navn: String? = null,
    val verdi: String? = null,
    val kodeverk: KodeverkDto? = null,
)

data class KodeverkDto(
    val id: Int? = null,
    val navn: String? = null,
)

data class PeriodeDto(
    val fra: LocalDate? = null,
    val til: LocalDate? = null,
)

data class RekvisisjonsrettDto(
    val type: KodeDto? = null,
    val periode: PeriodeDto? = null,
    val avsluttetStatus: KodeDto? = null,
    val administrativeReaksjoner: List<AdministrativReaksjonDto>? = null,
)

data class SpesialistgodkjenningDto(
    val type: KodeDto? = null,
    val periode: PeriodeDto? = null,
    val avsluttetStatus: KodeDto? = null,
    val administrativeReaksjoner: List<AdministrativReaksjonDto>? = null,
)

data class TilleggskompetanseDto(
    val type: KodeDto? = null,
    val periode: PeriodeDto? = null,
    val avsluttetStatus: KodeDto? = null,
)

data class VilkarDto(val type: KodeDto? = null)

data class AdministrativReaksjonDto(
    val type: KodeDto? = null,
    val periode: PeriodeDto? = null,
)

data class ProblemDetails(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
)

fun HelsepersonUtvidetDto.toBehandler(): Behandler =
    Behandler(
        godkjenninger = godkjenninger?.map { it.toGodkjenning() } ?: emptyList(),
        fnr = person?.nin,
        hprNummer = hprNummer,
        fornavn = person?.fornavn,
        mellomnavn = person?.mellomnavn,
        etternavn = person?.etternavn,
    )

fun GodkjenningDto.toGodkjenning(): Godkjenning =
    Godkjenning(
        helsepersonellkategori = helsepersonellkategori?.toKode(),
        autorisasjon = autorisasjon?.toKode(),
        tillegskompetanse = tilleggskompetanser?.map { it.toTilleggskompetanse() },
    )

fun KodeDto.toKode(): Kode =
    Kode(
        aktiv = true,
        oid = kodeverk?.id ?: 0,
        verdi = verdi,
    )

fun TilleggskompetanseDto.toTilleggskompetanse(): Tilleggskompetanse =
    Tilleggskompetanse(
        avsluttetStatus = avsluttetStatus?.toKode(),
        eTag = null,
        gyldig = periode?.toPeriode(),
        id = null,
        type = type?.toKode(),
    )

fun PeriodeDto.toPeriode(): Periode =
    Periode(
        fra = fra?.atStartOfDay(),
        til = til?.atStartOfDay(),
    )

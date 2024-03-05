package no.nav.syfo.sfs

import java.time.LocalDate
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.helsepersonell.Godkjenning
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.Tilleggskompetanse
import no.nav.syfo.securelog

private const val LEGE = "LE"
private const val TANNLEGE = "TL"
private const val FYSIO = "FT"
private const val MANUELLTERAPEUT = "MT"
private const val KIROPRAKTOR = "KI"

class SykmelderService(private val helsepersonellService: HelsepersonellService) {
    fun getPerson(fnr: String): Person {
        val behandler = helsepersonellService.finnBehandler(fnr) ?: return Person(false, "")

        val aktiveGodkjenninger =
            behandler.godkjenninger.filter {
                it.autorisasjon?.aktiv == true && it.helsepersonellkategori?.aktiv == true
            }

        val erAktivSykmelder =
            aktiveGodkjenninger.any {
                it.helsepersonellkategori?.verdi in listOf(LEGE, TANNLEGE, MANUELLTERAPEUT)
            } ||
                aktiveGodkjenninger.any {
                    it.helsepersonellkategori?.verdi in listOf(FYSIO, KIROPRAKTOR) &&
                        harGyldigTillegskompetanse(it)
                }

        securelog.info("Fant behandler: $behandler, erSykmelder: $erAktivSykmelder")
        return Person(erAktivSykmelder, getNavn(behandler))
    }

    private fun harGyldigTillegskompetanse(it: Godkjenning) =
        it.tillegskompetanse?.any { tillegskompetanse ->
            tillegskompetanse.avsluttetStatus == null &&
                tillegskompetanse.gyldigPeriode(LocalDate.now()) &&
                tillegskompetanse.type?.aktiv == true &&
                tillegskompetanse.type.oid == 7702 &&
                tillegskompetanse.type.verdi == "1"
        } == true

    private fun getNavn(behandler: Behandler): String {
        return "${behandler.fornavn} ${behandler.etternavn}"
    }

    private fun Tilleggskompetanse.gyldigPeriode(date: LocalDate): Boolean {
        val fom = gyldig?.fra?.toLocalDate()
        val tom = gyldig?.til?.toLocalDate()
        if (fom == null) {
            return false
        }
        return fom.minusDays(1).isBefore(date) && (tom == null || tom.plusDays(1).isAfter(date))
    }
}

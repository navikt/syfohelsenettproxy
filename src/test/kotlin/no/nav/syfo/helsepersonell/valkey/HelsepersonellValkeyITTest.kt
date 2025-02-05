package no.nav.syfo.helsepersonell.valkey

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockkClass
import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.syfo.helsepersonell.Behandler
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HelsepersonellRedisITTest {
    val helsepesronellValkey = mockkClass(HelsepersonellValkey::class, relaxed = true)

    @BeforeAll
    internal fun setup() {
        clearMocks(helsepesronellValkey)
    }

    @Test
    internal fun `Should not save when fnr is empty`() {
        val behandler = behandler().copy(fnr = "")

        val offsetDateTimeNow = OffsetDateTime.now(ZoneOffset.UTC)

        coEvery { helsepesronellValkey.getFromFnr(any()) } returns null
        coEvery { helsepesronellValkey.getFromHpr(any()) } returns
            JedisBehandlerModel(timestamp = offsetDateTimeNow, behandler = behandler)

        helsepesronellValkey.save(behandler, offsetDateTimeNow)
        val cachedBehandlerFnr = helsepesronellValkey.getFromFnr(behandler.fnr!!)
        val cachedBehandlerHpr = helsepesronellValkey.getFromHpr("${behandler.hprNummer}")

        cachedBehandlerFnr shouldBeEqualTo null
        cachedBehandlerHpr shouldBeEqualTo
            JedisBehandlerModel(
                timestamp = cachedBehandlerHpr!!.timestamp,
                behandler = behandler,
            )
    }

    @Test
    internal fun `Should not save when hprNummer = null`() {
        coEvery { helsepesronellValkey.getFromFnr(any()) } returns null
        coEvery { helsepesronellValkey.getFromHpr(any()) } returns null

        val behandler = behandler().copy(hprNummer = null)

        helsepesronellValkey.save(behandler)
        val cachedBehandlerFnr = helsepesronellValkey.getFromFnr(behandler.fnr!!)
        val cachedBehandlerHpr = helsepesronellValkey.getFromHpr("${behandler.hprNummer}")

        cachedBehandlerFnr shouldBeEqualTo null
        cachedBehandlerHpr shouldBeEqualTo null
    }

    @Test
    internal fun `should get null behandler`() {
        coEvery { helsepesronellValkey.getFromHpr(any()) } returns null

        val behandler = helsepesronellValkey.getFromHpr("10000001")
        behandler shouldBeEqualTo null
    }

    @Test
    internal fun `Should update when redis is older than 1 Hour`() {
        val timestamp = OffsetDateTime.now(ZoneOffset.UTC)
        coEvery { helsepesronellValkey.getFromHpr(any()) } returns
            JedisBehandlerModel(timestamp, behandler())

        helsepesronellValkey.save(
            behandler(),
            timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
        )
        helsepesronellValkey.save(behandler(), timestamp)

        helsepesronellValkey.getFromHpr("${behandler().hprNummer}") shouldBeEqualTo
            JedisBehandlerModel(timestamp, behandler())
    }
}

fun behandler(): Behandler {
    return Behandler(
        godkjenninger = emptyList(),
        fnr = "12345678912",
        hprNummer = 10000001,
        fornavn = "Fornavn",
        mellomnavn = null,
        etternavn = "etternavn",
    )
}

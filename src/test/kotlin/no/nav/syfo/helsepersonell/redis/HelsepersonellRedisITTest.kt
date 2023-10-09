package no.nav.syfo.helsepersonell.redis

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
    val helsepesronellRedis = mockkClass(HelsepersonellRedis::class, relaxed = true)

    @BeforeAll
    internal fun setup() {
        clearMocks(helsepesronellRedis)
    }

    @Test
    internal fun `Should not save when fnr is empty`() {
        val behandler = behandler().copy(fnr = "")

        val offsetDateTimeNow = OffsetDateTime.now(ZoneOffset.UTC)

        coEvery { helsepesronellRedis.getFromFnr(any()) } returns null
        coEvery { helsepesronellRedis.getFromHpr(any()) } returns
            JedisBehandlerModel(timestamp = offsetDateTimeNow, behandler = behandler)

        helsepesronellRedis.save(behandler, offsetDateTimeNow)
        val cachedBehandlerFnr = helsepesronellRedis.getFromFnr(behandler.fnr!!)
        val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("${behandler.hprNummer}")

        cachedBehandlerFnr shouldBeEqualTo null
        cachedBehandlerHpr shouldBeEqualTo
            JedisBehandlerModel(
                timestamp = cachedBehandlerHpr!!.timestamp,
                behandler = behandler,
            )
    }

    @Test
    internal fun `Should not save when hprNummer = null`() {
        coEvery { helsepesronellRedis.getFromFnr(any()) } returns null
        coEvery { helsepesronellRedis.getFromHpr(any()) } returns null

        val behandler = behandler().copy(hprNummer = null)

        helsepesronellRedis.save(behandler)
        val cachedBehandlerFnr = helsepesronellRedis.getFromFnr(behandler.fnr!!)
        val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("${behandler.hprNummer}")

        cachedBehandlerFnr shouldBeEqualTo null
        cachedBehandlerHpr shouldBeEqualTo null
    }

    @Test
    internal fun `should get null behandler`() {
        coEvery { helsepesronellRedis.getFromHpr(any()) } returns null

        val behandler = helsepesronellRedis.getFromHpr("10000001")
        behandler shouldBeEqualTo null
    }

    @Test
    internal fun `Should update when redis is older than 1 Hour`() {
        val timestamp = OffsetDateTime.now(ZoneOffset.UTC)
        coEvery { helsepesronellRedis.getFromHpr(any()) } returns
            JedisBehandlerModel(timestamp, behandler())

        helsepesronellRedis.save(
            behandler(),
            timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
        )
        helsepesronellRedis.save(behandler(), timestamp)

        helsepesronellRedis.getFromHpr("${behandler().hprNummer}") shouldBeEqualTo
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

package no.nav.syfo.helsepersonell.redis

import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockkClass
import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.syfo.helsepersonell.Behandler
import org.amshove.kluent.shouldBeEqualTo

class HelsepersonellRedisITTest :
    FunSpec({
        val helsepesronellRedis = mockkClass(HelsepersonellRedis::class, relaxed = true)

        beforeTest { clearMocks(helsepesronellRedis) }

        context("Test redis") {
            test("should not save when fnr is empty") {
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
                        behandler = behandler
                    )
            }

            test("Should not save when hprNummer = null") {
                coEvery { helsepesronellRedis.getFromFnr(any()) } returns null
                coEvery { helsepesronellRedis.getFromHpr(any()) } returns null

                val behandler = behandler().copy(hprNummer = null)

                helsepesronellRedis.save(behandler)
                val cachedBehandlerFnr = helsepesronellRedis.getFromFnr(behandler.fnr!!)
                val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("${behandler.hprNummer}")

                cachedBehandlerFnr shouldBeEqualTo null
                cachedBehandlerHpr shouldBeEqualTo null
            }

            test("should get null behandler") {
                coEvery { helsepesronellRedis.getFromHpr(any()) } returns null

                val behandler = helsepesronellRedis.getFromHpr("10000001")
                behandler shouldBeEqualTo null
            }

            test("Should update when redis is older than 1 Hour") {
                val timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                coEvery { helsepesronellRedis.getFromHpr(any()) } returns
                    JedisBehandlerModel(timestamp, behandler())

                helsepesronellRedis.save(
                    behandler(),
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)
                )
                helsepesronellRedis.save(behandler(), timestamp)

                helsepesronellRedis.getFromHpr("${behandler().hprNummer}") shouldBeEqualTo
                    JedisBehandlerModel(timestamp, behandler())
            }
        }
    })

fun behandler(): Behandler {
    val behandler =
        Behandler(
            godkjenninger = emptyList(),
            fnr = "12345678912",
            hprNummer = 10000001,
            fornavn = "Fornavn",
            mellomnavn = null,
            etternavn = "etternavn"
        )
    return behandler
}

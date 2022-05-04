package no.nav.syfo.helsepersonell.redis

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.helsepersonell.HelsepersonellRedis
import org.amshove.kluent.shouldBeEqualTo
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import redis.clients.jedis.JedisPool
import java.time.OffsetDateTime
import java.time.ZoneOffset

class HelsepersonellRedisITTest : FunSpec({

    val redisContainer: GenericContainer<Nothing> = GenericContainer("navikt/secure-redis:5.0.3-alpine-2")
    redisContainer.withExposedPorts(6379)
    redisContainer.withEnv("REDIS_PASSWORD", "secret")
    redisContainer.withClasspathResourceMapping(
        "redis.env",
        "/var/run/secrets/nais.io/vault/redis.env",
        BindMode.READ_ONLY
    )

    redisContainer.start()

    val jedisPool = JedisPool(JedisConfig(), redisContainer.host, redisContainer.getMappedPort(6379))

    val helsepesronellRedis = HelsepersonellRedis(jedisPool, "secret")
    afterSpec {
        redisContainer.stop()
    }

    beforeTest {
        val jedis = jedisPool.resource
        jedis.auth("secret")
        jedis.keys("**").forEach {
            jedis.del(it)
        }

        jedis.close()
    }

    context("Test redis") {
        test("Should cache behandler in redis") {
            val behandler = behandler()

            helsepesronellRedis.save(behandler)

            val cachedBehandlerFnr = helsepesronellRedis.getFromFnr("12345678912")
            val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("10000001")

            cachedBehandlerFnr shouldBeEqualTo JedisBehandlerModel(timestamp = cachedBehandlerFnr!!.timestamp, behandler = behandler)
            cachedBehandlerHpr shouldBeEqualTo JedisBehandlerModel(timestamp = cachedBehandlerFnr.timestamp, behandler = behandler)
        }

        test("should not save when fnr is empty") {
            val behandler = behandler().copy(fnr = "")

            helsepesronellRedis.save(behandler)
            val cachedBehandlerFnr = helsepesronellRedis.getFromFnr(behandler.fnr!!)
            val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("${behandler.hprNummer}")

            cachedBehandlerFnr shouldBeEqualTo null
            cachedBehandlerHpr shouldBeEqualTo JedisBehandlerModel(timestamp = cachedBehandlerHpr!!.timestamp, behandler = behandler)
        }

        test("Should not save when hprNummer = null") {
            val behandler = behandler().copy(hprNummer = null)

            helsepesronellRedis.save(behandler)
            val cachedBehandlerFnr = helsepesronellRedis.getFromFnr(behandler.fnr!!)
            val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("${behandler.hprNummer}")

            cachedBehandlerFnr shouldBeEqualTo null
            cachedBehandlerHpr shouldBeEqualTo null
        }

        test("should get null behandler") {
            val behandler = helsepesronellRedis.getFromHpr("10000001")
            behandler shouldBeEqualTo null
        }

        test("Should update when redis is older than 1 Hour") {
            val timestamp = OffsetDateTime.now(ZoneOffset.UTC)
            helsepesronellRedis.save(behandler(), timestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
            helsepesronellRedis.save(behandler(), timestamp)

            helsepesronellRedis.getFromHpr("${behandler().hprNummer}") shouldBeEqualTo JedisBehandlerModel(timestamp, behandler())
        }
    }
})

fun behandler(): Behandler {
    val behandler = Behandler(
        godkjenninger = emptyList(),
        fnr = "12345678912",
        hprNummer = 10000001,
        fornavn = "Fornavn",
        mellomnavn = null,
        etternavn = "etternavn"
    )
    return behandler
}

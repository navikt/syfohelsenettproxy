package no.nav.syfo.helsepersonell.redis

import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.helsepersonell.HelsepersonellRedis
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import redis.clients.jedis.JedisPool

class HelsepersonellRedisITTest : Spek({

    val redisContainer: GenericContainer<Nothing> = GenericContainer("navikt/secure-redis:5.0.3-alpine-2")
    redisContainer.withExposedPorts(6379)
    redisContainer.withEnv("REDIS_PASSWORD", "secret")
    redisContainer.withClasspathResourceMapping(
        "redis.env",
        "/var/run/secrets/nais.io/vault/redis.env",
        BindMode.READ_ONLY
    )

    redisContainer.start()

    val jedisPool = JedisPool(JedisConfig(), redisContainer.containerIpAddress, redisContainer.getMappedPort(6379))

    val helsepesronellRedis = HelsepersonellRedis(jedisPool, "secret")
    afterGroup {
        redisContainer.stop()
    }

    beforeEachTest {
        val jedis = jedisPool.resource
        jedis.auth("secret")
        jedis.keys("**").forEach {
            jedis.del(it)
        }

        jedis.close()
    }

    describe("Test redis") {
        it("Should cache behandler in redis") {
            val behandler = behandler()

            helsepesronellRedis.save(behandler)

            val cachedBehandlerFnr = helsepesronellRedis.getFromFnr("12345678912")
            val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("10000001")

            cachedBehandlerFnr shouldEqual behandler
            cachedBehandlerHpr shouldEqual behandler
        }

        it("should not save when fnr is empty") {
            val behandler = behandler().copy(fnr = "")

            helsepesronellRedis.save(behandler)
            val cachedBehandlerFnr = helsepesronellRedis.getFromFnr(behandler.fnr!!)
            val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("${behandler.hprNummer}")

            cachedBehandlerFnr shouldEqual null
            cachedBehandlerHpr shouldEqual behandler
        }

        it("Should not save when hprNummer = null") {
            val behandler = behandler().copy(hprNummer = null)

            helsepesronellRedis.save(behandler)
            val cachedBehandlerFnr = helsepesronellRedis.getFromFnr(behandler.fnr!!)
            val cachedBehandlerHpr = helsepesronellRedis.getFromHpr("${behandler.hprNummer}")

            cachedBehandlerFnr shouldEqual null
            cachedBehandlerHpr shouldEqual null
        }

        it("should get null behandler") {
            val behandler = helsepesronellRedis.getFromHpr("10000001")
            behandler shouldEqual null
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

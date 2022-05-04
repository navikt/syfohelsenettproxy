package no.nav.syfo.helsepersonell.redis

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.helsepersonell.HelsepersonellRedis
import org.amshove.kluent.shouldBeEqualTo
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisConnectionException

class HelsepersonellRedisTest : FunSpec({
    val redis = mockk<JedisPool>()
    val helseperonellRedis = HelsepersonellRedis(redis, "secret")
    context("Test HelsepersonellRedis") {
        test("Should not fail when could not get connect from pool") {
            every { redis.resource } throws JedisConnectionException("Could not get a resource from the pool")
            helseperonellRedis.save(behandler())
            helseperonellRedis.getFromHpr("10000001") shouldBeEqualTo null
            helseperonellRedis.getFromFnr("12345678912") shouldBeEqualTo null
        }
    }
})

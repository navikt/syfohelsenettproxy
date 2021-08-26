package no.nav.syfo.helsepersonell.redis

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.helsepersonell.HelsepersonellRedis
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisConnectionException

class HelsepersonellRedisTest : Spek({
    val redis = mockk<JedisPool>()
    val helseperonellRedis = HelsepersonellRedis(redis, "secret")
    describe("Test HelsepersonellRedis") {
        it("Should not fail when could not get connect from pool") {
            every { redis.resource } throws JedisConnectionException("Could not get a resource from the pool")
            helseperonellRedis.save(behandler())
            helseperonellRedis.getFromHpr("10000001") shouldBeEqualTo null
            helseperonellRedis.getFromFnr("12345678912") shouldBeEqualTo null
        }
    }
})

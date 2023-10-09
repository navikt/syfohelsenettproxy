package no.nav.syfo.helsepersonell.redis

import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisConnectionException

internal class HelsepersonellRedisTest {
    private val redis = mockk<JedisPool>()
    private val helseperonellRedis =
        HelsepersonellRedis(
            redis,
        )

    @Test
    internal fun `Should not fail when could not get connect from pool`() {
        every { redis.resource } throws
            JedisConnectionException("Could not get a resource from the pool")
        helseperonellRedis.save(behandler())
        helseperonellRedis.getFromHpr("10000001") shouldBeEqualTo null
        helseperonellRedis.getFromFnr("12345678912") shouldBeEqualTo null
    }
}

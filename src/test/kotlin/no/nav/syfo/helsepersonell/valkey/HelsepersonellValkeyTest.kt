package no.nav.syfo.helsepersonell.valkey

import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisConnectionException

internal class HelsepersonellValkeyTest {
    private val jedisPool = mockk<JedisPool>()
    private val helseperonellValkey =
        HelsepersonellValkey(
            jedisPool,
        )

    @Test
    internal fun `Should not fail when could not get connect from pool`() {
        every { jedisPool.resource } throws
            JedisConnectionException("Could not get a resource from the pool")
        helseperonellValkey.save(behandler())
        helseperonellValkey.getFromHpr("10000001") shouldBeEqualTo null
        helseperonellValkey.getFromFnr("12345678912") shouldBeEqualTo null
    }
}

package no.nav.syfo.helsepersonell.valkey

import io.mockk.every
import io.mockk.mockk
import io.valkey.JedisPool
import io.valkey.exceptions.JedisConnectionException
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class HelsepersonellValkeyTest {
    private val redis = mockk<JedisPool>()
    private val helseperonellValkey =
        HelsepersonellValkey(
            redis,
        )

    @Test
    internal fun `Should not fail when could not get connect from pool`() {
        every { redis.resource } throws
            JedisConnectionException("Could not get a resource from the pool")
        helseperonellValkey.save(behandler())
        helseperonellValkey.getFromHpr("10000001") shouldBeEqualTo null
        helseperonellValkey.getFromFnr("12345678912") shouldBeEqualTo null
    }
}

package no.nav.syfo.helsepersonell.valkey

import io.valkey.JedisPool
import io.valkey.JedisPoolConfig
import no.nav.syfo.helsepersonell.Behandler
import org.junit.AfterClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

val valkeyContainer: GenericContainer<Nothing> = GenericContainer("valkey/valkey:8.0.2-alpine")

internal class ValkeyTest {

    init {
        valkeyContainer.withExposedPorts(6379)
        valkeyContainer.waitingFor(Wait.forListeningPort())
        valkeyContainer.start()
    }

    private val jedisPool =
        JedisPool(JedisPoolConfig(), valkeyContainer.host, valkeyContainer.getMappedPort(6379))

    private val helsepersonellValkey = HelsepersonellValkey(jedisPool)

    @Test
    internal fun `Should cache behandler in valkey`() {
        val behandler =
            Behandler(
                godkjenninger = emptyList(),
                fnr = "12345678912",
                hprNummer = 10000001,
                fornavn = "Fornavn",
                mellomnavn = null,
                etternavn = "etternavn",
            )

        helsepersonellValkey.save(behandler)

        val cachedBehandler = helsepersonellValkey.getFromFnr(behandler.fnr!!)

        assertEquals(
            behandler.fnr,
            cachedBehandler?.behandler?.fnr,
        )
    }

    companion object {
        @JvmStatic
        @AfterClass
        fun stopValkey() {
            valkeyContainer.stop()
        }
    }
}

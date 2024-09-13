package no.nav.syfo.fastlegeinformasjon

import io.mockk.*


import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FastlegeinformasjonServiceTest {

    @BeforeEach
    internal fun setup() {
        clearAllMocks()
    }

    @Test
    fun shouldCreatePort() {
        assertDoesNotThrow {
            fastlegeinformasjonV2("", "", "")
        }

    }

}

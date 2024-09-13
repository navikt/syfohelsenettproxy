package no.nav.syfo.fastlegeinformasjon

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FastlegeinformasjonServiceTest {
    

    @Test
    fun shouldCreatePort() {
        assertDoesNotThrow {
            fastlegeinformasjonV2("", "", "")
        }

    }

}

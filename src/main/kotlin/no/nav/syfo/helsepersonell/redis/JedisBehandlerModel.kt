package no.nav.syfo.helsepersonell.redis

import no.nav.syfo.helsepersonell.Behandler
import java.time.OffsetDateTime

data class JedisBehandlerModel(
    val timestamp: OffsetDateTime,
    val behandler: Behandler
)

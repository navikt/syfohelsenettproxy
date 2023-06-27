package no.nav.syfo.helsepersonell.redis

import java.time.OffsetDateTime
import no.nav.syfo.helsepersonell.Behandler

data class JedisBehandlerModel(val timestamp: OffsetDateTime, val behandler: Behandler)

package no.nav.syfo.helsepersonell.valkey

import io.valkey.DefaultJedisClientConfig
import io.valkey.HostAndPort
import io.valkey.JedisPool
import java.net.URI
import no.nav.syfo.getEnvVar

class ValkeyConfig(
    valkeyUri: URI = URI(getEnvVar("VALKEY_URI_SYFOHELSENETTPROXY")),
    val valkeyUsername: String = getEnvVar("VALKEY_USERNAME_SYFOHELSENETTPROXY"),
    val valkeyPassword: String = getEnvVar("VALKEY_PASSWORD_SYFOHELSENETTPROXY"),
    val ssl: Boolean = true
) {
    val host: String = valkeyUri.host
    val port: Int = valkeyUri.port
}

fun createJedisPool(valkeyConfig: ValkeyConfig = ValkeyConfig()): JedisPool {
    return JedisPool(
        HostAndPort(valkeyConfig.host, valkeyConfig.port),
        DefaultJedisClientConfig.builder()
            .ssl(valkeyConfig.ssl)
            .user(valkeyConfig.valkeyUsername)
            .password(valkeyConfig.valkeyPassword)
            .build()
    )
}

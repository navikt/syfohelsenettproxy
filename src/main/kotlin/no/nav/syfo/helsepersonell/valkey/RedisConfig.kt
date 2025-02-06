package no.nav.syfo.helsepersonell.valkey

import io.valkey.DefaultJedisClientConfig
import io.valkey.HostAndPort
import io.valkey.JedisPool
import io.valkey.JedisPoolConfig
import no.nav.syfo.getEnvVar

class ValkeyConfig(
    val valkeyUsername: String = getEnvVar("VALKEY_USERNAME_SYFOHELSENETTPROXY"),
    val valkeyPassword: String = getEnvVar("VALKEY_PASSWORD_SYFOHELSENETTPROXY"),
    val host: String = getEnvVar("VALKEY_HOST_SYFOHELSENETTPROXY"),
    val port: Int = getEnvVar("VALKEY_PORT_SYFOHELSENETTPROXY").toInt(),
    val ssl: Boolean = true
)

fun createJedisPool(valkeyConfig: ValkeyConfig = ValkeyConfig()): JedisPool {
    return JedisPool(
        JedisPoolConfig(),
        HostAndPort(valkeyConfig.host, valkeyConfig.port),
        DefaultJedisClientConfig.builder()
            .ssl(valkeyConfig.ssl)
            .user(valkeyConfig.valkeyUsername)
            .password(valkeyConfig.valkeyPassword)
            .build()
    )
}

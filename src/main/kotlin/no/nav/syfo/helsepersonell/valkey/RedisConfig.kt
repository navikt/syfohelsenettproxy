package no.nav.syfo.helsepersonell.valkey

import no.nav.syfo.getEnvVar
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

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

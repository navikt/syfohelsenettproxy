package no.nav.syfo.helsepersonell.redis

import java.net.URI
import no.nav.syfo.getEnvVar
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisConfig(
    redisUri: URI = URI(getEnvVar("REDIS_URI_SYFOHELSENETTPROXY")),
    val redisUsername: String = getEnvVar("REDIS_USERNAME_SYFOHELSENETTPROXY"),
    val redisPassword: String = getEnvVar("REDIS_PASSWORD_SYFOHELSENETTPROXY"),
    val ssl: Boolean = true
) {
    val host: String = redisUri.host
    val port: Int = redisUri.port
}

fun createJedisPool(redisConfig: RedisConfig = RedisConfig()): JedisPool {
    return JedisPool(
        JedisPoolConfig(),
        HostAndPort(redisConfig.host, redisConfig.port),
        DefaultJedisClientConfig.builder()
            .ssl(redisConfig.ssl)
            .user(redisConfig.redisUsername)
            .password(redisConfig.redisPassword)
            .build()
    )
}

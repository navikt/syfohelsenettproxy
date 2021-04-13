package no.nav.syfo.helsepersonell.redis

import redis.clients.jedis.JedisPoolConfig

class JedisConfig : JedisPoolConfig() {
    init {
        testWhileIdle = true
        minEvictableIdleTimeMillis = 300000
        timeBetweenEvictionRunsMillis = 60_000
        maxTotal = 20
        maxIdle = 20
        maxWaitMillis = 1000
        minIdle = 10
        blockWhenExhausted = true
    }
}

package no.nav.syfo.helsepersonell

import no.nav.syfo.log
import no.nav.syfo.objectMapper
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

class HelsepersonellRedis(var jedisPool: JedisPool, private val redisSecret: String) {
    companion object {
        val REDIS_TIMEOUT_SECONDS = 3600
    }

    fun save(behandler: Behandler) {
        var jedis: Jedis? = null
        try {
            when (behandler.hprNummer != null && "${behandler.hprNummer}".length > 1) {
                true -> {
                    jedis = jedisPool.resource
                    jedis.auth(redisSecret)
                    jedis.setex(
                        "hpr:${behandler.hprNummer}",
                        REDIS_TIMEOUT_SECONDS,
                        objectMapper.writeValueAsString(behandler)
                    )
                    when (behandler.fnr.isNullOrBlank()) {
                        true -> log.warn("Behandler does not have fnr")
                        false -> jedis.setex("fnr:${behandler.fnr}", REDIS_TIMEOUT_SECONDS, "${behandler.hprNummer}")
                    }
                }
                false -> log.error("Behandler does not have HPR-number")
            }
        } catch (ex: Exception) {
            log.error("Could not save behandler in Redis", ex)
        } finally {
            jedis?.close()
        }
    }

    fun getFromFnr(fnr: String): Behandler? {
        return when (fnr.isNotBlank()) {
            true -> initRedis() { jedis ->
                jedis.get("fnr:$fnr")?.let {
                    getBehandlerFromRedis(jedis, it)
                }
            }
            false -> null
        }
    }

    fun getFromHpr(hprNummer: String): Behandler? {
        return initRedis { jedis ->
            getBehandlerFromRedis(jedis, hprNummer)
        }
    }

    private fun initRedis(block: (jedis: Jedis) -> Behandler?): Behandler? {
        var jedis: Jedis? = null
        return try {
            jedis = jedisPool.resource
            jedis.auth(redisSecret)
            block.invoke(jedis)
        } catch (ex: Exception) {
            log.error("Could not get behandler in Redis", ex)
            null
        } finally {
            jedis?.close()
        }
    }

    private fun getBehandlerFromRedis(
        jedis: Jedis,
        hprNummer: String
    ) = objectMapper.readValue(jedis.get("hpr:$hprNummer"), Behandler::class.java)
}

package no.nav.syfo.helsepersonell.redis

import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.logger
import no.nav.syfo.objectMapper
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

class HelsepersonellRedis(var jedisPool: JedisPool) {
    fun save(behandler: Behandler, timestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)) {
        var jedis: Jedis? = null
        try {
            when (behandler.hprNummer != null && "${behandler.hprNummer}".length > 1) {
                true -> {
                    jedis = jedisPool.resource
                    val jedisBehandlerModel = JedisBehandlerModel(timestamp, behandler)
                    jedis.set(
                        "hpr:${behandler.hprNummer}",
                        objectMapper.writeValueAsString(jedisBehandlerModel)
                    )
                    when (behandler.fnr.isNullOrBlank()) {
                        true -> logger.warn("Behandler does not have fnr from hpr")
                        false -> jedis.set("fnr:${behandler.fnr}", "${behandler.hprNummer}")
                    }
                }
                false -> logger.error("Behandler does not have HPR-number")
            }
        } catch (ex: Exception) {
            logger.error("Could not save behandler in Redis", ex)
        } finally {
            jedis?.close()
        }
    }

    fun getFromFnr(fnr: String): JedisBehandlerModel? {
        return when (fnr.isNotBlank()) {
            true ->
                initRedis() { jedis ->
                    jedis.get("fnr:$fnr")?.let { getBehandlerFromRedis(jedis, it) }
                }
            false -> null
        }
    }

    fun getFromHpr(hprNummer: String): JedisBehandlerModel? {
        return initRedis { jedis -> getBehandlerFromRedis(jedis, hprNummer) }
    }

    private fun initRedis(block: (jedis: Jedis) -> JedisBehandlerModel?): JedisBehandlerModel? {
        var jedis: Jedis? = null
        return try {
            jedis = jedisPool.resource
            block.invoke(jedis)
        } catch (ex: Exception) {
            logger.error("Could not get behandler in Redis", ex)
            null
        } finally {
            jedis?.close()
        }
    }

    private fun getBehandlerFromRedis(jedis: Jedis, hprNummer: String): JedisBehandlerModel? {
        val behandlerString = jedis.get("hpr:$hprNummer")
        return when (behandlerString.isNullOrBlank()) {
            true -> null
            false -> objectMapper.readValue(behandlerString, JedisBehandlerModel::class.java)
        }
    }
}

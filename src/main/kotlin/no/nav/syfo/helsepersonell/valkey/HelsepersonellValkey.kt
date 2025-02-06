package no.nav.syfo.helsepersonell.valkey

import io.valkey.Jedis
import io.valkey.JedisPool
import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.logger
import no.nav.syfo.objectMapper

class HelsepersonellValkey(var jedisPool: JedisPool) {

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
            logger.error("Could not save behandler in valkey", ex)
        } finally {
            jedis?.close()
        }
    }

    fun getFromFnr(fnr: String): JedisBehandlerModel? {
        return when (fnr.isNotBlank()) {
            true ->
                initJedis() { jedis ->
                    jedis.get("fnr:$fnr")?.let { getBehandlerFromValkey(jedis, it) }
                }
            false -> null
        }
    }

    fun getFromHpr(hprNummer: String): JedisBehandlerModel? {
        return initJedis { jedis -> getBehandlerFromValkey(jedis, hprNummer) }
    }

    private fun initJedis(block: (jedis: Jedis) -> JedisBehandlerModel?): JedisBehandlerModel? {
        var jedis: Jedis? = null
        return try {
            jedis = jedisPool.resource
            block.invoke(jedis)
        } catch (ex: Exception) {
            logger.error("Could not get behandler in valkey", ex)
            null
        } finally {
            jedis?.close()
        }
    }

    private fun getBehandlerFromValkey(jedis: Jedis, hprNummer: String): JedisBehandlerModel? {
        val behandlerString = jedis.get("hpr:$hprNummer")
        return when (behandlerString.isNullOrBlank()) {
            true -> null
            false -> objectMapper.readValue(behandlerString, JedisBehandlerModel::class.java)
        }
    }
}

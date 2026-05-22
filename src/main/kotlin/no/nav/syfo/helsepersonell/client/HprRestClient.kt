package no.nav.syfo.helsepersonell.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.LocalDate
import no.nav.syfo.helsepersonell.Behandler
import no.nav.syfo.objectMapper
import no.nav.syfo.plugins.HprAuthClient
import org.slf4j.LoggerFactory

class HprRestClient(
    private val texas: HprAuthClient,
    private val httpClient: HttpClient,
    private val hprUrl: String,
) {
    private val logger = LoggerFactory.getLogger(HprRestClient::class.java)
    private val securelog = LoggerFactory.getLogger("securelog")

    suspend fun getPersonFromIdent(ident: String): Behandler? {
        val token = texas.requestToken().token
        val response =
            httpClient.post("$hprUrl/v1/personerutvidet") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(ByNINRequest(nin = ident, atDate = LocalDate.now()))
            }
        return handleResponse(response, "ident")
    }

    suspend fun getPersonFromHpr(hpr: String): Behandler? {
        val token = texas.requestToken().token
        val response = httpClient.get("$hprUrl/v1/personerutvidet/$hpr") { bearerAuth(token) }
        return handleResponse(response, "hpr=$hpr")
    }

    private suspend fun handleResponse(response: HttpResponse, context: String): Behandler? {
        logger.info("HPR REST returnerer ${response.status} for $context")
        return when (response.status) {
            HttpStatusCode.OK -> {
                val responseText = response.bodyAsText()
                securelog.info("HPR REST response for $context: $responseText")
                objectMapper
                    .readValue(responseText, HelsepersonUtvidetDto::class.java)
                    .toBehandler()
            }
            HttpStatusCode.NotFound -> {
                logger.info("HPR REST: person ikke funnet for $context")
                null
            }
            else -> {
                val body = runCatching { response.bodyAsText() }.getOrDefault("")
                securelog.warn("HPR REST feilet for $context: ${response.status} $body")
                throw HprRestException("HPR REST returnerte ${response.status} for $context: $body")
            }
        }
    }
}

class HprRestException(message: String) : RuntimeException(message)

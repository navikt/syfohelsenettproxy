package no.nav.syfo.plugins

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.slf4j.LoggerFactory

data class TexasToken(val token: String)

class HprAuthClient(
    httpClient: HttpClient,
    private val hprAuthType: String,
    private val hprRestTargetScopes: String,
    private val texasUrl: String
) {
    private val logger = LoggerFactory.getLogger(HprAuthClient::class.java)

    private val texasHttpClient = httpClient.config { install(ContentNegotiation) { jackson {} } }

    suspend fun requestToken(): TexasToken {
        val target = hprRestTargetScopes
        val requestBody = TokenRequest(identityProvider = hprAuthType, target = target)

        val response =
            texasHttpClient.post(texasUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

        if (!response.status.isSuccess()) {
            response.logNonSuccess(target)
            throw IllegalStateException("Unable to request m2m token for: $target")
        }

        val body = response.body<TokenResponse>()
        return TexasToken(body.accessToken)
    }

    private suspend fun HttpResponse.logNonSuccess(target: String) {
        if (this.contentType()?.isTextType() == true) {
            logger.error(
                "Unable to request m2m token for: ${target}, texas says: ${this.body<String>()}"
            )
        } else {
            logger.error(
                "Unable to request m2m token for: ${target}, texas responded with status ${this.status} and no content type"
            )
        }
    }

    internal data class TokenRequest(
        @param:JsonProperty("identity_provider") val identityProvider: String,
        val target: String,
    )

    internal data class TokenResponse(
        @param:JsonProperty("access_token") val accessToken: String,
        @param:JsonProperty("expires_in") val expiresIn: Int,
        @param:JsonProperty("token_type") val tokenType: String,
    )
}

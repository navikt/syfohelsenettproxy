package no.nav.syfo.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import java.io.IOException
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

const val keyId = "localhost-signer"

/* Utsteder en Bearer-token (En slik vi ber AzureAd om). OBS: Det er viktig at KeyId matcher kid i jwkset.json
 */
fun genereateJWT(
    consumerClientId: String? = "consumerClientId",
    audience: String? = "helsenett-clientId-v2",
    expiry: LocalDateTime? = LocalDateTime.now().plusHours(1)
): String? {
    val now = Date()
    val key = getDefaultRSAKey()
    val alg = Algorithm.RSA256(key.toRSAPublicKey(), key.toRSAPrivateKey())

    return JWT.create()
        .withKeyId(keyId)
        .withSubject("subject")
        .withIssuer("https://sts.issuer.net/myidV2")
        .withAudience(audience)
        .withJWTId(UUID.randomUUID().toString())
        .withClaim("ver", "1.0")
        .withClaim("nonce", "myNonce")
        .withClaim("auth_time", now)
        .withClaim("nbf", now)
        .withClaim("iat", now)
        .withClaim("azp", consumerClientId)
        .withClaim("appid", consumerClientId)
        .withClaim("exp", Date.from(expiry?.atZone(ZoneId.systemDefault())?.toInstant()))
        .sign(alg)
}

private fun getDefaultRSAKey(): RSAKey {
    return getJWKSet().getKeyByKeyId(keyId) as RSAKey
}

private fun getJWKSet(): JWKSet {
    try {
        return JWKSet.parse(getFileAsString("src/test/resources/jwkset.json"))
    } catch (io: IOException) {
        throw RuntimeException(io)
    } catch (io: ParseException) {
        throw RuntimeException(io)
    }
}

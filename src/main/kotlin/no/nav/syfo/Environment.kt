package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "1").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "syfohelsenettproxy"),
    val securityTokenServiceUrl: String = getEnvVar("SECURITY_TOKEN_SERVICE_URL"),
    val helsepersonellv1EndpointURL: String = getEnvVar("HELSEPERSONELL_V1_ENDPOINT_URL"),
    val aadAccessTokenUrl: String = getEnvVar("AADACCESSTOKEN_URL"),
    val aadDiscoveryUrl: String = getEnvVar("AADDISCOVERY_URL"),
    val jwkKeysUrl: String = getEnvVar("JWKKEYS_URL", "https://login.microsoftonline.com/common/discovery/keys"),
    val jwtIssuer: String = getEnvVar("JWT_ISSUER"),
    val clientId: String = getEnvVar("CLIENT_ID")
)

data class VaultCredentials(
    val serviceuserUsername: String,
    val serviceuserPassword: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

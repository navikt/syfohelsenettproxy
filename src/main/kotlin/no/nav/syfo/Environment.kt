package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "1").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "syfohelsenettproxy"),
    val helsepersonellv1EndpointURL: String = getEnvVar("HELSEPERSONELL_V1_ENDPOINT_URL"),
    val redisHost: String =
        getEnvVar("REDIS_HOST", "syfohelsenettproxy-redis.teamsykmelding.svc.nais.local"),
    val redisPort: Int = getEnvVar("REDIS_PORT_SYKMELDINGER", "6379").toInt(),
    val redisSecret: String = getEnvVar("REDIS_PASSWORD"),
    val clientIdV2: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val jwkKeysUrlV2: String = getEnvVar("AZURE_OPENID_CONFIG_JWKS_URI"),
    val jwtIssuerV2: String = getEnvVar("AZURE_OPENID_CONFIG_ISSUER")
)

data class ServiceUser(
    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName)
        ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

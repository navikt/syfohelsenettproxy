package no.nav.syfo.plugins

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import no.nav.syfo.Environment
import no.nav.syfo.ServiceUser
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.fastlegeinformasjon.FastlegeinformasjonService
import no.nav.syfo.fastlegeinformasjon.fastlegeinformasjonV2
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.client.HprRestClient
import no.nav.syfo.helsepersonell.helsepersonellV1
import no.nav.syfo.helsepersonell.valkey.HelsepersonellValkey
import no.nav.syfo.helsepersonell.valkey.createJedisPool
import no.nav.syfo.sfs.SykmelderService
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureModules() {
    install(Koin) {
        slf4jLogger()

        modules(
            environmentModule,
            jedisModule,
            serviceUserModule,
            applicationStateModule,
            helsepersonellModule,
            authModule,
            sfsModule,
            fastlegeinformasjonModule,
        )
    }
}

val environmentModule = module { single { Environment() } }
val applicationStateModule = module { single { ApplicationState() } }
val serviceUserModule = module { single { ServiceUser() } }
val jedisModule = module { single { createJedisPool() } }
val authModule = module {
    single(named("AadAuthConfig")) { getAadAuthConfig(get()) }
    single(named("TokenXAuthConfig")) { getTokenXAuthConfig(get()) }
}
val helsepersonellModule = module {
    single<HttpClient>(named("hprHttpClient")) {
        HttpClient(Apache5) {
            install(ContentNegotiation) {
                jackson {
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                }
            }
        }
    }

    single {
        val env: Environment = get()
        HprAuthClient(
            httpClient = get(named("hprHttpClient")),
            hprAuthType = env.hprAuthType,
            hprRestTargetScopes = env.hprRestTargetScopes,
            texasUrl = env.texasUrl,
        )
    }
    single {
        val env = get<Environment>()
        HprRestClient(get(), get(named("hprHttpClient")), env.hprRestUrl)
    }
    single {
        val env = get<Environment>()
        val serviceUser = get<ServiceUser>()
        helsepersonellV1(
            env.helsepersonellv1EndpointURL,
            serviceUser.serviceuserUsername,
            serviceUser.serviceuserPassword,
        )
    }
    single { HelsepersonellValkey(get()) }
    single { HelsepersonellService(get(), get(), get()) }
}

val fastlegeinformasjonModule = module {
    single {
        val env = get<Environment>()
        val serviceUser = get<ServiceUser>()
        val operation =
            fastlegeinformasjonV2(
                env.fastlegeinformasjonv2EndpointURL,
                serviceUser.serviceuserUsername,
                serviceUser.serviceuserPassword,
            )
        FastlegeinformasjonService(operation)
    }
}
val sfsModule = module { single { SykmelderService(get()) } }

package no.nav.syfo.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.syfo.Environment
import no.nav.syfo.ServiceUser
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.helsepersonell.HelsepersonellService
import no.nav.syfo.helsepersonell.helsepersonellV1
import no.nav.syfo.helsepersonell.redis.HelsepersonellRedis
import no.nav.syfo.helsepersonell.redis.createJedisPool
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
    single {
        val env = get<Environment>()
        val serviceUser = get<ServiceUser>()
        helsepersonellV1(
            env.helsepersonellv1EndpointURL,
            serviceUser.serviceuserUsername,
            serviceUser.serviceuserPassword,
        )
    }
    single { HelsepersonellRedis(get()) }
    single { HelsepersonellService(get(), get()) }
}
val sfsModule = module { single { SykmelderService(get()) } }

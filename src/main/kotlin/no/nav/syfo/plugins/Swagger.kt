package no.nav.syfo.plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.routing

fun Application.configureSwagger() {

    routing { swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml") }
}

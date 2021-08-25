package no.nav.syfo.application.metrics

import io.ktor.application.ApplicationCall
import io.ktor.request.path
import io.ktor.util.pipeline.PipelineContext

fun monitorHttpRequests(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return {
        val label = context.request.path()
        val timer = HTTP_HISTOGRAM.labels(label).startTimer()
        proceed()
        timer.observeDuration()
    }
}

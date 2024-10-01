package no.nav.syfo.application.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "syfohelsenettproxy"

val HTTP_HISTOGRAM: Histogram =
    Histogram.Builder()
        .namespace(METRICS_NS)
        .labelNames("path")
        .name("requests_duration_seconds")
        .help("http requests durations for incoming requests in seconds")
        .register()

val AUTH_AZP_APP_ID: Counter =
    Counter.build()
        .namespace(METRICS_NS)
        .name("auth_azp_app_id")
        .labelNames("appid")
        .help("Counts the the application ID of the auth azp")
        .register()

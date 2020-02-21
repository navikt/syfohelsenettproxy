package api

import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import java.util.concurrent.TimeUnit
import no.nav.syfo.NAV_CALLID
import no.nav.syfo.callLogging
import no.nav.syfo.enforceCallId
import no.nav.syfo.setupContentNegotiation
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object CallIdLoggingSpek : Spek({

    val engine = TestApplicationEngine()
    engine.start(wait = false)
    engine.application.apply {
        callLogging()
        setupContentNegotiation()
        routing {
            enforceCallId()
            get("/test") {
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    afterGroup {
        engine.stop(0L, 0L, TimeUnit.SECONDS)
    }

    describe("CallIdLogging") {
        it("Kaster 400 om request mangler CallId") {
            with(engine.handleRequest(HttpMethod.Get, "/test")) {
                response.status() shouldEqual HttpStatusCode.BadRequest
            }
        }

        it("Slipper gjennom kall med CallId") {
            with(engine.handleRequest(HttpMethod.Get, "/test") {
                addHeader(NAV_CALLID, "callId")
            }) {
                response.status() shouldEqual HttpStatusCode.OK
            }
        }
    }
})

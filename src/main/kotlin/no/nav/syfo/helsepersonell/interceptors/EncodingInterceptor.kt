package no.nav.syfo.helsepersonell.interceptors

import kotlin.collections.set
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.message.Message

class EncodingInterceptor(phase: String) : AbstractSoapInterceptor(phase) {
    override fun handleMessage(message: SoapMessage?) {
        if (message == null) {
            return
        }

        message[Message.CONTENT_TRANSFER_ENCODING] = Charsets.UTF_8.name()
    }
}

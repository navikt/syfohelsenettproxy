package no.nav.syfo.helsepersonell.interceptors

import java.util.Collections
import java.util.TreeMap
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.common.util.UrlUtils
import org.apache.cxf.message.Message

/** Encoder SOAPAction til UTF-8 */
class ProtocolHeaderEncodingInterceptor(phase: String) : AbstractSoapInterceptor(phase) {
    override fun handleMessage(message: SoapMessage?) {
        if (message == null) {
            return
        }

        val headers = message[Message.PROTOCOL_HEADERS] as TreeMap<String, List<String>>
        var soapAction = headers["SOAPAction"] as List<String>
        val soapActionValue = soapAction[0]
        val encoded = UrlUtils.urlEncode(soapActionValue, Charsets.UTF_8.name())
        soapAction = Collections.singletonList(encoded)

        // setter oppdatert header i protocol headers
        headers.replace("SOAPAction", soapAction)
    }
}

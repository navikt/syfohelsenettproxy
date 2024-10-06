package no.nav.syfo.ws

import org.apache.cxf.Bus
import org.apache.cxf.endpoint.Client
import org.apache.cxf.feature.AbstractFeature
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy

class PortConfigurator<T> {
    var proxyConfigurator: JaxWsProxyFactoryBean.() -> Unit = {}
    var portConfigurator: T.() -> Unit = {}

    fun proxy(configurator: JaxWsProxyFactoryBean.() -> Unit) {
        proxyConfigurator = configurator
    }

    fun port(configurator: T.() -> Unit) {
        portConfigurator = configurator
    }

    fun T.withBasicAuth(username: String, password: String) = apply {
        (ClientProxy.getClient(this).conduit as HTTPConduit).apply {
            authorization.userName = username
            authorization.password = password
        }
    }
}

inline fun <reified T> createPort(
    endpoint: String,
    extraConfiguration: PortConfigurator<T>.() -> Unit = {}
): T =
    PortConfigurator<T>().let { configurator ->
        extraConfiguration(configurator)
        (JaxWsProxyFactoryBean()
                .apply {
                    address = endpoint
                    serviceClass = T::class.java
                    configurator.proxyConfigurator(this)
                }
                .create() as T)
            .apply { configurator.portConfigurator(this) }
    }

internal class TimeoutFeature(private val timeout: Long) : AbstractFeature() { 
  
     override fun initialize(client: Client, bus: Bus) { 
         val conduit = client.conduit 
         if (conduit is HTTPConduit) { 
             val policy = HTTPClientPolicy().apply { receiveTimeout = this@TimeoutFeature.timeout } 
             conduit.client = policy 
         } 
  
         super.initialize(client, bus) 
     } 
 }
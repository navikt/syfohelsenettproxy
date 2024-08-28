package no.nav.syfo.ws

import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.transport.http.HTTPConduit

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

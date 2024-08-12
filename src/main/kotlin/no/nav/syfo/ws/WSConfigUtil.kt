package no.nav.syfo.ws

import org.apache.cxf.Bus
import org.apache.cxf.binding.soap.Soap12
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.endpoint.Client
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.ws.policy.PolicyBuilder
import org.apache.cxf.ws.policy.PolicyEngine
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver
import org.apache.cxf.ws.security.SecurityConstants
import org.apache.cxf.ws.security.trust.STSClient

class PortConfigurator<T> {
    var proxyConfigurator: JaxWsProxyFactoryBean.() -> Unit = {}
    var portConfigurator: T.() -> Unit = {}

    fun proxy(configurator: JaxWsProxyFactoryBean.() -> Unit) {
        proxyConfigurator = configurator
    }

    fun port(configurator: T.() -> Unit) {
        portConfigurator = configurator
    }

    fun T.withSTS(username: String, password: String, endpoint: String) = apply {
        val client = ClientProxy.getClient(this)
        client.requestContext[SecurityConstants.STS_CLIENT] =
            createSystemUserSTSClient(client, username, password, endpoint, true)
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

var STS_CLIENT_AUTHENTICATION_POLICY = "classpath:sts/policies/untPolicy.xml"
var STS_REQUEST_SAML_POLICY = "classpath:sts/policies/requestSamlPolicy.xml"

fun createSystemUserSTSClient(
    client: Client,
    username: String,
    password: String,
    loc: String,
    cacheTokenInEndpoint: Boolean
): STSClient =
    STSClientWSTrust13And14(client.bus).apply {
        location = loc
        properties =
            mapOf(SecurityConstants.USERNAME to username, SecurityConstants.PASSWORD to password)

        isEnableAppliesTo = false
        isAllowRenewing = false
        setPolicy(STS_CLIENT_AUTHENTICATION_POLICY)

        requestContext[SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT] = cacheTokenInEndpoint

        val policy =
            RemoteReferenceResolver("", client.bus.getExtension(PolicyBuilder::class.java))
                .resolveReference(STS_REQUEST_SAML_POLICY)

        val endpointInfo = client.endpoint.endpointInfo
        val policyEngine = client.bus.getExtension(PolicyEngine::class.java)
        val soapMessage = SoapMessage(Soap12.getInstance())
        val endpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, null, soapMessage)
        policyEngine.setClientEndpointPolicy(
            endpointInfo,
            endpointPolicy.updatePolicy(policy, soapMessage)
        )
    }

class STSClientWSTrust13And14(b: Bus?) : STSClient(b) {
    override fun useSecondaryParameters(): Boolean = false
}

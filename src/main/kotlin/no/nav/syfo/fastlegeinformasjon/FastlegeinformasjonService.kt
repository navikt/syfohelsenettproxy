package no.nav.syfo.fastlegeinformasjon

import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName
import javax.xml.ws.soap.SOAPFaultException
import no.nav.syfo.logger
import no.nav.syfo.ws.TimeoutFeature
import no.nav.syfo.ws.createPort
import no.nhn.register.common2.ArrayOfCode
import no.nhn.register.common2.Code
import no.nhn.schemas.reg.flr.ContractsQueryParameters
import no.nhn.schemas.reg.flr.IFlrExportOperations
import no.nhn.schemas.reg.flr.IFlrExportOperationsExportGPContractsGenericFaultFaultFaultMessage
import no.nhn.schemas.reg.flr.ObjectFactory
import org.apache.cxf.ws.addressing.WSAddressingFeature

class FastlegeinformasjonService(
    private val fastlegeInformsjonOperations: IFlrExportOperations,
) {

    fun hentFastlegeinformasjonExport(kommuneNr: String): ByteArray {

        val contractsQueryParameters: ContractsQueryParameters =
            createContractsQueryParameters(
                kommuneNr = kommuneNr,
            )

        return try {
            fastlegeInformsjonOperations.exportGPContracts(contractsQueryParameters)
        } catch (e: IFlrExportOperationsExportGPContractsGenericFaultFaultFaultMessage) {
            logger.error("Helsenett gir ein generisk feilmelding: {}", e.message)
            throw FastlegeinformasjonException(message = e.message, cause = e.cause)
        } catch (e: SOAPFaultException) {
            logger.error("Helsenett gir feilmelding: {}", e.message)
            throw FastlegeinformasjonException(message = e.message, cause = e.cause)
        } catch (e: Exception) {
            logger.error(
                "Generel feil oppstod i hentet exportGPContracts feilmelding: {}",
                e.message,
            )
            throw FastlegeinformasjonException(message = e.message, cause = e.cause)
        }
    }

    private fun createContractsQueryParameters(kommuneNr: String): ContractsQueryParameters {

        val kode = Code()
        val codeValueJax: JAXBElement<String> =
            JAXBElement(
                QName("http://register.nhn.no/Common", "CodeValue"),
                String::class.java,
                "4625",
            )
        val simpleTypeJax: JAXBElement<String> =
            JAXBElement(
                QName("http://register.nhn.no/Common", "SimpleType"),
                String::class.java,
                "kommune",
            )

        val codeTextJax: JAXBElement<String> =
            JAXBElement(
                QName("http://register.nhn.no/Common", "CodeText"),
                String::class.java,
                "Austevoll",
            )

        kode.codeValue = codeValueJax
        kode.simpleType = simpleTypeJax
        kode.codeText = codeTextJax
        kode.isActive = true
        kode.oid = 3402

        val arrayOfCode = ArrayOfCode()
        arrayOfCode.code.add(kode)

        val objectFactory = ObjectFactory()
        val arrayOfCodeJAXBElement =
            objectFactory.createContractsQueryParametersMunicipalities(arrayOfCode)

        val contractsQueryParameters = ContractsQueryParameters()
        contractsQueryParameters.isGetFullPersonInfo = false
        contractsQueryParameters.isGetHistoricalData = false
        contractsQueryParameters.municipalities = arrayOfCodeJAXBElement

        return contractsQueryParameters
    }
}

fun fastlegeinformasjonV2(
    endpointUrl: String,
    serviceuserUsername: String,
    serviceuserPassword: String
) =
    createPort<IFlrExportOperations>(endpointUrl) {
        proxy {
            features.add(WSAddressingFeature())
            features.add(TimeoutFeature(1000 * 60 * 10))
        }
        port { withBasicAuth(serviceuserUsername, serviceuserPassword) }
    }

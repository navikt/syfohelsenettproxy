package no.nav.syfo.fastlegeinformasjon

import javax.xml.ws.soap.SOAPFaultException
import no.nav.syfo.logger
import no.nav.syfo.ws.TimeoutFeature
import no.nav.syfo.ws.createPort
import no.nhn.register.common2.ArrayOfCode
import no.nhn.register.common2.Code
import no.nhn.schemas.reg.flr.ContractsQueryParameters
import no.nhn.schemas.reg.flr.IFlrExportOperations
import no.nhn.schemas.reg.flr.IFlrExportOperationsExportGPContractsGenericFaultFaultFaultMessage
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
        kode.codeValue = kommuneNr
        kode.simpleType = "kommune"

        val arrayOfCode = ArrayOfCode()
        arrayOfCode.code.add(kode)

        val contractsQueryParameters = ContractsQueryParameters()
        contractsQueryParameters.isGetFullPersonInfo = false
        contractsQueryParameters.isGetHistoricalData = false
        contractsQueryParameters.municipalities = arrayOfCode

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

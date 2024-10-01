package no.nav.syfo.fastlegeinformasjon

import javax.xml.ws.soap.SOAPFaultException
import no.nav.syfo.logger
import no.nav.syfo.securelog
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

    fun hentFastlegeinformasjon(kommuneNr: String): ExportGPContracts {

        val contractsQueryParameters: ContractsQueryParameters =
            createContractsQueryParameters(
                kommuneNr = kommuneNr,
            )

        return try {
            fastlegeInformsjonOperations
                .exportGPContracts(contractsQueryParameters)
                .let { ws2ExportGPContracts(it) }
                .also {
                    logger.info("Hentet fastlegeinformasjon for kommunenr: $kommuneNr")
                    securelog.info("Hentet fastlegeinformasjon for kommunenr object: $it")
                }
        } catch (e: IFlrExportOperationsExportGPContractsGenericFaultFaultFaultMessage) {
            logger.error("Helsenett gir ein generisk feilmelding: {}", e.message)
            throw FastlegeinformasjonException(message = e.message, cause = e.cause)
        } catch (e: SOAPFaultException) {
            logger.error("Helsenett gir feilmelding: {}", e.message)
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

    private fun ws2ExportGPContracts(exportGPContractsResponse: ByteArray): ExportGPContracts =
        ExportGPContracts(
            exportGPContractsResult = exportGPContractsResponse,
        )
}

data class ExportGPContracts(val exportGPContractsResult: ByteArray)

fun fastlegeinformasjonV2(
    endpointUrl: String,
    serviceuserUsername: String,
    serviceuserPassword: String
) =
    createPort<IFlrExportOperations>(endpointUrl) {
        proxy { features.add(WSAddressingFeature()) }

        port { withBasicAuth(serviceuserUsername, serviceuserPassword) }
    }

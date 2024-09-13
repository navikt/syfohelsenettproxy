package no.nav.syfo.fastlegeinformasjon

import javax.xml.ws.soap.SOAPFaultException
import no.nav.syfo.logger
import no.nav.syfo.securelog
import no.nav.syfo.ws.createPort
import no.nhn.register.common2.ArrayOfCode
import no.nhn.register.common2.Code
import no.nhn.schemas.reg.flr.ContractsQueryParameters
import no.nhn.schemas.reg.flr.FlrExportService
import no.nhn.schemas.reg.flr.IFlrExportOperations
import no.nhn.schemas.reg.flr.IFlrExportOperationsExportGPContractsGenericFaultFaultFaultMessage
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase

class FastlegeinformasjonService(
    private val fastlegeInformsjonOperations: IFlrExportOperations,
) {

    fun hentFastlegeinformasjon(kommuneNr: String): ExportGPContracts {

        val contractsQueryParameters: ContractsQueryParameters =
            createContractsQueryParameters(
                historicalData = false,
                fullPersonInfo = false,
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

    private fun createContractsQueryParameters(
        historicalData: Boolean,
        fullPersonInfo: Boolean,
        kommuneNr: String
    ): ContractsQueryParameters {

        val kode = Code()
        kode.codeValue = kommuneNr
        kode.simpleType = "kommune"

        val arrayOfCode = ArrayOfCode()
        arrayOfCode.code.add(kode)

        val contractsQueryParameters = ContractsQueryParameters()
        contractsQueryParameters.isGetFullPersonInfo = historicalData
        contractsQueryParameters.isGetHistoricalData = fullPersonInfo
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
    createPort<FlrExportService>(endpointUrl) {
        proxy {
            // TODO: Contact someone about this hacky workaround
            // talk to HDIR about HPR about they claim to send a ISO-8859-1 but its really UTF-8
            // payload
            val interceptor =
                object : AbstractSoapInterceptor(Phase.RECEIVE) {
                    override fun handleMessage(message: SoapMessage?) {
                        if (message != null) {
                            message[Message.ENCODING] = "utf-8"
                        }
                    }
                }
            inInterceptors.add(interceptor)
            inFaultInterceptors.add(interceptor)
        }

        port { withBasicAuth(serviceuserUsername, serviceuserPassword) }
    }

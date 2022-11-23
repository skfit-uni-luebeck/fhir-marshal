package de.uksh.medic.fhirmarshal.interception

import ca.uhn.fhir.interceptor.api.Hook
import ca.uhn.fhir.interceptor.api.Interceptor
import ca.uhn.fhir.interceptor.api.Pointcut
import ca.uhn.fhir.validation.ValidationResult
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.utils.FHIRPathEngine
import java.lang.Exception

@Interceptor
class LocationFhirPathEvaluationInterceptor {

    companion object{
        //TODO: SimpleWorkerContext or HapiWorkerContext?
        private val fhirPathEngine = FHIRPathEngine(SimpleWorkerContext())

        private val logger: Logger = LogManager.getLogger(this::class)
    }

    @Hook(value = Pointcut.VALIDATION_COMPLETED)
    fun invoke(parsedResource: IBaseResource, rawResource: String?, validationResult: ValidationResult): EvaluatedValidationResult {
        val parser = validationResult.context.newJsonParser()
        val evaluatedMessages = MutableList(validationResult.messages.size) { EvaluatedValidationResult.EvaluatedValidationMessage() }
        if (parsedResource is Base) {
            validationResult.messages.forEachIndexed { idx, validationMessage ->
                //Try-Catch-Finally to avoid missing elements in new list due to exceptions occurring
                try {
                    //Assumed to be suitable candidate for FHIR Path expression
                    val fhirPathExpr = validationMessage.locationString
                    val results = fhirPathEngine.evaluate(parsedResource, fhirPathExpr)
                    results.forEach { result ->
                        val resource = when (result) {
                            is BaseResource -> result
                            is Type -> Container().apply{ setElement(result) }
                            else -> {
                                logger.warn("Couldn't parse result with type ${result.javaClass.simpleName} and path $fhirPathExpr")
                                null
                            }
                        }
                        if (resource != null) {
                            val encodedResource = parser.encodeResourceToString(resource as IBaseResource)
                            evaluatedMessages[idx].locationElements.add(encodedResource)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn(e.stackTraceToString())
                } finally {
                    evaluatedMessages[idx].apply {
                        locationCol = validationMessage.locationCol
                        locationLine = validationMessage.locationLine
                        message = validationMessage.message
                        severity = validationMessage.severity
                    }
                }
            }
        } else {
            logger.warn(
                "Resource with ID ${parsedResource.idElement} and meta data ${parsedResource.meta} could not be " +
                        "parsed to ${Base::class.qualifiedName} and thus no FHIR Path evaluation was performed"
            )
        }
        return EvaluatedValidationResult(validationResult.context, evaluatedMessages)
    }

}

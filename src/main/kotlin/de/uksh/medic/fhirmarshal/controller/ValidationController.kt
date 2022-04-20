package de.uksh.medic.fhirmarshal.controller

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import de.uksh.medic.fhirmarshal.services.ValidationService
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CodeSystem
import org.hl7.fhir.r4.model.OperationOutcome
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

private val logger: Logger = LoggerFactory.getLogger(ValidationController::class.java)

@RequestMapping("/validate")
@Controller
class ValidationController(
    @Autowired val validationService: ValidationService,
    @Autowired val fhirContext: FhirContext
) {

    @GetMapping
    fun getValidation(): OperationOutcome {
        // TODO: 20/04/22
        val resource = CodeSystem()
        return validationService.validateResource(resource)
    }

    /**
     * receives a message entity and carries out validation, the main entry point for the HTTP API
     * This also receives NDJSON, this might be more suited to batch validation with separate routes
     */
    @PostMapping(consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    fun postValidation(
        requestEntity: RequestEntity<String>
    ) {
        logger.debug(requestEntity.toString())
        val iBaseResource = consumeTextBody(requestEntity)
        // TODO: 20/04/22
    }

    fun consumeTextBody(requestEntity: RequestEntity<String>): IBaseResource? {
        val parsers = listOf(
            fhirContext.newJsonParser() to "json",
            fhirContext.newXmlParser() to "xml",
            fhirContext.newNDJsonParser() to "ndjson"
        )
        for ((parser, parserFormat) in parsers) {
            try {
                return parser.parseResource(requestEntity.body)
            } catch (e: DataFormatException) {
                logger.debug("Data is not in $parserFormat")
            }
        }
        logger.warn("No parser was able to handle resource; the HTTP headers were: ${requestEntity.headers}")
        return null
    }

}
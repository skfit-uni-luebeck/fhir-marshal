package de.uksh.medic.fhirmarshal.controller

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.parser.IParser
import de.uksh.medic.fhirmarshal.services.ValidationService
import org.hl7.fhir.instance.model.api.IBaseResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.ResponseStatusException

private val logger: Logger = LoggerFactory.getLogger(ValidationController::class.java)

@RequestMapping("/validate")
@Controller
class ValidationController(
    @Autowired val validationService: ValidationService,
    @Autowired val fhirContext: FhirContext
) {

    /**
     * receives a message entity and carries out validation, the main entry point for the HTTP API
     * This also receives NDJSON, this might be more suited to batch validation with separate routes
     */
    @PostMapping(consumes = ["application/json", "application/fhir+json", "application/xml", "application/fhir+xml", "application/fhir+ndjson", "application/ndjson"])
    @ResponseBody
    fun postValidation(requestEntity: RequestEntity<String>): String {
        val (iBaseResource, parser) = consumeTextBody(requestEntity) ?: (null to null)
        if (iBaseResource != null && parser != null) {
            val out = validationService.validateResource(iBaseResource)
            return parser.setPrettyPrint(true).encodeResourceToString(out).also {
                logger.info("${iBaseResource.fhirType()} from ${requestEntity.headers}, validated: ${out.issue.size} issues")
            }
        } else {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No parser was able to handle resource; the HTTP headers were: ${requestEntity.headers}"
            )
        }
    }

    fun consumeTextBody(requestEntity: RequestEntity<String>): Pair<IBaseResource, IParser>? {
        val parsers = listOf(
            fhirContext.newJsonParser() to "json",
            fhirContext.newXmlParser() to "xml",
            fhirContext.newNDJsonParser() to "ndjson"
        )
        for ((parser, parserFormat) in parsers) {
            try {
                return parser.parseResource(requestEntity.body) to parser
            } catch (e: DataFormatException) {
                logger.debug("Data is not in $parserFormat")
            }
        }
        logger.warn("No parser was able to handle resource; the HTTP headers were: ${requestEntity.headers}")
        return null
    }

}
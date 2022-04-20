package de.uksh.medic.fhirmarshal.services

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.OperationOutcome
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val logger: Logger = LoggerFactory.getLogger(ValidationService::class.java)

@Service
class ValidationService(
    @Autowired val fhirContext: FhirContext,
) {

    fun validateResource(
        resource: IBaseResource
    ) : OperationOutcome {
        TODO()
    }

}
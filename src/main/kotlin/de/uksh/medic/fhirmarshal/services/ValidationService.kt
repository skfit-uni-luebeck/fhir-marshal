package de.uksh.medic.fhirmarshal.services

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.validation.FhirValidator
import de.uksh.medic.fhirmarshal.AppProperties
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.OperationOutcome
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val logger: Logger = LoggerFactory.getLogger(ValidationService::class.java)

@Service
class ValidationService(val fhirValidator: FhirValidator) {

    fun validateResource(resource: IBaseResource) : OperationOutcome {
        return fhirValidator.validateWithResult(resource).toOperationOutcome() as OperationOutcome
    }
}
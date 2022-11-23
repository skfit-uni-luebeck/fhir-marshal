package de.uksh.medic.fhirmarshal

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.validation.FhirValidator
import de.uksh.medic.fhirmarshal.interception.InterceptorService
import de.uksh.medic.fhirmarshal.interception.LocationFhirPathEvaluationInterceptor
import de.uksh.medic.fhirmarshal.services.ValidationSupportConfiguration
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties
class FhirMarshal {

    @Bean
    fun appLog(): Logger = LoggerFactory.getLogger(FhirMarshal::class.java)

    @Bean
    fun fhirContext() : FhirContext = FhirContext.forR4()

    @Bean
    fun fhirValidator(validationSupportConfiguration: ValidationSupportConfiguration, fhirContext: FhirContext): FhirValidator {
        val validator: FhirValidator = fhirContext.newValidator()
        val instanceValidator = FhirInstanceValidator(fhirContext)
        //Add interceptor service for evaluation fhir path expression in location elements of validation result
        val interceptorService = InterceptorService()
        interceptorService.registerInterceptor(LocationFhirPathEvaluationInterceptor())
        validator.setInterceptorBroadcaster(interceptorService)
        instanceValidator.validationSupport = validationSupportConfiguration.configureChain()
        //TODO: Check if concurrent bundle validation improves performance and whether (due to this option) disabled
        // bundle structure validation is problematic
        validator.isConcurrentBundleValidation = true
        return validator.registerValidatorModule(instanceValidator)
    }
}

fun main(args: Array<String>) {
    runApplication<FhirMarshal>(*args)
}

package de.uksh.medic.fhirmarshal.services

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import de.uksh.medic.fhirmarshal.AppProperties
import org.hl7.fhir.common.hapi.validation.support.*
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CapabilityStatement
import org.hl7.fhir.r4.model.StructureDefinition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val logger: Logger = LoggerFactory.getLogger(ValidationSupportConfiguration::class.java)

@Component
class ValidationSupportConfiguration(
    @Autowired private val fhirContext: FhirContext,
    @Autowired private val properties: AppProperties
) {

    init {
        validateServerIsFhir(properties.remoteTerminologyServer, "terminology")
        validateServerIsFhir(properties.remoteStructureServer, "structure")
    }

    fun configureChain() = ValidationSupportChain().apply {
        this.addValidationSupport(DefaultProfileValidationSupport(this@ValidationSupportConfiguration.fhirContext))
        this.addValidationSupport(CommonCodeSystemsTerminologyService(this@ValidationSupportConfiguration.fhirContext))
        this.addValidationSupport(InMemoryTerminologyServerValidationSupport(this@ValidationSupportConfiguration.fhirContext))
        this.addValidationSupport(
            RemoteTerminologyServiceValidationSupport(
                this@ValidationSupportConfiguration.fhirContext,
                properties.remoteTerminologyServer
            )
        )
        this.addValidationSupport(SnapshotGeneratingValidationSupport(this@ValidationSupportConfiguration.fhirContext))
        val prePopulatedValidationSupport =
            PrePopulatedValidationSupport(this@ValidationSupportConfiguration.fhirContext)
        retrieveStructureDefinitions().forEach {
            prePopulatedValidationSupport.addStructureDefinition(it)
        }
        this.addValidationSupport(prePopulatedValidationSupport)
    }.let { chain ->
        CachingValidationSupport(chain, CachingValidationSupport.CacheTimeouts.defaultValues().apply {
            // TODO: 21/04/22 [JW] maybe tweak the timeout millis here
        })
    }

    private fun validateServerIsFhir(serverUrl: String, role: String) {
        val client = fhirContext.newRestfulGenericClient(serverUrl)
        try {
            val capabilities = client.capabilities().ofType(CapabilityStatement::class.java).execute()
            logger.info("Connected to $serverUrl; FHIR ${capabilities.fhirVersion}; Software ${capabilities.software.name} - ${capabilities.software.version}")
        } catch (e: Exception) {
            throw ConfigurationError("could not connect to $role at $serverUrl", e)
        }
    }

    private fun retrieveStructureDefinitions(): List<StructureDefinition> {
        val client = fhirContext.newRestfulGenericClient(properties.remoteStructureServer)

        val result: Bundle = client
            .search<IBaseBundle>()
            .forResource(StructureDefinition::class.java)
            .where(StructureDefinition.STATUS.exactly().code("active"))
            .returnBundle<Bundle>(Bundle::class.java)
            .execute()

        return result.entry.map { it.resource as StructureDefinition }.toMutableList()
    }
}

class ConfigurationError(message: String, cause: Throwable) : Exception(message, cause)
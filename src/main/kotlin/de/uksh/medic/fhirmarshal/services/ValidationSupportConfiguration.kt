package de.uksh.medic.fhirmarshal.services

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import de.uksh.medic.fhirmarshal.AppProperties
import org.hl7.fhir.common.hapi.validation.support.*
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CapabilityStatement
import org.hl7.fhir.r4.model.StructureDefinition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

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
                properties.remoteTerminologyServer.toString()
            )
        )
        this.addValidationSupport(SnapshotGeneratingValidationSupport(this@ValidationSupportConfiguration.fhirContext))
        val prePopulatedValidationSupport =
            PrePopulatedValidationSupport(this@ValidationSupportConfiguration.fhirContext)
        retrieveStructureDefinitions().forEach {
            prePopulatedValidationSupport.addStructureDefinition(it)
            /* TODO: 2022-08-29 [JW] We are currently keeping all the StructureDefinions in-memory.
                Maybe externalize to a temp directory?
                This would require a custom implementation of IValidationSupport that uses Files.createTempDirectory
             */
        }
        this.addValidationSupport(prePopulatedValidationSupport)
    }.let { chain ->
        CachingValidationSupport(chain)
    }

    private fun validateServerIsFhir(serverUrl: URI, role: String) {
        val client = fhirContext.newRestfulGenericClient(serverUrl.toString())
        try {
            val capabilities = client.capabilities().ofType(CapabilityStatement::class.java).execute()
            logger.info("Connected to $serverUrl; FHIR ${capabilities.fhirVersion}; Software '${capabilities.software.name} - ${capabilities.software.version}'")
        } catch (e: Exception) {
            throw ConfigurationError("could not connect to $role at $serverUrl", e)
        }
    }

    /**
     * we are not using the HAPI FHIR client, since Firely Server handles paging differently to HAPI FHIR.
     * By following the urls manually, and parsing using JsonParser, this code works on HAPI-FHIR-JPA and Firely Server.
     */
    private fun retrieveStructureDefinitions(): List<StructureDefinition> {
        val structureDefinitions = mutableListOf<StructureDefinition>()
        val startUri = UriComponentsBuilder.fromUri(properties.remoteStructureServer).apply {
            pathSegment("StructureDefinition")
            queryParam("_count", properties.retrievalPageSize)
            queryParam("_total", "accurate") // this might be ignored by some servers
            queryParam("status", "active")
        }.build().toUri()
        logger.info("Starting StructureDefinition retrieval (page size=${properties.retrievalPageSize}) at: $startUri")
        var currentUri: URI? = startUri
        val template = RestTemplate()
        var pageCount = 0
        while (currentUri != null) {
            val result = template.getForEntity(currentUri, String::class.java).let { entity ->
                fhirContext.newJsonParser().parseResource(Bundle::class.java, entity.body)
            }
            logger.info("Retrieved page ${++pageCount} of StructureDefinitions with ${result.entry.size} entries from $currentUri")
            if (pageCount == 0) logger.info("Server reported total=${result.total}")
            structureDefinitions.addAll(result.entry.mapNotNull { it.resource as? StructureDefinition })
            currentUri = result.getLink("next")?.let { nextLink ->
                URI.create(nextLink.url)
            } ?: null.also {
                logger.debug("Finished bundle retrieval from ${properties.remoteStructureServer}")
            }
        }

        return structureDefinitions
    }
}

class ConfigurationError(message: String, cause: Throwable) : Exception(message, cause)
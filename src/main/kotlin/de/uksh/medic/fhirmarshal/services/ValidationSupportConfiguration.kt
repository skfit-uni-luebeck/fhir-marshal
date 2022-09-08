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
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger(ValidationSupportConfiguration::class.java)

@Component
class ValidationSupportConfiguration(
    @Autowired private val fhirContext: FhirContext,
    @Autowired private val properties: AppProperties
) {

    init {
        validateServerIsFhir(properties.remoteTerminologyServers, "terminology")
        validateServerIsFhir(properties.remoteStructureServers, "structure")
    }

    fun configureChain() = ValidationSupportChain().apply {
        this.addValidationSupport(DefaultProfileValidationSupport(this@ValidationSupportConfiguration.fhirContext))
        this.addValidationSupport(CommonCodeSystemsTerminologyService(this@ValidationSupportConfiguration.fhirContext))
        this.addValidationSupport(InMemoryTerminologyServerValidationSupport(this@ValidationSupportConfiguration.fhirContext))
        properties.remoteTerminologyServers.forEach { (name, settings) ->
            logger.info("Added terminology support via $name at ${settings.url}")
            this.addValidationSupport(settings.configureValidationSupport(fhirContext))
        }

        this.addValidationSupport(SnapshotGeneratingValidationSupport(this@ValidationSupportConfiguration.fhirContext))
        val prePopulatedValidationSupport =
            PrePopulatedValidationSupport(this@ValidationSupportConfiguration.fhirContext)
        retrieveStructureDefinitions().forEach {
            prePopulatedValidationSupport.addStructureDefinition(it)
            logger.debug("Registered ${it.url}")
            /* TODO: 2022-08-29 [JW] We are currently keeping all the StructureDefinions in-memory.
                Maybe externalize to a temp directory?
                This would require a custom implementation of IValidationSupport that uses Files.createTempDirectory
             */
        }
        this.addValidationSupport(prePopulatedValidationSupport)
    }.let { chain ->
        CachingValidationSupport(chain)
    }

    private fun validateServerIsFhir(servers: Map<String, AppProperties.ServerSettings>, role: String) {
        servers.forEach { (name, settings) ->
            val client = settings.configureHapiClient(fhirContext)
            try {
                val capabilities = client.capabilities().ofType(CapabilityStatement::class.java).execute()
                logger.info("Connected to $role server '$name' at ${settings.url}; FHIR ${capabilities.fhirVersion}; Software '${capabilities.software.name} - ${capabilities.software.version}'")
            } catch (e: Exception) {
                throw ConfigurationError("could not connect to $role server '$name' at ${settings.url}", e)
            }
        }

    }

    /**
     * we are not using the HAPI FHIR client, since Firely Server handles paging differently to HAPI FHIR.
     * By following the urls manually, and parsing using JsonParser, this code works on HAPI-FHIR-JPA and Firely Server.
     */
    private fun retrieveStructureDefinitions(): List<StructureDefinition> =
        properties.remoteStructureServers.flatMap { (name, settings) ->
            retrieveStructureDefinitions(name, settings)
        }.also {
            logger.info("Finished structure definition retrieval with ${it.size} structure definitions from ${properties.remoteStructureServers.size} servers")
        }

    private fun retrieveStructureDefinitions(
        serverName: String,
        server: AppProperties.ServerSettings
    ): MutableList<StructureDefinition> {
        val structureDefinitions = mutableListOf<StructureDefinition>()
        val pageSize = server.overridePageSize ?: properties.retrievalPageSize
        val startUri = UriComponentsBuilder.fromUri(server.url).apply {
            pathSegment("StructureDefinition")
            queryParam("_count", pageSize)
            queryParam("_total", "accurate") // this might be ignored by some servers
            if (server.overrideRetrieveOnlyActiveProfiles ?: properties.retrieveOnlyActiveProfiles) {
                queryParam("status", "active")
            }
        }.build().toUri()
        logger.info("Starting StructureDefinition retrieval from $serverName (page size=$pageSize) at: $startUri")
        var currentUri: URI? = startUri
        val template = server.configureRestTemplate()
        var pageCount = 0
        while (currentUri != null) {
            val result = template.getForEntity(currentUri, String::class.java).let { entity ->
                fhirContext.newJsonParser().parseResource(Bundle::class.java, entity.body)
            }
            logger.debug("Retrieved page ${++pageCount} of StructureDefinitions with ${result.entry.size} entries from $currentUri")
            if (pageCount == 0) logger.debug("Server reported total=${result.total}")
            structureDefinitions.addAll(result.entry.mapNotNull { it.resource as? StructureDefinition })
            currentUri = result.getLink("next")?.let { nextLink ->
                URI.create(nextLink.url)
            } ?: null.also {
                logger.info("Finished bundle retrieval from $serverName, got ${structureDefinitions.size} resources")
            }
        }
        return structureDefinitions
    }
}

class ConfigurationError(message: String, cause: Throwable) : Exception(message, cause)
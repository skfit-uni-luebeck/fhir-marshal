package de.uksh.medic.fhirmarshal

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.URI

@ConfigurationProperties(prefix = "fhir")
@ConstructorBinding
data class AppProperties(
    val remoteStructureServer: URI,
    val remoteTerminologyServer: URI,
    val retrievalPageSize: Int = 3
)
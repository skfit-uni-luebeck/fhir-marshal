package de.uksh.medic.fhirmarshal

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "fhir")
@ConstructorBinding
data class AppProperties(
    val remoteStructureServer: String,
    val remoteTerminologyServer: String
)
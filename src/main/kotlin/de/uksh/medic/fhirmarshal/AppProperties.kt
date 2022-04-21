package de.uksh.medic.fhirmarshal

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties("fhir")
@Configuration
@EnableConfigurationProperties
class AppProperties {

    var remoteStructureServer: String? = null
    var remoteTerminologyServer: String? = null

}
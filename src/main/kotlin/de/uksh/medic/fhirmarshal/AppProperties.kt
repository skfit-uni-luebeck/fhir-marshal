package de.uksh.medic.fhirmarshal

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.http.client.support.BasicAuthenticationInterceptor
import org.springframework.web.client.RestTemplate
import java.net.URI

@ConfigurationProperties(prefix = "fhir")
@ConstructorBinding
data class AppProperties(
    val remoteStructureServers: Map<String, ServerSettings>,
    val remoteTerminologyServers: Map<String, ServerSettings>,
    val retrievalPageSize: Int = 3
) {
    data class ServerSettings(
        val url: URI, val authUser: String? = null, val authPassword: String? = null, val overridePageSize: Int? = null
    ) {
        fun configureRestTemplate() = RestTemplate().apply {
            if (authUser != null && authPassword != null) {
                interceptors.add(BasicAuthenticationInterceptor(authUser, authPassword))
            }
        }

        fun configureHapiClient(fhirContext: FhirContext) = fhirContext.newRestfulGenericClient(
            url.toString()
        ).apply {
            if (authUser != null && authPassword != null) {
                this.registerInterceptor(BasicAuthInterceptor(authUser, authPassword))
            }
        }

        fun configureValidationSupport(fhirContext: FhirContext) =
            RemoteTerminologyServiceValidationSupport(fhirContext, url.toString()).apply {
                if (authUser != null && authPassword != null) {
                    this.addClientInterceptor(BasicAuthInterceptor(authUser, authPassword))
                }
            }
    }
}
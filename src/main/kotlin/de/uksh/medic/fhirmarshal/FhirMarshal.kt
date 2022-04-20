package de.uksh.medic.fhirmarshal

import ca.uhn.fhir.context.FhirContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class FhirMarshal {

    @Bean
    fun appLog(): Logger = LoggerFactory.getLogger(FhirMarshal::class.java)

    @Bean
    fun fhirContext() : FhirContext = FhirContext.forR4()
}

fun main(args: Array<String>) {
    runApplication<FhirMarshal>(*args)
}

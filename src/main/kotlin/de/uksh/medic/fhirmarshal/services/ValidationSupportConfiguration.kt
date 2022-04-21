package de.uksh.medic.fhirmarshal.services

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import de.uksh.medic.fhirmarshal.AppProperties
import org.hl7.fhir.common.hapi.validation.support.*
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.StructureDefinition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ValidationSupportConfiguration(@Autowired private val fhirContext: FhirContext, @Autowired private val properties: AppProperties) {

   fun configureChain() = ValidationSupportChain().apply {
       this.addValidationSupport(DefaultProfileValidationSupport(this@ValidationSupportConfiguration.fhirContext))
       this.addValidationSupport(CommonCodeSystemsTerminologyService(this@ValidationSupportConfiguration.fhirContext))
       this.addValidationSupport(InMemoryTerminologyServerValidationSupport(this@ValidationSupportConfiguration.fhirContext))
       this.addValidationSupport(RemoteTerminologyServiceValidationSupport(this@ValidationSupportConfiguration.fhirContext, properties.remoteTerminologyServer))
       this.addValidationSupport(SnapshotGeneratingValidationSupport(this@ValidationSupportConfiguration.fhirContext))

       // TODO: fail early if the stucture server is not available
       val prePopulatedValidationSupport = PrePopulatedValidationSupport(this@ValidationSupportConfiguration.fhirContext)
       retrieveStructureDefinitions().forEach{
           prePopulatedValidationSupport.addStructureDefinition(it)
       }
       this.addValidationSupport(prePopulatedValidationSupport)
   }

   private fun retrieveStructureDefinitions(): List<StructureDefinition> {
       val client = fhirContext.newRestfulGenericClient(properties.remoteStructureServer)

       val result: Bundle = client
           .search<IBaseBundle>()
           .forResource(StructureDefinition::class.java)
           .where(StructureDefinition.STATUS.exactly().code("active"))
           .returnBundle<Bundle>(Bundle::class.java)
           .execute()

       val bundle = result as Bundle
       return bundle.entry.map { it.resource as StructureDefinition }.toMutableList()
   }
}
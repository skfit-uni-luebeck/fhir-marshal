package de.uksh.medic.fhirmarshal.interception.filter

import ca.uhn.fhir.validation.ValidationResult

fun interface FhirPathFilter {

    fun invoke(fhirPathExpression: String, results: List<ValidationResult>): List<ValidationResult>

}
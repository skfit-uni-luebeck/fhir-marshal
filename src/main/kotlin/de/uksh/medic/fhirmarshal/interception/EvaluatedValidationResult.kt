package de.uksh.medic.fhirmarshal.interception

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.Constants
import ca.uhn.fhir.util.OperationOutcomeUtil
import ca.uhn.fhir.validation.SingleValidationMessage
import ca.uhn.fhir.validation.ValidationResult
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome
import org.hl7.fhir.r4.model.OperationOutcome

class EvaluatedValidationResult constructor(ctx: FhirContext, messages: List<EvaluatedValidationMessage>): ValidationResult(ctx , messages) {

    override fun populateOperationOutcome(theOperationOutcome: IBaseOperationOutcome?) {
        for (next in messages) {
            val location: String? = if (StringUtils.isNotBlank(next.locationString)) {
                next.locationString
            } else if (next.locationLine != null || next.locationCol != null) {
                "Line[" + next.locationLine + "] Col[" + next.locationCol + "]"
            } else {
                null
            }
            val severity = if (next.severity != null) next.severity.code else null
            val issue = OperationOutcomeUtil.addIssue(
                context,
                theOperationOutcome,
                severity,
                next.message,
                location,
                Constants.OO_INFOSTATUS_PROCESSING
            )
            if (next.locationLine != null || next.locationCol != null) {
                val unknown = "(unknown)"
                var line = unknown
                if (next.locationLine != null && next.locationLine != -1) {
                    line = next.locationLine.toString()
                }
                var col = unknown
                if (next.locationCol != null && next.locationCol != -1) {
                    col = next.locationCol.toString()
                }
                if (unknown != line || unknown != col) {
                    OperationOutcomeUtil.addLocationToIssue(context, issue, "Line $line, Col $col")
                }
            }
            if(next is EvaluatedValidationMessage && next.locationElement.isNotEmpty()){
                OperationOutcomeUtil.addLocationToIssue(context, issue, next.locationElement)
            }
        }

        if (messages.isEmpty()) {
            val message = context.localizer.getMessage(ValidationResult::class.java, "noIssuesDetected")
            OperationOutcomeUtil.addIssue(context, theOperationOutcome, "information", message, null, "informational")
        }
    }

    class EvaluatedValidationMessage: SingleValidationMessage(){

        var locationElement: String = ""

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null) {
                return false
            }
            if (other !is SingleValidationMessage) {
                return false
            }
            val otherObj = other as EvaluatedValidationMessage
            val b = EqualsBuilder()
            b.append(locationCol, otherObj.locationCol)
            b.append(locationLine, otherObj.locationLine)
            b.append(locationString, otherObj.locationString)
            b.append(locationElement, otherObj.locationElement)
            b.append(message, otherObj.message)
            b.append(severity, otherObj.severity)
            return b.isEquals
        }

        override fun hashCode(): Int {
            val b = HashCodeBuilder()
            b.append(locationCol)
            b.append(locationLine)
            b.append(locationString)
            b.append(locationElement)
            b.append(message)
            b.append(severity)
            return b.toHashCode()
        }

        override fun toString(): String {
            val b = ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            if (locationCol != null || locationLine != null) {
                b.append("col", locationCol)
                b.append("row", locationLine)
            }
            b.append("locationElement", locationElement)
            if (locationString != null) {
                b.append("locationString", locationString)
            }
            b.append("message", message)
            if (severity != null) {
                b.append("severity", severity.code)
            }
            return b.toString()
        }


    }

}
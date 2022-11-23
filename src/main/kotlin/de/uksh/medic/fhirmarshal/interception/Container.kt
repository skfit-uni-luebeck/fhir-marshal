package de.uksh.medic.fhirmarshal.interception

import ca.uhn.fhir.model.api.IElement
import ca.uhn.fhir.model.api.annotation.Child
import ca.uhn.fhir.model.api.annotation.ResourceDef
import org.hl7.fhir.r4.model.Element
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType

@ResourceDef(name = "Container")
class Container: Resource() {

    companion object{
        private const val serialVersionUID: Long = 1L
    }

    @Child(name = "element", type = [Element::class], min = 1, max = 1, order = Child.ORDER_UNKNOWN)
    private var element: IElement = StringType("")

    override fun copy(): Resource {
        val container = Container()
        container.element = element
        return container
    }

    override fun getResourceType(): ResourceType? {
        return null
    }

    fun getElement(): IElement = element

    fun setElement(element: IElement){
        this.element = element
    }

}
package de.uksh.medic.fhirmarshal.interception

import ca.uhn.fhir.interceptor.api.*
import ca.uhn.fhir.interceptor.executor.BaseInterceptorService
import java.lang.reflect.Method
import java.util.*

class InterceptorService constructor(name: String = "default"): BaseInterceptorService<Pointcut>(name), IInterceptorBroadcaster{

    /**
     * Copied from existing implementation
     * @see {@link ca.uhn.fhir.interceptor.executor.InterceptorService#scanForHook()}
     */
    override fun scanForHook(nextMethod: Method?): Optional<HookDescriptor> {
        return findAnnotation(nextMethod, Hook::class.java).map { hook -> HookDescriptor(hook.value, hook.order) }
    }

}
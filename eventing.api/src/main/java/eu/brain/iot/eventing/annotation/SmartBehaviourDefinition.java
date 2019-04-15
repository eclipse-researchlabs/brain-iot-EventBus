/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package eu.brain.iot.eventing.annotation;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Capability;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * This annotation is designed to be applied to a Smart
 * Behaviour implementation so that the relevant Provide-Capability
 * and service properties are advertised
 */
@ComponentPropertyType
@Capability(namespace="eu.brain.iot.behaviour")
public @interface SmartBehaviourDefinition {

	static final String PREFIX_ = "eu.brain.iot.behaviour.";
	
    /**
     * The message types consumed by this smart behaviour 
     */
	@Attribute
	Class<?>[] consumed();
	
	/**
	 * A filter used to select the properties of the
	 * events that should be passed to this smart behaviour
	 * @return
	 */
	String filter() default "";
	
}

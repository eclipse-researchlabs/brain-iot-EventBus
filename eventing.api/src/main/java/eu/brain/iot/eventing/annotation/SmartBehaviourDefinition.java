/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package eu.brain.iot.eventing.annotation;

import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.CAPABILITY_AUTHOR_ATTRIBUTE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.CAPABILITY_CONSUMED_ATTRIBUTE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.CAPABILITY_NAME_ATTRIBUTE;

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
	@Attribute(value = CAPABILITY_CONSUMED_ATTRIBUTE)
	Class<?>[] consumed();

	/**
     * The author of this smart behaviour
     */
    @Attribute(value = CAPABILITY_AUTHOR_ATTRIBUTE)
    String author() default "";

    /**
     * The name of this smart behaviour
     */
    @Attribute(value = CAPABILITY_NAME_ATTRIBUTE)
    String name() default "";

    /**
     * The description of this smart behaviour
     */
    @Attribute(value = CAPABILITY_DESCRIPTION_ATTRIBUTE)
    String description() default "";

	/**
	 * A filter used to select the properties of the
	 * events that should be passed to this smart behaviour
	 * @return
	 */
	String filter() default "";

}

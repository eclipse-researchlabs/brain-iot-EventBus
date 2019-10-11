/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package eu.brain.iot.eventing.annotation;

import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.CAPABILITY_AUTHOR_ATTRIBUTE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.CAPABILITY_NAME_ATTRIBUTE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.SMART_BEHAVIOUR_NAMESPACE;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Capability;

/**
 * This annotation is designed to be applied to a Smart
 * Behaviour implementation so that the relevant Provide-Capability
 * and service properties are advertised to advertise this
 * as a consumer of last resort
 */
@LastResort
@Capability(namespace=SMART_BEHAVIOUR_NAMESPACE)
public @interface ConsumerOfLastResort {

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
	
}

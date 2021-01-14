/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

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

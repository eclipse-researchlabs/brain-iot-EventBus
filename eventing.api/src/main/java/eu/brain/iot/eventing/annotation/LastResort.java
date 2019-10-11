/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package eu.brain.iot.eventing.annotation;

import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * This annotation should not normally be applied directly,
 * please use {@link ConsumerOfLastResort} instead.
 * 
 * <p>
 * This annotation just tags a component as a consumer of last
 * resort without providing any Provide-Capability information
 */
@ComponentPropertyType
public @interface LastResort {

	static final String PREFIX_ = "eu.brain.iot.behaviour.consumer.of.";
	
}

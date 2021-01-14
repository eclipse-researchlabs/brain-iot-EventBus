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

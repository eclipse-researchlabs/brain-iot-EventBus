/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package eu.brain.iot.behaviour.namespace;

/**
 * This class contains the constants and descriptions of entries used in the 
 * Smart Behaviour Namespace.
 * 
 * <p>
 * This namespace is used to describe Smart Behaviours contained in bundles,
 * and should be applied in a Bundle's Provide-Capability metadata.
 */
public class SmartBehaviourNamespace {

	/**
	 * Namespace name for Smart Behaviour capabilities and requirements.
	 */
	public static final String SMART_BEHAVIOUR_NAMESPACE = "eu.brain.iot.behaviour";

	/**
	 * The capability attribute identifying the author of the smart behaviour
	 * if one is specified or {@code ""} if not specified. The value of this
	 * attribute must be of type {@code String}.
	 */
	public static final String CAPABILITY_AUTHOR_ATTRIBUTE = "author";
	
	/**
	 * The capability attribute identifying the consumed event types of the 
	 * smart behaviour. This value will not be present for consumers of last
	 * resort
	 */
	public static final String CAPABILITY_CONSUMED_ATTRIBUTE = "consumed";
	
	/**
	 * The capability attribute identifying the name of the smart behaviour
	 * if one is specified or "" if not specified. The value of this
	 * attribute must be of type {@code String}.
	 */
	public static final String CAPABILITY_NAME_ATTRIBUTE = "name";

	/**
	 * The capability attribute identifying the human readable description
	 * of the smart behaviour if one is specified or "" if not specified. 
	 * The value of this attribute must be of type {@code String}.
	 */
	public static final String CAPABILITY_DESCRIPTION_ATTRIBUTE = "description";
	
	
	private SmartBehaviourNamespace() {
		// empty
	}

}

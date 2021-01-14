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
 * This class contains constant values for headers used in the Manifest describing a
 * smart behaviour archive.
 */
public class SmartBehaviourManifest {

    /**
     * The Symbolic Name of the Smart Behaviour, as it should be represented in any index.
     * Must follow OSGi symbolic name syntax rules.
     */
	public static final String BRAIN_IOT_SMART_BEHEAVIOUR_SYMBOLIC_NAME = "BRAIN-IoT-Smart-Behaviour-SymbolicName";
	
	/**
	 * The Version of the Smart Behaviour, as it should be represented in any index.
	 * Must follow OSGi version syntax rules
	 */
	public static final String BRAIN_IOT_SMART_BEHEAVIOUR_VERSION = "BRAIN-IoT-Smart-Behaviour-Version";
	/**
	 * The Requirement(s) that should be used to resolve this smart behaviour when deploying it
	 * Must follow OSGi requirement syntax, as used for Require-Capability
	 * 
	 * <p>
	 * Should not be used in conjunction with {@link #BRAIN_IOT_DEPLOY_RESOURCES}
	 */
	public static final String BRAIN_IOT_DEPLOY_REQUIREMENTS = "BRAIN-IoT-Deploy-Requirement";
	
	/**
	 * The list of resources that should be used to resolve this smart behaviour when deploying it.
	 * Must follow OSGi requirement syntax, as used for Require-Capability.
	 * 
	 * <p>
	 * Should not be used in conjunction with {@link #BRAIN_IOT_DEPLOY_REQUIREMENTS}
	 */
	public static final String BRAIN_IOT_DEPLOY_RESOURCES = "BRAIN-IoT-Deploy-Resources";
	
	/**
	 * The classifier that should be used to identify a JAR file as a BRAIN-IoT smart behaviour
	 * when publishing using Maven.
	 */
	public static final String SMART_BEHAVIOUR_MAVEN_CLASSIFIER = "brain-iot-smart-behaviour";
	
}

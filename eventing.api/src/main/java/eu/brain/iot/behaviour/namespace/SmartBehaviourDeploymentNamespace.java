/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.behaviour.namespace;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.repository.ContentNamespace;

/**
 * This class contains the constants and descriptions of entries used in the 
 * Smart Behaviour Deployment Namespace
 * 
 *  * <p>
 * This namespace is used to describe the deployment of a Smart Behaviours,
 * and should typically be applied in a top-level marketplace index.
 */
public class SmartBehaviourDeploymentNamespace {

	/**
	 * Namespace name for Smart Behaviour capabilities and requirements.
	 */
	public static final String SMART_BEHAVIOUR_DEPLOYMENT_NAMESPACE = "eu.brain.iot.behaviour.deployment";

	/**
	 * The value of the {@link IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE} that should
	 * be used to identify something as a Smart Behaviour Deployment
	 */
	public static final String IDENTITY_TYPE_SMART_BEHAVIOUR = "eu.brain.iot.behaviour";
	
	/**
	 * The value of the {@link ContentNamespace#CAPABILITY_MIME_ATTRIBUTE} that should
	 * be used to identify a linked index in a Smart Behaviour Deployment
	 */
	public static final String CONTENT_MIME_TYPE_INDEX = "application/vnd.osgi.repository+xml";

	/**
	 * The capability attribute identifying the requirements used to resolve
	 * the smart behaviour from the associated repository index.
	 * The value of this attribute must be of type {@code List<String>}.
	 * 
	 * <p>
	 * This property should not used in conjunction with {@link #CAPABILITY_RESOURCES_ATTRIBUTE}
	 */
	public static final String CAPABILITY_REQUIREMENTS_ATTRIBUTE = "requirements";

	/**
	 * The capability attribute identifying the resources that make up
	 * this smart behaviour and should be installe, but not resolved,
	 * from the associated repository index. The value of this attribute 
	 * must be of type {@code List<String>}.
	 *  
	 * <p>
	 * This property should not used in conjunction with {@link #CAPABILITY_REQUIREMENTS_ATTRIBUTE}
	 */
	public static final String CAPABILITY_RESOURCES_ATTRIBUTE = "resources";
	
	private SmartBehaviourDeploymentNamespace() {
		// empty
	}

}

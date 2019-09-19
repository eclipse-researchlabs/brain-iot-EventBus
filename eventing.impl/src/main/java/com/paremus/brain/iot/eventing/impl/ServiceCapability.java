/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.impl;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.service.ServiceNamespace;

/**
 * This type is an internal convenience for tagging something as an 
 * OSGi service if it isn't being registered automatically by 
 * Declarative Services.
 */
@Capability(namespace=ServiceNamespace.SERVICE_NAMESPACE)
@interface ServiceCapability {

	@Attribute(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE)
	public Class<?>[] value();
	
}

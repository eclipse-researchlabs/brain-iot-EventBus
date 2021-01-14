/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

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

/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package config;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This is a simple component with the purpose of creating configurations
 * to start the Aries ZooKeeper discovery. It is not interesting to the
 * example, and is just some infrastructure boilerplate
 */
@RequireConfigurationAdmin
@Requirement(namespace=IdentityNamespace.IDENTITY_NAMESPACE, name="org.apache.aries.rsa.discovery.zookeeper")
@Requirement(namespace=IdentityNamespace.IDENTITY_NAMESPACE, name="org.apache.aries.rsa.core")
@Requirement(namespace=IdentityNamespace.IDENTITY_NAMESPACE, name="org.apache.aries.rsa.provider.tcp")
@Requirement(namespace=IdentityNamespace.IDENTITY_NAMESPACE, name="org.apache.aries.rsa.topology-manager")
@Component
public class ZooKeeperConfigurer {

	@Reference
	ConfigurationAdmin cm;
	
	@Activate
	void start(BundleContext ctx) throws IOException {
		// Set up the server
		if("true".equals(ctx.getProperty("server"))) {
			cm.getConfiguration("org.apache.aries.rsa.discovery.zookeeper.server", "?")
				.update(new Hashtable<String, Object>());
		}
		// Set up the client
		cm.getConfiguration("org.apache.aries.rsa.discovery.zookeeper", "?")
			.update(new Hashtable<String, Object>());
	}
	
}

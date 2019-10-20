/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;

import com.paremus.brain.iot.eventing.spi.remote.RemoteEventBus;

import eu.brain.iot.eventing.annotation.LastResort;
import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.UntypedSmartBehaviour;

/**
 * This is a JUnit test that will be run inside an OSGi framework.
 * 
 * It can interact with the framework by starting or stopping bundles, getting
 * or registering services, or in other ways, and then observing the result on
 * the bundle(s) being tested.
 */
@RunWith(JUnit4.class)
public class RemoteEventBusIntegrationTest extends AbstractIntegrationTest {

	private static final String REMOTE_BUS = RemoteEventBus.class.getName();
	private static final String UNTYPED_BEHAVIOUR = UntypedSmartBehaviour.class.getName();
	private Map<UUID, Framework> frameworks;
	private Map<UUID, ServiceTracker<?,?>> remoteServicePublishers = new ConcurrentHashMap<>();
	
	private BundleContext bundleContext;

	@Before
	public void setUpFrameworks() throws Exception {
		assertNotNull("OSGi Bundle tests must be run inside an OSGi framework", bundle);

		bundleContext = bundle.getBundleContext();

		frameworks = createFrameworks(2);
		frameworks.put(getMasterFrameworkUUID(), bundleContext.getBundle(0).adapt(Framework.class));
		
		for (Entry<UUID, Framework> entry : frameworks.entrySet()) {
			Framework f = entry.getValue();
			
			BundleContext context = f.getBundleContext();
			ServiceTracker<Object, Object> tracker = createCrossFrameworkPublisher(entry, context);
			
			remoteServicePublishers.put(entry.getKey(), tracker);
		}
	}

	private ServiceTracker<Object, Object> createCrossFrameworkPublisher(Entry<UUID, Framework> entry,
			BundleContext context) {
		ServiceTracker<Object, Object> tracker = new ServiceTracker<Object, Object>(context, 
				REMOTE_BUS, null) {
			
			Map<UUID, ServiceRegistration<?>> registered = new ConcurrentHashMap<>();

					@Override
					public Object addingService(ServiceReference<Object> reference) {
						
						if(reference.getBundle().getBundleId() == 0) {
							return null;
						}
						
						Object service = super.addingService(reference);

						for (Entry<UUID, Framework> e : frameworks.entrySet()) {
							UUID fwkId = entry.getKey();
							if(fwkId.equals(e.getKey())) {
								// Skip this framework as it's the same framework the service came from
								continue;
							}
							
							Framework fw = e.getValue();
							
							registered.put(fwkId, fw.getBundleContext().registerService(
									REMOTE_BUS, new EventHandlerFactory(service, REMOTE_BUS), 
									getRegistrationProps(reference)));
						}
						
						return service;
					}
					
					Dictionary<String, Object> getRegistrationProps(ServiceReference<?> ref) {
						Dictionary<String, Object> toReturn = new Hashtable<String, Object>();
						Dictionary<String,Object> props = ref.getProperties();
						Enumeration<String> keys = props.keys();
						while(keys.hasMoreElements()) {
							String key = keys.nextElement();
							toReturn.put(key, props.get(key));
						}
						toReturn.put("service.imported", true);
						return toReturn;
					}

					@Override
					public void modifiedService(ServiceReference<Object> reference, Object service) {
						for(ServiceRegistration<?> reg : registered.values()) {
							reg.setProperties(getRegistrationProps(reference));
						}
					}

					@Override
					public void removedService(ServiceReference<Object> reference, Object service) {
						for (ServiceRegistration<?> registration : registered.values()) {
							try {
								registration.unregister();
							} catch (Exception e) {
								// Never mind
							}
						}
						registered.clear();
						super.removedService(reference, service);
					}
			
		};
		tracker.open(true);
		return tracker;
	}
	
	@After
	public void shutdownFrameworks() {
		
		frameworks.remove(getMasterFrameworkUUID());
		
		remoteServicePublishers.values().forEach(ServiceTracker::close);
		remoteServicePublishers.clear();
		
		frameworks.values().forEach(f -> {
			try {
				f.stop();
			} catch (BundleException be) {
				// Never mind
			}
		});
		
		frameworks.clear();
	}

	private Map<UUID, Framework> createFrameworks(int size) throws BundleException {
		
		BundleContext context = bundle.getBundleContext();
		
		FrameworkFactory ff = ServiceLoader.load(FrameworkFactory.class, 
    			FrameworkFactory.class.getClassLoader()).iterator().next();
    	
    	List<String> locations = new ArrayList<>();
    	
    	for(Bundle b : context.getBundles()) {
    		if(b.getSymbolicName().equals("org.apache.felix.scr") ||
    				b.getSymbolicName().equals("com.paremus.brain.iot.eventing.api") ||
    				b.getSymbolicName().equals("com.paremus.brain.iot.eventing.impl") ||
    				b.getSymbolicName().equals("org.osgi.util.function") ||
    				b.getSymbolicName().equals("org.osgi.util.promise") ||
    				b.getSymbolicName().startsWith("org.osgi.util.pushstream")) {
    			locations.add(b.getLocation());
    		}
    	}
    	
    	Map<UUID, Framework> frameworks = new HashMap<UUID, Framework>();
        for(int i = 1; i < size; i++) {
        	Map<String, String> fwConfig = new HashMap<>();
        	fwConfig.put(Constants.FRAMEWORK_STORAGE, new File(context.getDataFile(""), "Test-Cluster" + i).getAbsolutePath());
        	fwConfig.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        	Framework f = ff.newFramework(fwConfig);
        	f.init();
        	for(String s : locations) {
        		f.getBundleContext().installBundle(s);
        	}
        	f.start();
        	f.adapt(FrameworkWiring.class).resolveBundles(Collections.emptySet());
        	for(Bundle b : f.getBundleContext().getBundles()) {
        		if(b.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
        			b.start();
        		}
        	}
        	frameworks.put(getUUID(f), f);
        }
		return frameworks;
	}

	private UUID getMasterFrameworkUUID() {
		return UUID.fromString(bundle.getBundleContext().getProperty(Constants.FRAMEWORK_UUID));
	}
	
	private UUID getUUID(Framework f) {
		return UUID.fromString(f.getBundleContext().getProperty(Constants.FRAMEWORK_UUID));
	}


	public static class EventHandlerFactory implements ServiceFactory<Object> {

		private final Object delegate;
		private final String typeToMimic;
		
		public EventHandlerFactory(Object delegate, String typeToMimic) {
			this.delegate = delegate;
			this.typeToMimic = typeToMimic;
		}

		@Override
		public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
			
			try {
				Class<?> loadClass = bundle.loadClass(typeToMimic);
				
				return Proxy.newProxyInstance(loadClass.getClassLoader(), new Class<?>[] {loadClass}, 
						(o,m,a) -> {
							
							if("notify".equals(m.getName())) {
								return delegate.getClass().getMethod("notify", String.class, Map.class)
										.invoke(delegate, a);
							} else {
								return m.invoke(delegate, a);
							}
						});
				
			} catch (Exception e) {
				throw new ServiceException("failed to create service", e);
			}
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	@Test
	public void testSendToRemoteFramework() throws InterruptedException {
		
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(LastResort.PREFIX_ + "last.resort", true);
		
		regs.add(bundleContext.registerService(UNTYPED_BEHAVIOUR, untypedBehaviourA, props));
		
		TestEvent event = new TestEvent();
    	event.message = "boo";
		
		impl.deliver(event);
		
		assertTrue(untypedSemA.tryAcquire(100, TimeUnit.MILLISECONDS));
		
		verify(untypedBehaviourA)
			.notify(eq(TestEvent.class.getName()), argThat(isUntypedTestEventWithMessage("boo")));
		
		
		BundleContext remoteContext = frameworks.values().stream()
				.filter(fw -> !getUUID(fw).equals(getMasterFrameworkUUID()))
				.flatMap(fw -> Arrays.stream(fw.getBundleContext().getBundles()))
				.filter(b -> b.getSymbolicName().equals("com.paremus.brain.iot.eventing.api"))
				.map(Bundle::getBundleContext)
				.findFirst()
				.get();
		
		props = new Hashtable<>();
		props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
		props.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=boo)");
		
		regs.add(remoteContext.registerService(UNTYPED_BEHAVIOUR, 
				new EventHandlerFactory(untypedBehaviourB, UNTYPED_BEHAVIOUR), props));
		
		
		impl.deliver(event);
		assertFalse(untypedSemA.tryAcquire(1000, TimeUnit.MILLISECONDS));
		assertTrue(untypedSemB.tryAcquire(100, TimeUnit.MILLISECONDS));
		
		verify(untypedBehaviourB)
			.notify(eq(TestEvent.class.getName()), argThat(isUntypedTestEventWithMessage("boo")));
	}
	
}
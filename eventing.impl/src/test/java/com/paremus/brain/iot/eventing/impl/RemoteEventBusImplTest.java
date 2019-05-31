/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package com.paremus.brain.iot.eventing.impl;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.osgi.framework.FrameworkUtil.createFilter;

import java.util.Arrays;
import java.util.Dictionary;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

import com.paremus.brain.iot.eventing.spi.remote.RemoteEventBus;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;

@SuppressWarnings("unchecked")
public class RemoteEventBusImplTest {
	
	@Rule
	public MockitoRule rule = MockitoJUnit.rule();
	
	@Mock
	BundleContext context;
	
	@Mock
	ServiceRegistration<RemoteEventBus> remoteReg;
	
	@Mock
	EventBusImpl eventBusImpl;
	
	
	RemoteEventBusImpl remoteImpl;
	
	
	@Before
	public void start() {
		
		Mockito.when(context.registerService(Mockito.eq(RemoteEventBus.class), 
				Mockito.any(RemoteEventBus.class), Mockito.any())).thenReturn(remoteReg);
		
		remoteImpl = new RemoteEventBusImpl(eventBusImpl);
	}

	
	@After
	public void destroy() {
		remoteImpl.destroy();
	}
	
	@Test
	public void testEmptyStart() {
		remoteImpl.init(context);
		
		ArgumentCaptor<Dictionary<String, Object>> propsCaptor = ArgumentCaptor.forClass(Dictionary.class); 
		
		Mockito.verify(context).registerService(Mockito.eq(RemoteEventBus.class), Mockito.same(remoteImpl),
				propsCaptor.capture());
		
		Dictionary<String, Object> props = propsCaptor.getValue();
		assertNull(props);
		
		Mockito.verify(remoteReg).setProperties(propsCaptor.capture());
		
		props = propsCaptor.getValue();
		
		assertEquals(RemoteEventBus.class.getName(), props.get("service.exported.interfaces"));
		assertEquals(emptyList(), props.get(SmartBehaviourDefinition.PREFIX_ + "consumed"));
	}

	@Test
	public void testStartWithDetails() throws InvalidSyntaxException {
		
		remoteImpl.updateLocalInterest(42L, Arrays.asList("FOO"), createFilter("(fizz=buzz)"));
		
		remoteImpl.init(context);
		
		ArgumentCaptor<Dictionary<String, Object>> propsCaptor = ArgumentCaptor.forClass(Dictionary.class); 
		
		Mockito.verify(context).registerService(Mockito.eq(RemoteEventBus.class), Mockito.same(remoteImpl),
				propsCaptor.capture());
	
		Dictionary<String, Object> props = propsCaptor.getValue();
		assertNull(props);

		Mockito.verify(remoteReg).setProperties(propsCaptor.capture());
		
		props = propsCaptor.getValue();
		
		assertEquals(RemoteEventBus.class.getName(), props.get("service.exported.interfaces"));
		assertEquals(Arrays.asList("FOO"), props.get(SmartBehaviourDefinition.PREFIX_ + "consumed"));
		assertEquals(Arrays.asList("(fizz=buzz)"), props.get("filter-FOO"));
	}
	
	@Test
	public void testLateRegisterOfListener() throws InvalidSyntaxException {
		remoteImpl.init(context);
		
		ArgumentCaptor<Dictionary<String, Object>> propsCaptor = ArgumentCaptor.forClass(Dictionary.class); 
		
		Mockito.verify(context).registerService(Mockito.eq(RemoteEventBus.class), Mockito.same(remoteImpl),
				propsCaptor.capture());
		
		Dictionary<String, Object> props = propsCaptor.getValue();
		assertNull(props);
		
		Mockito.verify(remoteReg).setProperties(propsCaptor.capture());
		
		props = propsCaptor.getValue();
		
		assertEquals(RemoteEventBus.class.getName(), props.get("service.exported.interfaces"));
		assertEquals(emptyList(), props.get(SmartBehaviourDefinition.PREFIX_ + "consumed"));
		
		// Add a listener to the remote
		
		remoteImpl.updateLocalInterest(42L, Arrays.asList("FOO"), createFilter("(fizz=buzz)"));
		
		Mockito.verify(remoteReg, Mockito.times(2)).setProperties(propsCaptor.capture());
		
		props = propsCaptor.getValue();
		
		assertEquals(RemoteEventBus.class.getName(), props.get("service.exported.interfaces"));
		assertEquals(Arrays.asList("FOO"), props.get(SmartBehaviourDefinition.PREFIX_ + "consumed"));
		assertEquals(Arrays.asList("(fizz=buzz)"), props.get("filter-FOO"));
	}
}

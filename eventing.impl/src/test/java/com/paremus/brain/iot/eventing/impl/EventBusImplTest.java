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

import static eu.brain.iot.eventing.message.integrity.api.ValidationResult.ILLEGAL_SIGNATURE;
import static eu.brain.iot.eventing.message.integrity.api.ValidationResult.INVALID;
import static eu.brain.iot.eventing.message.integrity.api.ValidationResult.MISSING;
import static eu.brain.iot.eventing.message.integrity.api.ValidationResult.VALID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.converter.Converters;

import com.paremus.brain.iot.eventing.spi.remote.RemoteEventBus;

import eu.brain.iot.eventing.annotation.LastResort;
import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.SmartBehaviour;
import eu.brain.iot.eventing.api.UntypedSmartBehaviour;
import eu.brain.iot.eventing.message.integrity.api.MessageIntegrityService;
import eu.brain.iot.privacy.client.api.PrivacyClient;
import eu.brain.iot.privacy.pojo.ServiceSpec;


public class EventBusImplTest {
    
	public static class TestEvent extends BrainIoTEvent {
		public String message;
	}

	public static class TestEvent2 extends BrainIoTEvent {
		public int count;
	}
	
	
	@Rule
	public MockitoRule rule = MockitoJUnit.rule();
	
	@Mock
	MessageIntegrityService mis;

	@Mock
	BundleContext context;
	
	@Mock
	PrivacyClient privacyClient;
	
	@Mock
	ServiceRegistration<RemoteEventBus> remoteReg;
	
	@Mock
	SmartBehaviour<BrainIoTEvent> behaviourA, behaviourB;

	@Mock
	UntypedSmartBehaviour untypedBehaviourA, untypedBehaviourB;

	@Mock
	RemoteEventBus remoteA, remoteB;
	
	Semaphore semA = new Semaphore(0), semB = new Semaphore(0),
			untypedSemA = new Semaphore(0), untypedSemB = new Semaphore(0),
			remoteSemA = new Semaphore(0), remoteSemB = new Semaphore(0);
	
	EventBusImpl impl;
	
	@Before
	public void start() {
		
		Mockito.when(mis.generateSecurityToken(Mockito.anyMap())).thenReturn(new byte[] {1, 2, 3, 4});
		Mockito.doAnswer(i -> {
				Map<String, Object> eventData = i.getArgument(0);
				Object token = eventData.get("securityToken");
				
				if(token == null) {
					return MISSING;
				} else if (!(token instanceof byte[])) {
					return ILLEGAL_SIGNATURE;
				} else {
					return Arrays.equals(new byte[] {1, 2, 3, 4}, (byte[])token) ? VALID : INVALID;
				}
			}).when(mis).validateEvent(Mockito.anyMap());
		
		Mockito.doAnswer(i -> {
				semA.release();
				return null;
			}).when(behaviourA).notify(Mockito.any());

		Mockito.doAnswer(i -> {
				semB.release();
				return null;
			}).when(behaviourB).notify(Mockito.any());

		Mockito.doAnswer(i -> {
				untypedSemA.release();
				return null;
			}).when(untypedBehaviourA).notify(Mockito.anyString(), Mockito.any());
		
		Mockito.doAnswer(i -> {
				untypedSemB.release();
				return null;
			}).when(untypedBehaviourB).notify(Mockito.anyString(), Mockito.any());

		Mockito.doAnswer(i -> {
				remoteSemA.release();
				return null;
			}).when(remoteA).notify(Mockito.anyString(), Mockito.any());
		
		Mockito.doAnswer(i -> {
				remoteSemB.release();
				return null;
			}).when(remoteB).notify(Mockito.anyString(), Mockito.any());
		
		Mockito.when(context.registerService(Mockito.eq(RemoteEventBus.class), 
				Mockito.any(RemoteEventBus.class), Mockito.any())).thenReturn(remoteReg);
		
		Mockito.doAnswer(i -> {
			
			ArrayList<ServiceSpec> specs = new ArrayList<ServiceSpec>();
			for (String s : (ArrayList<String>) i.getArgument(1)) {
				
				ServiceSpec service = new ServiceSpec();
				service.setName(s);
				specs.add(service);

			}
			return specs;
		}).when(privacyClient).filter(Mockito.anyMap(),Mockito.anyList());
		

		
		impl = new EventBusImpl();
		
		impl.messageIntegrityService = mis;
		impl.privacyClient = privacyClient;
		impl.start(context);
	}
	
	@After
	public void stop() {
		impl.stop();
	}
	
	/**
	 * Tests that events are delivered to Smart Behaviours
	 * based on type
	 * 
	 * @throws InterruptedException
	 */
    @Test
    public void testEventSending() throws InterruptedException {
        
    	TestEvent event = new TestEvent();
    	event.message = "boo";
    	
    	Map<String, Object> serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(Constants.SERVICE_ID, 42L);
    	
    	
    	impl.addSmartBehaviour(behaviourA, serviceProperties);

    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	serviceProperties.put(Constants.SERVICE_ID, 43L);
    	
    	
    	impl.addSmartBehaviour(behaviourB, serviceProperties);
    	
    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(Constants.SERVICE_ID, 44L);
    	
    	
    	impl.addUntypedSmartBehaviour(untypedBehaviourA, serviceProperties);

    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	serviceProperties.put(Constants.SERVICE_ID, 45L);
    	
    	
    	impl.addUntypedSmartBehaviour(untypedBehaviourB, serviceProperties);
    	
    	impl.deliver(event);
    	
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));

    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("boo")));
    	
    	assertFalse(semB.tryAcquire(1, TimeUnit.SECONDS));

    	assertTrue(untypedSemA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(untypedBehaviourA).notify(Mockito.anyString(), 
    			Mockito.argThat(isUntypedTestEventWithMessage("boo")));
    	
    	assertFalse(untypedSemB.tryAcquire(1, TimeUnit.SECONDS));
    	
    }

    /**
     * Tests that events are delivered to Smart Behaviours
     * based on type
     * 
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
	@Test
    public void testUntypedEventSending() throws InterruptedException {
    	
    	TestEvent event = new TestEvent();
    	event.message = "boo";
    	
    	Map<String, Object> serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(Constants.SERVICE_ID, 42L);
    	
    	
    	impl.addSmartBehaviour(behaviourA, serviceProperties);
    	
    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	serviceProperties.put(Constants.SERVICE_ID, 43L);
    	
    	
    	impl.addSmartBehaviour(behaviourB, serviceProperties);
    	
    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(Constants.SERVICE_ID, 44L);
    	
    	
    	impl.addUntypedSmartBehaviour(untypedBehaviourA, serviceProperties);
    	
    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	serviceProperties.put(Constants.SERVICE_ID, 45L);
    	
    	
    	impl.addUntypedSmartBehaviour(untypedBehaviourB, serviceProperties);
    	
    	impl.deliver(event.getClass().getName(), Converters.standardConverter().convert(event).to(Map.class));
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("boo")));
    	
    	assertFalse(semB.tryAcquire(1, TimeUnit.SECONDS));
    	
    	assertTrue(untypedSemA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(untypedBehaviourA).notify(Mockito.anyString(), 
    			Mockito.argThat(isUntypedTestEventWithMessage("boo")));
    	
    	assertFalse(untypedSemB.tryAcquire(1, TimeUnit.SECONDS));
    	
    }

    /**
     * Tests that filtering is applied to message sending/receiving
     * @throws InterruptedException
     */
    @Test
    public void testEventFiltering() throws InterruptedException {
    	
    	Map<String, Object> serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=foo)");
    	serviceProperties.put(Constants.SERVICE_ID, 42L);
    	
    	
    	impl.addSmartBehaviour(behaviourA, serviceProperties);
    	
    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=bar)");
    	serviceProperties.put(Constants.SERVICE_ID, 43L);
    	
    	impl.addSmartBehaviour(behaviourB, serviceProperties);

    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=foo)");
    	serviceProperties.put(Constants.SERVICE_ID, 44L);
    	
    	
    	impl.addUntypedSmartBehaviour(untypedBehaviourA, serviceProperties);
    	
    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=bar)");
    	serviceProperties.put(Constants.SERVICE_ID, 45L);
    	
    	
    	impl.addUntypedSmartBehaviour(untypedBehaviourB, serviceProperties);
    	
    	
    	TestEvent event = new TestEvent();
    	event.message = "foo";
    	
    	impl.deliver(event);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("foo")));
    	
    	assertFalse(semB.tryAcquire(1, TimeUnit.SECONDS));

    	assertTrue(untypedSemA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(untypedBehaviourA).notify(Mockito.anyString(), 
    			Mockito.argThat(isUntypedTestEventWithMessage("foo")));
    	
    	assertFalse(untypedSemB.tryAcquire(1, TimeUnit.SECONDS));
    	
    	
    	event = new TestEvent();
    	event.message = "bar";
    	
    	
    	impl.deliver(event);
    	
    	assertTrue(semB.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(behaviourB).notify(Mockito.argThat(isTestEventWithMessage("bar")));
    	
    	assertFalse(semA.tryAcquire(1, TimeUnit.SECONDS));

    	
    	assertTrue(untypedSemB.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(untypedBehaviourB).notify(Mockito.anyString(), 
    			Mockito.argThat(isUntypedTestEventWithMessage("bar")));
    	
    	assertFalse(untypedSemA.tryAcquire(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that filtering is applied to message sending/receiving
     * @throws InterruptedException
     */
    @Test
    public void testEventFilteringWithEmptyStringFilter() throws InterruptedException {
    	
    	Map<String, Object> serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "filter", "");
    	serviceProperties.put(Constants.SERVICE_ID, 42L);
    	
    	
    	impl.addSmartBehaviour(behaviourA, serviceProperties);
    	
    	TestEvent event = new TestEvent();
    	event.message = "foo";
    	
    	impl.deliver(event);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));
    }
    
    /**
     * Tests that the consumer of last resort gets called appropriately
     * @throws InterruptedException
     */
    @Test
    public void testConsumerOfLastResort() throws InterruptedException {
    	
    	Map<String, Object> serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=foo)");
    	serviceProperties.put(Constants.SERVICE_ID, 42L);
    	
    	
    	impl.addSmartBehaviour(behaviourA, serviceProperties);
    	
    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(LastResort.PREFIX_ + "last.resort", true);
    	serviceProperties.put(Constants.SERVICE_ID, 45L);
    	
    	impl.addConsumerOfLastResort(untypedBehaviourA, serviceProperties);
    	
    	
    	TestEvent event = new TestEvent();
    	event.message = "foo";
    	
    	impl.deliver(event);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("foo")));
    	
    	assertFalse(untypedSemA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	
    	event = new TestEvent();
    	event.message = "bar";
    	
    	
    	impl.deliver(event);
    	
    	assertTrue(untypedSemA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(untypedBehaviourA).notify(Mockito.anyString(), 
    			Mockito.argThat(isUntypedTestEventWithMessage("bar")));

    	
    	assertFalse(semA.tryAcquire(1, TimeUnit.SECONDS));

    }
    
	/**
	 * Tests that events are delivered to Smart Behaviours
	 * based on type
	 * 
	 * @throws InterruptedException
	 */
    @Test
    public void testRemoteEventSending() throws InterruptedException {
        
    	TestEvent event = new TestEvent();
    	event.message = "boo";
    	
    	Map<String, Object> serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	serviceProperties.put("filter-" + TestEvent.class.getName(), "(message=boo)");
    	serviceProperties.put(Constants.SERVICE_ID, 42L);
    	
    	impl.addRemoteEventBus(remoteA, serviceProperties);

    	serviceProperties = new HashMap<>();
    	
    	serviceProperties.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	serviceProperties.put("filter-" + TestEvent2.class.getName(), "(count>=12)");
    	serviceProperties.put(Constants.SERVICE_ID, 43L);
    	
    	impl.addRemoteEventBus(remoteB, serviceProperties);
    	
    	serviceProperties = new HashMap<>();
    	
    	impl.deliver(event);
    	
    	assertTrue(remoteSemA.tryAcquire(1, TimeUnit.SECONDS));

    	Mockito.verify(remoteA).notify(Mockito.anyString(), 
    			Mockito.argThat(isUntypedTestEventWithMessage("boo")));
    	
    	assertFalse(remoteSemB.tryAcquire(1, TimeUnit.SECONDS));
    }
    
    
    ArgumentMatcher<BrainIoTEvent> isTestEventWithMessage(String message) {
    	return new ArgumentMatcher<BrainIoTEvent>() {
			
			@Override
			public boolean matches(BrainIoTEvent argument) {
				return argument instanceof TestEvent && 
						message.equals(((TestEvent)argument).message);
			}
		};
    }

    ArgumentMatcher<Map<String, Object>> isUntypedTestEventWithMessage(String message) {
    	return new ArgumentMatcher<Map<String, Object>>() {
    		
    		@Override
    		public boolean matches(Map<String, Object> argument) {
    			return argument != null && message.equals(argument.get("message"));
    		}
    	};
    }
    
}

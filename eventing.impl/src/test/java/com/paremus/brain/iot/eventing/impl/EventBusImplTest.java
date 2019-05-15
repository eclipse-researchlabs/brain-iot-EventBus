/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
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
import org.osgi.framework.Constants;
import org.osgi.util.converter.Converters;

import eu.brain.iot.eventing.annotation.ConsumerOfLastResort;
import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.SmartBehaviour;
import eu.brain.iot.eventing.api.UntypedSmartBehaviour;


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
	SmartBehaviour<BrainIoTEvent> behaviourA, behaviourB;

	@Mock
	UntypedSmartBehaviour untypedBehaviourA, untypedBehaviourB;
	
	Semaphore semA = new Semaphore(0), semB = new Semaphore(0),
			untypedSemA = new Semaphore(0), untypedSemB = new Semaphore(0);
	
	EventBusImpl impl;
	
	@Before
	public void start() {
		
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
		
		impl = new EventBusImpl();
		impl.start();
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
    	
    	serviceProperties.put(ConsumerOfLastResort.PREFIX_ + "consumer.of.last.resort", true);
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

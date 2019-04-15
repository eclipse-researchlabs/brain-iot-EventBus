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

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.SmartBehaviour;


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
	
	Semaphore semA = new Semaphore(0), semB = new Semaphore(0);
	
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
		
		impl = new EventBusImpl();
		impl.start();
	}
	
	@After
	public void stop() {
		impl.stop();
	}
	
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
    	
    	impl.deliver(event);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));

    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("boo")));
    	
    	assertFalse(semB.tryAcquire(1, TimeUnit.SECONDS));
    	
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
    
}

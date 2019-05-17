/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.test;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.UntypedSmartBehaviour;
/**
 * This is a JUnit test that will be run inside an OSGi framework.
 * 
 * It can interact with the framework by starting or stopping bundles,
 * getting or registering services, or in other ways, and then observing
 * the result on the bundle(s) being tested.
 */
public abstract class AbstractIntegrationTest {
    
    protected final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    
	@Rule
	public MockitoRule rule = MockitoJUnit.rule();
	
	@Mock
	protected TestEventConsumer behaviourA, behaviourB;
	@Mock
	protected TestEvent2Consumer behaviourA2, behaviourB2;
	@Mock
	protected UntypedSmartBehaviour untypedBehaviourA, untypedBehaviourB;
	
	protected final Semaphore semA = new Semaphore(0), semB = new Semaphore(0),
			untypedSemA = new Semaphore(0), untypedSemB = new Semaphore(0);

	private ServiceTracker<EventBus, EventBus> serviceTracker;
	
	protected final List<ServiceRegistration<?>> regs = new ArrayList<ServiceRegistration<?>>();

	protected EventBus impl;
    
    @Before
    public void setUp() throws Exception {
        assertNotNull("OSGi Bundle tests must be run inside an OSGi framework", bundle);
        
        Mockito.doAnswer(i -> {
			semA.release();
			return null;
		}).when(behaviourA).notify(Mockito.any());

        Mockito.doAnswer(i -> {
			semB.release();
			return null;
		}).when(behaviourB).notify(Mockito.any());

        Mockito.doAnswer(i -> {
        	semA.release();
        	return null;
        }).when(behaviourA2).notify(Mockito.any());
        
        Mockito.doAnswer(i -> {
        	semB.release();
        	return null;
        }).when(behaviourB2).notify(Mockito.any());
        
        Mockito.doAnswer(i -> {
			untypedSemA.release();
			return null;
		}).when(untypedBehaviourA).notify(Mockito.anyString(), Mockito.any());
	
        Mockito.doAnswer(i -> {
			untypedSemB.release();
			return null;
		}).when(untypedBehaviourB).notify(Mockito.anyString(), Mockito.any());
        
        serviceTracker = new ServiceTracker<>(bundle.getBundleContext(), EventBus.class, null);
        serviceTracker.open();
        
        impl = serviceTracker.waitForService(500);
        
        assertNotNull(impl);
    }
    
    @After
    public void tearDown() throws Exception {
        serviceTracker.close();
        
        regs.forEach(sr -> {
        	try {
        		sr.unregister();
        	} catch (Exception e) { }
        });
    }
    
    protected ArgumentMatcher<TestEvent> isTestEventWithMessage(String message) {
    	return new ArgumentMatcher<TestEvent>() {
			
			@Override
			public boolean matches(TestEvent argument) {
				return message.equals(argument.message);
			}
		};
    }

    protected ArgumentMatcher<TestEvent2> isTestEvent2WithMessage(String message) {
    	return new ArgumentMatcher<TestEvent2>() {
    		
    		@Override
    		public boolean matches(TestEvent2 argument) {
    			return message.equals(argument.subEvent.message);
    		}
    	};
    }
    
    protected ArgumentMatcher<Map<String, Object>> isUntypedTestEventWithMessage(String message) {
    	return new ArgumentMatcher<Map<String, Object>>() {
    		
    		@Override
    		public boolean matches(Map<String, Object> argument) {
    			return argument != null && message.equals(argument.get("message"));
    		}
    	};
    }
}
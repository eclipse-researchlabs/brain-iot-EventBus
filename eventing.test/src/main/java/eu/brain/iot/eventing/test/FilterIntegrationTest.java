/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.SmartBehaviour;
/**
 * This is a JUnit test that will be run inside an OSGi framework.
 * 
 * It can interact with the framework by starting or stopping bundles,
 * getting or registering services, or in other ways, and then observing
 * the result on the bundle(s) being tested.
 */
@RunWith(JUnit4.class)
public class FilterIntegrationTest {
    
    private final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    
	@Rule
	public MockitoRule rule = MockitoJUnit.rule();
	
	@Mock
	SmartBehaviour<BrainIoTEvent> behaviourA, behaviourB;
	
	Semaphore semA = new Semaphore(0), semB = new Semaphore(0);

	private ServiceTracker<EventBus, EventBus> serviceTracker;
	
	private List<ServiceRegistration<?>> regs = new ArrayList<ServiceRegistration<?>>();

	private EventBus impl;
    
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
    
    @Test
    public void testFilteredListener() throws Exception {
    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	props.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=foo)");
    	
    	
        regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, behaviourA, props));
        
        props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	props.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=bar)");
    	
    	
        regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, behaviourB, props));
        
        TestEvent event = new TestEvent();
    	event.message = "foo";
    	
    	impl.deliver(event);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("foo")));
    	
    	assertFalse(semB.tryAcquire(1, TimeUnit.SECONDS));
    	
    	
    	event = new TestEvent();
    	event.message = "bar";
    	
    	
    	impl.deliver(event);
    	
    	assertTrue(semB.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(behaviourB).notify(Mockito.argThat(isTestEventWithMessage("bar")));
    	
    	assertFalse(semA.tryAcquire(1, TimeUnit.SECONDS));
    }

    @Test
    public void testFilteredListenerEmptyString() throws Exception {
    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	props.put(SmartBehaviourDefinition.PREFIX_ + "filter", "");
    	
    	
    	regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, behaviourA, props));
    	
    	TestEvent event = new TestEvent();
    	event.message = "foo";
    	
    	impl.deliver(event);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));
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
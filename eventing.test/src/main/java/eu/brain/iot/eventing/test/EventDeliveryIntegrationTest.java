/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.SmartBehaviour;
import eu.brain.iot.eventing.api.UntypedSmartBehaviour;
/**
 * This is a JUnit test that will be run inside an OSGi framework.
 * 
 * It can interact with the framework by starting or stopping bundles,
 * getting or registering services, or in other ways, and then observing
 * the result on the bundle(s) being tested.
 */
@RunWith(JUnit4.class)
public class EventDeliveryIntegrationTest extends AbstractIntegrationTest {
    
	
	/**
	 * Tests that events are delivered to Smart Behaviours
	 * based on type
	 * 
	 * @throws InterruptedException
	 */
    @Test
    public void testEventReceiving() throws InterruptedException {
        
    	TestEvent event = new TestEvent();
    	event.message = "boo";
    	
    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	
    	regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, behaviourA, props));

    	props = new Hashtable<>();
    	
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	
    	regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, behaviourB, props));
    	
    	impl.deliver(event);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));

    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("boo")));
    	
    	assertFalse(semB.tryAcquire(1, TimeUnit.SECONDS));

    }

    /**
     * Tests that events are delivered to Smart Behaviours
     * based on type
     * 
     * @throws InterruptedException
     */
    @Test
    public void testEventReceivingUntyped() throws InterruptedException {
    	
    	TestEvent event = new TestEvent();
    	event.message = "boo";
    	
    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	
    	regs.add(bundle.getBundleContext().registerService(UntypedSmartBehaviour.class, untypedBehaviourA, props));
    	
    	props = new Hashtable<>();
    	
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	
    	regs.add(bundle.getBundleContext().registerService(UntypedSmartBehaviour.class, untypedBehaviourB, props));
    	
    	
    	impl.deliver(event);
    	
    	assertTrue(untypedSemA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(untypedBehaviourA).notify(Mockito.anyString(), 
    			Mockito.argThat(isUntypedTestEventWithMessage("boo")));
    	
    	assertFalse(untypedSemB.tryAcquire(1, TimeUnit.SECONDS));
    	
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
    
}
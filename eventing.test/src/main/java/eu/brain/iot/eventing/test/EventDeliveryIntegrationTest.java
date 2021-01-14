/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package eu.brain.iot.eventing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.SmartBehaviour;
import eu.brain.iot.eventing.api.UntypedSmartBehaviour;
import eu.brain.iot.eventing.test.TestEvent2.EventType;
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
    public void testSendComplexEvent() throws Exception {
    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	
        regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, behaviourA2, props));
        
        TestEvent event = new TestEvent();
    	event.message = "foo";
    	event.securityToken = new byte[] {0xa, 0xb, 0xc};
    	
    	TestEvent2 event2 = TestEvent2.create(event);
    	
    	impl.deliver(event2);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	Mockito.verify(behaviourA2).notify(Mockito.argThat(isTestEvent2WithMessage("foo")));
    }

    @Test
    public void testSendComplexEventToUntypedReceiver() throws Exception {
    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	
    	regs.add(bundle.getBundleContext().registerService(UntypedSmartBehaviour.class, 
    			untypedBehaviourA, props));
    	
    	TestEvent event = new TestEvent();
    	event.message = "foo";
    	event.securityToken = new byte[] {0xa, 0xb, 0xc};
    	
    	TestEvent2 event2 = TestEvent2.create(event);
    	
    	impl.deliver(event2);
    	
    	assertTrue(untypedSemA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    	
    	Mockito.verify(untypedBehaviourA).notify(eq(TestEvent2.class.getName()), captor.capture());
    	
    	Map<String, Object> map = captor.getValue();
    	
    	// Should be a String not an enum as we can't see the types
    	assertEquals("RED", map.get("eventType"));
    	@SuppressWarnings("unchecked")
		Map<String, Object> subMap = (Map<String, Object>) map.get("subEvent");
    	
    	assertEquals("foo", subMap.get("message"));
    }
    
    @Test
    public void testSendComplexUntypedEventToTypedReceiver() throws Exception {
    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent2.class.getName());
    	
    	regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, 
    			behaviourA2, props));
    	
    	Map<String, Object> event = new HashMap<>();
    	event.put("message", "foo");
    	event.put("securityToken", new byte[] {0xa, 0xb, 0xc});
    	
    	Map<String, Object> event2 = new HashMap<>();
    	event2.put("subEvent", event);
    	event2.put("eventType", "BLUE");
    	
    	impl.deliver(TestEvent2.class.getName(), event2);
    	
    	assertTrue(semA.tryAcquire(1, TimeUnit.SECONDS));
    	
    	ArgumentCaptor<TestEvent2> captor = ArgumentCaptor.forClass(TestEvent2.class);
    	
    	Mockito.verify(behaviourA2).notify(captor.capture());
    	
    	TestEvent2 received = captor.getValue();
    	
    	// Should be a String not an enum as we can't see the types
    	assertEquals(EventType.BLUE, received.eventType);
    	
    	assertEquals("foo", received.subEvent.message);
    }
    
    
    
}

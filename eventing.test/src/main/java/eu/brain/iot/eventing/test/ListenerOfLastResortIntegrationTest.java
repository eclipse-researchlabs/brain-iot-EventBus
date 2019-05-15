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

import eu.brain.iot.eventing.annotation.ConsumerOfLastResort;
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
public class ListenerOfLastResortIntegrationTest extends AbstractIntegrationTest {
    
	   /**
     * Tests that the consumer of last resort gets called appropriately
     * @throws InterruptedException
     */
    @Test
    public void testConsumerOfLastResort() throws InterruptedException {
    	
    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());
    	props.put(SmartBehaviourDefinition.PREFIX_ + "filter", "(message=foo)");
    	
    	
        regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, behaviourA, props));
    	
        props = new Hashtable<>();
    	
        props.put(ConsumerOfLastResort.PREFIX_ + "consumer.of.last.resort", true);
    	
        regs.add(bundle.getBundleContext().registerService(UntypedSmartBehaviour.class, untypedBehaviourA, props));
    	
    	
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
    
}
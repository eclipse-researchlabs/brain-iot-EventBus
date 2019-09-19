/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import eu.brain.iot.eventing.monitoring.api.FilterDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.SmartBehaviour;
/**
 * This is a JUnit test that will be run inside an OSGi framework.
 *
 * It can interact with the framework by starting or stopping bundles,
 * getting or registering services, or in other ways, and then observing
 * the result on the bundle(s) being tested.
 */
import eu.brain.iot.eventing.monitoring.api.EventMonitor;
import eu.brain.iot.eventing.monitoring.api.MonitorEvent;
@RunWith(JUnit4.class)
public class EventMonitorIntegrationTest extends AbstractIntegrationTest {

	ServiceTracker<EventMonitor, EventMonitor> tracker;

	EventMonitor monitor;

    @Before
    public void setUpMonitor() throws Exception {
        assertNotNull("OSGi Bundle tests must be run inside an OSGi framework", bundle);

        tracker = new ServiceTracker<>(bundle.getBundleContext(), EventMonitor.class, null);
        tracker.open();

        monitor = tracker.waitForService(500);

        assertNotNull(monitor);
    }

    @After
    public void tearDownMonitor() throws BundleException {
    	Bundle eventBus = tracker.getServiceReference().getBundle();

    	// Needed to clear history from previous tests
    	eventBus.stop();
    	eventBus.start();
    }

	/**
	 * Tests that events are delivered to the monitor
	 *
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 */
    @Test
    public void testEventMonitor1() throws InterruptedException, InvocationTargetException {

    	Promise<List<MonitorEvent>> eventsPromise = monitor.monitorEvents()
    			.limit(2)
    			.collect(Collectors.toList());

    	TestEvent event = new TestEvent();
    	event.message = "boo";

    	Dictionary<String, Object> props = new Hashtable<>();
    	props.put(SmartBehaviourDefinition.PREFIX_ + "consumed", TestEvent.class.getName());

    	regs.add(bundle.getBundleContext().registerService(SmartBehaviour.class, behaviourA, props));

    	impl.deliver(event);

    	event = new TestEvent();
    	event.message = "bam";

    	impl.deliver(event);

    	assertTrue(semA.tryAcquire(2, 2, TimeUnit.SECONDS));

    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("boo")));
    	Mockito.verify(behaviourA).notify(Mockito.argThat(isTestEventWithMessage("bam")));

    	List<MonitorEvent> events = eventsPromise.timeout(100).getValue();

    	assertEquals(2, events.size());

    	assertEquals(TestEvent.class.getName(), events.get(0).eventType);
    	assertEquals(TestEvent.class.getName(), events.get(1).eventType);

    	assertEquals("boo", events.get(0).eventData.get("message"));
    	assertEquals("bam", events.get(1).eventData.get("message"));


    }


    /**
     * Tests that events are delivered to the monitor even when nobody is listening
     *
     * @throws InterruptedException
     * @throws InvocationTargetException
     */
    @Test
    public void testEventMonitor2() throws InterruptedException, InvocationTargetException {

    	Promise<List<MonitorEvent>> eventsPromise = monitor.monitorEvents()
    			.limit(2)
    			.collect(Collectors.toList());

    	TestEvent event = new TestEvent();
    	event.message = "boo";

    	impl.deliver(event);

    	event = new TestEvent();
    	event.message = "bam";

    	impl.deliver(event);

    	List<MonitorEvent> events = eventsPromise.timeout(2000).getValue();

    	assertEquals(2, events.size());

    	assertEquals(TestEvent.class.getName(), events.get(0).eventType);
    	assertEquals(TestEvent.class.getName(), events.get(1).eventType);

    	assertEquals("boo", events.get(0).eventData.get("message"));
    	assertEquals("bam", events.get(1).eventData.get("message"));


    }

    /**
     * Test that events can be filtered by LDAP
     */
    @Test
    public void testEventMonitor3() throws Exception {

        FilterDTO filter = new FilterDTO();
        filter.type = FilterDTO.FilterType.LDAP;
        filter.expression = "(!(message=bam))";

        Promise<List<MonitorEvent>> eventsPromise = monitor.monitorEvents(filter)
                .limit(2)
                .collect(Collectors.toList());

        TestEvent event = new TestEvent();
        event.message = "boo";

        impl.deliver(event);

        event = new TestEvent();
        event.message = "bam";

        impl.deliver(event);

        event = new TestEvent();
        event.message = "baz";

        impl.deliver(event);

        List<MonitorEvent> events = eventsPromise.timeout(2000).getValue();

        assertEquals(2, events.size());

        assertEquals(TestEvent.class.getName(), events.get(0).eventType);
        assertEquals(TestEvent.class.getName(), events.get(1).eventType);

        assertEquals("boo", events.get(0).eventData.get("message"));
        assertEquals("baz", events.get(1).eventData.get("message"));
    }

    /**
     * Test that events can be filtered by regex
     */
    @Test
    public void testEventMonitor4() throws InterruptedException, InvocationTargetException {
        FilterDTO filter = new FilterDTO();
        filter.type = FilterDTO.FilterType.REGEX;
        filter.expression = "message:ba";

        Promise<List<MonitorEvent>> eventsPromise = monitor.monitorEvents(filter)
                .limit(2)
                .collect(Collectors.toList());

        TestEvent event = new TestEvent();
        event.message = "boo";

        impl.deliver(event);

        event = new TestEvent();
        event.message = "bam";

        impl.deliver(event);

        event = new TestEvent();
        event.message = "baz";

        impl.deliver(event);

        List<MonitorEvent> events = eventsPromise.timeout(2000).getValue();

        assertEquals(2, events.size());

        assertEquals(TestEvent.class.getName(), events.get(0).eventType);
        assertEquals(TestEvent.class.getName(), events.get(1).eventType);

        assertEquals("bam", events.get(0).eventData.get("message"));
        assertEquals("baz", events.get(1).eventData.get("message"));
    }

    /**
     * Test that sub-events can be filtered by LDAP
     */
    @Test
    public void testEventMonitor5() throws Exception {
    	
    	FilterDTO filter = new FilterDTO();
    	filter.type = FilterDTO.FilterType.LDAP;
    	filter.expression = "(!(subEvent.message=bam))";
    	
    	Promise<List<MonitorEvent>> eventsPromise = monitor.monitorEvents(filter)
    			.limit(2)
    			.collect(Collectors.toList());
    	
    	TestEvent event = new TestEvent();
    	event.message = "boo";
    	
    	impl.deliver(TestEvent2.create(event));
    	
    	event = new TestEvent();
    	event.message = "bam";
    	
    	impl.deliver(TestEvent2.create(event));
    	
    	event = new TestEvent();
    	event.message = "baz";
    	
    	impl.deliver(TestEvent2.create(event));
    	
    	List<MonitorEvent> events = eventsPromise.timeout(2000).getValue();
    	
    	assertEquals(2, events.size());
    	
    	assertEquals(TestEvent2.class.getName(), events.get(0).eventType);
    	assertEquals(TestEvent2.class.getName(), events.get(1).eventType);
    	
    	assertEquals("boo", ((Map<?,?>)events.get(0).eventData.get("subEvent")).get("message"));
    	assertEquals("baz", ((Map<?,?>)events.get(1).eventData.get("subEvent")).get("message"));
    }
    
    /**
     * Test that events can be filtered by regex
     */
    @Test
    public void testEventMonitor6() throws InterruptedException, InvocationTargetException {
    	FilterDTO filter = new FilterDTO();
    	filter.type = FilterDTO.FilterType.REGEX;
    	filter.expression = "subEvent.message:ba";
    	
    	Promise<List<MonitorEvent>> eventsPromise = monitor.monitorEvents(filter)
    			.limit(2)
    			.collect(Collectors.toList());
    	
    	TestEvent event = new TestEvent();
    	event.message = "boo";
    	
    	impl.deliver(TestEvent2.create(event));
    	
    	event = new TestEvent();
    	event.message = "bam";
    	
    	impl.deliver(TestEvent2.create(event));
    	
    	event = new TestEvent();
    	event.message = "baz";
    	
    	impl.deliver(TestEvent2.create(event));
    	
    	List<MonitorEvent> events = eventsPromise.timeout(2000).getValue();
    	
    	assertEquals(2, events.size());
    	
    	assertEquals(TestEvent2.class.getName(), events.get(0).eventType);
    	assertEquals(TestEvent2.class.getName(), events.get(1).eventType);
    	
    	assertEquals("bam", ((Map<?,?>)events.get(0).eventData.get("subEvent")).get("message"));
    	assertEquals("baz", ((Map<?,?>)events.get(1).eventData.get("subEvent")).get("message"));
    }

	/**
	 * Tests that event history is delivered to the monitor
	 *
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 */
    @Test
    public void testEventMonitorHistory1() throws InterruptedException, InvocationTargetException {

    	TestEvent event = new TestEvent();
    	event.message = "boo";

    	impl.deliver(event);

    	event = new TestEvent();
    	event.message = "bam";

    	impl.deliver(event);

    	Thread.sleep(500);

    	Promise<List<MonitorEvent>> eventsPromise = monitor.monitorEvents()
    			.limit(Duration.ofSeconds(1))
    			.collect(Collectors.toList())
    			.timeout(2000);

    	List<MonitorEvent> events = eventsPromise.getValue();

    	assertTrue(events.isEmpty());

    	eventsPromise = monitor.monitorEvents(5)
    			.limit(Duration.ofSeconds(1))
    			.collect(Collectors.toList())
    			.timeout(2000);

    	events = eventsPromise.getValue();

    	assertEquals(events.toString(), 2, events.size());

    	assertEquals(TestEvent.class.getName(), events.get(0).eventType);
    	assertEquals(TestEvent.class.getName(), events.get(1).eventType);

    	assertEquals("boo", events.get(0).eventData.get("message"));
    	assertEquals("bam", events.get(1).eventData.get("message"));

    	eventsPromise = monitor.monitorEvents(1)
    			.limit(Duration.ofSeconds(1))
    			.collect(Collectors.toList())
    			.timeout(2000);

    	events = eventsPromise.getValue();

    	assertEquals(1, events.size());

    	assertEquals(TestEvent.class.getName(), events.get(0).eventType);

    	assertEquals("bam", events.get(0).eventData.get("message"));


    }

    /**
     * Tests that event history is delivered to the monitor
     *
     * @throws InterruptedException
     * @throws InvocationTargetException
     */
    @Test
    public void testEventMonitorHistory2() throws InterruptedException, InvocationTargetException {

    	Instant beforeFirst = Instant.now().minus(Duration.ofMillis(500));

    	TestEvent event = new TestEvent();
    	event.message = "boo";

    	impl.deliver(event);

    	Instant afterFirst = Instant.now().plus(Duration.ofMillis(500));

    	Thread.sleep(1000);

    	event = new TestEvent();
    	event.message = "bam";

    	impl.deliver(event);

    	Instant afterSecond = Instant.now().plus(Duration.ofMillis(500));

    	Thread.sleep(500);

    	Promise<List<MonitorEvent>> eventsPromise = monitor.monitorEvents()
    			.limit(Duration.ofSeconds(1))
    			.collect(Collectors.toList())
    			.timeout(2000);


    	List<MonitorEvent> events = eventsPromise.getValue();

    	assertTrue(events.isEmpty());

    	eventsPromise = monitor.monitorEvents(beforeFirst)
    			.limit(Duration.ofSeconds(1))
    			.collect(Collectors.toList())
    			.timeout(2000);

    	events = eventsPromise.getValue();

    	assertEquals(2, events.size());

    	assertEquals(TestEvent.class.getName(), events.get(0).eventType);
    	assertEquals(TestEvent.class.getName(), events.get(1).eventType);

    	assertEquals("boo", events.get(0).eventData.get("message"));
    	assertEquals("bam", events.get(1).eventData.get("message"));

    	eventsPromise = monitor.monitorEvents(afterFirst)
    			.limit(Duration.ofSeconds(1))
    			.collect(Collectors.toList())
    			.timeout(2000);

    	events = eventsPromise.getValue();

    	assertEquals(1, events.size());

    	assertEquals(TestEvent.class.getName(), events.get(0).eventType);

    	assertEquals("bam", events.get(0).eventData.get("message"));

    	eventsPromise = monitor.monitorEvents(afterSecond)
    			.limit(Duration.ofSeconds(1))
    			.collect(Collectors.toList())
    			.timeout(2000);

    	events = eventsPromise.getValue();

    	assertTrue(events.isEmpty());
    }

}

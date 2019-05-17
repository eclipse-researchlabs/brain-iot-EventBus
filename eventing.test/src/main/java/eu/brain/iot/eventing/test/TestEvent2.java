/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.test;

import eu.brain.iot.eventing.api.BrainIoTEvent;

public class TestEvent2 extends BrainIoTEvent {
	public TestEvent subEvent;
	public EventType eventType;
	
	public static TestEvent2 create(TestEvent event) {
		TestEvent2 event2 = new TestEvent2();
		event2.subEvent = event;
		event2.eventType = EventType.RED;
		return event2;
	}
	
	
	public static enum EventType {
		RED, GREEN, BLUE;
	}
}
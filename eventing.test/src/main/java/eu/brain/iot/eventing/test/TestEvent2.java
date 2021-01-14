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

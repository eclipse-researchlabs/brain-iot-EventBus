/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package com.paremus.brain.iot.eventing.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Map;

import org.junit.Test;
import org.osgi.util.converter.Converters;

import eu.brain.iot.eventing.api.BrainIoTEvent;

public class EventConverterTest {

	public static class TestEvent extends BrainIoTEvent {
		public String message;
	}

	
	@Test
	public void testSimpleFlattenAndReconstitute() {
		
		TestEvent te = new TestEvent();
		te.message = "FOO";
		te.securityToken = new byte[] {1,2,3};
		te.sourceNode = "FIZZBUZZ";
		Instant now = Instant.now();
		te.timestamp = now;
		
		
		Map<String, Object> map = EventConverter.convert(te);
		
		assertEquals("FOO", map.get("message"));
		assertEquals("FIZZBUZZ", map.get("sourceNode"));
		assertArrayEquals(new byte[] {1,2,3}, (byte[]) map.get("securityToken"));
		assertEquals(now.toString(), map.get("timestamp"));
		
		TestEvent testEvent = Converters.standardConverter().convert(map).to(TestEvent.class);
		
		assertEquals(te.message, testEvent.message);
		assertEquals(te.sourceNode, testEvent.sourceNode);
		assertArrayEquals(te.securityToken, testEvent.securityToken);
		assertEquals(te.timestamp, testEvent.timestamp);
		
	}

}

/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package com.paremus.brain.iot.eventing.impl;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Map;

import org.junit.Test;
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converters;

import eu.brain.iot.eventing.api.BrainIoTEvent;

public class EventConverterTest {

	public static class TestEvent extends BrainIoTEvent {
		public String message;
	}

	
	public static class NestedEventHolder extends BrainIoTEvent {
		public TestEvent event;
	}

	static class DefaultVisibilityNestedEventHolder extends BrainIoTEvent {
		public TestEvent event;
	}
	
	public static class NestedEventHolderNotAProperDTO extends BrainIoTEvent {
		public TestEvent event;
		
		public static NestedEventHolderNotAProperDTO factory(TestEvent event) {
			NestedEventHolderNotAProperDTO holder = new NestedEventHolderNotAProperDTO();
			holder.event = event;
			return holder;
		}
	}
	
	public static class DoublyNestedEventHolderWithIssues extends BrainIoTEvent {
		public NestedEventHolderNotAProperDTO event;
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

	@Test
	public void testNestedFlattenAndReconstitute() {
		
		TestEvent te = new TestEvent();
		te.message = "FOO";
		
		
		NestedEventHolder holder = new  NestedEventHolder();
		holder.event = te;
		holder.securityToken = new byte[] {1,2,3};
		holder.sourceNode = "FIZZBUZZ";
		Instant now = Instant.now();
		holder.timestamp = now;
		
		Map<String, Object> map = EventConverter.convert(holder);
		
		assertEquals("FIZZBUZZ", map.get("sourceNode"));
		assertArrayEquals(new byte[] {1,2,3}, (byte[]) map.get("securityToken"));
		assertEquals(now.toString(), map.get("timestamp"));
		
		
		
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) map.getOrDefault("event", emptyMap());
		assertEquals("FOO", nested.get("message"));

		
		NestedEventHolder testEvent = EventConverter.convert(map, NestedEventHolder.class);
		
		assertEquals(te.message, testEvent.event.message);
		assertEquals(holder.sourceNode, testEvent.sourceNode);
		assertArrayEquals(holder.securityToken, testEvent.securityToken);
		assertEquals(holder.timestamp, testEvent.timestamp);
		
	}
	
	@Test
	public void testDefaultVisibiltyNestedFlattenAndReconstitute() {
		
		TestEvent te = new TestEvent();
		te.message = "FOO";
		
		
		NestedEventHolder holder = new  NestedEventHolder();
		holder.event = te;
		holder.securityToken = new byte[] {1,2,3};
		holder.sourceNode = "FIZZBUZZ";
		Instant now = Instant.now();
		holder.timestamp = now;
		
		Map<String, Object> map = EventConverter.convert(holder);
		
		assertEquals("FIZZBUZZ", map.get("sourceNode"));
		assertArrayEquals(new byte[] {1,2,3}, (byte[]) map.get("securityToken"));
		assertEquals(now.toString(), map.get("timestamp"));
		
		
		
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) map.getOrDefault("event", emptyMap());
		assertEquals("FOO", nested.get("message"));
		
		
		try {
			EventConverter.convert(map, DefaultVisibilityNestedEventHolder.class);
			fail("Should not succeed in creating a Default Visibility type");
		} catch (ConversionException ce) {
			assertEquals(IllegalAccessException.class, ce.getCause().getClass());
		}
	}

	@Test
	public void testNestedFlattenAndReconstituteNotAProperDTO() {
		
		TestEvent te = new TestEvent();
		te.message = "FOO";
		
		
		NestedEventHolderNotAProperDTO holder = new  NestedEventHolderNotAProperDTO();
		holder.event = te;
		holder.securityToken = new byte[] {1,2,3};
		holder.sourceNode = "FIZZBUZZ";
		Instant now = Instant.now();
		holder.timestamp = now;
		
		Map<String, Object> map = EventConverter.convert(holder);
		
		assertEquals("FIZZBUZZ", map.get("sourceNode"));
		assertArrayEquals(new byte[] {1,2,3}, (byte[]) map.get("securityToken"));
		assertEquals(now.toString(), map.get("timestamp"));
		
		
		
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) map.getOrDefault("event", emptyMap());
		assertEquals("FOO", nested.get("message"));
		
		
		NestedEventHolderNotAProperDTO testEvent = EventConverter.convert(map, NestedEventHolderNotAProperDTO.class);
		
		assertEquals(te.message, testEvent.event.message);
		assertEquals(holder.sourceNode, testEvent.sourceNode);
		assertArrayEquals(holder.securityToken, testEvent.securityToken);
		assertEquals(holder.timestamp, testEvent.timestamp);
		
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDoublyNestedFlattenAndReconstitute() {
		
		TestEvent te = new TestEvent();
		te.message = "FOO";
		
		
		NestedEventHolderNotAProperDTO holder = new  NestedEventHolderNotAProperDTO();
		holder.event = te;
		
		DoublyNestedEventHolderWithIssues doubleHolder = new DoublyNestedEventHolderWithIssues();
		doubleHolder.event = holder;
		
		
		doubleHolder.securityToken = new byte[] {1,2,3};
		doubleHolder.sourceNode = "FIZZBUZZ";
		Instant now = Instant.now();
		doubleHolder.timestamp = now;
		
		Map<String, Object> map = EventConverter.convert(doubleHolder);
		
		assertEquals("FIZZBUZZ", map.get("sourceNode"));
		assertArrayEquals(new byte[] {1,2,3}, (byte[]) map.get("securityToken"));
		assertEquals(now.toString(), map.get("timestamp"));
		
		
		
		Map<String, Object> nested = (Map<String, Object>) map.getOrDefault("event", emptyMap());
		assertTrue(nested.containsKey("event"));
		nested = (Map<String, Object>) nested.get("event");
		assertEquals("FOO", nested.get("message"));
		
		
		DoublyNestedEventHolderWithIssues testEvent = EventConverter.convert(map, DoublyNestedEventHolderWithIssues.class);
		
		assertEquals(te.message, testEvent.event.event.message);
		assertEquals(doubleHolder.sourceNode, testEvent.sourceNode);
		assertArrayEquals(doubleHolder.securityToken, testEvent.securityToken);
		assertEquals(doubleHolder.timestamp, testEvent.timestamp);
		
	}

}
